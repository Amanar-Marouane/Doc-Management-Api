package com.example.backend.service;

import com.example.backend.contract.DeadlineServiceContract;
import com.example.backend.dto.DeadlineRequestDTO;
import com.example.backend.dto.DeadlineResponseDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Deadline;
import com.example.backend.entity.Societe;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.DeadlineRepository;
import com.example.backend.repository.SocieteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeadlineService implements DeadlineServiceContract {

    private final DeadlineRepository deadlineRepository;
    private final SocieteRepository societeRepository;

    @Transactional
    @Override
    public DeadlineResponseDTO createDeadline(DeadlineRequestDTO request) {
        Societe societe = societeRepository.findById(request.getSocieteId())
                .orElseThrow(() -> new ResourceNotFoundException("Société", request.getSocieteId().toString()));

        Deadline.DeadlineStatus resolvedStatus = request.getStatus() != null
                ? request.getStatus()
                : computeStatus(request.getDueDate());

        Deadline deadline = Deadline.builder()
                .client(societe)
                .fiscalYear(request.getFiscalYear())
                .dueDate(request.getDueDate())
                .documentCategory(request.getDocumentCategory())
                .status(resolvedStatus)
                .build();

        return mapToDTO(deadlineRepository.save(deadline));
    }

    @Transactional(readOnly = true)
    @Override
    public DeadlineResponseDTO getDeadlineById(Long id) {
        return mapToDTO(findById(id));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<DeadlineResponseDTO> getDeadlines(Long societeId, Deadline.DeadlineStatus status,
                                                          int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy != null ? sortBy : "dueDate"));

        Page<Deadline> resultPage = deadlineRepository.findByFilters(societeId, status, pageable);

        List<DeadlineResponseDTO> content = resultPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PageResponse.<DeadlineResponseDTO>builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .first(resultPage.isFirst())
                .build();
    }

    @Transactional
    @Override
    public DeadlineResponseDTO updateDeadline(Long id, DeadlineRequestDTO request) {
        Deadline deadline = findById(id);

        Societe societe = societeRepository.findById(request.getSocieteId())
                .orElseThrow(() -> new ResourceNotFoundException("Société", request.getSocieteId().toString()));

        deadline.setClient(societe);
        deadline.setFiscalYear(request.getFiscalYear());
        deadline.setDueDate(request.getDueDate());
        deadline.setDocumentCategory(request.getDocumentCategory());

        if (request.getStatus() != null) {
            deadline.setStatus(request.getStatus());
        } else {
            deadline.setStatus(computeStatus(request.getDueDate()));
        }

        return mapToDTO(deadlineRepository.save(deadline));
    }

    @Transactional
    @Override
    public DeadlineResponseDTO updateDeadlineStatus(Long id, Deadline.DeadlineStatus status) {
        Deadline deadline = findById(id);
        deadline.setStatus(status);
        return mapToDTO(deadlineRepository.save(deadline));
    }

    @Transactional
    @Override
    public void deleteDeadline(Long id) {
        findById(id);
        deadlineRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void refreshOverdueStatuses() {
        List<Deadline> overdueDeadlines = deadlineRepository
                .findByDueDateBeforeAndStatusNot(LocalDate.now(), Deadline.DeadlineStatus.COMPLETED);
        overdueDeadlines.forEach(d -> d.setStatus(Deadline.DeadlineStatus.OVERDUE));
        deadlineRepository.saveAll(overdueDeadlines);
    }

    private Deadline findById(Long id) {
        return deadlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline", id.toString()));
    }

    private Deadline.DeadlineStatus computeStatus(LocalDate dueDate) {
        return LocalDate.now().isAfter(dueDate)
                ? Deadline.DeadlineStatus.OVERDUE
                : Deadline.DeadlineStatus.UPCOMING;
    }

    private DeadlineResponseDTO mapToDTO(Deadline deadline) {
        return DeadlineResponseDTO.builder()
                .id(deadline.getId())
                .societeId(deadline.getClient().getId())
                .societeRaisonSociale(deadline.getClient().getRaisonSociale())
                .fiscalYear(deadline.getFiscalYear())
                .dueDate(deadline.getDueDate())
                .documentCategory(deadline.getDocumentCategory())
                .status(deadline.getStatus())
                .createdAt(deadline.getCreatedAt())
                .updatedAt(deadline.getUpdatedAt())
                .build();
    }
}
