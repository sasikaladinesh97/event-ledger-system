package com.eventledger.event_gateway.controller;

import com.eventledger.event_gateway.entity.Event;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.accountService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.accountService.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.accountService.failure-rate-threshold=50"
})
class EventControllerTests {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        private CircuitBreakerRegistry circuitBreakerRegistry;

        private MockRestServiceServer mockServer;

        @BeforeEach
        void setUp() {
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountService");
                circuitBreaker.reset();
                this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                this.mockServer = MockRestServiceServer.createServer(restTemplate);
        }

    @Test
    void shouldRejectInvalidEventPayload() throws Exception {
        String eventJson = "{\"eventId\":\"evt-invalid\",\"accountId\":\"acct-1\",\"amount\":0," +
                "\"type\":\"CREDIT\",\"eventTimestamp\":\"2026-01-01T00:00:00Z\"}";

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPropagateTraceHeaderToAccountService() throws Exception {
        mockServer.expect(requestTo(startsWith("http://account-service:8081/accounts/")))
                .andExpect(header("X-Trace-Id", notNullValue()))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        String eventJson = buildEventJson("evt-1", "acct-1", 10.0, "CREDIT", "2026-01-01T00:00:00Z");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isOk());

        mockServer.verify();
    }

    @Test
    void shouldFallbackWhenAccountServiceIsUnavailableAndOpenCircuit() throws Exception {
        // Simulate two server errors to trigger circuit breaker failures
        mockServer.expect(requestTo(startsWith("http://account-service:8081/accounts/")))
                .andRespond(withServerError());
        mockServer.expect(requestTo(startsWith("http://account-service:8081/accounts/")))
                .andRespond(withServerError());

        String eventJson = buildEventJson("evt-2", "acct-1", 55.0, "DEBIT", "2026-01-02T00:00:00Z");

        // First two attempts should get server error -> ServiceUnavailable
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(eventJson))
                    .andExpect(status().isServiceUnavailable());
        }

        // Third attempt should be short-circuited by circuit breaker (no server call)
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isServiceUnavailable());

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountService");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        mockServer.verify();
    }

    private String buildEventJson(String eventId, String accountId, double amount, String type, String timestamp) {
        return "{\"eventId\":\"" + eventId + "\"," +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":" + amount + "," +
                "\"type\":\"" + type + "\"," +
                "\"eventTimestamp\":\"" + timestamp + "\"," +
                "\"currency\":\"USD\"," +
                "\"metadata\":{\"source\":\"tests\"}}";
    }
}
