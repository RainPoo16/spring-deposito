# TICKET-005: Deterministic Rejection Reasons and Error Contract

## Summary
Return deterministic, typed failure reasons for rejected credit/debit operations and align error behavior to PRD contract rules.

## PRD Mapping
- FR-4.6
- FR-5.1
- FR-5.2
- Acceptance Criteria 3 and 7
- Acceptance Criteria 4 (typed error schemas in OpenAPI)
- Acceptance Criteria 5 and 6 (negative-path and matrix rejection verification)

## Scope
- Define typed failure reasons for each rejection category.
- Map failures to semantic HTTP status codes (`400`, `404`, and success `200` paths per PRD).
- Ensure payload format aligns with existing typed exception response contract.
- Document typed error responses and rejection reason models in OpenAPI.
- Add integration tests asserting deterministic typed rejection reasons across decision-matrix failure paths.

## Out of Scope
- Non-DDA API-wide error-contract redesign.

## Acceptance Criteria
- Rejected transaction responses include deterministic typed reason.
- Missing resource path returns typed not-found response.
- Invalid semantics returns typed validation/business error response.
- OpenAPI spec explicitly references typed error payloads for relevant endpoints.
- Integration tests assert typed failure reason and expected status code for rejected operations.

## Dependencies
- Existing exception handling advice and error DTOs.
- Eligibility outcomes from TICKET-004.

## Definition of Done
- Error mapping table documented in code/tests.
- Controller/service paths return stable error payload shapes.
- Negative-path tests confirm typed reasons and HTTP status mapping.
- API documentation reflects final typed error contracts.
