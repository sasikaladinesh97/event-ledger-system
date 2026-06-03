package com.eventledger.event_gateway.entity;

import com.eventledger.event_gateway.util.JsonAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

@Entity
public class Event {
    @Id
    @NotBlank
    private String eventId;

    @NotBlank
    private String accountId;

    @Pattern(regexp = "CREDIT|DEBIT", message = "Type must be CREDIT or DEBIT")
    private String type;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private double amount;

    @NotBlank
    private String currency;

    @NotNull
    private Instant eventTimestamp;

    @Convert(converter = JsonAttributeConverter.class)
    @Column(columnDefinition = "CLOB")
    private Map<String, Object> metadata;

    // --- Getters and Setters ---
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
