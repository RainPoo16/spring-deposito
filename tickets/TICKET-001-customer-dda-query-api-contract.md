# TICKET-001: Customer DDA Query API Contract

## Summary
Implement/confirm customer endpoint contract for `GET /demand-deposit-accounts` with required customer context and success response shape.

## PRD Mapping
- FR-1.1
- FR-1.3
- Section 10.1 (Security and Access)
- Acceptance Criteria 4 (OpenAPI coverage for customer endpoint)
- Acceptance Criteria 5 (integration test coverage for customer query)

## Scope
- Enforce `x-customer-id` request header requirement.
- Ensure scope/authorization checks are applied where configured.
- Return `200` with JSON payload for successful customer account retrieval.
- Update OpenAPI documentation for `GET /demand-deposit-accounts` including header requirements and success/error responses.
- Add integration tests for happy path and key request-level negative paths (`400` for invalid header semantics).

## Out of Scope
- Internal/admin account information endpoint (FR-2 descoped).
- Front-end/channel implementation.

## Acceptance Criteria
- Request without `x-customer-id` is rejected with typed client error.
- Authorized request returns `200` and contract-consistent payload wrapper.
- Response content type is JSON and consistent across success paths.
- OpenAPI spec documents request headers, `200`, and typed error responses for this endpoint.
- Integration tests verify status code, content type, and key response fields for query flows.

## Dependencies
- Existing controller/service wiring for customer query.
- Authentication/scope middleware configuration.

## Definition of Done
- Endpoint behavior implemented and manually verified.
- Regression and integration tests updated/added for customer query paths.
- OpenAPI annotations/spec for this endpoint are updated and validated.
