package edu.udla.integracion.semana8.service;

import edu.udla.integracion.semana8.model.BillingCommand;
import edu.udla.integracion.semana8.model.OrderCreatedEvent;
import edu.udla.integracion.semana8.model.OrderPayload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageValidationService {

    public List<String> validateBillingCommand(BillingCommand command) {
        List<String> errors = new ArrayList<>();

        if (command == null) {
            errors.add("comando de facturacion es obligatorio");
            return errors;
        }

        requireText(command.getMessageId(), "messageId es obligatorio", errors);
        if (!hasText(command.getMessageType())) {
            errors.add("messageType es obligatorio");
        } else if (!"GenerarFactura".equals(command.getMessageType())) {
            errors.add("messageType debe ser GenerarFactura");
        }
        requireText(command.getOrderId(), "orderId es obligatorio", errors);
        requireText(command.getCustomerId(), "customerId es obligatorio", errors);
        if (command.getTotal() == null) {
            errors.add("total es obligatorio");
        } else if (!isPositive(command.getTotal())) {
            errors.add("total debe ser mayor a 0");
        }

        return errors;
    }

    public List<String> validateOrderCreatedEvent(OrderCreatedEvent event) {
        List<String> errors = new ArrayList<>();

        if (event == null) {
            errors.add("evento PedidoCreado es obligatorio");
            return errors;
        }

        requireText(event.getEventId(), "eventId es obligatorio", errors);
        if (!hasText(event.getEventType())) {
            errors.add("eventType es obligatorio");
        } else if (!"PedidoCreado".equals(event.getEventType())) {
            errors.add("eventType debe ser PedidoCreado");
        }
        requireText(event.getOccurredAt(), "occurredAt es obligatorio", errors);
        requireText(event.getSource(), "source es obligatorio", errors);
        validatePayload(event.getPayload(), errors);

        return errors;
    }

    public List<String> validateOrderPayload(OrderPayload payload) {
        List<String> errors = new ArrayList<>();
        validatePayload(payload, errors);
        return errors;
    }

    public boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public String joinErrors(List<String> errors) {
        return String.join("; ", errors);
    }

    private void validatePayload(OrderPayload payload, List<String> errors) {
        if (payload == null) {
            errors.add("payload es obligatorio");
            return;
        }

        requireText(payload.getOrderId(), "payload.orderId es obligatorio", errors);
        requireText(payload.getCustomerId(), "payload.customerId es obligatorio", errors);
        if (payload.getTotal() == null) {
            errors.add("payload.total es obligatorio");
        } else if (!isPositive(payload.getTotal())) {
            errors.add("payload.total debe ser mayor a 0");
        }
    }

    private void requireText(String value, String errorMessage, List<String> errors) {
        if (!hasText(value)) {
            errors.add(errorMessage);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
