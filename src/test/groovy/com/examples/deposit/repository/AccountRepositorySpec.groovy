package com.examples.deposit.repository

import java.math.BigDecimal

import com.examples.deposit.domain.aggregate.Account
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import spock.lang.Specification

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AccountRepositorySpec extends Specification {

	@Autowired
	private AccountRepository accountRepository

	@Autowired
	private EntityManager entityManager

	def "should persist account with UUID"() {
		given:
		def account = Account.create("Alice", "ACCT-001")

		when:
		def saved = accountRepository.saveAndFlush(account)

		then:
		saved.id != null
		accountRepository.findById(saved.id).present
	}

	def "should enforce unique account number"() {
		given:
		def first = Account.create("Alice", "ACCT-UNIQUE-001")
		def second = Account.create("Bob", "ACCT-UNIQUE-001")
		accountRepository.saveAndFlush(first)

		when:
		accountRepository.saveAndFlush(second)

		then:
		thrown(DataIntegrityViolationException)
	}

	def "should increment version on update"() {
		given:
		def account = Account.create("Alice", "ACCT-VER-001")
		def saved = accountRepository.saveAndFlush(account)
		def originalVersion = saved.version

		when:
		saved.credit(new BigDecimal("50.0000"))
		def updated = accountRepository.saveAndFlush(saved)

		then:
		updated.version != null
		updated.version > originalVersion
	}

	def "should throw on stale update"() {
		given:
		def account = Account.create("Alice", "ACCT-OPT-001")
		def saved = accountRepository.saveAndFlush(account)
		entityManager.clear()

		def firstReader = accountRepository.findById(saved.id).orElseThrow()
		entityManager.detach(firstReader)

		def secondReader = accountRepository.findById(saved.id).orElseThrow()
		entityManager.detach(secondReader)

		firstReader.credit(new BigDecimal("10.0000"))
		accountRepository.saveAndFlush(firstReader)

		when:
		secondReader.credit(new BigDecimal("5.0000"))
		accountRepository.saveAndFlush(secondReader)

		then:
		thrown(ObjectOptimisticLockingFailureException)
	}

}