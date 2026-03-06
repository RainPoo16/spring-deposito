package com.examples.deposit.domain

import spock.lang.Specification
import spock.lang.Unroll

class BlockCodeSpec extends Specification {

    @Unroll
    def "resolves metadata for code #rawCode"() {
        when:
        def blockCode = BlockCode.fromCode(rawCode)

        then:
        blockCode == expectedBlockCode
        blockCode.direction == expectedDirection
        blockCode.requestedBy == expectedRequestedBy
        blockCode.customerAllowed == expectedCustomerAllowed

        where:
        rawCode | expectedBlockCode | expectedDirection       | expectedRequestedBy         | expectedCustomerAllowed
        "ACB"   | BlockCode.ACB      | BlockDirection.INCOMING | BlockRequestedBy.BANK       | false
        "ACC"   | BlockCode.ACC      | BlockDirection.INCOMING | BlockRequestedBy.CUSTOMER   | true
        "ACG"   | BlockCode.ACG      | BlockDirection.INCOMING | BlockRequestedBy.GOVERNMENT | false
        "ADB"   | BlockCode.ADB      | BlockDirection.OUTGOING | BlockRequestedBy.BANK       | false
    }

    def "rejects unknown block code"() {
        when:
        BlockCode.fromCode("UNKNOWN")

        then:
        thrown(IllegalArgumentException)
    }
}