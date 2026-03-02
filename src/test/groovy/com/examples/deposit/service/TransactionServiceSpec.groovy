package com.examples.deposit.service

import java.math.BigDecimal
import java.util.Optional

import com.examples.deposit.domain.aggregate.Account
import com.examples.deposit.domain.constant.TransactionType
import com.examples.deposit.exception.InsufficientBalanceException
import com.examples.deposit.exception.InvalidAccountStateException
import com.examples.deposit.repository.AccountRepository
import com.examples.deposit.repository.TransactionRepository
import com.examples.deposit.service.transaction.TransactionService
import spock.lang.Specification

class TransactionServiceSpec extends Specification {

	private AccountRepository accountRepository = Mock()

	private TransactionRepository transactionRepository = Mock()

	private TransactionService transactionService = new TransactionService(accountRepository, transactionRepository)

	def "credit adds to balance and records transaction"() {
		given:
		def account = Account.create("Alice", "ACCT-TX-SVC-001")
		accountRepository.findByAccountNumber("ACCT-TX-SVC-001") >> Optional.of(account)

		when:
		def transaction = transactionService.credit("ACCT-TX-SVC-001", new BigDecimal("100.0000"))

		then:
		account.balance.compareTo(new BigDecimal("100.0000")) == 0
		transaction.type == TransactionType.CREDIT
		transaction.amount.compareTo(new BigDecimal("100.0000")) == 0
		transaction.balanceAfter.compareTo(new BigDecimal("100.0000")) == 0
		1 * accountRepository.save(account) >> { Account saved -> saved }
		1 * transactionRepository.save(_ as com.examples.deposit.domain.entity.Transaction) >> {
			com.examples.deposit.domain.entity.Transaction saved -> saved
		}
	}

	def "credit rejects non-active account"() {
		given:
		def account = Account.create("Alice", "ACCT-TX-SVC-002")
		account.freeze()
		accountRepository.findByAccountNumber("ACCT-TX-SVC-002") >> Optional.of(account)

		when:
		transactionService.credit("ACCT-TX-SVC-002", new BigDecimal("10.0000"))

		then:
		thrown(InvalidAccountStateException)
	}

	def "credit rejects negative amount"() {
		given:
		def account = Account.create("Alice", "ACCT-TX-SVC-003")
		accountRepository.findByAccountNumber("ACCT-TX-SVC-003") >> Optional.of(account)

		when:
		transactionService.credit("ACCT-TX-SVC-003", new BigDecimal("-1.0000"))

		then:
		thrown(InvalidAccountStateException)
	}

	def "debit subtracts from balance and records transaction"() {
		given:
		def account = Account.create("Alice", "ACCT-TX-SVC-004")
		account.credit(new BigDecimal("100.0000"))
		accountRepository.findByAccountNumber("ACCT-TX-SVC-004") >> Optional.of(account)

		when:
		def transaction = transactionService.debit("ACCT-TX-SVC-004", new BigDecimal("40.0000"))

		then:
		account.balance.compareTo(new BigDecimal("60.0000")) == 0
		transaction.type == TransactionType.DEBIT
		transaction.amount.compareTo(new BigDecimal("40.0000")) == 0
		transaction.balanceAfter.compareTo(new BigDecimal("60.0000")) == 0
		1 * accountRepository.save(account) >> { Account saved -> saved }
		1 * transactionRepository.save(_ as com.examples.deposit.domain.entity.Transaction) >> {
			com.examples.deposit.domain.entity.Transaction saved -> saved
		}
	}

	def "debit rejects overdraw"() {
		given:
		def account = Account.create("Alice", "ACCT-TX-SVC-005")
		account.credit(new BigDecimal("100.0000"))
		accountRepository.findByAccountNumber("ACCT-TX-SVC-005") >> Optional.of(account)

		when:
		transactionService.debit("ACCT-TX-SVC-005", new BigDecimal("200.0000"))

		then:
		def ex = thrown(InsufficientBalanceException)
		ex.message == "Insufficient balance for debit of 200.0000"
	}

	def "debit rejects non-active account"() {
		given:
		def account = Account.create("Alice", "ACCT-TX-SVC-006")
		account.freeze()
		accountRepository.findByAccountNumber("ACCT-TX-SVC-006") >> Optional.of(account)

		when:
		transactionService.debit("ACCT-TX-SVC-006", new BigDecimal("10.0000"))

		then:
		thrown(InvalidAccountStateException)
	}

}