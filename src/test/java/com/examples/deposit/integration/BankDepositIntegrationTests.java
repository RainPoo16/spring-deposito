package com.examples.deposit.integration;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BankDepositIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullAccountLifecycleFlow() throws Exception {
		String accountNumber = "ACCT-E2E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		this.mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("""
				{"ownerName":"Alice Integration","accountNumber":"%s"}
				""".formatted(accountNumber)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.balance").value(0.0000));

		this.mockMvc
			.perform(post("/api/accounts/{accountNumber}/credit", accountNumber).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":1000.0000}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balanceAfter").value(1000.0000));

		this.mockMvc
			.perform(post("/api/accounts/{accountNumber}/debit", accountNumber).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":300.0000}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balanceAfter").value(700.0000));

		this.mockMvc.perform(post("/api/admin/accounts/{accountNumber}/freeze", accountNumber))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FROZEN"));

		this.mockMvc.perform(post("/api/admin/accounts/{accountNumber}/unfreeze", accountNumber))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		this.mockMvc
			.perform(post("/api/accounts/{accountNumber}/debit", accountNumber).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":700.0000}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balanceAfter").value(0.0000));

		this.mockMvc.perform(post("/api/admin/accounts/{accountNumber}/close", accountNumber))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CLOSED"));
	}

	@Test
	void negativeFlows() throws Exception {
		String overdrawAccountNumber = "ACCT-E2E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		this.mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("""
				{"ownerName":"Bob Integration","accountNumber":"%s"}
				""".formatted(overdrawAccountNumber))).andExpect(status().isCreated());

		this.mockMvc
			.perform(post("/api/accounts/{accountNumber}/credit", overdrawAccountNumber)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":500.0000}
						"""))
			.andExpect(status().isOk());

		this.mockMvc
			.perform(post("/api/accounts/{accountNumber}/debit", overdrawAccountNumber)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":700.0000}
						"""))
			.andExpect(status().isUnprocessableContent());

		String closedAccountNumber = "ACCT-E2E-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		this.mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("""
				{"ownerName":"Carol Integration","accountNumber":"%s"}
				""".formatted(closedAccountNumber))).andExpect(status().isCreated());

		this.mockMvc.perform(post("/api/admin/accounts/{accountNumber}/close", closedAccountNumber))
			.andExpect(status().isOk());

		this.mockMvc
			.perform(post("/api/accounts/{accountNumber}/credit", closedAccountNumber)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":10.0000}
						"""))
			.andExpect(status().isConflict());

		this.mockMvc.perform(get("/api/accounts/NONEXISTENT")).andExpect(status().isNotFound());
	}

}