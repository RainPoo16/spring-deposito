## Phase 5 Complete: Integration Coverage and Regression Baseline for Ticket 01

Implemented end-to-end integration coverage for create/replay idempotency and one-time activation behavior using real Spring context wiring. This phase validates Ticket-01 acceptance behavior and establishes a stable regression baseline for upcoming tickets.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy

**Functions created/changed:**
- DemandDepositAccountCreationIntegrationSpec.create and replay with same idempotency key returns same account and keeps db record count one
- DemandDepositAccountCreationIntegrationSpec.activation transitions to active exactly once and second activation fails

**Tests created/changed:**
- DemandDepositAccountCreationIntegrationSpec.create and replay with same idempotency key returns same account and keeps db record count one
- DemandDepositAccountCreationIntegrationSpec.activation transitions to active exactly once and second activation fails

**Review Status:** APPROVED

**Git Commit Message:**
test: add dda integration regression coverage

- add end-to-end integration spec for create and replay idempotency
- verify one-time activation transition and duplicate activation rejection
- assert persistence invariants for account count and lifecycle events
