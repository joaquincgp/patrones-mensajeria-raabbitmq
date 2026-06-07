package edu.udla.integracion.semana8.model;

public class OrderCreatedEvent {

    private String eventId;
    private String eventType;
    private String occurredAt;
    private String source;
    private OrderPayload payload;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(String eventId, String eventType, String occurredAt, String source, OrderPayload payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.source = source;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OrderPayload getPayload() {
        return payload;
    }

    public void setPayload(OrderPayload payload) {
        this.payload = payload;
    }
}
