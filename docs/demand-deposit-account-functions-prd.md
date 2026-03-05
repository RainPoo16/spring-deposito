# Product Requirements Document (PRD)
## DemandDepositAccount Functions
### Date: 2026-03-05
### Status: Draft
### Owners: Deposit Domain (Product + Engineering)

## 1. Purpose
Define product requirements for DemandDepositAccount (DDA) capabilities in the Deposit service, covering customer account visibility, internal account information access, and credit/debit transaction interaction with account restrictions.

This PRD is aligned to existing implementation in:
- `src/main/java/com/ytl/deposit/controller/account/DemandDepositAccountCustomerQueryController.java`
- `src/main/java/com/ytl/deposit/controller/internalaccount/DemandDepositAccountInfoController.java`

## 2. Problem Statement
Customers and internal services need a consistent and safe way to view demand deposit accounts. Current requirements must ensure:
- Customers can view accurate account standing and balances.
- Internal services can retrieve account details for orchestration.

## 3. Goals
- Provide complete account visibility across customer and internal contexts.
- Ensure data consistency for balances, status, and account attributes.
- Support downstream internal orchestration with stable and predictable response contracts.
- Define explicit transaction decision rules for credit/debit under restrictions and account status conditions.

## 4. Non-Goals
- Introducing new deposit products or pricing models.
- Replacing core banking integration patterns.
- Redesigning external authentication/authorization systems.
- Defining UI implementation details for customer channels.

## 5. Personas
- Customer: Views own DDA details and account status.
- Internal Service Consumer: Reads account information for internal business flows.

## 6. Scope
### 6.1 In Scope
- Customer DDA detail and minimal detail retrieval.
- Internal DDA retrieval by customer and account ID.
- Account visibility attributes including balances, status, account type, and active restrictions in read responses.
- Restriction and transaction interaction rules for debit and credit evaluation.

### 6.2 Out of Scope
- Mobile/web front-end screens.
- Core banking system functional changes.
- New event schema families beyond existing demand deposit facts/events.
- All admin-facing demand deposit account functions.

## 7. Functional Requirements
### FR-1 Customer Account Query
1. System shall return full DDA details for authenticated customer via `GET /demand-deposit-accounts`.
2. Response shall include account status, balances, interest summaries/rates, active blocks, active earmarks, credit/debit flags, and closure metadata when applicable.
3. System shall return minimal account details via `GET /demand-deposit-accounts/basic` for lightweight use cases.
4. Requests shall require `x-customer-id` header and enforce scope check where configured.

### FR-2 Internal Account Information
1. System shall expose internal account query endpoints under `/internal/demand-deposit-accounts`.
2. Internal consumers shall retrieve account(s) by customer ID and by account ID.
3. Internal responses shall include enough account state for downstream orchestration (status, balance, account number, earmarks).

### FR-3 Restriction Visibility and Evaluation
1. System shall expose active restrictions in read models, including blocks and earmarks relevant to transaction decisions.
2. Each active block shall indicate effective debit/credit impact (`debitDisabled`, `creditDisabled`) where applicable.
3. Earmark data shall be reflected such that consuming services can derive amount unavailable for debit operations.
4. Restriction state used for transaction decisions shall be based on current effective window and status.

### FR-4 Credit and Debit Transaction Interaction
1. System shall evaluate debit and credit eligibility using account status, account-level flags (`debitDisabled`, `creditDisabled`), active blocks, and earmarks.
2. Debit requests shall be rejected when any applicable rule disables debit, including prohibited account statuses and debit-disabling blocks.
3. Credit requests shall be rejected when any applicable rule disables credit, including prohibited account statuses and credit-disabling blocks.
4. Debit requests shall validate spendable balance derived from current balance minus on-hold and earmarked amounts according to domain rules.
5. For statuses with restricted behavior (for example `PENDING_VERIFICATION`), transaction eligibility shall follow explicit transaction-type and transaction-code allowlists.
6. Transaction evaluation outcomes shall be deterministic and return typed failure reasons for rejected operations.

### FR-5 Error Handling and Contract Behavior
1. System shall return semantic HTTP status codes:
- `200` for successful query/update actions
- `400` for malformed/invalid request semantics
- `404` for missing account resources
2. Error payloads shall use typed error responses consistent with existing exception response contracts.

## 8. Business Rules
- Account read responses must reflect latest persisted account state.
- Account hierarchy (main account and child pockets) shall be represented consistently in customer/internal read models.
- Pending verification accounts have restricted transaction behavior (credit/debit eligibility based on domain rules).
- Debit eligibility shall consider both state-based restriction (status/flags/blocks) and amount-based restriction (earmarks and holds).
- Credit and debit restriction checks shall use the same effective-time view of block and earmark records.

### 8.1 Credit and Debit Interaction Matrix
| Condition | Credit | Debit | Notes |
|---|---|---|---|
| `creditDisabled = true` | Reject | Evaluate other rules | Account-level credit stop |
| `debitDisabled = true` | Evaluate other rules | Reject | Account-level debit stop |
| Active block disables credit | Reject | Evaluate other rules | Block-level override for credit |
| Active block disables debit | Evaluate other rules | Reject | Block-level override for debit |
| Active earmark amount present | Allow | Allow if spendable balance sufficient | Earmark reduces debit-available funds |
| Status is transaction-restricted (for example `PENDING_VERIFICATION`) | Allow only allowed credit types/codes | Allow only allowed debit types/codes | Enforce domain allowlist |

### 8.2 Example Transaction Scenarios
1. Active debit block with sufficient spendable balance:
Debit request is rejected because block-level debit restriction takes precedence over available funds.

2. Active credit block with otherwise eligible account:
Credit request is rejected because block-level credit restriction takes precedence over status and balance checks.

3. No debit block, no debit-disabled flag, but earmark reduces spendable amount below request:
Debit request is rejected for insufficient spendable balance even if current balance is sufficient.

4. `PENDING_VERIFICATION` account receives credit using allowed interbank code:
Credit request is accepted when transaction type/code is in the permitted allowlist for restricted status.

5. `PENDING_VERIFICATION` account initiates debit using disallowed transaction type:
Debit request is rejected with typed failure reason indicating transaction type/code not permitted for current status.

## 9. Data and Event Requirements
- DDA aggregate state is persisted under demand deposit account domain entities and repositories.
- Significant state changes shall publish fact/events for projections and downstream integration.
- Event payload schemas shall follow versioned schema management under `schemas/` and `kafka-apis-manifest.yaml`.
- Transaction-related state mutations and restriction changes shall remain traceable through account facts/events consumed by projections.

## 10. Non-Functional Requirements
### 10.1 Security and Access
- Customer endpoints require customer identity context (`x-customer-id`).
- Internal endpoints must be restricted by profile and service/auth controls.

### 10.2 Reliability
- Read operations must tolerate downstream retry and transient dependency issues without returning inconsistent payload shapes.

### 10.3 Auditability and Observability
- Account query requests shall be traceable by account ID and customer ID where applicable.
- Service shall emit operational logs and tracing attributes for key account retrieval operations.

## 11. API Inventory (Current)
- Customer:
- `GET /demand-deposit-accounts`
- `GET /demand-deposit-accounts/basic`
- Internal:
- `GET /internal/demand-deposit-accounts`
- `GET /internal/demand-deposit-accounts/{demandDepositAccountId}`

## 12. Success Metrics
- >= 99.5% successful response rate for DDA read APIs (excluding client errors).
- >= 99.5% successful response rate for internal DDA read APIs (excluding client errors).
- < 0.5% monthly customer support tickets attributable to DDA data visibility mismatch.

## 13. Dependencies
- Customer/profile services for customer metadata and validation.
- Core banking integration for account state and transaction synchronization.
- Event bus and schema repository for fact/event publication and consumption.

## 14. Risks and Mitigations
- Risk: Data projection lag causes temporary mismatch in customer/internal account views.
- Mitigation: Monitor projection freshness and alert on processing delays.

- Risk: Downstream dependency issues impact read API stability.
- Mitigation: Add retries and fallback handling where applicable.

## 15. Release and Rollout
- Phase 1: Confirm customer/internal API contract behavior in lower environments.
- Phase 2: Roll out with monitoring dashboards for API health and error rates.
- Phase 3: Track production quality metrics and close open questions.

## 16. Acceptance Criteria
1. Customer can retrieve DDA full and basic details with accurate balances and status.
2. Internal consumers can retrieve DDA list/details with contract-consistent payloads.
3. Read responses include status and balance fields consistent with persisted state.
4. Missing account requests return typed not-found errors with expected HTTP code.
5. All listed endpoints are documented in OpenAPI and return contract-consistent payloads.
6. Integration tests cover happy path and key not-found/invalid-request cases for customer/internal queries.
7. Integration tests verify credit/debit decisions for combinations of account status, block impact, and earmark amount.
8. Integration tests verify rejected transactions return deterministic and typed failure reasons.

## 17. Open Questions
- What freshness expectation is required for account projections consumed by customer/internal queries?
