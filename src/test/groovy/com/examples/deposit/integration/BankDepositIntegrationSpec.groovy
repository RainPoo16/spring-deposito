package com.examples.deposit.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BankDepositIntegrationSpec extends Specification {

	@Autowired
	private MockMvc mockMvc

	def "full account lifecycle flow"() {
		given:
		def accountNumber = "ACCT-E2E-FLOW-001"

		when:
		def openResult = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("""
				{"ownerName":"Alice Integration","accountNumber":"$accountNumber"}
				"""))

		then:
		openResult.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('ACTIVE'))
			.andExpect(jsonPath('$.balance').value(0.0d))

		when:
		def creditResult = mockMvc.perform(post("/api/accounts/{accountNumber}/credit", accountNumber)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount":1000.0000}
					"""))

		then:
		creditResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.balanceAfter').value(1000.0d))

		when:
		def debitResult = mockMvc.perform(post("/api/accounts/{accountNumber}/debit", accountNumber)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount":300.0000}
					"""))

		then:
		debitResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.balanceAfter').value(700.0d))

		when:
		def freezeResult = mockMvc.perform(post("/api/admin/accounts/{accountNumber}/freeze", accountNumber))

		then:
		freezeResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('FROZEN'))

		when:
		def unfreezeResult = mockMvc.perform(post("/api/admin/accounts/{accountNumber}/unfreeze", accountNumber))

		then:
		unfreezeResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('ACTIVE'))

		when:
		def finalDebitResult = mockMvc.perform(post("/api/accounts/{accountNumber}/debit", accountNumber)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount":700.0000}
					"""))

		then:
		finalDebitResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.balanceAfter').value(0.0d))

		when:
		def closeResult = mockMvc.perform(post("/api/admin/accounts/{accountNumber}/close", accountNumber))

		then:
		closeResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('CLOSED'))
	}

	def "negative flows"() {
		given:
		def overdrawAccountNumber = "ACCT-E2E-NEG-001"

		when:
		def createOverdrawAccountResult = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"ownerName":"Bob Integration","accountNumber":"$overdrawAccountNumber"}
					"""))

		then:
		createOverdrawAccountResult.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

		when:
		def creditOverdrawAccountResult = mockMvc.perform(post("/api/accounts/{accountNumber}/credit", overdrawAccountNumber)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount":500.0000}
					"""))

		then:
		creditOverdrawAccountResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

		when:
		def overdrawResult = mockMvc.perform(post("/api/accounts/{accountNumber}/debit", overdrawAccountNumber)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount":700.0000}
					"""))

		then:
		overdrawResult.andExpect(status().isUnprocessableContent())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))

		when:
		def closedAccountNumber = "ACCT-E2E-NEG-002"
		def createClosedAccountResult = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"ownerName":"Carol Integration","accountNumber":"$closedAccountNumber"}
					"""))

		then:
		createClosedAccountResult.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

		when:
		def closeAccountResult = mockMvc.perform(post("/api/admin/accounts/{accountNumber}/close", closedAccountNumber))

		then:
		closeAccountResult.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

		when:
		def creditClosedAccountResult = mockMvc.perform(post("/api/accounts/{accountNumber}/credit", closedAccountNumber)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount":10.0000}
					"""))

		then:
		creditClosedAccountResult.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))

		when:
		def missingAccountResult = mockMvc.perform(get('/api/accounts/NONEXISTENT'))

		then:
		missingAccountResult.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
	}

}
