package edu.udla.integracion.semana8.model;

import java.math.BigDecimal;

public class OrderPayload {

    private String orderId;
    private String customerId;
    private BigDecimal total;

    public OrderPayload() {
    }

    public OrderPayload(String orderId, String customerId, BigDecimal total) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.total = total;
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
