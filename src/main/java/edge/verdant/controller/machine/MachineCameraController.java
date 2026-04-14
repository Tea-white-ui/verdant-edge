package edge.verdant.controller.machine;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("machineMachineCameraController")
@RequestMapping("/machine/machineCamera")
@Slf4j
@Tag(name = "设备摄影接口")
public class MachineCameraController {
}
