package com.example.backend.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.contract.FileStorageService;
import com.example.backend.exception.BusinessException;
import com.example.backend.util.FileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String ROOT_DIR = "uploads/documents/";

    @Override
    public String save(MultipartFile file, Long clientId, Integer year) {
        try {
            // uploads/documents/client/{id}/year/{year}/
            Path uploadPath = Paths.get(
                    ROOT_DIR,
                    "client",
                    String.valueOf(clientId),
                    "year",
                    String.valueOf(year));

            Files.createDirectories(uploadPath);

            String extension = FileHelper.getFileExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();

        } catch (IOException e) {
            throw new BusinessException("FILE_SAVE_ERROR", e.getMessage());
        }
    }

    @Override
    public byte[] read(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_ERROR", e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            throw new BusinessException("FILE_DELETE_ERROR", e.getMessage());
        }
    }

    
}