package com.examples.deposit.controller

import java.math.BigDecimal

import com.examples.deposit.controller.account.AccountController
import com.examples.deposit.domain.aggregate.Account
import com.examples.deposit.domain.constant.TransactionType
import com.examples.deposit.domain.entity.Transaction
import com.examples.deposit.exception.AccountNotFoundException
import com.examples.deposit.exception.GlobalExceptionHandler
import com.examples.deposit.service.account.AccountService
import com.examples.deposit.service.transaction.TransactionService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AccountController)
@Import(GlobalExceptionHandler)
class AccountControllerSpec extends Specification {

	@Autowired
	private MockMvc mockMvc

	@SpringBean
	private AccountService accountService = Mock()

	@SpringBean
	private TransactionService transactionService = Mock()

	def "openAccount returns 201"() {
		when:
		def result = mockMvc
			.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
				.content('{"ownerName":"Alice","accountNumber":"ACCT-CTRL-001"}'))

		then:
		1 * accountService.openAccount("Alice", "ACCT-CTRL-001") >> Account.create("Alice", "ACCT-CTRL-001")
		result.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.accountNumber').value('ACCT-CTRL-001'))
			.andExpect(jsonPath('$.ownerName').value('Alice'))
		0 * _
	}

	def "openAccount returns 400 for blank owner name"() {
		when:
		def result = mockMvc
			.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
				.content('{"ownerName":"","accountNumber":"ACCT-CTRL-002"}'))

		then:
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath('$.title').value('Validation Error'))
			.andExpect(jsonPath('$.errors.ownerName').value('Owner name is required'))

		0 * _
	}

	def "credit returns 200"() {
		when:
		def result = mockMvc
			.perform(post("/api/accounts/ACCT-CTRL-003/credit").contentType(MediaType.APPLICATION_JSON)
				.content('{"amount":100.0000}'))

		then:
		1 * transactionService.credit("ACCT-CTRL-003", new BigDecimal("100.0000")) >> {
			def account = Account.create("Alice", "ACCT-CTRL-003")
			Transaction.create(account, TransactionType.CREDIT, new BigDecimal("100.0000"), new BigDecimal("100.0000"))
		}
		result.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.type').value('CREDIT'))
		0 * _
	}

	def "debit returns 200"() {
		when:
		def result = mockMvc
			.perform(post("/api/accounts/ACCT-CTRL-004/debit").contentType(MediaType.APPLICATION_JSON)
				.content('{"amount":40.0000}'))

		then:
		1 * transactionService.debit("ACCT-CTRL-004", new BigDecimal("40.0000")) >> {
			def account = Account.create("Alice", "ACCT-CTRL-004")
			Transaction.create(account, TransactionType.DEBIT, new BigDecimal("40.0000"), new BigDecimal("60.0000"))
		}
		result.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.type').value('DEBIT'))
		0 * _
	}

	def "getAccount returns 200"() {
		when:
		def result = mockMvc.perform(get("/api/accounts/ACCT-CTRL-005"))

		then:
		1 * accountService.getAccount("ACCT-CTRL-005") >> Account.create("Alice", "ACCT-CTRL-005")
		result
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.accountNumber').value('ACCT-CTRL-005'))
		0 * _
	}

	def "getAccount returns 404 when not found"() {
		when:
		def result = mockMvc.perform(get("/api/accounts/ACCT-MISSING"))

		then:
		1 * accountService.getAccount("ACCT-MISSING") >> { throw new AccountNotFoundException("ACCT-MISSING") }
		result
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath('$.title').value('Account Not Found'))
		0 * _
	}

}