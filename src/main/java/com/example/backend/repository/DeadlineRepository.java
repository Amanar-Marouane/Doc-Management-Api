package com.example.backend.repository;

import com.example.backend.entity.Deadline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    @Query("SELECT d FROM Deadline d WHERE " +
            "(:societeId IS NULL OR d.client.id = :societeId) AND " +
            "(:status IS NULL OR d.status = :status)")
    Page<Deadline> findByFilters(@Param("societeId") Long societeId,
                                 @Param("status") Deadline.DeadlineStatus status,
                                 Pageable pageable);

    List<Deadline> findByDueDateBeforeAndStatusNot(LocalDate date, Deadline.DeadlineStatus status);
}
