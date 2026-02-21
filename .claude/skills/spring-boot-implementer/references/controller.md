# REST Controller and Exception Handler

## Table of Contents

- [RestController Template](#restcontroller-template)
- [POST Endpoint (Create Resource)](#post-endpoint-create-resource)
- [GET Endpoint (Retrieve Resource)](#get-endpoint-retrieve-resource)
- [GlobalExceptionHandler Extension](#globalexceptionhandler-extension)
- [ErrorCode-to-HttpStatus Mappings](#errorcode-to-httpstatus-mappings)

---

## RestController Template

Controllers are routing-only — delegate to services immediately, no business logic.

```java
package com.example.bill_manager.TODO_MODULE;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/TODO_RESOURCE")
public class TODO_FeatureController {

  // TODO: Inject service interfaces (not implementations)
  private final TODO_FeatureService featureService;

  public TODO_FeatureController(final TODO_FeatureService featureService) {
    this.featureService = featureService;
  }

  // TODO: Add endpoint methods (see POST/GET templates below)
}
```

---

## POST Endpoint (Create Resource)

```java
@PostMapping
public ResponseEntity<TODO_Response> create(final TODO_Request request) {
  // Delegate to service — no logic here
  final TODO_Result result = featureService.process(request);
  final UUID id = UUID.randomUUID();
  final TODO_Response response = new TODO_Response(id, result, Instant.now());
  return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

For file uploads, use `@RequestParam("file") final MultipartFile file`:

```java
@PostMapping("/upload")
public ResponseEntity<BillAnalysisResponse> uploadBill(
    @RequestParam("file") final MultipartFile file) {
  fileValidationService.validateFile(file);
  final String sanitizedFilename =
      fileValidationService.sanitizeFilename(file.getOriginalFilename());
  final UUID id = UUID.randomUUID();
  final BillAnalysisResponse response =
      new BillAnalysisResponse(id, sanitizedFilename, null, Instant.now());
  billResultStore.save(id, response);
  return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

## GET Endpoint (Retrieve Resource)

```java
@GetMapping("/{id}")
public ResponseEntity<TODO_Response> getById(@PathVariable final UUID id) {
  final TODO_Response result =
      store.findById(id).orElseThrow(() -> new TODO_NotFoundException(id));
  return ResponseEntity.ok(result);
}
```

---

## GlobalExceptionHandler Extension

When adding a new custom exception, register it in
`src/main/java/com/example/bill_manager/exception/GlobalExceptionHandler.java`:

**Step 1:** Add handler method:

```java
@ExceptionHandler(TODO_FeatureException.class)
public ResponseEntity<ErrorResponse> handleTODO_Feature(final TODO_FeatureException ex) {
  final HttpStatus status = mapTODO_FeatureErrorCodeToStatus(ex.getErrorCode());
  final ErrorResponse response =
      new ErrorResponse(ex.getErrorCode().name(), ex.getMessage(), Instant.now());
  return ResponseEntity.status(status).body(response);
}
```

**Step 2:** Add mapping method with exhaustive switch (no `default` case — compiler catches
missing enum values):

```java
private HttpStatus mapTODO_FeatureErrorCodeToStatus(
    final TODO_FeatureException.ErrorCode errorCode) {
  return switch (errorCode) {
    case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
    case PROCESSING_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
    // TODO: Map all ErrorCode values
  };
}
```

**Step 3:** Add import for the new exception class.

**Important:** The generic `Exception` catch-all handler already exists — it returns
`"An unexpected error occurred"` with 500 status. Do not modify it.

---

## ErrorCode-to-HttpStatus Mappings

Common mappings used in this project:

| ErrorCode pattern | HttpStatus | Code |
|------------------|------------|------|
| `*_REQUIRED`, `INVALID_*` | `BAD_REQUEST` | 400 |
| `*_NOT_FOUND` | `NOT_FOUND` | 404 |
| `*_TOO_LARGE`, `PROMPT_TOO_LARGE` | `PAYLOAD_TOO_LARGE` | 413 |
| `UNSUPPORTED_*` | `UNSUPPORTED_MEDIA_TYPE` | 415 |
| `*_READ_FAILED` | `UNPROCESSABLE_ENTITY` | 422 |
| `*_FAILED`, `INVALID_RESPONSE` | `INTERNAL_SERVER_ERROR` | 500 |
| `SERVICE_UNAVAILABLE` | `SERVICE_UNAVAILABLE` | 503 |
