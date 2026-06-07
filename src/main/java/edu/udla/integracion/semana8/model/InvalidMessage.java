package edu.udla.integracion.semana8.model;

public class InvalidMessage {

    private String errorId;
    private String occurredAt;
    private String reason;
    private String originalPayload;
    private String originalMessageType;

    public InvalidMessage() {
    }

    public InvalidMessage(String errorId, String occurredAt, String reason, String originalPayload, String originalMessageType) {
        this.errorId = errorId;
        this.occurredAt = occurredAt;
        this.reason = reason;
        this.originalPayload = originalPayload;
        this.originalMessageType = originalMessageType;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOriginalPayload() {
        return originalPayload;
    }

    public void setOriginalPayload(String originalPayload) {
        this.originalPayload = originalPayload;
    }

    public String getOriginalMessageType() {
        return originalMessageType;
    }

    public void setOriginalMessageType(String originalMessageType) {
        this.originalMessageType = originalMessageType;
    }
}
