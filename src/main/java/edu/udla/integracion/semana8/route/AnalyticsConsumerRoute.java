package edu.udla.integracion.semana8.route;

import edu.udla.integracion.semana8.model.OrderCreatedEvent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.consumers.analytics.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsConsumerRoute extends RouteBuilder {

    private static final String ANALYTICS_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.orders-exchange}}"
            + "?queues={{app.rabbitmq.analytics-queue}}"
            + "&exchangeType=fanout"
            + "&autoDeclare=false";

    @Override
    public void configure() {
        from(ANALYTICS_ENDPOINT)
                .routeId("analytics-service-consumer")
                .unmarshal().json(JsonLibrary.Jackson, OrderCreatedEvent.class)
                .log("[ANALYTICS-SERVICE] Evento PedidoCreado recibido. Registrando metrica para orderId=${body.payload.orderId}, total=${body.payload.total}");
    }
}
