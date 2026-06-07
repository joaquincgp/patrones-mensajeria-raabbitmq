package edu.udla.integracion.semana8.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udla.integracion.semana8.model.ApiResponse;
import edu.udla.integracion.semana8.model.BillingCommand;
import edu.udla.integracion.semana8.model.InvalidMessage;
import edu.udla.integracion.semana8.model.OrderCreatedEvent;
import edu.udla.integracion.semana8.model.OrderPayload;
import edu.udla.integracion.semana8.service.MessageFactoryService;
import edu.udla.integracion.semana8.service.MessageValidationService;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final ProducerTemplate producerTemplate;
    private final MessageValidationService validationService;
    private final MessageFactoryService factoryService;
    private final ObjectMapper objectMapper;

    public OrderController(ProducerTemplate producerTemplate,
                           MessageValidationService validationService,
                           MessageFactoryService factoryService,
                           ObjectMapper objectMapper) {
        this.producerTemplate = producerTemplate;
        this.validationService = validationService;
        this.factoryService = factoryService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse> createOrder(@RequestBody OrderPayload payload) {
        List<String> errors = validateCreateOrderPayload(payload);
        if (!errors.isEmpty()) {
            sendInvalidMessage(validationService.joinErrors(errors), toJson(payload), "OrderPayload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(
                    "REJECTED",
                    "Pedido invalido: " + validationService.joinErrors(errors),
                    "N/A"
            ));
        }

        BillingCommand billingCommand = factoryService.buildBillingCommandFromPayload(payload);
        OrderCreatedEvent orderCreatedEvent = factoryService.buildOrderCreatedEventFromPayload(payload);

        producerTemplate.sendBody("direct:send-billing-command", billingCommand);
        producerTemplate.sendBody("direct:publish-order-created-event", orderCreatedEvent);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse(
                "ACCEPTED",
                "Pedido recibido y mensajes enviados a las rutas de integracion",
                payload.getOrderId()
        ));
    }

    @PostMapping("/test/billing-command")
    public ResponseEntity<ApiResponse> sendBillingCommand(@RequestBody BillingCommand command) {
        List<String> errors = validationService.validateBillingCommand(command);
        producerTemplate.sendBody("direct:validate-billing-command", command);

        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(
                    "REJECTED",
                    "Comando invalido: " + validationService.joinErrors(errors),
                    command != null && command.getOrderId() != null ? command.getOrderId() : "N/A"
            ));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse(
                "ACCEPTED",
                "Comando GenerarFactura enviado a la ruta de validacion",
                command.getOrderId()
        ));
    }

    @PostMapping("/test/order-created-event")
    public ResponseEntity<ApiResponse> sendOrderCreatedEvent(@RequestBody OrderCreatedEvent event) {
        List<String> errors = validationService.validateOrderCreatedEvent(event);
        producerTemplate.sendBody("direct:validate-order-created-event", event);

        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(
                    "REJECTED",
                    "Evento invalido: " + validationService.joinErrors(errors),
                    event != null && event.getPayload() != null && event.getPayload().getOrderId() != null
                            ? event.getPayload().getOrderId()
                            : "N/A"
            ));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse(
                "ACCEPTED",
                "Evento PedidoCreado enviado a la ruta de validacion",
                event.getPayload().getOrderId()
        ));
    }

    @PostMapping(
            value = "/test/raw",
            consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<ApiResponse> sendRawJson(@RequestBody String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            if (root.has("messageType")) {
                BillingCommand command = objectMapper.treeToValue(root, BillingCommand.class);
                List<String> errors = validationService.validateBillingCommand(command);
                producerTemplate.sendBody("direct:validate-billing-command", command);
                return responseForRawValidation(errors, "Comando JSON crudo recibido", command.getOrderId());
            }

            if (root.has("eventType")) {
                OrderCreatedEvent event = objectMapper.treeToValue(root, OrderCreatedEvent.class);
                List<String> errors = validationService.validateOrderCreatedEvent(event);
                producerTemplate.sendBody("direct:validate-order-created-event", event);
                String orderId = event.getPayload() == null ? "N/A" : event.getPayload().getOrderId();
                return responseForRawValidation(errors, "Evento JSON crudo recibido", orderId);
            }

            sendInvalidMessage("No se pudo detectar si el JSON corresponde a comando o evento", rawPayload, "UNKNOWN");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(
                    "REJECTED",
                    "Mensaje invalido: no se pudo detectar si corresponde a comando o evento",
                    "N/A"
            ));
        } catch (JsonProcessingException ex) {
            sendInvalidMessage("JSON mal formado: " + ex.getOriginalMessage(), rawPayload, "MALFORMED_JSON");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(
                    "REJECTED",
                    "Mensaje invalido: JSON mal formado",
                    "N/A"
            ));
        }
    }

    private ResponseEntity<ApiResponse> responseForRawValidation(List<String> errors, String successMessage, String referenceId) {
        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(
                    "REJECTED",
                    "Mensaje invalido: " + validationService.joinErrors(errors),
                    referenceId == null ? "N/A" : referenceId
            ));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse(
                "ACCEPTED",
                successMessage,
                referenceId == null ? "N/A" : referenceId
        ));
    }

    private List<String> validateCreateOrderPayload(OrderPayload payload) {
        List<String> errors = new ArrayList<>();
        if (payload == null) {
            errors.add("pedido es obligatorio");
            return errors;
        }
        if (payload.getOrderId() == null || payload.getOrderId().trim().isEmpty()) {
            errors.add("orderId es obligatorio");
        }
        if (payload.getCustomerId() == null || payload.getCustomerId().trim().isEmpty()) {
            errors.add("customerId es obligatorio");
        }
        if (payload.getTotal() == null) {
            errors.add("total es obligatorio");
        } else if (!validationService.isPositive(payload.getTotal())) {
            errors.add("total debe ser mayor a 0");
        }
        return errors;
    }

    private void sendInvalidMessage(String reason, String originalPayload, String originalMessageType) {
        InvalidMessage invalidMessage = factoryService.buildInvalidMessage(reason, originalPayload, originalMessageType);
        producerTemplate.sendBody("direct:send-invalid-message", invalidMessage);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
