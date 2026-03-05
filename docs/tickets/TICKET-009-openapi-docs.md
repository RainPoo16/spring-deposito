# TICKET-009 — OpenAPI Documentation

**Type**: Chore  
**Priority**: Medium  
**FR**: —  
**Depends on**: TICKET-001, TICKET-002, TICKET-003, TICKET-004, TICKET-008  

## Description

Ensure all DDA endpoints are fully documented in the OpenAPI specification. Documentation must cover request parameters, headers, response schemas, and error responses so that consuming teams can integrate without reading source code.

## Acceptance Criteria

1. The following endpoints are documented in OpenAPI:
   - `GET /demand-deposit-accounts`
   - `GET /demand-deposit-accounts/basic`
   - `GET /internal/demand-deposit-accounts`
   - `GET /internal/demand-deposit-accounts/{demandDepositAccountId}`
2. Each endpoint documents:
   - Required and optional headers (e.g., `x-customer-id`)
   - Query parameters and path variables with type and validation constraints
   - `200` response schema (all fields, with descriptions)
   - `400` and `404` error response schemas
3. Response schemas use `$ref` components to avoid duplication between full and shared structures.
4. Internal endpoints are marked appropriately (e.g., tagged separately or annotated as internal-only).
5. OpenAPI spec can be generated/validated without build errors.

## Technical Notes

- Use SpringDoc / `@Operation`, `@ApiResponse`, `@Schema` annotations rather than a manually maintained YAML file.
- Mark sensitive fields (e.g., account number) with appropriate schema metadata — do not include PII examples in the spec.
- Validate that the generated spec is consistent with actual endpoint behavior by running the integration tests in TICKET-010 against the documented contract.
