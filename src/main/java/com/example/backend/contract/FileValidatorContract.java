package com.example.backend.contract;

import org.springframework.web.multipart.MultipartFile;

public interface FileValidatorContract {
    void validate(MultipartFile file);
}
