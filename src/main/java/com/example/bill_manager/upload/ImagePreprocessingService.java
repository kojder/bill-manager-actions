package com.example.bill_manager.upload;

public interface ImagePreprocessingService {

  byte[] preprocess(byte[] fileContent, String mimeType);
}
