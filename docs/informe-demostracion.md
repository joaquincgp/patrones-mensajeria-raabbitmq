# Informe y guia de demostracion

## 1. Objetivo

Implementar una integracion asincrona de pedidos para una tienda en linea usando Spring Boot, Apache Camel y RabbitMQ. El sistema demuestra Point-to-Point, Publish/Subscribe, Command Message, Event Message, Invalid Message Channel y manejo basico de errores.

## 2. Arquitectura

La aplicacion es un solo proyecto Spring Boot sin interfaz grafica, autenticacion, base de datos ni microservicios separados. Los servicios se simulan con rutas Apache Camel:

- `billing-service-consumer-1`: consume comandos desde `billing.queue`.
- `notification-service-consumer`: consume eventos desde `notification.queue`.
- `analytics-service-consumer`: consume eventos desde `analytics.queue`.
- `error-handler-consumer`: consume mensajes invalidos desde `invalid-message.queue`.

## 3. Entidades RabbitMQ

Exchanges:

- `billing.exchange`: direct, durable.
- `orders.exchange`: fanout, durable.
- `invalid.exchange`: direct, durable.

Queues:

- `billing.queue`: durable.
- `notification.queue`: durable.
- `analytics.queue`: durable.
- `invalid-message.queue`: durable.

Bindings:

- `billing.exchange` -> `billing.queue`, routing key `billing.generate`.
- `orders.exchange` -> `notification.queue`.
- `orders.exchange` -> `analytics.queue`.
- `invalid.exchange` -> `invalid-message.queue`, routing key `invalid.message`.

## 4. Justificacion de patrones

Point-to-Point se demuestra con `billing.queue`: cada comando `GenerarFactura` debe ser procesado por una sola instancia logica de facturacion. Si se activa una segunda instancia, los mensajes se reparten entre consumidores y no se duplican.

Publish/Subscribe se demuestra con `orders.exchange` de tipo fanout: un mismo evento `PedidoCreado` se distribuye a `notification.queue` y `analytics.queue`.

Command Message se representa con `BillingCommand`, cuyo `messageType` obligatorio es `GenerarFactura`.

Event Message se representa con `OrderCreatedEvent`, cuyo `eventType` obligatorio es `PedidoCreado`.

Invalid Message Channel se representa con `invalid.exchange` y `invalid-message.queue`, donde se envian payloads incompletos, con tipos incorrectos o JSON mal formado.

## 5. Preparacion

Requisitos:

- Java 17.
- Maven.
- Docker Desktop activo.

Compilar:

```bash
mvn -DskipTests package
```

Levantar RabbitMQ:

```bash
docker compose up -d
```

Validar consola web:

```text
http://localhost:15672
usuario: admin
clave: admin123
```

Ejecutar la aplicacion:

```bash
mvn spring-boot:run
```

## 6. Demostracion 1: flujo completo de pedido

Comando:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-1001",
    "customerId": "CLI-2001",
    "total": 59.90
  }'
```

Resultado esperado:

- HTTP `202`.
- Se genera un `BillingCommand`.
- Se genera un `OrderCreatedEvent`.
- El comando se envia a `billing.exchange` y llega a `billing.queue`.
- El evento se publica en `orders.exchange` y llega a `notification.queue` y `analytics.queue`.

Logs esperados:

```text
[PRODUCER][P2P] Comando GenerarFactura enviado a billing.queue para orderId=ORD-1001
[PRODUCER][PUBSUB] Evento PedidoCreado publicado en orders.exchange para orderId=ORD-1001
[BILLING-SERVICE] Procesando GenerarFactura: orderId=ORD-1001, customerId=CLI-2001, total=59.90
[NOTIFICATION-SERVICE] Evento PedidoCreado recibido. Simulando notificacion para customerId=CLI-2001, orderId=ORD-1001
[ANALYTICS-SERVICE] Evento PedidoCreado recibido. Registrando metrica para orderId=ORD-1001, total=59.90
```

## 7. Demostracion 2: Command Message valido

```bash
curl -i -X POST http://localhost:8080/api/test/billing-command \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "msg-001",
    "messageType": "GenerarFactura",
    "orderId": "ORD-1001",
    "customerId": "CLI-2001",
    "total": 59.90
  }'
```

Resultado esperado:

- HTTP `202`.
- `billing-service` consume el mensaje desde `billing.queue`.

## 8. Demostracion 3: Event Message valido

```bash
curl -i -X POST http://localhost:8080/api/test/order-created-event \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "eventType": "PedidoCreado",
    "occurredAt": "2026-05-26T10:30:00Z",
    "source": "orders-api",
    "payload": {
      "orderId": "ORD-1001",
      "customerId": "CLI-2001",
      "total": 59.90
    }
  }'
```

Resultado esperado:

- HTTP `202`.
- `notification-service` y `analytics-service` reciben el mismo evento.

## 9. Demostracion 4: mensaje invalido sin orderId

```bash
curl -i -X POST http://localhost:8080/api/test/billing-command \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "msg-002",
    "messageType": "GenerarFactura",
    "customerId": "CLI-2001",
    "total": 59.90
  }'
```

Resultado esperado:

- HTTP `400`.
- No se procesa en `billing-service`.
- Se envia un `InvalidMessage` a `invalid-message.queue`.

Logs esperados:

```text
[ERROR][INVALID] Mensaje enviado a invalid-message.queue. Motivo: orderId es obligatorio
[ERROR-HANDLER] Mensaje invalido recibido. reason=orderId es obligatorio, originalMessageType=GenerarFactura
```

## 10. Demostracion 5: total invalido

```bash
curl -i -X POST http://localhost:8080/api/test/billing-command \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "msg-003",
    "messageType": "GenerarFactura",
    "orderId": "ORD-1002",
    "customerId": "CLI-2002",
    "total": 0
  }'
```

Resultado esperado:

- HTTP `400`.
- Se envia a `invalid-message.queue`.
- No aparece log de procesamiento en `billing-service`.

## 11. Demostracion 6: JSON mal formado

Usar `text/plain` para evitar que Spring rechace el cuerpo antes de llegar al controlador:

```bash
curl -i -X POST http://localhost:8080/api/test/raw \
  -H "Content-Type: text/plain" \
  -d '{
    "messageId": "msg-004",
    "messageType": "GenerarFactura",
    "orderId": "ORD-1003",
    "customerId": "CLI-2003",
    "total":
  }'
```

Resultado esperado:

- HTTP `400`.
- Se crea `InvalidMessage` con `originalMessageType=MALFORMED_JSON`.
- Se envia a `invalid-message.queue`.

## 12. Evidencia opcional con RabbitMQ Management API

Consultar colas:

```bash
curl -u admin:admin123 http://localhost:15672/api/queues/%2F
```

Consultar bindings:

```bash
curl -u admin:admin123 http://localhost:15672/api/bindings/%2F
```

Consultar una cola especifica:

```bash
curl -u admin:admin123 http://localhost:15672/api/queues/%2F/invalid-message.queue
```

## 13. Evidencia dejando invalidos visibles en RabbitMQ

Detener la aplicacion y volver a ejecutarla con el consumidor de errores desactivado:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.consumers.error-handler.enabled=false"
```

Enviar un mensaje invalido y revisar `invalid-message.queue` en la consola web. El mensaje quedara acumulado porque `error-handler-consumer` no estara activo.

## 14. Evidencia Point-to-Point con dos consumidores

Detener la aplicacion y volver a ejecutarla con una segunda instancia logica de facturacion:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.consumers.billing.second-instance-enabled=true"
```

Enviar varias veces el comando valido del punto 7. En consola deben alternarse logs entre:

```text
[BILLING-SERVICE] Procesando GenerarFactura: orderId=...
[BILLING-SERVICE-2] Procesando GenerarFactura: orderId=...
```

Esto demuestra que dos consumidores conectados a la misma cola compiten por mensajes: un mensaje se entrega a una sola instancia.

## 15. Apagado

Detener la aplicacion con `Ctrl+C`.

Detener RabbitMQ:

```bash
docker compose down
```

Si se necesita reiniciar completamente las colas y definiciones:

```bash
docker compose down -v
docker compose up -d
```
