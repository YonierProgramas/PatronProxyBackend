# Patrón Proxy

El contrato exacto está en `shared-contracts/src/main/java/edu/monitor/contract/MicroserviceProxy.java`:

```java
public interface MicroserviceProxy<T> {
    T execute(String operation, Object... params);
}
```

| Participante | Implementación |
| --- | --- |
| Subject | MicroserviceProxy<T> |
| Proxy | LoggingProxy<T>, recibe por composición un delegado del mismo contrato |
| Real Subject adaptado | HttpServiceClient, representa al servicio remoto y adapta el contrato común a HTTP |
| Negocio remoto real | InventoryService, OrderService, PaymentService en aplicaciones separadas |
| Client | ServiceGateway, usado por MonitorController y SimulationService |

```mermaid
classDiagram
 class MicroserviceProxy~T~ {
  <<interface>>
  +execute(String operation, Object... params) T
 }
 class LoggingProxy~T~ {
  -MicroserviceProxy~T~ delegate
  -AuditStore store
  +execute(String operation, Object... params) T
 }
 class HttpServiceClient {
  +execute(String operation, Object... params) Object
 }
 MicroserviceProxy <|.. LoggingProxy
 MicroserviceProxy <|.. HttpServiceClient
 LoggingProxy o--> MicroserviceProxy : delegate
 ServiceGateway --> LoggingProxy
 HttpServiceClient --> BusinessController : HTTP
 BusinessController --> BusinessService : execute
 LoggingProxy --> AuditStore : una fila final
```

LoggingProxy llama exactamente una vez al delegado, conserva el objeto devuelto, mide con System.nanoTime, registra SUCCESS o ERROR y propaga ServiceException. Errores inesperados se convierten en error tipado de upstream. Su catch no engloba la persistencia: una avería de auditoría nunca vuelve a ejecutar el negocio. El stack resumido conserva clase, código y hasta tres marcos, sin rutas de archivos, variables ni stack completo. No se usa AOP ni interceptor como sustituto del Proxy.

```mermaid
sequenceDiagram
 participant UI as Frontend
 participant API as MonitorController
 participant G as ServiceGateway
 participant P as LoggingProxy
 participant H as HttpServiceClient
 participant S as Servicio remoto
 participant DB as AuditStore
 UI->>API: POST /api/services/{service}/{operation}
 Note over API: Filtro crea UUID
 API->>G: execute
 G->>P: execute
 Note over P: SLF4J inicio + nanoTime
 P->>H: delegate.execute (una vez)
 H->>S: POST /internal/{operation}, X-Request-Id
 alt Éxito
 S-->>H: response
 H-->>P: resultado
 P->>DB: SUCCESS, duración, parámetros, resultado
 P-->>G: mismo resultado
 G-->>API: OperationResponse con requestId
 API-->>UI: 200
 else Fallo de negocio o transporte
 S-->>H: error / timeout
 H-->>P: ServiceException
 P->>DB: ERROR, código, resumen
 P-->>API: propaga ServiceException
 API-->>UI: error HTTP con requestId
 end
```

Una solicitud JSON inválida rechazada antes del Proxy no se cuenta como invocación de negocio. Una operación desconocida que llega al Proxy sí registra error. Las llamadas directas a `/internal` no se exponen al frontend ni se auditan: mantener privados los tres servicios es parte de la topología de despliegue.
