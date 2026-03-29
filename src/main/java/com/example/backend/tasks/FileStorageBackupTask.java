package com.example.backend.tasks;

import com.example.backend.contract.BackupTask;
import com.example.backend.dto.BackupTaskResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class FileStorageBackupTask implements BackupTask {

    private final String sourceDir;
    private final String backupDir;

    public FileStorageBackupTask(
            @Value("${app.backup.files.source:uploads/documents}") String sourceDir,
            @Value("${app.backup.files.dir:backups/files}") String backupDir) {
        this.sourceDir = sourceDir;
        this.backupDir = backupDir;
    }

    public String getSourceDir() { return sourceDir; }
    public String getBackupDir()  { return backupDir; }

    @Override
    public String getName() {
        return "File Storage Backup";
    }

    @Override
    public BackupTaskResult execute() {
        long start = System.currentTimeMillis();
        try {
            Path source = Path.of(sourceDir);

            if (!Files.exists(source)) {
                return new BackupTaskResult(
                        getName(),
                        false,
                        "Source directory does not exist: " + source.toAbsolutePath(),
                        System.currentTimeMillis() - start);
            }

            Files.createDirectories(Path.of(backupDir));

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path zipFile = Path.of(backupDir, "files_" + timestamp + ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
                Files.walk(source)
                        .filter(p -> !Files.isDirectory(p))
                        .forEach(p -> {
                            String entryName = source.relativize(p).toString();
                            ZipEntry entry = new ZipEntry(entryName);
                            try {
                                zos.putNextEntry(entry);
                                Files.copy(p, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }

            long sizeKb = Files.size(zipFile) / 1024;
            return new BackupTaskResult(
                    getName(),
                    true,
                    "Files archived: " + zipFile.toAbsolutePath() + " (" + sizeKb + " KB)",
                    System.currentTimeMillis() - start);

        } catch (Exception e) {
            return new BackupTaskResult(
                    getName(),
                    false,
                    "File backup failed: " + e.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }
}
