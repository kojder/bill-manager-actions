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
  public boolean isFileSizeValid(final long fileSizeBytes) {
    return fileSizeBytes > 0 && fileSizeBytes <= maxFileSizeBytes;
  }

  /**
   * Checks if the claimed MIME type is in the allowed list.
   * <p>
   * <strong>WARNING:</strong> This does NOT validate file content. This method only checks
   * if the provided MIME type string matches one of the allowed types configured in
   * {@code application.properties}.
   * <p>
   * Actual file content validation (magic bytes inspection) MUST be performed in the
   * upload service layer to prevent security vulnerabilities. Per CLAUDE.md Upload Module rules:
   * "Validate by file content (magic bytes), NOT by extension or Content-Type header"
   *
   * @param mimeType the MIME type string to check (typically from Content-Type header)
   * @return {@code true} if the MIME type is in the allowed list, {@code false} otherwise
   */
  public boolean isMimeTypeAllowed(final String mimeType) {
    return mimeType != null && allowedMimeTypes.contains(mimeType);
  }
}
