package edge.verdant.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("adminMachineRecordController")
@Tag(name = "设备记录接口")
@RequestMapping("/machine/machineRecord")
public class MachineRecordController {
}
