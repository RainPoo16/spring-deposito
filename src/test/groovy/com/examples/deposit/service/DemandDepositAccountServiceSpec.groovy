package com.examples.deposit.service

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountLifecycleEvent
import com.examples.deposit.domain.DemandDepositAccountLifecycleEventType
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.exception.AccountCreationConflictException
import com.examples.deposit.exception.AccountLifecycleException
import com.examples.deposit.exception.AccountNotFoundException
import com.examples.deposit.repository.DemandDepositAccountLifecycleEventRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import com.examples.deposit.service.dto.CreateDemandDepositAccountCommand
import com.github.f4b6a3.uuid.alt.GUID
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

class DemandDepositAccountServiceSpec extends Specification {

    private final DemandDepositAccountRepository accountRepository = Mock()
    private final DemandDepositAccountLifecycleEventRepository lifecycleEventRepository = Mock()
    private final DemandDepositAccountService service = new DemandDepositAccountService(accountRepository, lifecycleEventRepository)

    def "createMainAccount returns existing account flagged replay when idempotency key already exists"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-service-replay-001"
        def command = new CreateDemandDepositAccountCommand(customerId, idempotencyKey)
        def existing = DemandDepositAccount.createPending(customerId, idempotencyKey)

        when:
        def result = service.createMainAccount(command)

        then:
        1 * accountRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey) >> Optional.of(existing)
        0 * accountRepository.saveAndFlush(_)
        0 * lifecycleEventRepository.save(_)
        result.accountId() == existing.id
        result.status() == DemandDepositAccountStatus.PENDING_VERIFICATION
        result.replay()
    }

    def "createMainAccount persists pending account and lifecycle event for first request"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-service-create-001"
        def command = new CreateDemandDepositAccountCommand(customerId, idempotencyKey)

        when:
        def result = service.createMainAccount(command)

        then:
        1 * accountRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey) >> Optional.empty()
        1 * accountRepository.saveAndFlush({ DemandDepositAccount account ->
            account.customerId == customerId &&
                account.idempotencyKey == idempotencyKey &&
                account.status == DemandDepositAccountStatus.PENDING_VERIFICATION
        }) >> { args -> args[0] }
        1 * lifecycleEventRepository.save({ DemandDepositAccountLifecycleEvent event ->
            event.accountId != null &&
                event.customerId == customerId &&
                event.eventType == DemandDepositAccountLifecycleEventType.ACCOUNT_CREATED &&
                event.accountStatus == DemandDepositAccountStatus.PENDING_VERIFICATION
        })
        result.customerId() == customerId
        result.status() == DemandDepositAccountStatus.PENDING_VERIFICATION
        !result.replay()
    }

    def "createMainAccount resolves data integrity race by returning existing account as replay"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-service-race-001"
        def command = new CreateDemandDepositAccountCommand(customerId, idempotencyKey)
        def existing = DemandDepositAccount.createPending(customerId, idempotencyKey)
        def duplicateConstraintMessage = "Unique index or primary key violation: \"PUBLIC.UQ_DEMAND_DEPOSIT_ACCOUNTS_CUSTOMER_IDEMPOTENCY\""

        when:
        def result = service.createMainAccount(command)

        then:
        2 * accountRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey) >>> [Optional.empty(), Optional.of(existing)]
        1 * accountRepository.saveAndFlush(_ as DemandDepositAccount) >> { throw new DataIntegrityViolationException(duplicateConstraintMessage) }
        0 * lifecycleEventRepository.save(_)
        result.accountId() == existing.id
        result.replay()
    }

    def "createMainAccount throws conflict when race cannot be resolved from repository"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-service-race-002"
        def command = new CreateDemandDepositAccountCommand(customerId, idempotencyKey)
        def duplicateConstraintMessage = "Unique index or primary key violation: \"PUBLIC.UQ_DEMAND_DEPOSIT_ACCOUNTS_CUSTOMER_IDEMPOTENCY\""

        when:
        service.createMainAccount(command)

        then:
        2 * accountRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey) >>> [Optional.empty(), Optional.empty()]
        1 * accountRepository.saveAndFlush(_ as DemandDepositAccount) >> { throw new DataIntegrityViolationException(duplicateConstraintMessage) }
        0 * lifecycleEventRepository.save(_)
        thrown(AccountCreationConflictException)
    }

    def "createMainAccount rethrows non-idempotency data integrity violations"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-service-race-003"
        def command = new CreateDemandDepositAccountCommand(customerId, idempotencyKey)

        when:
        service.createMainAccount(command)

        then:
        1 * accountRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey) >> Optional.empty()
        1 * accountRepository.saveAndFlush(_ as DemandDepositAccount) >> {
            throw new DataIntegrityViolationException("referential integrity constraint violation")
        }
        0 * lifecycleEventRepository.save(_)
        thrown(DataIntegrityViolationException)
    }

    def "createMainAccount command rejects idempotency key longer than 128"() {
        when:
        new CreateDemandDepositAccountCommand(GUID.v7().toUUID(), "x" * 129)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "idempotencyKey must be <= 128 characters"
    }

    def "activateAccount transitions pending account to active and persists lifecycle event"() {
        given:
        def account = DemandDepositAccount.createPending(GUID.v7().toUUID(), "idem-service-activate-001")

        when:
        def result = service.activateAccount(account.id)

        then:
        1 * accountRepository.findById(account.id) >> Optional.of(account)
        1 * accountRepository.saveAndFlush({ DemandDepositAccount saved ->
            saved.id == account.id && saved.status == DemandDepositAccountStatus.ACTIVE
        }) >> { args -> args[0] }
        1 * lifecycleEventRepository.save({ DemandDepositAccountLifecycleEvent event ->
            event.accountId == account.id &&
                event.customerId == account.customerId &&
                event.eventType == DemandDepositAccountLifecycleEventType.ACCOUNT_ACTIVATED
        })
        result.accountId() == account.id
        result.customerId() == account.customerId
        result.status() == DemandDepositAccountStatus.ACTIVE
        !result.replay()
    }

    def "activateAccount throws not found when account does not exist"() {
        given:
        def accountId = GUID.v7().toUUID()

        when:
        service.activateAccount(accountId)

        then:
        1 * accountRepository.findById(accountId) >> Optional.empty()
        0 * accountRepository.saveAndFlush(_)
        0 * lifecycleEventRepository.save(_)
        thrown(AccountNotFoundException)
    }

    def "activateAccount throws lifecycle exception when transition is invalid"() {
        given:
        def account = DemandDepositAccount.createPending(GUID.v7().toUUID(), "idem-service-activate-002")
        account.activate()

        when:
        service.activateAccount(account.id)

        then:
        1 * accountRepository.findById(account.id) >> Optional.of(account)
        0 * accountRepository.saveAndFlush(_)
        0 * lifecycleEventRepository.save(_)
        thrown(AccountLifecycleException)
    }
}
