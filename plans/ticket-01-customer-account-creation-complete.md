## Plan Complete: Ticket 01 Customer Account Creation

Implemented Ticket-01 end-to-end with Spring Boot best practices across schema, domain, repository, service, API, and integration testing layers. The solution now supports customer-facing idempotent account creation, lifecycle transition from `PENDING_VERIFICATION` to `ACTIVE`, and persisted lifecycle event hooks for FR-AC-4 readiness. Error contracts are standardized via RFC7807 ProblemDetail responses with sanitized, non-sensitive details.

**Phases Completed:** 5 of 5
1. ✅ Phase 1: Establish Domain and Persistence Baseline
2. ✅ Phase 2: Add Repository and Idempotency Storage Guarantees
3. ✅ Phase 3: Implement Application Service for Create + Activation Lifecycle
4. ✅ Phase 4: Expose Customer API Contract and Error Mapping
5. ✅ Phase 5: Integration Coverage and Regression Baseline for Ticket 01

**All Files Created/Modified:**
- pom.xml
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountReq.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountResp.java
- src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountLifecycleEvent.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountLifecycleEventType.java
- src/main/java/com/examples/deposit/exception/AccountCreationConflictException.java
- src/main/java/com/examples/deposit/exception/AccountLifecycleException.java
- src/main/java/com/examples/deposit/exception/AccountNotFoundException.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountLifecycleEventRepository.java
- src/main/java/com/examples/deposit/service/DemandDepositAccountService.java
- src/main/java/com/examples/deposit/service/dto/CreateDemandDepositAccountCommand.java
- src/main/java/com/examples/deposit/service/dto/DemandDepositAccountResult.java
- src/main/resources/db/migration/V1__create_demand_deposit_accounts.sql
- src/main/resources/db/migration/V2__create_demand_deposit_account_lifecycle_events.sql
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy
- src/test/groovy/com/examples/deposit/domain/DemandDepositAccountSpec.groovy
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionalIntegrationSpec.groovy
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy

**Key Functions/Classes Added:**
- DemandDepositAccountController
- DemandDepositAccountService
- GlobalExceptionHandler
- DemandDepositAccount
- DemandDepositAccountLifecycleEvent
- DemandDepositAccountRepository
- DemandDepositAccountLifecycleEventRepository
- CreateDemandDepositAccountCommand
- DemandDepositAccountResult

**Test Coverage:**
- Total tests written: 29
- All tests passing: ✅

**Recommendations for Next Steps:**
- Introduce activation API endpoint in future ticket if activation must be externally triggered.
- Add PostgreSQL-backed CI integration profile to supplement H2 behavior checks for constraint parsing paths.
