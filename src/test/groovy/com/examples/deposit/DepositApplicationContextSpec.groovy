package com.examples.deposit

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class DepositApplicationContextSpec extends Specification {

	def "context loads"() {
		expect:
		true
	}

}
