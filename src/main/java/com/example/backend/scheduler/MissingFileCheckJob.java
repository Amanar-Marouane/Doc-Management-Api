package com.example.backend.scheduler;

import com.example.backend.entity.Document;
import com.example.backend.entity.MissingFileWarning;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.MissingFileWarningRepository;
import com.example.backend.util.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MissingFileCheckJob {

    private final DocumentRepository documentRepository;
    private final MissingFileWarningRepository warningRepository;

    /**
     * Runs every day at midnight (00:00).
     * Clears stale warnings, then re-checks every document still in the DB.
     * Any document whose file is absent on disk gets a fresh warning record.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkMissingFiles() {
        AppLogger.info("[MissingFileCheckJob] Starting daily missing-file check...");

        // 1. Wipe previous run's records (fresh slate each day)
        warningRepository.deleteAllInBatch();

        // 2. Every record in the DB (any status) should have its file on disk
        // until explicitly purged (purge removes the DB record too)
        List<Document> allDocs = documentRepository.findAll();

        List<MissingFileWarning> warnings = allDocs.stream()
                .filter(doc -> !Files.exists(Paths.get(doc.getCheminFichier())))
                .map(doc -> MissingFileWarning.builder()
                        .document(doc)
                        .expectedPath(doc.getCheminFichier())
                        .build())
                .collect(Collectors.toList());

        if (warnings.isEmpty()) {
            AppLogger.info("[MissingFileCheckJob] All files present — no warnings generated.");
        } else {
            warningRepository.saveAll(warnings);
            AppLogger.info("[MissingFileCheckJob] " + warnings.size() + " missing file(s) detected and recorded.");
        }
    }
}
