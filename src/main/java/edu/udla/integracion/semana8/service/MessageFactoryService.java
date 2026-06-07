package edu.udla.integracion.semana8.service;

import edu.udla.integracion.semana8.model.BillingCommand;
import edu.udla.integracion.semana8.model.InvalidMessage;
import edu.udla.integracion.semana8.model.OrderCreatedEvent;
import edu.udla.integracion.semana8.model.OrderPayload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class MessageFactoryService {

    public BillingCommand buildBillingCommandFromPayload(OrderPayload payload) {
        return new BillingCommand(
                UUID.randomUUID().toString(),
                "GenerarFactura",
                payload.getOrderId(),
                payload.getCustomerId(),
                payload.getTotal()
        );
    }

    public OrderCreatedEvent buildOrderCreatedEventFromPayload(OrderPayload payload) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "PedidoCreado",
                Instant.now().toString(),
                "orders-api",
                new OrderPayload(payload.getOrderId(), payload.getCustomerId(), payload.getTotal())
        );
    }

    public InvalidMessage buildInvalidMessage(String reason, String originalPayload, String originalMessageType) {
        return new InvalidMessage(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                reason,
                originalPayload,
                originalMessageType
        );
    }
}
