package edge.verdant.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminMachineCameraController")
@RequestMapping("/machine/machineCamera")
@Slf4j
@Tag(name = "设备摄影接口")
public class MachineCameraController {
}
