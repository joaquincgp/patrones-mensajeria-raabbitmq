package edu.udla.integracion.semana8.route;

import edu.udla.integracion.semana8.model.BillingCommand;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.consumers.billing.enabled", havingValue = "true", matchIfMissing = true)
public class BillingConsumerRoute extends RouteBuilder {

    private static final String BILLING_CONSUMER_ENDPOINT = "spring-rabbitmq:{{app.rabbitmq.billing-exchange}}"
            + "?queues={{app.rabbitmq.billing-queue}}"
            + "&routingKey=billing.generate"
            + "&exchangeType=direct"
            + "&autoDeclare=false";

    private final boolean secondInstanceEnabled;

    public BillingConsumerRoute(@Value("${app.consumers.billing.second-instance-enabled:false}") boolean secondInstanceEnabled) {
        this.secondInstanceEnabled = secondInstanceEnabled;
    }

    @Override
    public void configure() {
        from(BILLING_CONSUMER_ENDPOINT)
                .routeId("billing-service-consumer-1")
                .unmarshal().json(JsonLibrary.Jackson, BillingCommand.class)
                .log("[BILLING-SERVICE] Procesando GenerarFactura: orderId=${body.orderId}, customerId=${body.customerId}, total=${body.total}");

        if (secondInstanceEnabled) {
            from(BILLING_CONSUMER_ENDPOINT)
                    .routeId("billing-service-consumer-2")
                    .unmarshal().json(JsonLibrary.Jackson, BillingCommand.class)
                    .log("[BILLING-SERVICE-2] Procesando GenerarFactura: orderId=${body.orderId}, customerId=${body.customerId}, total=${body.total}");
        }
    }
}
