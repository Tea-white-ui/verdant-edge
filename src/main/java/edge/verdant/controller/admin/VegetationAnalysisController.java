package edge.verdant.controller.admin;

import edge.verdant.service.VegetationAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController()
@Tag(name = "植被分析接口")
@RequestMapping("/admin/vegetationAnalysis")
@RequiredArgsConstructor
public class VegetationAnalysisController {
    private final VegetationAnalysisService vegetationAnalysisService;

    /**
     * 根据设备id分析植被种植建议（SSE流式响应）
     */
    // todo 没做好
    @PostMapping(value = "/getByMachineId", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "根据设备id分析植被种植建议（流式响应）")
    public SseEmitter VegetationAnalysis(@RequestParam String machineId) {
        return vegetationAnalysisService.ByMachineIdStream(machineId);
    }

}
