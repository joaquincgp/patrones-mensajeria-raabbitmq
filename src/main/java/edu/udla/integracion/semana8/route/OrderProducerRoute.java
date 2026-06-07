package edu.udla.integracion.semana8.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udla.integracion.semana8.model.BillingCommand;
import edu.udla.integracion.semana8.model.InvalidMessage;
import edu.udla.integracion.semana8.model.OrderCreatedEvent;
import edu.udla.integracion.semana8.service.MessageFactoryService;
import edu.udla.integracion.semana8.service.MessageValidationService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderProducerRoute extends RouteBuilder {

    private static final String BILLING_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.billing-exchange}}"
            + "?queues={{app.rabbitmq.billing-queue}}"
            + "&routingKey=billing.generate"
            + "&exchangeType=direct"
            + "&autoDeclare=false";

    private static final String ORDERS_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.orders-exchange}}"
            + "?exchangeType=fanout"
            + "&autoDeclare=false";

    private static final String INVALID_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.invalid-exchange}}"
            + "?queues={{app.rabbitmq.invalid-queue}}"
            + "&routingKey=invalid.message"
            + "&exchangeType=direct"
            + "&autoDeclare=false";

    private final MessageValidationService validationService;
    private final MessageFactoryService factoryService;
    private final ObjectMapper objectMapper;

    public OrderProducerRoute(MessageValidationService validationService,
                              MessageFactoryService factoryService,
                              ObjectMapper objectMapper) {
        this.validationService = validationService;
        this.factoryService = factoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("[EXCEPTION] Error procesando mensaje: ${exception.message}")
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    Object originalBody = exchange.getIn().getBody();
                    InvalidMessage invalid = factoryService.buildInvalidMessage(
                            ex.getMessage(),
                            String.valueOf(originalBody),
                            "PROCESSING_ERROR"
                    );
                    exchange.getIn().setBody(invalid);
                })
                .to("direct:send-invalid-message");

        from("direct:validate-billing-command")
                .routeId("validate-billing-command")
                .process(exchange -> {
                    BillingCommand command = exchange.getIn().getBody(BillingCommand.class);
                    List<String> errors = validationService.validateBillingCommand(command);
                    exchange.setProperty("validationErrors", errors);
                    exchange.setProperty("isInvalid", !errors.isEmpty());
                    if (!errors.isEmpty()) {
                        exchange.getIn().setBody(factoryService.buildInvalidMessage(
                                validationService.joinErrors(errors),
                                objectMapper.writeValueAsString(command),
                                command == null ? "UNKNOWN" : command.getMessageType()
                        ));
                    }
                })
                .choice()
                    .when(exchangeProperty("isInvalid").isEqualTo(true))
                        .to("direct:send-invalid-message")
                    .otherwise()
                        .to("direct:send-billing-command")
                .end();

        from("direct:send-billing-command")
                .routeId("send-billing-command")
                .process(exchange -> {
                    BillingCommand command = exchange.getIn().getBody(BillingCommand.class);
                    exchange.setProperty("orderId", command.getOrderId());
                })
                .marshal().json(JsonLibrary.Jackson)
                .to(BILLING_ENDPOINT)
                .log("[PRODUCER][P2P] Comando GenerarFactura enviado a billing.queue para orderId=${exchangeProperty.orderId}");

        from("direct:validate-order-created-event")
                .routeId("validate-order-created-event")
                .process(exchange -> {
                    OrderCreatedEvent event = exchange.getIn().getBody(OrderCreatedEvent.class);
                    List<String> errors = validationService.validateOrderCreatedEvent(event);
                    exchange.setProperty("validationErrors", errors);
                    exchange.setProperty("isInvalid", !errors.isEmpty());
                    if (!errors.isEmpty()) {
                        exchange.getIn().setBody(factoryService.buildInvalidMessage(
                                validationService.joinErrors(errors),
                                objectMapper.writeValueAsString(event),
                                event == null ? "UNKNOWN" : event.getEventType()
                        ));
                    }
                })
                .choice()
                    .when(exchangeProperty("isInvalid").isEqualTo(true))
                        .to("direct:send-invalid-message")
                    .otherwise()
                        .to("direct:publish-order-created-event")
                .end();

        from("direct:publish-order-created-event")
                .routeId("publish-order-created-event")
                .process(exchange -> {
                    OrderCreatedEvent event = exchange.getIn().getBody(OrderCreatedEvent.class);
                    exchange.setProperty("orderId", event.getPayload().getOrderId());
                })
                .marshal().json(JsonLibrary.Jackson)
                .to(ORDERS_ENDPOINT)
                .log("[PRODUCER][PUBSUB] Evento PedidoCreado publicado en orders.exchange para orderId=${exchangeProperty.orderId}");

        from("direct:send-invalid-message")
                .routeId("send-invalid-message")
                .process(exchange -> {
                    InvalidMessage invalid = exchange.getIn().getBody(InvalidMessage.class);
                    exchange.setProperty("invalidReason", invalid.getReason());
                })
                .marshal().json(JsonLibrary.Jackson)
                .to(INVALID_ENDPOINT)
                .log("[ERROR][INVALID] Mensaje enviado a invalid-message.queue. Motivo: ${exchangeProperty.invalidReason}");
    }
}
