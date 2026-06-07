# Semana 8 - Patrones basicos de mensajeria con RabbitMQ

Sistema academico para la materia de Integración de Sistemas desarrollado en Java 17, Spring Boot 3.3.5, Apache Camel 4.8.0 y RabbitMQ 3.13 Management para demostrar patrones basicos de mensajeria en una tienda en linea.

## Patrones implementados

- Point-to-Point: `GenerarFactura` se envia a `billing.queue` y lo procesa un consumidor logico de facturacion.
- Publish/Subscribe: `PedidoCreado` se publica en `orders.exchange` y llega a `notification.queue` y `analytics.queue`.
- Command Message: `BillingCommand` con `messageType=GenerarFactura`.
- Event Message: `OrderCreatedEvent` con `eventType=PedidoCreado`.
- Invalid Message Channel: mensajes invalidos a `invalid-message.queue`.
- Manejo basico de errores: `onException(Exception.class)` en rutas productoras.

## Nota de compatibilidad Camel 4

El taller menciona URIs `rabbitmq:` y `camel-rabbitmq-starter`. En Apache Camel 4.x el starter compatible disponible para Spring Boot es `camel-spring-rabbitmq-starter`, por eso este proyecto usa URIs `spring-rabbitmq:`. Se mantienen las mismas entidades RabbitMQ, exchange types, colas, routing keys y comportamiento solicitado.

## Ejecucion rapida

1. Levantar RabbitMQ:

```bash
docker compose up -d
```

2. Abrir RabbitMQ Management:

```text
http://localhost:15672
usuario: admin
clave: admin123
```

3. Ejecutar la aplicacion:

```bash
mvn spring-boot:run
```

4. Probar el flujo completo:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-1001",
    "customerId": "CLI-2001",
    "total": 59.90
  }'
```

5. Detener todo:

```bash
docker compose down
```

## Configuracion util para evidencias

Para dejar mensajes invalidos visibles en `invalid-message.queue`, ejecutar la app con:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.consumers.error-handler.enabled=false"
```

Para demostrar dos consumidores Point-to-Point compitiendo por `billing.queue`:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.consumers.billing.second-instance-enabled=true"
```
