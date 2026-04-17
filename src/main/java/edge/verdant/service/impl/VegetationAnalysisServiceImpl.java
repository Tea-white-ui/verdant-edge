package edge.verdant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import edge.verdant.mapper.MachineMapper;
import edge.verdant.mapper.MachineRecordMapper;
import edge.verdant.pojo.data.VegetationSensorData;
import edge.verdant.pojo.entity.Machine;
import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.properties.DashScopeProperties;
import edge.verdant.service.VegetationAnalysisService;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class VegetationAnalysisServiceImpl implements VegetationAnalysisService {
    private final WebClient dashScopeWebClient;
    private final DashScopeProperties properties;
    private final MachineMapper machineMapper;
    private final MachineRecordMapper machineRecordMapper;
    private final ObjectMapper objectMapper;

    // 有界线程池：核心线程10，最大线程50，队列容量100
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // SSE事件名称常量
    private static final String EVENT_PROGRESS = "progress";
    private static final String EVENT_CHUNK = "chunk";
    private static final String EVENT_COMPLETE = "complete";
    private static final String EVENT_ERROR = "error";

    // 超时时间常量（毫秒）
    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final long SHUTDOWN_TIMEOUT_SEC = 30;



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
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        // 注册完成回调，防止重复完成
        emitter.onCompletion(() -> {
            log.debug("SSE连接已完成: machineId={}", machineId);
            isCompleted.set(true);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: machineId={}", machineId);
            safeSendError(emitter, "⚠️ 分析请求超时，请稍后重试", isCompleted);
        });

        emitter.onError((e) -> {
            log.error("SSE连接发生错误: machineId={}", machineId, e);
            isCompleted.set(true);
        });

        executorService.execute(() -> {
            try {
                // 步骤1：验证设备
                sendSseEvent(emitter, EVENT_PROGRESS, "正在查询设备信息...");
                Long id = Long.parseLong(machineId);

                Machine machine = machineMapper.selectById(id);
                if (machine == null) {
                    safeSendError(emitter, "⚠️ 设备不存在", isCompleted);
                    return;
                }
                sendSseEvent(emitter, EVENT_PROGRESS, "设备信息查询成功：" + machine.getName());

                // 步骤2：查询传感器数据
                sendSseEvent(emitter, EVENT_PROGRESS, "正在获取传感器数据...");
                LambdaQueryWrapper<MachineRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MachineRecord::getMachineId, id)
                       .orderByDesc(MachineRecord::getCreateTime)
                       .last("LIMIT 1");
                MachineRecord latestRecord = machineRecordMapper.selectOne(wrapper);

                if (latestRecord == null) {
                    safeSendError(emitter, "⚠️ 暂无传感器数据", isCompleted);
                    return;
                }
                sendSseEvent(emitter, EVENT_PROGRESS, "传感器数据获取成功");

                // 步骤3：准备分析数据
                VegetationSensorData sensorData = new VegetationSensorData();
                sensorData.setPlantName(machine.getPlantName());
                sensorData.setAirTemp(latestRecord.getAirTemp());
                sensorData.setAirHumidity(latestRecord.getAirHumidity());
                sensorData.setSoilMoisture(latestRecord.getSoilMoisture());
                sensorData.setLightIntensity(latestRecord.getLightIntensity());

                sendSseEvent(emitter, EVENT_PROGRESS, "正在调用AI分析服务...");

                // 步骤4：调用AI分析（流式）
                String result = analyzeVegetationStream(sensorData, emitter, isCompleted);

                // 步骤5：发送最终结果
                if (!isCompleted.get()) {
                    sendSseEvent(emitter, EVENT_COMPLETE, result);
                    safeComplete(emitter, isCompleted);
                }

            } catch (Exception e) {
                log.error("流式分析植被数据失败: machineId={}", machineId, e);
                safeSendError(emitter, "⚠️ 分析服务异常：" + e.getMessage(), isCompleted);
            }
        });

        return emitter;
    }

    /**
     * 安全地完成SSE发射器
     */
    private void safeComplete(SseEmitter emitter, AtomicBoolean isCompleted) {
        if (isCompleted.compareAndSet(false, true)) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("完成SSE发射器时发生异常（可能已自动完成）", e);
            }
        }
    }

    /**
     * 安全地发送错误事件并完成
     */
    private void safeSendError(SseEmitter emitter, String errorMessage, AtomicBoolean isCompleted) {
        if (isCompleted.get()) {
            return;
        }
        try {
            sendSseEvent(emitter, EVENT_ERROR, errorMessage);
            safeComplete(emitter, isCompleted);
        } catch (Exception e) {
            log.error("发送SSE错误事件失败", e);
            safeComplete(emitter, isCompleted);
        }
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
    private String analyzeVegetationStream(VegetationSensorData sensorData, SseEmitter emitter, AtomicBoolean isCompleted) {
        if (isCompleted.get()) {
            return "";
        }

        String prompt = buildAnalysisPrompt(sensorData);
        log.info("开始流式AI分析，模型: {}, 提示词长度: {}", properties.getModel(), prompt.length());

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
        AtomicBoolean hasReceivedData = new AtomicBoolean(false);

        try {
            dashScopeWebClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.<RuntimeException>error(new RuntimeException("DashScope调用失败: " + errorBody))))
                    .bodyToFlux(String.class)
                    .doOnNext(line -> {
                        // 检查客户端是否已断开
                        if (isCompleted.get()) {
                            return;
                        }

                        // 跳过空行
                        if (line == null || line.trim().isEmpty()) {
                            return;
                        }

                        // DashScope 可能返回直接 JSON 或 SSE 格式
                        String data;
                        if (line.startsWith("data:")) {
                            data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) {
                                return;
                            }
                        } else if (line.trim().startsWith("{")) {
                            data = line.trim();
                        } else {
                            return;
                        }

                        try {
                            Map<String, Object> chunk = parseStreamChunk(data);
                            if (chunk == null || chunk.isEmpty()) {
                                log.warn("解析后的chunk为空，原始数据: {}", data);
                                return;
                            }

                            // 检查是否有错误信息
                            if (chunk.containsKey("error")) {
                                log.error("API返回错误: {}", chunk.get("error"));
                                return;
                            }

                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> firstChoice = choices.get(0);
                                Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");

                                if (delta != null && delta.containsKey("content")) {
                                    String content = (String) delta.get("content");
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        hasReceivedData.set(true);

                                        // 发送流式内容片段
                                        try {
                                            sendSseEvent(emitter, EVENT_CHUNK, content);
                                        } catch (IOException e) {
                                            log.warn("发送SSE chunk失败，客户端可能已断开", e);
                                            isCompleted.set(true);
                                        }
                                    }
                                } else if (firstChoice.containsKey("message")) {
                                    // 有些API可能在message字段中返回内容
                                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                                    if (message != null && message.containsKey("content")) {
                                        String content = (String) message.get("content");
                                        if (content != null && !content.isEmpty()) {
                                            fullContent.append(content);
                                            hasReceivedData.set(true);
                                            try {
                                                sendSseEvent(emitter, EVENT_CHUNK, content);
                                            } catch (IOException e) {
                                                log.warn("发送SSE chunk失败", e);
                                                isCompleted.set(true);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("解析流式响应失败: {}, 错误: {}", data, e.getMessage(), e);
                        }
                    })
                    .doOnError(error -> {
                        log.error("流式响应接收过程中发生错误", error);
                    })
                    .doOnComplete(() -> {
                        log.debug("流式响应接收完成，是否收到有效数据: {}", hasReceivedData.get());
                    })
                    .blockLast(Duration.ofMillis(properties.getTimeout()));

            // 检查是否接收到数据
            if (!hasReceivedData.get()) {
                log.warn("AI分析未返回任何有效数据");
                return "⚠️ 分析服务未返回数据，请稍后重试。";
            }

            String result = fullContent.toString();
            log.info("AI分析完成，返回内容长度: {}", result.length());
            return result;

        } catch (Exception e) {
            log.error("调用通义千问流式分析植被数据失败", e);
            return "⚠️ 分析服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 解析流式响应块
     */
    private Map<String, Object> parseStreamChunk(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("解析JSON失败: {}", json, e);
            return Map.of();
        }
    }

    /**
     * 应用关闭时优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭植被分析服务线程池...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(VegetationAnalysisServiceImpl.SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                log.warn("线程池未能在{}秒内完成，强制关闭", VegetationAnalysisServiceImpl.SHUTDOWN_TIMEOUT_SEC);
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("关闭线程池时被打断", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
