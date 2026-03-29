package com.example.backend.repository;

import com.example.backend.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByUsername(String username);

    List<ActivityLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
}