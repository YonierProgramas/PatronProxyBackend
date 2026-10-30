# Render Free + Vercel — variante autorizada

El usuario autorizó explícitamente usar **cuatro Web Services gratuitos**, con endpoints internos de negocio protegidos por un secreto, después de informar que Render Free no admite Private Services. Se conservan InventoryService, OrderService, PaymentService, LoggingProxy, cuatro JVM independientes, PostgreSQL de auditoría y todos los endpoints públicos. No se contratan recursos de pago.

Repositorios: [backend](https://github.com/YonierProgramas/PatronProxyBackend), [frontend](https://github.com/YonierProgramas/PatronProxyFrontend). Frontend sigue en Vercel. No hay URL pública verificada todavía; consultar DEPLOYMENT_STATUS.md.

## Recursos manuales, todos Free

| Recurso | Tipo | Dockerfile | Salud |
| --- | --- | --- | --- |
| inventory-service | Web Service Free | Dockerfile.inventory-service | /actuator/health |
| order-service | Web Service Free | Dockerfile.order-service | /actuator/health |
| payment-service | Web Service Free | Dockerfile.payment-service | /actuator/health |
| monitor-api | Web Service Free | Dockerfile.monitor-api | /actuator/health |
| proxy-audit-db | Render Postgres Free, versión 16 | no aplica | indicador del proveedor |

Mismo workspace y región (ejemplo Oregon), una instancia por aplicación. Los tres negocios tendrán URL pública HTTPS por limitación del plan, pero `/internal/**` exige X-Internal-Service-Token. El frontend solo utiliza monitor-api. No se reemplazan tres servicios por clases en una sola aplicación.

## Seguridad interna

`INTERNAL_SERVICE_TOKEN`: mismo valor aleatorio de **32 caracteres como mínimo** en las cuatro aplicaciones, exclusivamente en Render. En prod, una aplicación sin un token válido no arranca. InternalServiceFilter rechaza solicitudes internas sin el token correcto con 401/INTERNAL_UNAUTHORIZED, conserva requestId y nunca devuelve la credencial. HttpServiceClient agrega el header al delegar; LoggingProxy sigue auditando la invocación del gateway una sola vez. El token no se almacena en inputParams/response ni se envía al navegador.

Healthcheck permanece abierto, sin detalles. Las rutas `/api/**` del gateway continúan públicas para la demostración académica; este secreto protege la comunicación interna, no es un login de usuarios. En local/test se permite token vacío para conservar la ejecución local previa; cualquier token configurado activa la protección. **No poner INTERNAL_SERVICE_TOKEN en VITE_, GitHub, comentarios, logs ni este documento.**

El Blueprint genera el token en inventory-service y lo referencia mediante fromService/envVarKey en los otros tres. En creación manual, generar uno con el gestor de secretos del proveedor y copiar exactamente el mismo valor a los cuatro recursos.

## Variables

En los cuatro servicios: `PORT=10000`, `SPRING_PROFILES_ACTIVE=prod`, `INTERNAL_SERVICE_TOKEN=<secreto compartido>`. Cada contenedor escucha 0.0.0.0 y lee PORT; no compartir procesos ni fijar el puerto en Java. No elegir puertos reservados de Render.

Solo monitor-api:

| Variable | Valor |
| --- | --- |
| INVENTORY_SERVICE_URL | origen HTTPS **real y público** del Web Service de inventario |
| ORDER_SERVICE_URL | origen HTTPS **real y público** de pedidos |
| PAYMENT_SERVICE_URL | origen HTTPS **real y público** de pagos |
| CORS_ALLOWED_ORIGINS | origen real de Vercel, sin slash final ni comodines; varios separados por comas sin espacios |
| INTERNAL_TIMEOUT_MS | 10000 inicialmente, ajustable; sin reintentos de POST |
| SIMULATION_COOLDOWN_MS | 10000 |
| DB_MAX_POOL_SIZE | 5 |
| SPRING_DATASOURCE_URL | jdbc:postgresql://HOST_INTERNO_DB:5432/NOMBRE_DB?sslmode=require |
| SPRING_DATASOURCE_USERNAME | usuario PostgreSQL del proveedor |
| SPRING_DATASOURCE_PASSWORD | secreto PostgreSQL del proveedor |

El Blueprint inyecta alternativamente PGHOST, PGPORT y PGDATABASE y utiliza tools/render-entrypoint.sh para formar JDBC. No copiar postgres://usuario:password@host/db directamente como JDBC. La conexión a PostgreSQL sí usa la red interna: Free Web puede enviar tráfico privado a su base. El Blueprint deshabilita acceso externo de DB con ipAllowList vacío. TLS interno usa sslmode=require; certificados internos autofirmados no admiten verify-full. [Conexión PostgreSQL oficial](https://render.com/docs/postgresql-creating-connecting).

Las variables *_SERVICE_HOSTPORT del ejemplo privado **no deben usarse en Free**: los Web Services Free no reciben tráfico privado. Para ellos se requieren los tres orígenes HTTPS reales anteriores. En prod no hay fallback de destinos a localhost ni de datasource a H2. Flyway migra V1; no ddl-auto=create ni consola H2. Solo monitor-api tiene credenciales de DB.

## Maven y Docker

Todos los Web Services usan el mismo repositorio backend, main, runtime Docker, **Root Directory vacío/raíz**, Docker Context `.`, y el Dockerfile de su fila. No usar la subcarpeta del módulo como contexto: el build necesita pom.xml raíz, shared-contracts y los POM del reactor.

Build interno: `mvn -B -ntp -pl <module> -am package -DskipTests`. Pruebas separadas con Maven Wrapper verify. JRE Java 21, usuario no root 10001, memoria por porcentaje y entrypoint con exec para SIGTERM. No copiar JARs manualmente ni reemplazar Start Command: usar el ENTRYPOINT.

```bash
docker build -f Dockerfile.inventory-service -t proxy-inventory .
docker build -f Dockerfile.order-service -t proxy-orders .
docker build -f Dockerfile.payment-service -t proxy-payments .
docker build -f Dockerfile.monitor-api -t proxy-monitor .
```

Mismos comandos válidos en PowerShell desde la raíz backend. Docker no está disponible aquí: coherencia de los cuatro Dockerfiles y esquema oficial del Blueprint comprobados, build real de imágenes pendiente.

## Despliegue manual en orden

1. Instalar y conectar Render y Vercel dentro de ChatGPT con permisos sobre YonierProgramas, si se desea que el agente ejecute el despliegue. Iniciar sesión solo en sus webs no habilita ese acceso. Nunca compartir tokens en el chat. También es posible completar estos pasos personalmente en los Dashboards.
2. Confirmar que el main publicado contiene render.yaml y la protección interna. Seleccionar siempre plan **Free** y detenerse si Render exige un plan pago o un cargo no autorizado.
3. Crear Postgres Free en la región elegida. Guardar sus datos internos en variables privadas. Registrar su fecha de vencimiento.
4. Crear primero los tres negocios como Web Services Docker Free con sus Dockerfiles, perfil prod, PORT y el mismo secreto fuerte. Health Check Path /actuator/health. Auto-deploy desactivado para controlar esta entrega.
5. Copiar sus tres orígenes HTTPS reales del Dashboard. Esperar que arranquen. Una solicitud directa a /internal/getStock sin secreto debe recibir 401; health debe recibir UP.
6. Crear monitor-api Free con perfil prod, mismo secreto, esos tres orígenes, DB y CORS explícito. Hasta tener Vercel puede usarse provisionalmente http://localhost:5173, sustituyéndolo por el dominio final antes de la prueba pública.
7. Esperar migración Flyway y salud UP del gateway. Copiar su origen HTTPS real.
8. En Vercel importar PatronProxyFrontend, framework Vite, root del repo, npm ci, build npm run build, output dist. Configurar VITE_API_BASE_URL con el origen real del gateway, **sin /api**. No exponer secretos internos ni DB.
9. Desplegar frontend, copiar su dominio real y autorizarlo en CORS_ALLOWED_ORIGINS del gateway. Aplicar la configuración y probar el flujo.

### Blueprint gratuito

`render.yaml` contiene cuatro Web Services plan free y una DB plan free. No selecciona recursos pagos. Los tres *_SERVICE_URL y CORS usan sync:false para introducir los valores reales del Dashboard; no hay dominios supuestos. Para evitar problemas al introducir URLs aún inexistentes, se recomienda la secuencia manual anterior o crear primero los negocios y completar después el gateway al sincronizar el Blueprint. No se deben inventar URLs de antemano.

`autoDeployTrigger: off` deshabilita despliegues futuros por push, pero crear/sincronizar el Blueprint sí ejecuta el primero. El ejemplo privado pago anterior se conserva únicamente como render.paid.example.yaml y **no se aplica**. [Especificación oficial](https://render.com/docs/blueprint-spec).

## Límites Free y presupuesto

Consultado el 30 de octubre de 2026: Web Free se suspende tras 15 minutos inactivo; despertar puede tardar aproximadamente un minuto. El workspace comparte **750 horas mensuales** entre sus Web Services: cuatro siempre encendidos consumirían aproximadamente 2880 horas en 30 días. También hay límites de transferencia/build y posibles suspensiones. No usar pings artificiales para mantenerlos activos. No añadir recursos pagos ni aceptar sobrecostes; revisar límites y gasto en el Dashboard.

Postgres Free: una instancia por workspace, 1 GB, vence a los 30 días, sin backups ni pooling administrado. Tras vencer hay 14 días para actualizar antes de borrado. Es una demostración temporal, no una base gratuita permanente. [Límites oficiales](https://render.com/docs/free).

Los arranques en frío de los negocios pueden producir UPSTREAM_TIMEOUT/504 antes de que despierten. Esperar salud UP de los tres antes de la demostración y realizar una operación manual. No reintentar automáticamente un POST, dado que podría haber tenido efectos. La simulación sigue ejecutando exactamente 50 intentos y registra sus resultados reales incluso si un servicio está frío.

Referencia de la topología privada anterior, **no contratada**: cuatro instancias de 7 USD + PostgreSQL 6 USD + disco 1 GB 0.30 USD ≈ 34.30 USD/mes antes de extras/impuestos; los IDs actuales son 0.5c-512mb y 0.1c-256mb. Confirmar precios si en el futuro se autoriza pago. [Precios oficiales](https://render.com/pricing).

## Prueba productiva obligatoria

1. Health UP en cuatro aplicaciones y PostgreSQL activo. Sin secreto, los tres /internal/** devuelven 401; health no expone datos.
2. GET /api/metrics/summary: tres servicios, sin logs fabricados; en DB nueva tasas/promedio null y total cero. Polling no aumenta llamadas.
3. Una operación real de inventario mediante gateway devuelve UUID; localizar una sola entrada, duración y respuesta en logs. Crear/consultar pedido y pago ficticios. 503 SIMULATED_FAILURE de pagos es esperado y debe registrarse ERROR.
4. Capturar total de llamadas, POST /api/metrics/simulate-load con {}, comprobar requestedCalls=completedCalls=50 y aumento exacto +50, sin otros operadores ejecutando negocios. UUID por llamada distintos. No exigir cinco fallos: probabilidad 0.10 por llamada válida de pagos.
5. Desde Vercel: tarjetas, gráfica, filtros, detalles, simulación y polling con datos reales. Confirmar preflight CORS del dominio real, rechazo de orígenes ajenos y ausencia de credenciales internas en navegador.
6. Reiniciar gateway y comprobar que logs persisten en PostgreSQL. Negocios en memoria se reinician con sus propios procesos, como permite el taller. Verificar fechas UTC, paginación y métricas consistentes.

No se declarará la entrega productiva terminada hasta disponer de las dos URL reales y esta prueba pública verificada.
