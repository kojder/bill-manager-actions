# REST API Plan: Bill-Manager

## 1. General Information

| Parameter | Value |
|-----------|-------|
| Base URL | `/api` |
| Format | JSON (response), multipart/form-data (upload) |
| Versioning | None (POC) |
| Authentication | None (POC) |

## 2. Endpoints

### POST /api/bills/upload

Upload a bill file and trigger automatic AI analysis.

**Request:**
- Content-Type: `multipart/form-data`
- Parameter: `file` — image file (JPEG, PNG) or PDF

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "originalFileName": "receipt_grocery.jpg",
  "analysis": {
    "merchantName": "Grocery Store",
    "items": [
      {
        "name": "Milk 3.2%",
        "quantity": 2,
        "unitPrice": 3.49,
        "totalPrice": 6.98
      },
      {
        "name": "Wheat Bread",
        "quantity": 1,
        "unitPrice": 4.99,
        "totalPrice": 4.99
      }
    ],
    "totalAmount": 11.97,
    "currency": "PLN",
    "categoryTags": ["grocery", "dairy", "bakery"]
  },
  "analyzedAt": "2026-02-06T14:30:00"
}
```

**Error Codes:**

| Code | Scenario |
|------|----------|
| 201 | Success — file uploaded and analyzed |
| 400 | Missing file in request or empty file |
| 413 | File exceeds size limit (10MB) |
| 415 | Unsupported MIME type (not JPEG/PNG/PDF) |
| 500 | Internal server error |
| 503 | Groq API unavailable (after retries exhausted) |

---

### GET /api/bills/{id}

Retrieve a previous analysis result.

**Request:**
- Path parameter: `id` (UUID)

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "originalFileName": "receipt_grocery.jpg",
  "analysis": {
    "merchantName": "Grocery Store",
    "items": [...],
    "totalAmount": 11.97,
    "currency": "PLN",
    "categoryTags": ["grocery"]
  },
  "analyzedAt": "2026-02-06T14:30:00"
}
```

**Error Codes:**

| Code | Scenario |
|------|----------|
| 200 | Success — result found |
| 400 | Invalid ID format (not UUID) |
| 404 | Analysis with given ID does not exist |

---

### GET /api/health

Health check endpoint.

**Response (200 OK):**
```json
{
  "status": "UP"
}
```

## 3. Data Models (Java Records)

### BillAnalysisResponse

Response wrapper with metadata.

```java
public record BillAnalysisResponse(
    UUID id,
    String originalFileName,
    BillAnalysisResult analysis,    // nullable until AI completes
    Instant analyzedAt
) {}
```

### BillAnalysisResult

AI analysis result — data extracted from the bill.

```java
public record BillAnalysisResult(
    String merchantName,
    List<LineItem> items,
    BigDecimal totalAmount,
    String currency,
    List<String> categoryTags
) {}
```

### LineItem

Single line item on the bill.

```java
public record LineItem(
    String name,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}
```

### ErrorResponse

Standardized error response.

```java
public record ErrorResponse(
    String code,
    String message,
    Instant timestamp
) {}
```

## 4. Validation Rules

### Upload (POST /api/bills/upload)

| Rule | Validation | Error Code |
|------|-----------|------------|
| File required | `file` must not be null/empty | 400 |
| MIME type | Validation by content (magic bytes): JPEG, PNG, PDF | 415 |
| Size | Max 10MB | 413 |
| Filename | Sanitization: remove `..`, special characters, path separators | (internal) |

### Retrieval (GET /api/bills/{id})

| Rule | Validation | Error Code |
|------|-----------|------------|
| ID format | Valid UUID | 400 |
| Existence | Analysis with given ID must exist | 404 |

## 5. Error Format

All errors returned in standardized format:

```json
{
  "code": "UNSUPPORTED_MEDIA_TYPE",
  "message": "File type not supported. Allowed: JPEG, PNG, PDF",
  "timestamp": "2026-02-06T14:30:00Z"
}
```

Application error codes:

| Code | Description |
|------|-------------|
| `FILE_REQUIRED` | No file in request |
| `FILE_TOO_LARGE` | Exceeded 10MB limit |
| `UNSUPPORTED_MEDIA_TYPE` | Unsupported file type |
| `ANALYSIS_NOT_FOUND` | Analysis with given ID not found |
| `INVALID_ID_FORMAT` | ID is not a valid UUID |
| `INVALID_INPUT` | Missing or null image data / MIME type |
| `UNSUPPORTED_FORMAT` | PDF upload (vision API does not support PDF) |
| `PROMPT_TOO_LARGE` | Image exceeds 5MB size limit for analysis |
| `ANALYSIS_FAILED` | Unexpected error during AI analysis |
| `INVALID_RESPONSE` | LLM returned unparseable or invalid response |
| `SERVICE_UNAVAILABLE` | Groq API unavailable after retries exhausted |
| `INTERNAL_ERROR` | Unexpected server error |
