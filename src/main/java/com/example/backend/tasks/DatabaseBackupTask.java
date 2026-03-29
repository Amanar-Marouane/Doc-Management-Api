package com.example.backend.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.backend.contract.BackupTask;
import com.example.backend.dto.BackupTaskResult;

@Component
public class DatabaseBackupTask implements BackupTask {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String backupDir;

    public DatabaseBackupTask(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${app.backup.database.dir:backups/database}") String backupDir) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.backupDir = backupDir;
    }

    @Override
    public String getName() {
        return "Database Backup";
    }

    @Override
    public BackupTaskResult execute() {
        long start = System.currentTimeMillis();
        try {
            String dbName = extractDatabaseName(jdbcUrl);
            String dbType = detectDbType(jdbcUrl);

            Files.createDirectories(Path.of(backupDir));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path outputFile = Path.of(backupDir, dbName + "_" + timestamp + ".sql");

            List<String> command = buildDumpCommand(dbType, dbName, outputFile);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            if ("postgres".equals(dbType) && password != null && !password.isBlank()) {
                pb.environment().put("PGPASSWORD", password);
            }

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return new BackupTaskResult(
                        getName(),
                        false,
                        "DB backup failed (exit " + exitCode + "): " + output,
                        System.currentTimeMillis() - start);
            }

            return new BackupTaskResult(
                    getName(),
                    true,
                    "DB dump completed successfully: " + outputFile,
                    System.currentTimeMillis() - start);

        } catch (Exception e) {
            return new BackupTaskResult(
                    getName(),
                    false,
                    "DB failed: " + e.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }

    private String detectDbType(String url) {
        if (url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:mariadb:"))
            return "mysql";
        if (url.startsWith("jdbc:postgresql:"))
            return "postgres";
        throw new IllegalArgumentException("Unsupported database type for backup: " + url);
    }

    private String extractDatabaseName(String url) {
        String noParams = url.split("\\?")[0];
        int idx = noParams.lastIndexOf('/');
        if (idx < 0 || idx == noParams.length() - 1) {
            throw new IllegalArgumentException("Cannot extract database name from URL: " + url);
        }
        return noParams.substring(idx + 1);
    }

    private List<String> buildDumpCommand(String dbType, String dbName, Path outputFile) {
        List<String> command = new ArrayList<>();
        if ("mysql".equals(dbType)) {
            command.add("mysqldump");
            command.add("-u");
            command.add(username);
            if (password != null && !password.isBlank()) {
                command.add("-p" + password);
            }
            command.add(dbName);
            command.add("--result-file=" + outputFile.toAbsolutePath());
        } else if ("postgres".equals(dbType)) {
            command.add("pg_dump");
            command.add("-U");
            command.add(username);
            command.add("-d");
            command.add(dbName);
            command.add("-f");
            command.add(outputFile.toAbsolutePath().toString());
        } else {
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        return command;
    }
}