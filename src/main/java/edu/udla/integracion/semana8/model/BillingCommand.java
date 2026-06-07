package edu.udla.integracion.semana8.model;

import java.math.BigDecimal;

public class BillingCommand {

    private String messageId;
    private String messageType;
    private String orderId;
    private String customerId;
    private BigDecimal total;

    public BillingCommand() {
    }

    public BillingCommand(String messageId, String messageType, String orderId, String customerId, BigDecimal total) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.orderId = orderId;
        this.customerId = customerId;
        this.total = total;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
