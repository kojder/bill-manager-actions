package com.example.bill_manager.upload;

import org.springframework.web.multipart.MultipartFile;

public interface FileValidationService {

  String validateFile(MultipartFile file);

  String sanitizeFilename(String originalFilename);
}
