package com.example.backend.controller;

import com.example.backend.contract.BackupServiceContract;
import com.example.backend.dto.BackupTaskResult;
import com.example.backend.dto.FileRecoveryResult;
import com.example.backend.entity.MissingFileWarning;
import com.example.backend.repository.MissingFileWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupServiceContract backupService;
    private final MissingFileWarningRepository warningRepository;

    /**
     * POST /api/v1/backups/full
     *
     * Triggers a full backup: database dump + file storage zip.
     * Returns a list of results, one per task.
     * ADMIN only.
     */
    @PostMapping("/full")
    public ResponseEntity<List<BackupTaskResult>> fullBackup() {
        List<BackupTaskResult> results = backupService.runFullBackup();
        boolean allSuccess = results.stream().allMatch(BackupTaskResult::success);
        return allSuccess
                ? ResponseEntity.ok(results)
                : ResponseEntity.status(207).body(results); // 207 Multi-Status on partial failure
    }

    /**
     * POST /api/v1/backups/database
     *
     * Triggers a database-only backup (mysqldump / pg_dump).
     * ADMIN only.
     */
    @PostMapping("/database")
    public ResponseEntity<BackupTaskResult> databaseBackup() {
        BackupTaskResult result = backupService.runDatabaseBackup();
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(500).body(result);
    }

    /**
     * POST /api/v1/backups/files
     *
     * Triggers a file-storage-only backup (zip of uploads directory).
     * ADMIN only.
     */
    @PostMapping("/files")
    public ResponseEntity<BackupTaskResult> fileBackup() {
        BackupTaskResult result = backupService.runFileBackup();
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(500).body(result);
    }

    /**
     * POST /api/v1/backups/recover/{warningId}
     *
     * Attempts to restore the missing file referenced by a MissingFileWarning
     * by scanning available file backup archives (newest first).
     * On success the warning record is deleted automatically.
     * ADMIN only.
     */
    @PostMapping("/recover/{warningId}")
    public ResponseEntity<FileRecoveryResult> recoverFile(@PathVariable Long warningId) {
        Optional<MissingFileWarning> opt = warningRepository.findById(warningId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MissingFileWarning warning = opt.get();
        FileRecoveryResult result = backupService.recoverFile(warning.getExpectedPath());

        if (result.success()) {
            warningRepository.delete(warning);
        }

        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(500).body(result);
    }
}
