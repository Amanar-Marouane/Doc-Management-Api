package com.example.backend.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.stereotype.Service;

import com.example.backend.contract.ActivityLogContract;
import com.example.backend.contract.BackupServiceContract;
import com.example.backend.contract.BackupTask;
import com.example.backend.dto.BackupTaskResult;
import com.example.backend.dto.FileRecoveryResult;
import com.example.backend.tasks.DatabaseBackupTask;
import com.example.backend.tasks.FileStorageBackupTask;
import com.example.backend.util.AppLogger;

@Service
public class BackupService implements BackupServiceContract {

    private final ActivityLogContract activityLogContract;
    private final DatabaseBackupTask databaseBackupTask;
    private final FileStorageBackupTask fileStorageBackupTask;

    public BackupService(
            ActivityLogContract activityLogContract,
            DatabaseBackupTask databaseBackupTask,
            FileStorageBackupTask fileStorageBackupTask) {
        this.activityLogContract = activityLogContract;
        this.databaseBackupTask = databaseBackupTask;
        this.fileStorageBackupTask = fileStorageBackupTask;
    }

    @Override
    public List<BackupTaskResult> runFullBackup() {
        AppLogger.header("Starting Full System Backup");
        activityLogContract.log("BACKUP_START", "SYSTEM", null, "system", "Full backup process initiated");

        try {
            List<BackupTask> tasks = List.of(databaseBackupTask, fileStorageBackupTask);

            List<BackupTaskResult> results = tasks.stream()
                    .map(task -> {
                        AppLogger.info("Executing: {}...", task.getName());
                        return task.execute();
                    })
                    .toList();

            results.forEach(this::logResult);

            boolean allSuccess = results.stream().allMatch(BackupTaskResult::success);
            if (!allSuccess) {
                AppLogger.alert("Backup process completed with partial failures.");
                activityLogContract.log("BACKUP_PARTIAL_FAILURE", "SYSTEM", null, "system",
                        "Full backup completed with partial failures");
            } else {
                AppLogger.success("Backup process completed successfully.");
                activityLogContract.log("BACKUP_SUCCESS", "SYSTEM", null, "system",
                        "Full backup completed successfully");
            }

            AppLogger.footer("Full System Backup Process Ended");
            return results;
        } catch (Exception e) {
            AppLogger.error("Full System Backup encountered a critical error: {}", e.getMessage());
            activityLogContract.log("BACKUP_FAILURE", "SYSTEM", null, "system",
                    "Full backup failed due to an exception: " + e.getMessage());
            throw e; // Rethrow to let the scheduler or caller handle it
        }
    }

    @Override
    public BackupTaskResult runDatabaseBackup() {
        AppLogger.header("Starting Database Backup");
        activityLogContract.log("BACKUP_START", "DATABASE", null, "system", "Database backup started");

        BackupTaskResult result = databaseBackupTask.execute();
        logResult(result);

        if (result.success()) {
            activityLogContract.log("BACKUP_SUCCESS", "DATABASE", null, "system",
                    "Database backup completed: " + result.message());
        } else {
            activityLogContract.log("BACKUP_FAILURE", "DATABASE", null, "system",
                    "Database backup failed: " + result.message());
        }

        AppLogger.footer("Database Backup");
        return result;
    }

    @Override
    public BackupTaskResult runFileBackup() {
        AppLogger.header("Starting File Storage Backup");
        activityLogContract.log("BACKUP_START", "FILES", null, "system", "File storage backup started");

        BackupTaskResult result = fileStorageBackupTask.execute();
        logResult(result);

        if (result.success()) {
            activityLogContract.log("BACKUP_SUCCESS", "FILES", null, "system",
                    "File storage backup completed: " + result.message());
        } else {
            activityLogContract.log("BACKUP_FAILURE", "FILES", null, "system",
                    "File storage backup failed: " + result.message());
        }

        AppLogger.footer("File Storage Backup");
        return result;
    }

    @Override
    public FileRecoveryResult recoverFile(String expectedPath) {
        String sourceDir = fileStorageBackupTask.getSourceDir();
        String backupDir = fileStorageBackupTask.getBackupDir();

        try {
            // Normalize paths to ensure we can clip the source prefix reliably
            Path fullPath = Path.of(expectedPath).toAbsolutePath();
            Path sourcePath = Path.of(sourceDir).toAbsolutePath();
            
            // The entry in the zip is relative to the SOURCE directory
            String entryName = sourcePath.relativize(fullPath).toString();
            
            // Security/sanity check: replace backslashes on windows just in case,
            // as zip entries use forward slashes.
            entryName = entryName.replace("\\", "/");

            Path backupDirPath = Path.of(backupDir);
            if (!Files.exists(backupDirPath)) {
                return new FileRecoveryResult(false, expectedPath, null, "Backup directory not found: " + backupDir);
            }

            // Collect all matching zip archives sorted newest first (timestamp in filename)
            List<Path> zips;
            try (Stream<Path> stream = Files.list(backupDirPath)) {
                zips = stream
                        .filter(p -> p.getFileName().toString().startsWith("files_")
                                && p.toString().endsWith(".zip"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString(), Comparator.reverseOrder()))
                        .toList();
            }

            if (zips.isEmpty()) {
                return new FileRecoveryResult(false, expectedPath, null, "No file backups found in: " + backupDir);
            }

            // Search each archive from newest to oldest
            for (Path zipPath : zips) {
                try (ZipFile zf = new ZipFile(zipPath.toFile())) {
                    ZipEntry entry = zf.getEntry(entryName);
                    if (entry == null) {
                        continue;  // not in this archive, try older one
                    }

                    Path destination = Path.of(expectedPath);
                    Files.createDirectories(destination.getParent());

                    try (InputStream in = zf.getInputStream(entry)) {
                        Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                    }

                    String zipName = zipPath.getFileName().toString();
                    AppLogger.success("[RECOVER] Restored {} from {}", expectedPath, zipName);
                    activityLogContract.log("FILE_RECOVERED", "FILE", null, "system",
                            "Recovered " + expectedPath + " from backup " + zipName);

                    return new FileRecoveryResult(true, expectedPath, zipName,
                            "File restored from backup: " + zipName);
                }
            }

            return new FileRecoveryResult(false, expectedPath, null,
                    "File not found in any of the " + zips.size() + " available backup(s)");

        } catch (Exception e) {
            AppLogger.error("[RECOVER] Failed to recover {}: {}", expectedPath, e.getMessage());
            activityLogContract.log("FILE_RECOVERY_FAILED", "FILE", null, "system",
                    "Recovery failed for " + expectedPath + ": " + e.getMessage());
            return new FileRecoveryResult(false, expectedPath, null, "Recovery error: " + e.getMessage());
        }
    }

    private void logResult(BackupTaskResult result) {
        String status = result.success() ? "[SUCCESS]" : "[FAILED ]";
        AppLogger.info("{} {}: {} ({} ms)",
                status, result.taskName(), result.message(), result.durationMs());
    }
}
