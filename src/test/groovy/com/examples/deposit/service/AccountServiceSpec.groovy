package com.examples.deposit.service

import java.math.BigDecimal
import java.util.Optional

import com.examples.deposit.domain.aggregate.Account
import com.examples.deposit.domain.constant.AccountStatus
import com.examples.deposit.exception.AccountNotFoundException
import com.examples.deposit.exception.InvalidAccountStateException
import com.examples.deposit.repository.AccountRepository
import com.examples.deposit.service.account.AccountService
import spock.lang.Specification

class AccountServiceSpec extends Specification {

	private AccountRepository accountRepository = Mock()

	private AccountService accountService = new AccountService(accountRepository)

	def "openAccount creates active account with zero balance"() {
		when:
		def account = accountService.openAccount("Alice", "ACCT-SVC-001")

		then:
		account.status == AccountStatus.ACTIVE
		account.balance.compareTo(BigDecimal.ZERO.setScale(4)) == 0
		1 * accountRepository.save(_ as Account) >> { Account saved -> saved }
	}

	def "freezeAccount transitions active to frozen"() {
		given:
		def account = Account.create("Alice", "ACCT-SVC-002")
		accountRepository.findByAccountNumber("ACCT-SVC-002") >> Optional.of(account)

		when:
		def result = accountService.freezeAccount("ACCT-SVC-002")

		then:
		result.status == AccountStatus.FROZEN
		1 * accountRepository.save(account) >> { Account saved -> saved }
	}

	def "unfreezeAccount transitions frozen to active"() {
		given:
		def account = Account.create("Alice", "ACCT-SVC-003")
		account.freeze()
		accountRepository.findByAccountNumber("ACCT-SVC-003") >> Optional.of(account)

		when:
		def result = accountService.unfreezeAccount("ACCT-SVC-003")

		then:
		result.status == AccountStatus.ACTIVE
		1 * accountRepository.save(account) >> { Account saved -> saved }
	}

	def "closeAccount closes zero-balance active account"() {
		given:
		def account = Account.create("Alice", "ACCT-SVC-004")
		accountRepository.findByAccountNumber("ACCT-SVC-004") >> Optional.of(account)

		when:
		def result = accountService.closeAccount("ACCT-SVC-004")

		then:
		result.status == AccountStatus.CLOSED
		1 * accountRepository.save(account) >> { Account saved -> saved }
	}

	def "closeAccount rejects non-zero balance"() {
		given:
		def account = Account.create("Alice", "ACCT-SVC-005")
		account.credit(new BigDecimal("10.0000"))
		accountRepository.findByAccountNumber("ACCT-SVC-005") >> Optional.of(account)

		when:
		accountService.closeAccount("ACCT-SVC-005")

		then:
		thrown(InvalidAccountStateException)
	}

	def "getAccount throws when not found"() {
		given:
		accountRepository.findByAccountNumber("ACCT-NOT-FOUND") >> Optional.empty()

		when:
		accountService.getAccount("ACCT-NOT-FOUND")

		then:
		def ex = thrown(AccountNotFoundException)
		ex.message == "Account not found: ACCT-NOT-FOUND"
	}

}