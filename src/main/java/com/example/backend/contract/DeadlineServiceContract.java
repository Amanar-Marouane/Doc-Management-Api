package com.example.backend.contract;

import com.example.backend.dto.DeadlineRequestDTO;
import com.example.backend.dto.DeadlineResponseDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Deadline;

public interface DeadlineServiceContract {

    DeadlineResponseDTO createDeadline(DeadlineRequestDTO request);

    DeadlineResponseDTO getDeadlineById(Long id);

    PageResponse<DeadlineResponseDTO> getDeadlines(Long societeId, Deadline.DeadlineStatus status,
                                                   int page, int size, String sortBy, String sortDir);

    DeadlineResponseDTO updateDeadline(Long id, DeadlineRequestDTO request);

    DeadlineResponseDTO updateDeadlineStatus(Long id, Deadline.DeadlineStatus status);

    void deleteDeadline(Long id);

    void refreshOverdueStatuses();
}
