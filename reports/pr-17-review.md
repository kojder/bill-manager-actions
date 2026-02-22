# Code Review Report — PR #17
**feat(ui): style upload page with Pico CSS**
**Author:** kojder | **Branch:** feat/task-16-frontend-styling → master

---

## Execution Plan

Checked the following in order:
1. `gh pr diff 17` — full unified diff of all 4 changed files
2. `CLAUDE.md` — global review scope, upload module rules, path-specific rules
3. `src/main/resources/static/index.html` — full file read to confirm line numbers and full context
4. `src/main/java/com/example/bill_manager/health/HealthController.java` — full file read to verify `@RequestMapping` base path and understand endpoint mapping

---

## Summary and Strengths

The PR delivers a clean, well-structured UI upgrade. Highlights:

- **Versioned CDN URL** (`@2.0.6`), not `@latest` — correct practice
- **Semantic HTML** (`<main>`, `<hgroup>`, `<article>`, `<fieldset>`, `<table>`) — proper Pico CSS usage
- **XSS escaping** implemented consistently via `escapeHtml()` using the DOM `createTextNode` technique — the correct approach for HTML string building
- **Accessibility**: `role="alert"` on `#error`, `aria-busy` on submit button, `hidden` attribute instead of inline `display:none`
- **No raw JSON exposed** — structured result card replaces the previous `JSON.stringify` dump
- **Zero backend changes** in the UI task (HealthController belongs to a prior commit on this branch)

---

## Risks / Potential Bugs

### [WARNING] Missing null-guard on `data.analysis` and `data.analysis.items`
**File:** `src/main/resources/static/index.html` — lines 54–55

```js
const a = data.analysis;           // could be null if AI parsing fails
const itemsHtml = a.items.map(…);  // TypeError if a is null or a.items is not an array
```

If the API returns a partial or malformed analysis object, `renderResult()` throws a `TypeError`. The `catch` block catches it and renders it as `NETWORK_ERROR` — misleading to the user and masking the real cause.

**Suggested fix:** Add guard at the top of `renderResult()`:
```js
if (!a || !Array.isArray(a.items)) {
  return renderError(0, { code: 'PARSE_ERROR', message: 'Unexpected response format from server.' });
}
```

---

### [WARNING] `data.id` not passed through `escapeHtml()`
**File:** `src/main/resources/static/index.html` — line 82

```js
+ '<span>ID: ' + data.id.substring(0, 8) + '&hellip;</span>'
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ no escapeHtml()
```

All other server-controlled fields in this function go through `escapeHtml()`. The UUID from `java.util.UUID` only contains hex chars and `-`, so actual XSS risk is zero today — but the inconsistency is a code smell and becomes a real issue if the field type ever changes.

**Suggested fix:**
```js
+ '<span>ID: ' + escapeHtml(data.id.substring(0, 8)) + '&hellip;</span>'
```

---

### [WARNING] Readiness probe unconditionally returns READY
**File:** `src/main/java/com/example/bill_manager/health/HealthController.java` — line 20–22

```java
@GetMapping("/health/ready")
public ResponseEntity<HealthResponse> ready() {
  return ResponseEntity.ok(new HealthResponse("READY"));
}
```

This endpoint always returns HTTP 200 regardless of whether the application's dependencies (database, Groq API) are reachable. Container orchestrators rely on readiness probes to stop routing traffic when a service is degraded. An unconditional READY probe provides no benefit over the liveness probe.

**Minimum acceptable fix:** Return HTTP `503` when critical dependencies are unavailable. For a simple placeholder, at least add a TODO comment documenting the limitation so the behaviour is intentional, not accidental.

---

### [NIT] `formatTimestamp()` result not escaped
**File:** `src/main/resources/static/index.html` — line 83

```js
+ '<span>' + formatTimestamp(data.analyzedAt) + '</span>'
```

`toLocaleString()` output is locale-dependent. Standard locale formatters don't emit HTML metacharacters, but passing through `escapeHtml()` would be consistent with all other fields.

**Suggested fix:**
```js
+ '<span>' + escapeHtml(formatTimestamp(data.analyzedAt)) + '</span>'
```

---

## Path-Specific Rule Compliance

### Global Review Scope (applicable)
| Rule | Status |
|------|--------|
| No secrets/API keys in HTML/JS | ✅ Pass |
| XSS escaping for user-visible data | ⚠️ Inconsistent (data.id, formatTimestamp) |
| Structured error responses, no raw server internals | ✅ Pass |
| Null checks for API response fields | ⚠️ Missing on data.analysis / data.analysis.items |

### Upload Module (`**/upload/**`)
Not applicable — this PR makes zero backend upload changes.

---

## Next Actions for Author

1. **Fix null-guard** on `data.analysis` and `data.analysis.items` in `renderResult()` (lines 54–55)
2. **Wrap `data.id.substring(0, 8)`** with `escapeHtml()` (line 82)
3. **Wrap `formatTimestamp()` result** with `escapeHtml()` (line 83) — nit, but completes the escaping pattern
4. **Readiness probe** — either implement a real dependency check or add a TODO comment acknowledging the current probe is a placeholder only
