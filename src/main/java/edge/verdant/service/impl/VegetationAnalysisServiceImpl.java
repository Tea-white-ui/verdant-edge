package edge.verdant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edge.verdant.mapper.MachineMapper;
import edge.verdant.mapper.MachineRecordMapper;
import edge.verdant.pojo.data.VegetationSensorData;
import edge.verdant.pojo.entity.Machine;
import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.properties.DashScopeProperties;
import edge.verdant.service.VegetationAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VegetationAnalysisServiceImpl implements VegetationAnalysisService {
    private final WebClient dashScopeWebClient;
    private final DashScopeProperties properties;
    private final MachineMapper machineMapper;
    private final MachineRecordMapper machineRecordMapper;

    private final ExecutorService executorService = Executors.newCachedThreadPool();



    /**
     * 调用大模型分析植被数据并返回建议
     * @param sensorData 分析数据依靠数据（可以来自 MyBatis-Plus 查询结果）
     * @return 结构化分析结果
     */
    private String analyzeVegetation(VegetationSensorData sensorData) {
        String prompt = buildAnalysisPrompt(sensorData);

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一位资深农业与植物生态专家。请基于提供的监测数据，给出科学、可操作的种植与养护建议。输出请保持结构化，便于程序解析。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 1500,
                "enable_thinking", false
        );

        try {
            Map<String, Object> response = dashScopeWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.<RuntimeException>error(new RuntimeException("DashScope调用失败: " + errorBody))))
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(properties.getTimeout()));

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("调用通义千问分析植被数据失败", e);
            return "⚠️ 分析服务暂时不可用，请稍后重试。";
        }
    }

    private String buildAnalysisPrompt(VegetationSensorData data) {
        return String.format("""
            当前植被监测数据如下：
            - 植物名称：%s
            - 空气温度：%.2f℃ | 空气湿度：%.2f%%
            - 土壤湿度：%.2f%%
            - 光照强度：%.2f lux
            
            请输出以下格式的建议，内容简明概要（保持纯文本，不要使用Markdown）：
            【现状诊断】：
            【紧急处理】：
            【养护建议】：
            【长期规划】：
            """,
                data.getPlantName() != null ? data.getPlantName() : "未知植物",
                data.getAirTemp() != null ? data.getAirTemp().doubleValue() : 0.0,
                data.getAirHumidity() != null ? data.getAirHumidity().doubleValue() : 0.0,
                data.getSoilMoisture() != null ? data.getSoilMoisture().doubleValue() : 0.0,
                data.getLightIntensity() != null ? data.getLightIntensity().doubleValue() : 0.0
        );
    }

    /**
     * 根据设备ID分析植被状况
     * @param machineId 设备ID
     * @return AI分析的植被养护建议
     */
    @Override
    public String ByMachineId(String machineId) {
        Long id = Long.parseLong(machineId);

        Machine machine = machineMapper.selectById(id);
        if (machine == null) {
            return "⚠️ 设备不存在";
        }

        LambdaQueryWrapper<MachineRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineRecord::getMachineId, id)
               .orderByDesc(MachineRecord::getCreateTime)
               .last("LIMIT 1");
        MachineRecord latestRecord = machineRecordMapper.selectOne(wrapper);

        if (latestRecord == null) {
            return "⚠️ 暂无传感器数据";
        }

        VegetationSensorData sensorData = new VegetationSensorData();
        sensorData.setPlantName(machine.getPlantName());
        sensorData.setAirTemp(latestRecord.getAirTemp());
        sensorData.setAirHumidity(latestRecord.getAirHumidity());
        sensorData.setSoilMoisture(latestRecord.getSoilMoisture());
        sensorData.setLightIntensity(latestRecord.getLightIntensity());

        return analyzeVegetation(sensorData);
    }

    /**
     * 根据设备ID分析植被状况（SSE流式响应）
     * @param machineId 设备ID
     * @return SseEmitter 流式响应
     */
    @Override
    public SseEmitter ByMachineIdStream(String machineId) {
        SseEmitter emitter = new SseEmitter(0L); // 0表示不超时

        executorService.execute(() -> {
            try {
                // 步骤1：验证设备
                sendSseEvent(emitter, "progress", "正在查询设备信息...");
                Long id = Long.parseLong(machineId);

                Machine machine = machineMapper.selectById(id);
                if (machine == null) {
                    sendSseEvent(emitter, "error", "⚠️ 设备不存在");
                    emitter.complete();
                    return;
                }
                sendSseEvent(emitter, "progress", "设备信息查询成功：" + machine.getPlantName());

                // 步骤2：查询传感器数据
                sendSseEvent(emitter, "progress", "正在获取传感器数据...");
                LambdaQueryWrapper<MachineRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MachineRecord::getMachineId, id)
                       .orderByDesc(MachineRecord::getCreateTime)
                       .last("LIMIT 1");
                MachineRecord latestRecord = machineRecordMapper.selectOne(wrapper);

                if (latestRecord == null) {
                    sendSseEvent(emitter, "error", "⚠️ 暂无传感器数据");
                    emitter.complete();
                    return;
                }
                sendSseEvent(emitter, "progress", "传感器数据获取成功");

                // 步骤3：准备分析数据
                VegetationSensorData sensorData = new VegetationSensorData();
                sensorData.setPlantName(machine.getPlantName());
                sensorData.setAirTemp(latestRecord.getAirTemp());
                sensorData.setAirHumidity(latestRecord.getAirHumidity());
                sensorData.setSoilMoisture(latestRecord.getSoilMoisture());
                sensorData.setLightIntensity(latestRecord.getLightIntensity());

                sendSseEvent(emitter, "progress", "正在调用AI分析服务...");

                // 步骤4：调用AI分析（流式）
                String result = analyzeVegetationStream(sensorData, emitter);

                // 步骤5：发送最终结果
                sendSseEvent(emitter, "complete", result);
                emitter.complete();

            } catch (Exception e) {
                log.error("流式分析植被数据失败", e);
                try {
                    sendSseEvent(emitter, "error", "⚠️ 分析服务异常：" + e.getMessage());
                } catch (IOException ex) {
                    log.error("发送SSE错误事件失败", ex);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, String event, String data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event)
                .data(data));
    }

    /**
     * 流式分析植被数据
     */
    private String analyzeVegetationStream(VegetationSensorData sensorData, SseEmitter emitter) {
        String prompt = buildAnalysisPrompt(sensorData);

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一位资深农业与植物生态专家。请基于提供的监测数据，给出科学、可操作的种植与养护建议。输出请保持结构化，便于程序解析。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 1500,
                "enable_thinking", false,
                "stream", true
        );

        StringBuilder fullContent = new StringBuilder();

        try {
            dashScopeWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.<RuntimeException>error(new RuntimeException("DashScope调用失败: " + errorBody))))
                    .bodyToFlux(String.class)
                    .doOnNext(line -> {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) {
                                return;
                            }
                            try {
                                Map<String, Object> chunk = parseStreamChunk(data);
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                    if (delta != null && delta.containsKey("content")) {
                                        String content = (String) delta.get("content");
                                        fullContent.append(content);
                                        // 发送流式内容片段
                                        try {
                                            sendSseEvent(emitter, "chunk", content);
                                        } catch (IOException e) {
                                            log.error("发送SSE chunk失败", e);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("解析流式响应失败: {}", data, e);
                            }
                        }
                    })
                    .blockLast(Duration.ofMillis(properties.getTimeout()));

            return fullContent.toString();

        } catch (Exception e) {
            log.error("调用通义千问流式分析植被数据失败", e);
            return "⚠️ 分析服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 解析流式响应块（简单实现，可根据需要完善）
     */
    private Map<String, Object> parseStreamChunk(String json) {
        // 使用简单的手动解析，避免引入额外依赖
        // 实际项目中可以使用 Jackson 或 Gson
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("解析JSON失败: {}", json, e);
            return Map.of();
        }
    }
}