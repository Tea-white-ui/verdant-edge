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
     * 构建植被分析提示词
     * <p>
     * 根据传感器数据生成结构化的AI分析提示词，包含当前环境参数和输出格式要求。
     * 提示词采用纯文本格式，避免使用Markdown，便于客户端直接展示。
     * </p>
     *
     * @param data 植被传感器数据对象，包含植物名称、温度、湿度、光照等监测数据
     * @return 格式化后的提示词字符串，可直接用于AI模型调用
     */
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
                // 处理植物名称：为空时显示"未知植物"
                data.getPlantName() != null ? data.getPlantName() : "未知植物",
                // 处理空气温度：为空时默认为0.0
                data.getAirTemp() != null ? data.getAirTemp().doubleValue() : 0.0,
                // 处理空气湿度：为空时默认为0.0
                data.getAirHumidity() != null ? data.getAirHumidity().doubleValue() : 0.0,
                // 处理土壤湿度：为空时默认为0.0
                data.getSoilMoisture() != null ? data.getSoilMoisture().doubleValue() : 0.0,
                // 处理光照强度：为空时默认为0.0
                data.getLightIntensity() != null ? data.getLightIntensity().doubleValue() : 0.0
        );
    }


    /**
     * 根据设备ID分析植被状况（SSE流式响应）
     * <p>
     * 该方法通过SSE（Server-Sent Events）实现流式响应，使客户端能够实时接收分析进度和结果。
     * 整个分析过程在独立线程中异步执行，主要包括以下步骤：
     * 1. 创建SSE发射器并注册生命周期回调（完成、超时、错误）
     * 2. 在线程池中异步执行分析任务
     * 3. 逐步推送分析进度事件（查询设备 → 获取数据 → 调用AI → 返回结果）
     * 4. 流式转发AI模型的输出内容至客户端
     * </p>
     *
     * @param machineId 设备ID，用于查询对应的设备和传感器数据
     * @return SseEmitter SSE发射器实例，客户端可通过此对象接收流式事件
     */
    @Override
    public SseEmitter ByMachineIdStream(String machineId) {
        // 创建SSE发射器，设置超时时间为60秒
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 使用AtomicBoolean标记SSE连接是否已完成，防止重复调用complete或send导致异常
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        // 注册SSE生命周期回调 -----------------------------------------------------------

        // 完成回调：当SSE连接正常结束时触发，将完成标志设为true
        emitter.onCompletion(() -> {
            log.debug("SSE连接已完成: machineId={}", machineId);
            isCompleted.set(true);
        });

        // 超时回调：当SSE连接超过设定时间无活动时触发，发送超时错误信息
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: machineId={}", machineId);
            safeSendError(emitter, "⚠️ 分析请求超时，请稍后重试", isCompleted);
        });

        // 错误回调：当SSE连接发生异常时触发，标记连接已完成以阻止后续发送
        emitter.onError((e) -> {
            log.error("SSE连接发生错误: machineId={}", machineId, e);
            isCompleted.set(true);
        });

        // 在线程池中异步执行分析任务，避免阻塞请求线程 -------------------------------------
        executorService.execute(() -> {
            try {
                // 步骤1：验证设备是否存在
                sendSseEvent(emitter, EVENT_PROGRESS, "正在查询设备信息...");
                Long id = Long.parseLong(machineId);

                Machine machine = machineMapper.selectById(id);
                if (machine == null) {
                    // 设备不存在，发送错误事件并终止
                    safeSendError(emitter, "⚠️ 设备不存在", isCompleted);
                    return;
                }
                sendSseEvent(emitter, EVENT_PROGRESS, "设备信息查询成功：" + machine.getName());

                // 步骤2：查询该设备最新的传感器记录
                sendSseEvent(emitter, EVENT_PROGRESS, "正在获取传感器数据...");
                LambdaQueryWrapper<MachineRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MachineRecord::getMachineId, id)
                       .orderByDesc(MachineRecord::getCreateTime)  // 按创建时间降序排列
                       .last("LIMIT 1");                           // 只取最新一条记录
                MachineRecord latestRecord = machineRecordMapper.selectOne(wrapper);

                if (latestRecord == null) {
                    // 没有传感器数据，发送错误事件并终止
                    safeSendError(emitter, "⚠️ 暂无传感器数据", isCompleted);
                    return;
                }
                sendSseEvent(emitter, EVENT_PROGRESS, "传感器数据获取成功");

                // 步骤3：组装植被传感器数据对象，将设备信息与传感器记录合并
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

    // 内容缓冲区大小阈值（字符数），超过此值时发送累积内容
    private static final int CONTENT_BUFFER_THRESHOLD = 50;

    /**
     * 流式分析植被数据
     */
    private String analyzeVegetationStream(VegetationSensorData sensorData, SseEmitter emitter, AtomicBoolean isCompleted) {
        if (isCompleted.get()) {
            return "";
        }
        // 定义提示词
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
        // 内容缓冲区，用于累积内容片段，减少发送次数
        StringBuilder contentBuffer = new StringBuilder();

        // 标记是否已接收到有效数据，用于判断API是否正常返回
        AtomicBoolean hasReceivedData = new AtomicBoolean(false);

        try {
            // 发起流式HTTP POST请求 -------------------------------------------------------
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
                                        contentBuffer.append(content);
                                        hasReceivedData.set(true);

                                        // 当缓冲区内容超过阈值时，发送累积内容
                                        if (contentBuffer.length() >= CONTENT_BUFFER_THRESHOLD) {
                                            try {
                                                sendSseEvent(emitter, EVENT_CHUNK, contentBuffer.toString());
                                                contentBuffer.setLength(0); // 清空缓冲区
                                            } catch (IOException e) {
                                                log.warn("发送SSE chunk失败，客户端可能已断开", e);
                                                isCompleted.set(true);
                                            }
                                        }
                                    }
                                } else if (firstChoice.containsKey("message")) {
                                    // 有些API可能在message字段中返回内容
                                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                                    if (message != null && message.containsKey("content")) {
                                        String content = (String) message.get("content");
                                        if (content != null && !content.isEmpty()) {
                                            fullContent.append(content);
                                            contentBuffer.append(content);
                                            hasReceivedData.set(true);

                                            // 当缓冲区内容超过阈值时，发送累积内容
                                            if (contentBuffer.length() >= CONTENT_BUFFER_THRESHOLD) {
                                                try {
                                                    sendSseEvent(emitter, EVENT_CHUNK, contentBuffer.toString());
                                                    contentBuffer.setLength(0); // 清空缓冲区
                                                } catch (IOException e) {
                                                    log.warn("发送SSE chunk失败", e);
                                                    isCompleted.set(true);
                                                }
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

            // 发送缓冲区中剩余的内容
            if (contentBuffer.length() > 0 && !isCompleted.get()) {
                try {
                    sendSseEvent(emitter, EVENT_CHUNK, contentBuffer.toString());
                } catch (IOException e) {
                    log.warn("发送剩余SSE chunk失败", e);
                }
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
