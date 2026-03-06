package com.examples.deposit.domain

import spock.lang.Specification
import spock.lang.Unroll

class AccountBlockEligibilitySpec extends Specification {

    private static final List<BlockRequestedBy> ACTORS = BlockRequestedBy.values().toList()
    private static final List<BlockCode> BLOCK_CODES = BlockCode.values().toList()
    private static final List<AccountBlockStatus> STATUSES = AccountBlockStatus.values().toList()

    @Unroll
    def "create eligibility matrix covers actor x block code for #actor and #blockCode"() {
        given:
        def actual = blockCode.isEligibleForCreateBy(actor)

        expect:
        actual == expected

        where:
        actor                       | blockCode     || expected
        BlockRequestedBy.BANK       | BlockCode.ACB || true
        BlockRequestedBy.BANK       | BlockCode.ACC || false
        BlockRequestedBy.BANK       | BlockCode.ACG || false
        BlockRequestedBy.BANK       | BlockCode.ADB || true
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC || true
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG || true
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB || false
    }

    @Unroll
    def "update eligibility matrix covers actor x block code x status for #actor #blockCode #status"() {
        given:
        def actual = status.isEligibleForUpdate(actor, blockCode)

        expect:
        actual == expected

        where:
        actor                       | blockCode     | status                        || expected
        BlockRequestedBy.BANK       | BlockCode.ACB | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.BANK       | BlockCode.ACB | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.BANK       | BlockCode.ACB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.BANK       | BlockCode.ACC | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.BANK       | BlockCode.ACC | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.BANK       | BlockCode.ACC | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.BANK       | BlockCode.ACG | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.BANK       | BlockCode.ACG | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.BANK       | BlockCode.ACG | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.BANK       | BlockCode.ADB | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.BANK       | BlockCode.ADB | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.BANK       | BlockCode.ADB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB | AccountBlockStatus.CANCELLED  || false
    }

    @Unroll
    def "cancel eligibility matrix covers actor x block code x status for #actor #blockCode #status"() {
        given:
        def actual = status.isEligibleForCancel(actor, blockCode)

        expect:
        actual == expected

        where:
        actor                       | blockCode     | status                        || expected
        BlockRequestedBy.BANK       | BlockCode.ACB | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.BANK       | BlockCode.ACB | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.BANK       | BlockCode.ACB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.BANK       | BlockCode.ACC | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.BANK       | BlockCode.ACC | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.BANK       | BlockCode.ACC | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.BANK       | BlockCode.ACG | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.BANK       | BlockCode.ACG | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.BANK       | BlockCode.ACG | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.BANK       | BlockCode.ADB | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.BANK       | BlockCode.ADB | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.BANK       | BlockCode.ADB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.CUSTOMER   | BlockCode.ACC | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ACG | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.CUSTOMER   | BlockCode.ADB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACB | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACC | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG | AccountBlockStatus.PENDING    || true
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG | AccountBlockStatus.ACTIVE     || true
        BlockRequestedBy.GOVERNMENT | BlockCode.ACG | AccountBlockStatus.CANCELLED  || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB | AccountBlockStatus.PENDING    || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB | AccountBlockStatus.ACTIVE     || false
        BlockRequestedBy.GOVERNMENT | BlockCode.ADB | AccountBlockStatus.CANCELLED  || false
    }

    def "update and cancel eligibility are intentionally identical for every actor x block code x status"() {
        when:
        def allCombinations = ACTORS.collectMany { actor ->
            BLOCK_CODES.collectMany { blockCode ->
                STATUSES.collect { status -> [actor: actor, blockCode: blockCode, status: status] }
            }
        }

        then:
        allCombinations.size() == ACTORS.size() * BLOCK_CODES.size() * STATUSES.size()
        allCombinations.every { combination ->
            combination.status.isEligibleForUpdate(combination.actor, combination.blockCode) ==
                combination.status.isEligibleForCancel(combination.actor, combination.blockCode)
        }
    }
}