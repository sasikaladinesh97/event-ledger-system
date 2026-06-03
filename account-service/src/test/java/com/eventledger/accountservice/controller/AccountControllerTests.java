package com.eventledger.accountservice.controller;

import com.eventledger.accountservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AccountControllerTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldApplyTransactionAndComputeBalance() throws Exception {
        String transactionJson = "{\"eventId\":\"tx-1\",\"accountId\":\"acct-1\",\"amount\":100.0,\"type\":\"CREDIT\",\"eventTimestamp\":\"2026-01-01T12:00:00Z\",\"metadata\":{\"source\":\"tests\"}}";

        mockMvc.perform(post("/accounts/acct-1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson))
                .andExpect(status().isOk())
                .andExpect(content().json(transactionJson));

        mockMvc.perform(get("/accounts/acct-1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("100.0"));
    }

    @Test
    void shouldRejectInvalidTransactionRequest() throws Exception {
        String invalidJson = "{\"eventId\":\"tx-2\",\"accountId\":\"acct-1\",\"amount\":-20.0,\"type\":\"DEBIT\",\"eventTimestamp\":\"2026-01-01T12:00:00Z\"}";

        mockMvc.perform(post("/accounts/acct-1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnTransactionsOrderedByTimestamp() throws Exception {
        String first = "{\"eventId\":\"tx-10\",\"accountId\":\"acct-2\",\"amount\":50.0,\"type\":\"CREDIT\",\"eventTimestamp\":\"2026-01-04T15:00:00Z\",\"metadata\":{\"source\":\"tests\"}}";
        String second = "{\"eventId\":\"tx-11\",\"accountId\":\"acct-2\",\"amount\":20.0,\"type\":\"DEBIT\",\"eventTimestamp\":\"2026-01-04T16:00:00Z\",\"metadata\":{\"source\":\"tests\"}}";
        String third = "{\"eventId\":\"tx-12\",\"accountId\":\"acct-2\",\"amount\":30.0,\"type\":\"CREDIT\",\"eventTimestamp\":\"2026-01-04T14:00:00Z\",\"metadata\":{\"source\":\"tests\"}}";

        mockMvc.perform(post("/accounts/acct-2/transactions").contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isOk());
        mockMvc.perform(post("/accounts/acct-2/transactions").contentType(MediaType.APPLICATION_JSON).content(second)).andExpect(status().isOk());
        mockMvc.perform(post("/accounts/acct-2/transactions").contentType(MediaType.APPLICATION_JSON).content(third)).andExpect(status().isOk());

        String expectedResponse = "[" + third + "," + first + "," + second + "]";

        mockMvc.perform(get("/accounts/acct-2"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    void shouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/accounts/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account Service is healthy"));
    }
}
