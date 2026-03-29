package com.example.backend.contract;

import com.example.backend.dto.BackupTaskResult;

public interface BackupTask {
    String getName();

    BackupTaskResult execute();
}