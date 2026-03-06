package com.examples.deposit.service

import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException
import com.examples.deposit.domain.exception.IdempotencyConflictException
import com.examples.deposit.repository.AccountCreationIdempotencyRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.UUID

@SpringBootTest
@Transactional
class DemandDepositAccountServiceSpec extends Specification {

    @Autowired
    DemandDepositAccountService demandDepositAccountService

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    AccountCreationIdempotencyRepository accountCreationIdempotencyRepository

    @org.spockframework.spring.SpringBean
    AccountCreationEligibilityService accountCreationEligibilityService = Mock()

    @org.spockframework.spring.SpringBean
    AccountLifecycleEventPublisher accountLifecycleEventPublisher = Mock()

    def "creates account in pending verification for eligible customer"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-create-001"
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> true

        when:
        def createdAccount = demandDepositAccountService.createMainAccount(customerId, idempotencyKey)

        then:
        createdAccount.customerId == customerId
        createdAccount.status.name() == "PENDING_VERIFICATION"
        demandDepositAccountRepository.count() == 1
        accountCreationIdempotencyRepository.count() == 1
        1 * accountLifecycleEventPublisher.publishAccountCreated(_, customerId)
    }

    def "returns same account without duplicate rows for replayed idempotency key"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-replay-001"
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> true

        when:
        def firstCreate = demandDepositAccountService.createMainAccount(customerId, idempotencyKey)
        def replayCreate = demandDepositAccountService.createMainAccount(customerId, idempotencyKey)

        then:
        replayCreate.id == firstCreate.id
        demandDepositAccountRepository.count() == 1
        accountCreationIdempotencyRepository.count() == 1
        1 * accountLifecycleEventPublisher.publishAccountCreated(_, customerId)
    }

    def "rejects ineligible customer and persists nothing"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-ineligible-001"
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> false

        when:
        demandDepositAccountService.createMainAccount(customerId, idempotencyKey)

        then:
        thrown(CustomerNotEligibleForAccountCreationException)
        demandDepositAccountRepository.count() == 0
        accountCreationIdempotencyRepository.count() == 0
        0 * accountLifecycleEventPublisher._
    }

    def "concurrent requests with same idempotency key keep one account row and publish once"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-race-001"
        int parallelRequests = 8
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> true

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests)
        CountDownLatch startGate = new CountDownLatch(1)

        List<Callable<UUID>> tasks = (1..parallelRequests).collect {
            return {
                startGate.await(5, TimeUnit.SECONDS)
                return demandDepositAccountService.createMainAccount(customerId, idempotencyKey).id
            } as Callable<UUID>
        }

        when:
        def futures = tasks.collect { executor.submit(it) }
        startGate.countDown()
        List<UUID> returnedIds = futures.collect { it.get(10, TimeUnit.SECONDS) }

        then:
        returnedIds.toSet().size() == 1
        demandDepositAccountRepository.findAll().count { it.customerId == customerId } == 1
        accountCreationIdempotencyRepository.findAll().count {
            it.customerId == customerId && it.idempotencyKey == idempotencyKey
        } == 1
        1 * accountLifecycleEventPublisher.publishAccountCreated(_, customerId)

        cleanup:
        executor.shutdownNow()
    }

    def "throws idempotency conflict when fallback cannot load consistent account after integrity violation"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-conflict-fallback-001"

        DemandDepositAccountRepository localAccountRepository = Mock()
        AccountCreationIdempotencyRepository localIdempotencyRepository = Mock()
        AccountCreationEligibilityService localEligibilityService = Mock()
        AccountLifecycleEventPublisher localEventPublisher = Mock()
        TransactionTemplate localTransactionTemplate = Mock()

        DemandDepositAccountService service = new DemandDepositAccountService(
            localAccountRepository,
            localIdempotencyRepository,
            localEligibilityService,
            localEventPublisher,
            localTransactionTemplate
        )

        when:
        service.createMainAccount(customerId, idempotencyKey)

        then:
        thrown(IdempotencyConflictException)
        1 * localTransactionTemplate.execute(_) >> { throw new DataIntegrityViolationException("duplicate key") }
        1 * localIdempotencyRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey) >> Optional.empty()
        0 * localAccountRepository._
        0 * localEligibilityService._
        0 * localEventPublisher._
        0 * localIdempotencyRepository.saveAndFlush(_)
    }
}
