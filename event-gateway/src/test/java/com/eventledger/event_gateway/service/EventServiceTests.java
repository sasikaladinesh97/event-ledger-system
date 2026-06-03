package com.eventledger.event_gateway.service;

import com.eventledger.event_gateway.entity.Event;
import com.eventledger.event_gateway.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EventServiceTests {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @Test
    void saveEventIsIdempotent() {
        Event event = buildEvent("evt-1", "acct-1", 100.0, "CREDIT", Instant.parse("2026-01-01T12:00:00Z"));

        Event firstSave = eventService.saveEvent(event);
        Event secondSave = eventService.saveEvent(event);

        assertThat(firstSave.getEventId()).isEqualTo("evt-1");
        assertThat(secondSave.getEventId()).isEqualTo("evt-1");
        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    void getEventsByAccountReturnsEventsOrderedByTimestamp() {
        Event older = buildEvent("evt-1", "acct-1", 50.0, "CREDIT", Instant.parse("2026-01-01T10:00:00Z"));
        Event newer = buildEvent("evt-2", "acct-1", 25.0, "DEBIT", Instant.parse("2026-01-01T12:00:00Z"));
        Event middle = buildEvent("evt-3", "acct-1", 75.0, "CREDIT", Instant.parse("2026-01-01T11:00:00Z"));

        eventService.saveEvent(newer);
        eventService.saveEvent(older);
        eventService.saveEvent(middle);

        List<Event> events = eventService.getEventsByAccount("acct-1");

        assertThat(events).extracting(Event::getEventId)
                .containsExactly("evt-1", "evt-3", "evt-2");
    }

    private Event buildEvent(String eventId, String accountId, double amount, String type, Instant timestamp) {
        Event event = new Event();
        event.setEventId(eventId);
        event.setAccountId(accountId);
        event.setAmount(amount);
        event.setType(type);
        event.setEventTimestamp(timestamp);
        event.setCurrency("USD");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "tests");
        event.setMetadata(metadata);
        return event;
    }
}
