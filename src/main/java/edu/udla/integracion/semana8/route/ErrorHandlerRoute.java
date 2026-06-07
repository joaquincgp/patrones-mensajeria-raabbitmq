package edu.udla.integracion.semana8.route;

import edu.udla.integracion.semana8.model.InvalidMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.consumers.error-handler.enabled", havingValue = "true", matchIfMissing = true)
public class ErrorHandlerRoute extends RouteBuilder {

    private static final String INVALID_CONSUMER_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.invalid-exchange}}"
            + "?queues={{app.rabbitmq.invalid-queue}}"
            + "&routingKey=invalid.message"
            + "&exchangeType=direct"
            + "&autoDeclare=false";

    @Override
    public void configure() {
        from(INVALID_CONSUMER_ENDPOINT)
                .routeId("error-handler-consumer")
                .unmarshal().json(JsonLibrary.Jackson, InvalidMessage.class)
                .log("[ERROR-HANDLER] Mensaje invalido recibido. reason=${body.reason}, originalMessageType=${body.originalMessageType}");
    }
}
