# TICKET-008 — API Error Handling and Contract Behavior

**Type**: Feature  
**Priority**: Medium  
**FR**: FR-5  
**Depends on**: —  
**Blocks**: TICKET-001, TICKET-002, TICKET-003, TICKET-004 (all endpoints must conform)

## Description

Ensure all DDA endpoints return consistent, typed error payloads using the project's existing exception response contract and correct semantic HTTP status codes. This ticket covers the error handling layer shared by all DDA endpoints.

## Acceptance Criteria

1. HTTP status codes are applied semantically:
   - `200` — successful query response
   - `400` — malformed or semantically invalid request (e.g., missing required header, invalid UUID format)
   - `404` — requested account or resource not found
2. All error responses use the typed error response structure consistent with the existing exception handler (no ad-hoc plain-text or raw exception responses).
3. Error payloads include at minimum:
   - An error code / type identifier
   - A human-readable message safe for logging (no PII in error messages)
4. Missing `x-customer-id` header on customer endpoints returns `400` (not `500`).
5. Valid UUID format but non-existent account ID returns `404` (not `400`).
6. Malformed UUID (e.g., non-UUID string) in path variable returns `400` (not `500`).
7. Unexpected errors surface as `500` with a generic error payload — stack traces are not exposed in the response body.

## Technical Notes

- Wire into the existing `@ControllerAdvice` / exception handler rather than adding per-endpoint try/catch.
- Do not include PII (customer ID values, account numbers) in error messages (see PII protection guidelines).
- Ensure the error contract is documented in OpenAPI (see TICKET-009).
