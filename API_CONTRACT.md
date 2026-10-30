# Contrato API v1

Origen local: `http://localhost:8080`; producción pendiente. JSON keys y operaciones en inglés. Mensajes en español. Content-Type application/json. Sin tarjetas ni datos personales.

## Operaciones públicas

`POST /api/services/inventory/{operation}`
`POST /api/services/orders/{operation}`
`POST /api/services/payments/{operation}`

Cuerpo: `{"params":[]}` para listas; `{"params":[{...}]}` para operaciones con un objeto. Máximo un objeto y 32 KiB. Los IDs son textos no vacíos hasta 100 caracteres; cantidades enteras 1–100000. Los errores de negocio llevan código HTTP y requestId.

| Servicio | operation | params | response (dentro del sobre) |
| --- | --- | --- | --- |
| inventory | listProducts | [] | [{"productId":"SKU-001","stock":30}, ...] |
| inventory | getStock | [{"productId":"SKU-001"}] | {"productId":"SKU-001","stock":30} |
| inventory | addStock | [{"productId":"SKU-001","quantity":3}] | {"productId":"SKU-001","stock":33} |
| inventory | removeStock | [{"productId":"SKU-001","quantity":2}] | {"productId":"SKU-001","stock":31} |
| orders | createOrder | [{"productId":"SKU-001","quantity":2}] | {"orderId":"<UUID>","productId":"SKU-001","quantity":2,"createdAt":"<UTC>"} |
| orders | getOrder | [{"orderId":"<UUID devuelto>"}] | mismo pedido |
| orders | listOrders | [] | array de pedidos |
| payments | processPayment | [{"orderId":"DEMO-ORDER","amount":12.50}] | {"paymentId":"<UUID>","orderId":"DEMO-ORDER","amount":12.50,"status":"SUCCESS","createdAt":"<UTC>"} |
| payments | getPayment | [{"paymentId":"<UUID devuelto>"}] | mismo pago, salvo fallo aleatorio |

Ejemplos de valores iniciales; resultados de stock varían con operaciones previas. Pedidos no reservan inventario; pagos no verifican existencia remota de pedidos. Son operaciones académicas independientes. El importe usa BigDecimal positivo, dos decimales como máximo y hasta 100000000.

Éxito HTTP 200:

```json
{"requestId":"<UUID real generado por llamada>","response":{"productId":"SKU-001","stock":30}}
```

Fallo:

```json
{"requestId":"<UUID real>","errorCode":"INSUFFICIENT_STOCK","errorMessage":"Existencias insuficientes."}
```

| HTTP | Códigos |
| --- | --- |
| 400 | INVALID_INPUT, UNKNOWN_OPERATION |
| 404 | NOT_FOUND |
| 409 | INSUFFICIENT_STOCK, STOCK_LIMIT, SIMULATION_RUNNING |
| 413 | BODY_TOO_LARGE |
| 429 | SIMULATION_RATE_LIMIT |
| 500 | AUDIT_PERSISTENCE_FAILED, INTERNAL_ERROR |
| 502 | UPSTREAM_ERROR |
| 503 | SIMULATED_FAILURE, SERVICE_UNAVAILABLE |
| 504 | UPSTREAM_TIMEOUT |

No reintentar POST automáticamente. Un fallo de auditoría puede aparecer después de un cambio de negocio. La aplicación permanece en ejecución ante errores esperados.

## Métricas

`GET /api/metrics/summary` devuelve un array con tres objetos:

```json
[{"serviceId":"inventory","totalCalls":0,"successCount":0,"errorCount":0,"successRate":null,"errorRate":null,"averageDurationMs":null,"recentCalls":[]}]
```

El ejemplo muestra uno de los tres elementos por brevedad. recentCalls contiene hasta 20 logs por servicio, cronológicos. Tasas en porcentaje 0–100, duración en ms con precisión fraccionaria. Promedio incluye éxitos y errores. Siempre incluye inventory, orders y payments.

`GET /api/metrics/logs?service=inventory&status=SUCCESS&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z&page=0&size=20`

Todos los filtros son opcionales y combinables. Omitir filtros vacíos. Estados SUCCESS/ERROR; servicios inventory/orders/payments. Fechas ISO 8601 con zona UTC, límites **inclusivos**, from <= to. page >= 0, size 1–100, defecto 20. Respuesta:

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
```

Cada log tiene serviceId, operation, durationMs, status, requestId, timestamp, inputParams, response, errorCode, errorMessage y stackTraceSummary. Los tres últimos son null en éxito; response es null ante error. timestamp representa finalización, con Instant en UTC. Orden descendente estable por timestamp e ID interno (el ID interno no se expone). Un refresh puede mover registros entre páginas al llegar nuevas llamadas: es paginación por offset, no una instantánea congelada.

## Simulación

`POST /api/metrics/simulate-load` con `{}`. Respuesta síncrona:

```json
{"batchId":"<UUID>","requestedCalls":50,"completedCalls":50,"successCount":48,"errorCount":2,"startedAt":"<UTC>","finishedAt":"<UTC>"}
```

Los conteos del ejemplo son ilustrativos, no resultados prometidos. Pago falla con probabilidad 0.10 por llamada válida. Invocaciones secuenciales distribuidas 17/17/16. Una ejecución por instancia; cooldown de 10 s entre inicios. Health: `GET /actuator/health` en cada proceso. Polling no produce logs de negocio.

## Ejemplo Bash

```bash
curl -i http://localhost:8080/api/services/inventory/getStock -H 'Content-Type: application/json' -d '{"params":[{"productId":"SKU-001"}]}'
curl http://localhost:8080/api/metrics/summary
curl -X POST http://localhost:8080/api/metrics/simulate-load -H 'Content-Type: application/json' -d '{}'
```

## Ejemplo PowerShell

```powershell
$body = @{ params = @(@{ productId = 'SKU-001' }) } | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/services/inventory/getStock' -ContentType 'application/json' -Body $body
Invoke-RestMethod -Uri 'http://localhost:8080/api/metrics/summary'
```

## Protección entre servicios en la variante Free

/internal/** de los negocios requiere X-Internal-Service-Token en prod; falta o valor incorrecto devuelve 401 INTERNAL_UNAUTHORIZED. El gateway lo agrega internamente. El frontend continúa usando exclusivamente /api/** y nunca recibe ese secreto. Las rutas públicas del taller no cambian.
