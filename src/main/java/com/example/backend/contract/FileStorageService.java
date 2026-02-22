package com.example.backend.contract;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String save(MultipartFile file, Long clientId, Integer year);

    byte[] read(String path);

    void delete(String path);
}