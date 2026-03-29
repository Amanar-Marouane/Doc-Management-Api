package com.example.backend.scheduler;

import com.example.backend.contract.BackupServiceContract;
import com.example.backend.contract.ActivityLogContract;
import com.example.backend.util.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupServiceContract backupService;
    private final ActivityLogContract activityLogContract;

    /**
     * Executes a full system backup every day at midnight (00:00:00).
     */
    @Scheduled(cron = "0 * * * * *")
    public void scheduledFullBackup() {
        AppLogger.info("[BackupScheduler] Triggering scheduled full system backup...");
        
        try {
            backupService.runFullBackup();
            AppLogger.info("[BackupScheduler] Scheduled full system backup process ended.");
        } catch (Exception e) {
            AppLogger.error("[BackupScheduler] CRITICAL: Scheduled backup failed with exception: {}", e.getMessage());
            activityLogContract.log(
                "BACKUP_CRITICAL_FAILURE", 
                "SYSTEM", 
                null, 
                "system-scheduler", 
                "Backup scheduler crashed: " + e.getMessage()
            );
        }
    }
}
