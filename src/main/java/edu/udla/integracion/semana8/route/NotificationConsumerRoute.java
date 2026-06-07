package edu.udla.integracion.semana8.route;

import edu.udla.integracion.semana8.model.OrderCreatedEvent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.consumers.notification.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationConsumerRoute extends RouteBuilder {

    private static final String NOTIFICATION_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.orders-exchange}}"
            + "?queues={{app.rabbitmq.notification-queue}}"
            + "&exchangeType=fanout"
            + "&autoDeclare=false";

    @Override
    public void configure() {
        from(NOTIFICATION_ENDPOINT)
                .routeId("notification-service-consumer")
                .unmarshal().json(JsonLibrary.Jackson, OrderCreatedEvent.class)
                .log("[NOTIFICATION-SERVICE] Evento PedidoCreado recibido. Simulando notificacion para customerId=${body.payload.customerId}, orderId=${body.payload.orderId}");
    }
}
