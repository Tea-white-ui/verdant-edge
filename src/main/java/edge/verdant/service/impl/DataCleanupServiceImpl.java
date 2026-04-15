package edge.verdant.service.impl;

import edge.verdant.mapper.MachineRecordMapper;
import edge.verdant.pojo.entity.MachineRecord;
import edge.verdant.properties.DataRetentionProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edge.verdant.service.DataCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataCleanupServiceImpl implements DataCleanupService {
    
    private final MachineRecordMapper machineRecordMapper;
    private final DataRetentionProperties retentionProperties;

    /**
     * 定期删除过期记录，记录只保存90天
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupMachineRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionProperties.getMachineRecordDays());
        
        LambdaQueryWrapper<MachineRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(MachineRecord::getCreateTime, cutoffTime);
        
        int deletedCount = machineRecordMapper.delete(wrapper);
        log.info("清理设备传感器记录：删除 {} 条 {} 天前的数据", deletedCount, retentionProperties.getMachineRecordDays());
    }
}
