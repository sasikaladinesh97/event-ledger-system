package com.eventledger.event_gateway.controller;

import com.eventledger.event_gateway.entity.Event;
import com.eventledger.event_gateway.service.EventService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventService service;
    private final RestTemplate restTemplate;
    private final String accountServiceUrl;
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    public EventController(EventService service, RestTemplate restTemplate,
                           @Value("${account.service.url:http://account-service:8081}") String accountServiceUrl) {
        this.service = service;
        this.restTemplate = restTemplate;
        this.accountServiceUrl = accountServiceUrl;
    }

    @PostMapping
    @CircuitBreaker(name = "accountService", fallbackMethod = "accountServiceFallback")
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event) {
        // Generate trace ID
        String traceId = UUID.randomUUID().toString();
        logger.info("traceId={} Received event {}", traceId, event.getEventId());

        Event saved = service.saveEvent(event);

        // Call Account Service with trace propagation
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-Id", traceId);
        HttpEntity<Event> request = new HttpEntity<>(event, headers);

        restTemplate.postForEntity(accountServiceUrl + "/accounts/" + event.getAccountId() + "/transactions",
                request, String.class);

        return ResponseEntity.ok(saved);
    }

    public ResponseEntity<Event> accountServiceFallback(Event event, Throwable t) {
        logger.error("Account Service unavailable, traceId={}, error={}", UUID.randomUUID(), t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(event);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable String id) {
        return service.getEvent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Event> getEventsByAccount(@RequestParam String account) {
        return service.getEventsByAccount(account);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway is healthy");
    }
}
