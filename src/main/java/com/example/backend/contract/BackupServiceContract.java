package com.example.backend.contract;

import com.example.backend.dto.BackupTaskResult;
import com.example.backend.dto.FileRecoveryResult;
import java.util.List;

public interface BackupServiceContract {
    List<BackupTaskResult> runFullBackup();

    BackupTaskResult runDatabaseBackup();

    BackupTaskResult runFileBackup();

    FileRecoveryResult recoverFile(String expectedPath);
}
