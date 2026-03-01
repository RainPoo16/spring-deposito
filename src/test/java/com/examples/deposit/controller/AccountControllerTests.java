package com.examples.deposit.controller;

import java.math.BigDecimal;

import com.examples.deposit.controller.account.AccountController;
import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.TransactionType;
import com.examples.deposit.domain.entity.Transaction;
import com.examples.deposit.exception.AccountNotFoundException;
import com.examples.deposit.exception.GlobalExceptionHandler;
import com.examples.deposit.service.account.AccountService;
import com.examples.deposit.service.transaction.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@MockitoBean
	private TransactionService transactionService;

	@Test
	void openAccount_returns201() throws Exception {
		Account account = Account.create("Alice", "ACCT-CTRL-001");
		when(this.accountService.openAccount("Alice", "ACCT-CTRL-001")).thenReturn(account);

		this.mockMvc
			.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"ownerName\":\"Alice\",\"accountNumber\":\"ACCT-CTRL-001\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accountNumber").value("ACCT-CTRL-001"))
			.andExpect(jsonPath("$.ownerName").value("Alice"));
	}

	@Test
	void openAccount_returns400ForBlankOwnerName() throws Exception {
		this.mockMvc
			.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"ownerName\":\"\",\"accountNumber\":\"ACCT-CTRL-002\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation Error"))
			.andExpect(jsonPath("$.errors.ownerName").value("Owner name is required"));
	}

	@Test
	void credit_returns200() throws Exception {
		Account account = Account.create("Alice", "ACCT-CTRL-003");
		Transaction transaction = Transaction.create(account, TransactionType.CREDIT, new BigDecimal("100.0000"),
				new BigDecimal("100.0000"));
		when(this.transactionService.credit("ACCT-CTRL-003", new BigDecimal("100.0000"))).thenReturn(transaction);

		this.mockMvc
			.perform(post("/api/accounts/ACCT-CTRL-003/credit").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":100.0000}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("CREDIT"));
	}

	@Test
	void debit_returns200() throws Exception {
		Account account = Account.create("Alice", "ACCT-CTRL-004");
		Transaction transaction = Transaction.create(account, TransactionType.DEBIT, new BigDecimal("40.0000"),
				new BigDecimal("60.0000"));
		when(this.transactionService.debit("ACCT-CTRL-004", new BigDecimal("40.0000"))).thenReturn(transaction);

		this.mockMvc
			.perform(post("/api/accounts/ACCT-CTRL-004/debit").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":40.0000}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("DEBIT"));
	}

	@Test
	void getAccount_returns200() throws Exception {
		Account account = Account.create("Alice", "ACCT-CTRL-005");
		when(this.accountService.getAccount("ACCT-CTRL-005")).thenReturn(account);

		this.mockMvc.perform(get("/api/accounts/ACCT-CTRL-005"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountNumber").value("ACCT-CTRL-005"));
	}

	@Test
	void getAccount_returns404WhenNotFound() throws Exception {
		when(this.accountService.getAccount("ACCT-MISSING")).thenThrow(new AccountNotFoundException("ACCT-MISSING"));

		this.mockMvc.perform(get("/api/accounts/ACCT-MISSING"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Account Not Found"));
	}

}