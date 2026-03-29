package com.example.backend.contract;

public interface ActivityLogContract {
    void log(String action, String entityType, Long entityId, String username, String description);
}
