package com.examples.deposit.repository

import java.math.BigDecimal

import com.examples.deposit.domain.aggregate.Account
import com.examples.deposit.domain.constant.TransactionType
import com.examples.deposit.domain.entity.Transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import spock.lang.Specification

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class TransactionRepositorySpec extends Specification {

	@Autowired
	private AccountRepository accountRepository

	@Autowired
	private TransactionRepository transactionRepository

	def "should persist transaction linked to account"() {
		given:
		def account = accountRepository.saveAndFlush(Account.create("Alice", "ACCT-TXN-001"))
		def transaction = Transaction.create(account, TransactionType.CREDIT, new BigDecimal("100.0000"),
				new BigDecimal("100.0000"))

		when:
		def saved = transactionRepository.saveAndFlush(transaction)

		then:
		saved.id != null
		saved.account.id == account.id
	}

	def "should find transactions by account id"() {
		given:
		def account = accountRepository.saveAndFlush(Account.create("Alice", "ACCT-TXN-002"))
		transactionRepository.saveAndFlush(
				Transaction.create(account, TransactionType.CREDIT, new BigDecimal("100.0000"), new BigDecimal("100.0000")))
		transactionRepository.saveAndFlush(
				Transaction.create(account, TransactionType.DEBIT, new BigDecimal("40.0000"), new BigDecimal("60.0000")))

		when:
		def transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.id)

		then:
		transactions.size() == 2
		transactions[0].createdAt >= transactions[1].createdAt
	}

}