package com.example.backend.service;

import com.example.backend.contract.ActivityLogContract;
import com.example.backend.entity.ActivityLog;
import com.example.backend.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService implements ActivityLogContract {

    private final ActivityLogRepository repository;

    public ActivityLogService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(String action, String entityType, Long entityId, String username, String description) {
        ActivityLog log = new ActivityLog(
                action,
                entityType,
                entityId,
                username,
                description);

        repository.save(log);
    }
}