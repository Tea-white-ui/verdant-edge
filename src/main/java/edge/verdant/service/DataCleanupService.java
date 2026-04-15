package edge.verdant.service;

public interface DataCleanupService {
    /**
     * 定期删除过期记录，记录只保存90天
     */
     void cleanupMachineRecords();
}
