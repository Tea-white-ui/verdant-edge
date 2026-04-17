package edge.verdant.controller.admin;

import edge.verdant.service.VegetationAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 植被分析控制器
 * 提供基于AI的植被种植建议分析服务，支持SSE流式响应
 */
@Slf4j
@RestController()
@Tag(name = "植被分析接口")
@RequestMapping("/admin/vegetationAnalysis")
@RequiredArgsConstructor
public class VegetationAnalysisController {

    private final VegetationAnalysisService vegetationAnalysisService;

    /**
     * 根据设备id分析植被种植建议（SSE流式响应）
     *
     * @param machineId 设备ID（必需）
     * @return SSE流式响应，包含进度更新和AI分析结果
     */
    @GetMapping(value = "/{machineId}/analysis", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "根据设备id分析植被种植建议（流式响应）",
            description = "通过SSE流式返回植被分析进度和AI生成的种植建议。事件类型包括：progress(进度)、chunk(内容片段)、complete(完成)、error(错误)"
    )
    public SseEmitter getVegetationAnalysisByMachineId(
            @Parameter(description = "设备ID", required = true, example = "1")
            @PathVariable String machineId) {
        log.info("收到植被分析请求: machineId={}", machineId);
        return vegetationAnalysisService.ByMachineIdStream(machineId);
    }

}
