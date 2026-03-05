package com.examples.deposit.domain

import com.github.f4b6a3.uuid.alt.GUID
import spock.lang.Specification

import java.math.BigDecimal

class DemandDepositAccountSpec extends Specification {

    def "createPending initializes account baseline in pending verification"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-create-main-account-001"

        when:
        def account = DemandDepositAccount.createPending(customerId, idempotencyKey)

        then:
        account.id != null
        account.customerId == customerId
        account.idempotencyKey == idempotencyKey
        account.status == DemandDepositAccountStatus.PENDING_VERIFICATION
        account.ledgerBalance == BigDecimal.ZERO
        account.availableBalance == BigDecimal.ZERO
        account.createdAt != null
        account.updatedAt != null
    }

    def "activate transitions account from pending verification to active"() {
        given:
        def account = DemandDepositAccount.createPending(GUID.v7().toUUID(), "idem-activate-001")

        when:
        account.activate()

        then:
        account.status == DemandDepositAccountStatus.ACTIVE
        !account.updatedAt.isBefore(account.createdAt)
    }

    def "activate throws when account is not in pending verification"() {
        given:
        def account = DemandDepositAccount.createPending(GUID.v7().toUUID(), "idem-activate-002")
        account.activate()

        when:
        account.activate()

        then:
        thrown(IllegalStateException)
    }

    def "createPending throws when idempotency key is longer than 128 characters"() {
        when:
        DemandDepositAccount.createPending(GUID.v7().toUUID(), "x" * 129)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "idempotencyKey must be <= 128 characters"
    }
}
