package com.example.bill_manager.upload;

import com.example.bill_manager.config.UploadProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationServiceImpl implements FileValidationService {

  private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = {
      (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
  private static final int MAX_MAGIC_BYTES_LENGTH = 8;
  private static final int MAX_FILENAME_LENGTH = 255;
  private static final String DEFAULT_FILENAME = "unnamed_file";

  private final UploadProperties uploadProperties;

  public FileValidationServiceImpl(UploadProperties uploadProperties) {
    this.uploadProperties = uploadProperties;
  }

  @Override
  public void validateFile(MultipartFile file) {
    validateFilePresence(file);
    validateFileSize(file);
    validateMimeType(file);
  }

  @Override
  public String sanitizeFilename(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return DEFAULT_FILENAME;
    }

    String sanitized = originalFilename;
    sanitized = sanitized.replace("\\", "_").replace("/", "_");

    while (sanitized.contains("..")) {
      sanitized = sanitized.replace("..", "");
    }

    sanitized = sanitized.codePoints()
        .filter(cp -> cp >= 32)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

    sanitized = sanitized.strip();
    while (sanitized.startsWith(".")) {
      sanitized = sanitized.substring(1);
    }

    if (sanitized.length() > MAX_FILENAME_LENGTH) {
      sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
    }

    return sanitized.isBlank() ? DEFAULT_FILENAME : sanitized;
  }

  private void validateFilePresence(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new FileValidationException(
          FileValidationException.ErrorCode.FILE_REQUIRED,
          "File is required and must not be empty");
    }
  }

  private void validateFileSize(MultipartFile file) {
    if (!uploadProperties.isFileSizeValid(file.getSize())) {
      throw new FileValidationException(
          FileValidationException.ErrorCode.FILE_TOO_LARGE,
          "File size exceeds maximum allowed size of "
              + uploadProperties.maxFileSizeBytes() + " bytes");
    }
  }

  private void validateMimeType(MultipartFile file) {
    String detectedMimeType = detectMimeTypeFromContent(file);
    if (detectedMimeType == null
        || !uploadProperties.isMimeTypeAllowed(detectedMimeType)) {
      throw new FileValidationException(
          FileValidationException.ErrorCode.UNSUPPORTED_MEDIA_TYPE,
          "File type not supported. Allowed: "
              + String.join(", ", uploadProperties.allowedMimeTypes()));
    }
  }

  private String detectMimeTypeFromContent(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      byte[] header = new byte[MAX_MAGIC_BYTES_LENGTH];
      int bytesRead = inputStream.read(header);
      if (bytesRead < JPEG_MAGIC.length) {
        return null;
      }

      if (startsWith(header, bytesRead, JPEG_MAGIC)) {
        return "image/jpeg";
      }
      if (startsWith(header, bytesRead, PNG_MAGIC)) {
        return "image/png";
      }
      if (startsWith(header, bytesRead, PDF_MAGIC)) {
        return "application/pdf";
      }
      return null;
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file content for MIME detection", e);
    }
  }

  private boolean startsWith(byte[] data, int dataLength, byte[] prefix) {
    if (dataLength < prefix.length) {
      return false;
    }
    return Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
  }
}
