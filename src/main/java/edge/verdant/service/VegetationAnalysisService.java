package edge.verdant.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface VegetationAnalysisService {
    /**
     * 根据设备id分析植被
     */
    String ByMachineId(String machineId);

    /**
     * 根据设备id分析植被（SSE流式响应）
     */
    SseEmitter ByMachineIdStream(String machineId);
}
