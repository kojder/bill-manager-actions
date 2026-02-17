package com.example.bill_manager.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "upload")
@Validated
public record UploadProperties(
    @NotNull(message = "Max file size must not be null")
    @Min(value = 1, message = "Max file size must be at least 1 byte")
    Long maxFileSizeBytes,

    @NotEmpty(message = "Allowed MIME types must not be empty")
    List<String> allowedMimeTypes
) {
  public static final long MAX_FILE_SIZE_10MB = 10 * 1024 * 1024;

  public boolean isFileSizeValid(long fileSizeBytes) {
    return fileSizeBytes > 0 && fileSizeBytes <= maxFileSizeBytes;
  }

  public boolean isMimeTypeAllowed(String mimeType) {
    return mimeType != null && allowedMimeTypes.contains(mimeType);
  }
}
