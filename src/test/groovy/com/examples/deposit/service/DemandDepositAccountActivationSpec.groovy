package com.examples.deposit.service

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.repository.DemandDepositAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.UUID

@SpringBootTest
class DemandDepositAccountActivationSpec extends Specification {

    @Autowired
    DemandDepositAccountService demandDepositAccountService

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @org.spockframework.spring.SpringBean
    AccountCreationEligibilityService accountCreationEligibilityService = Mock()

    @org.spockframework.spring.SpringBean
    AccountLifecycleEventPublisher accountLifecycleEventPublisher = Mock()

    def "activates pending account once when eligibility criteria pass"() {
        given:
        UUID accountId = UUID.randomUUID()
        UUID customerId = UUID.randomUUID()
        demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> true

        when:
        def activatedAccount = demandDepositAccountService.activateAccountIfEligible(accountId)

        then:
        activatedAccount.id == accountId
        activatedAccount.status == DemandDepositAccountStatus.ACTIVE
        demandDepositAccountRepository.findById(accountId).orElseThrow().status == DemandDepositAccountStatus.ACTIVE
        1 * accountLifecycleEventPublisher.publishAccountActivated(accountId, customerId)
        0 * accountLifecycleEventPublisher.publishAccountCreated(_, _)
    }

    def "duplicate activation attempts are idempotent and emit event once"() {
        given:
        UUID accountId = UUID.randomUUID()
        UUID customerId = UUID.randomUUID()
        demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >>> [true, true]

        when:
        def firstActivation = demandDepositAccountService.activateAccountIfEligible(accountId)
        def secondActivation = demandDepositAccountService.activateAccountIfEligible(accountId)

        then:
        firstActivation.status == DemandDepositAccountStatus.ACTIVE
        secondActivation.status == DemandDepositAccountStatus.ACTIVE
        demandDepositAccountRepository.findById(accountId).orElseThrow().status == DemandDepositAccountStatus.ACTIVE
        1 * accountLifecycleEventPublisher.publishAccountActivated(accountId, customerId)
        0 * accountLifecycleEventPublisher.publishAccountCreated(_, _)
    }

    def "does not transition when eligibility criteria fail"() {
        given:
        UUID accountId = UUID.randomUUID()
        UUID customerId = UUID.randomUUID()
        demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> false

        when:
        def resultAccount = demandDepositAccountService.activateAccountIfEligible(accountId)

        then:
        resultAccount.status == DemandDepositAccountStatus.PENDING_VERIFICATION
        demandDepositAccountRepository.findById(accountId).orElseThrow().status == DemandDepositAccountStatus.PENDING_VERIFICATION
        0 * accountLifecycleEventPublisher._
    }

    def "non-pending account activation path is safely handled"() {
        given:
        UUID accountId = UUID.randomUUID()
        UUID customerId = UUID.randomUUID()
        demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.ACTIVE)
        )

        when:
        def resultAccount = demandDepositAccountService.activateAccountIfEligible(accountId)

        then:
        resultAccount.status == DemandDepositAccountStatus.ACTIVE
        demandDepositAccountRepository.findById(accountId).orElseThrow().status == DemandDepositAccountStatus.ACTIVE
        0 * accountCreationEligibilityService._
        0 * accountLifecycleEventPublisher._
    }

    def "concurrent activation attempts publish side effect once"() {
        given:
        UUID accountId = UUID.randomUUID()
        UUID customerId = UUID.randomUUID()
        demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )

        def gate = new CountDownLatch(2)
        def release = new CountDownLatch(1)
        accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId) >> {
            gate.countDown()
            assert release.await(2, TimeUnit.SECONDS)
            true
        }

        def executor = Executors.newFixedThreadPool(2)

        when:
        def futures = (1..2).collect {
            executor.submit {
                try {
                    demandDepositAccountService.activateAccountIfEligible(accountId)
                    null
                } catch (Exception ex) {
                    ex
                }
            }
        }

        assert gate.await(2, TimeUnit.SECONDS)
        release.countDown()
        def outcomes = futures.collect { it.get(5, TimeUnit.SECONDS) }

        then:
        demandDepositAccountRepository.findById(accountId).orElseThrow().status == DemandDepositAccountStatus.ACTIVE
        outcomes.findAll { it != null }.size() <= 1
        1 * accountLifecycleEventPublisher.publishAccountActivated(accountId, customerId)
        0 * accountLifecycleEventPublisher.publishAccountCreated(_, _)

        cleanup:
        executor.shutdownNow()
    }
}
