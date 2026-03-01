package com.examples.deposit.controller;

import com.examples.deposit.controller.admin.AccountAdminController;
import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.exception.GlobalExceptionHandler;
import com.examples.deposit.exception.InvalidAccountStateException;
import com.examples.deposit.service.account.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountAdminController.class)
@Import(GlobalExceptionHandler.class)
class AccountAdminControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@Test
	void freeze_returns200() throws Exception {
		Account account = Account.create("Alice", "ACCT-ADMIN-001");
		account.freeze();
		when(this.accountService.freezeAccount("ACCT-ADMIN-001")).thenReturn(account);

		this.mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-001/freeze"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FROZEN"));
	}

	@Test
	void unfreeze_returns200() throws Exception {
		Account account = Account.create("Alice", "ACCT-ADMIN-002");
		account.freeze();
		account.unfreeze();
		when(this.accountService.unfreezeAccount("ACCT-ADMIN-002")).thenReturn(account);

		this.mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-002/unfreeze"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void close_returns200() throws Exception {
		Account account = Account.create("Alice", "ACCT-ADMIN-003");
		account.close();
		when(this.accountService.closeAccount("ACCT-ADMIN-003")).thenReturn(account);

		this.mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-003/close"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CLOSED"));
	}

	@Test
	void freeze_returns409ForInvalidState() throws Exception {
		when(this.accountService.freezeAccount("ACCT-ADMIN-004"))
			.thenThrow(new InvalidAccountStateException("Only active accounts can be frozen"));

		this.mockMvc.perform(post("/api/admin/accounts/ACCT-ADMIN-004/freeze"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.title").value("Invalid Account State"));
	}

}