package com.example.backend.service.storage.extras;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.contract.FileValidatorContract;
import com.example.backend.exception.BusinessException;
import com.example.backend.util.FileHelper;

@Service
public class FileValidatorService implements FileValidatorContract {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");

    @Override
    public void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Le fichier est vide");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE",
                    "La taille du fichier ne doit pas dépasser 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("INVALID_FILENAME", "Nom de fichier invalide");
        }

        String extension = FileHelper.getFileExtension(originalFilename.toLowerCase());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("INVALID_FILE_FORMAT",
                    String.format("Format de fichier non autorisé. Formats acceptés: %s",
                            String.join(", ", ALLOWED_EXTENSIONS.stream()
                                    .map(String::toUpperCase)
                                    .collect(Collectors.toList()))));
        }
    }
}
