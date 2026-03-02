package com.examples.deposit.mapper

import java.math.BigDecimal

import com.examples.deposit.domain.aggregate.Account
import com.examples.deposit.domain.constant.TransactionType
import com.examples.deposit.domain.entity.Transaction
import spock.lang.Specification

class AccountMapperSpec extends Specification {

	def "toAccountResponse maps all fields"() {
		given:
		def account = Account.create("Alice", "ACCT-MAP-001")
		account.credit(new BigDecimal("10.0000"))

		when:
		def response = AccountMapper.toResponse(account)

		then:
		response.id() == account.getId()
		response.accountNumber() == account.getAccountNumber()
		response.ownerName() == account.getOwnerName()
		response.balance().compareTo(account.getBalance()) == 0
		response.status() == account.getStatus().name()
		response.createdAt() == account.getCreatedAt()
		response.updatedAt() == account.getUpdatedAt()
	}

	def "toTransactionResponse maps all fields"() {
		given:
		def account = Account.create("Alice", "ACCT-MAP-002")
		def transaction = Transaction.create(account, TransactionType.CREDIT, new BigDecimal("20.0000"),
				new BigDecimal("20.0000"))

		when:
		def response = AccountMapper.toResponse(transaction)

		then:
		response.id() == transaction.getId()
		response.type() == transaction.getType().name()
		response.amount().compareTo(transaction.getAmount()) == 0
		response.balanceAfter().compareTo(transaction.getBalanceAfter()) == 0
		response.createdAt() == transaction.getCreatedAt()
	}

}