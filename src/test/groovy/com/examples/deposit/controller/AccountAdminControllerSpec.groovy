package com.examples.deposit.controller

import com.examples.deposit.controller.admin.AccountAdminController
import com.examples.deposit.domain.aggregate.Account
import com.examples.deposit.exception.GlobalExceptionHandler
import com.examples.deposit.exception.InvalidAccountStateException
import com.examples.deposit.service.account.AccountService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AccountAdminController)
@Import(GlobalExceptionHandler)
class AccountAdminControllerSpec extends Specification {

	@Autowired
	private MockMvc mockMvc

	@SpringBean
	private AccountService accountService = Mock()

	def "freeze returns 200"() {
		when:
		def result = mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-001/freeze"))

		then:
		1 * accountService.freezeAccount("ACCT-ADMIN-001") >> {
			def account = Account.create("Alice", "ACCT-ADMIN-001")
			account.freeze()
			account
		}
		result
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('FROZEN'))
		0 * _
	}

	def "unfreeze returns 200"() {
		when:
		def result = mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-002/unfreeze"))

		then:
		1 * accountService.unfreezeAccount("ACCT-ADMIN-002") >> {
			def account = Account.create("Alice", "ACCT-ADMIN-002")
			account.freeze()
			account.unfreeze()
			account
		}
		result
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('ACTIVE'))
		0 * _
	}

	def "close returns 200"() {
		when:
		def result = mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-003/close"))

		then:
		1 * accountService.closeAccount("ACCT-ADMIN-003") >> {
			def account = Account.create("Alice", "ACCT-ADMIN-003")
			account.close()
			account
		}
		result
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath('$.status').value('CLOSED'))
		0 * _
	}

	def "freeze returns 409 for invalid state"() {
		when:
		def result = mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-004/freeze"))

		then:
		1 * accountService.freezeAccount("ACCT-ADMIN-004") >> {
			throw new InvalidAccountStateException("Only active accounts can be frozen")
		}
		result
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath('$.title').value('Invalid Account State'))
		0 * _
	}

}