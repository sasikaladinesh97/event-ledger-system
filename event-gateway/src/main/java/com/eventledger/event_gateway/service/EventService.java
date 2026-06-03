package com.eventledger.event_gateway.service;

import com.eventledger.event_gateway.entity.Event;
import com.eventledger.event_gateway.repository.EventRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;

@Service
public class EventService {
    private final EventRepository repo;

    public EventService(EventRepository repo) {
        this.repo = repo;
    }

    public Event saveEvent(Event event) {
        Optional<Event> existing = repo.findById(event.getEventId());
        return existing.orElseGet(() -> repo.save(event));
    }

    public Optional<Event> getEvent(String id) {
        return repo.findById(id);
    }

    public List<Event> getEventsByAccount(String accountId) {
        return repo.findByAccountIdOrderByEventTimestampAsc(accountId);
    }
}
