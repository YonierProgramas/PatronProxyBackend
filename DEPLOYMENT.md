# Despliegue backend en Render

Plataforma actual: **Render**. Sustituye a Railway por decisión del usuario, sin cambiar el taller. Frontend continúa en Vercel. El usuario autorizó commits y push de esta preparación. **No se creó infraestructura ni se contrató un plan**; la restricción de no usar recursos de pago sigue vigente. Los dos repositorios oficiales siguen siendo:

- Backend: https://github.com/YonierProgramas/PatronProxyBackend
- Frontend: https://github.com/YonierProgramas/PatronProxyFrontend

## 1. Arquitectura y recursos manuales

Crear estos cinco recursos, en el mismo workspace y región (el ejemplo usa Oregon):

| Nombre sugerido | Tipo en Render | Plan del Blueprint | Dockerfile / dato | Exposición |
| --- | --- | --- | --- | --- |
| inventory-service | Private Service | 0.5c-512mb | Dockerfile.inventory-service | privada |
| order-service | Private Service | 0.5c-512mb | Dockerfile.order-service | privada |
| payment-service | Private Service | 0.5c-512mb | Dockerfile.payment-service | privada |
| monitor-api | Web Service | 0.5c-512mb | Dockerfile.monitor-api | HTTPS público |
| proxy-audit-db | Render Postgres | 0.1c-256mb, disco 1 GB | PostgreSQL 16 | privada |

No usar Background Workers para los tres negocios: necesitan recibir HTTP. Mantener una instancia por aplicación; sus almacenes en memoria y el limitador de simulación no son distribuidos. Solo monitor-api accede a PostgreSQL. Las existencias, pedidos y pagos se reinician con su proceso; los logs permanecen en la base.

Fuentes: [Private Services](https://render.com/docs/private-services), [red privada](https://render.com/docs/private-network), [planes vigentes](https://render.com/docs/compute-plans).

## 2. Costes y límites, consultados el 30 de octubre de 2026

| Recurso | Cantidad | Precio base de referencia USD/mes |
| --- | ---: | ---: |
| Aplicación 0.5c-512mb (antes Starter) | 4 × 7 | 28 |
| PostgreSQL 0.1c-256mb (antes Basic-256mb) | 1 × 6 | 6 |
| Disco PostgreSQL 1 GB | 1 × 0.30 | 0.30 |
| **Estimación base de la arquitectura completa** | | **34.30** |

Es una estimación calculada, no una cotización ni una contratación. Excluye impuestos, uso adicional, transferencia, minutos de build, un eventual workspace de pago y aumentos de recursos. Cada Java dispone de 512 MB y 0.5 CPU; la DB, 256 MB y 0.1 CPU. La suficiencia del tamaño para la carga real debe medirse. Si requiere más memoria, revisar presupuesto antes de cambiar plan. Confirmar precios en el Dashboard antes de crear recursos. Fuentes: [precios oficiales](https://render.com/pricing), [ejemplo oficial con precios de aplicación y Postgres](https://render.com/articles/production-rails-hosting-guide).

**No hay una opción íntegramente gratuita que conserve esta topología privada.** Private Services no tienen plan Free. Web Free se suspende tras 15 minutos inactivo, comparte 750 horas mensuales por workspace y no recibe tráfico privado. Puede enviar solicitudes privadas a servicios pagos de su región. No agrupar los tres negocios en un proceso ni volverlos públicos para ocultar el coste.

PostgreSQL Free: una instancia por workspace, 1 GB, vence a los 30 días; tras vencer hay 14 días para actualizar antes de borrado. Sin backups ni pooling administrado. Puede reiniciarse o estar temporalmente indisponible por mantenimiento. No es persistencia duradera para una entrega que deba mantenerse. Fuente: [límites Free oficiales](https://render.com/docs/free).

No se eligió una variante Free en el Blueprint. Contratar cualquier recurso requiere aprobación previa del usuario.

## 3. Docker y Maven multimódulo

Cada servicio usa el mismo repositorio backend y rama main, **Root Directory vacío/raíz**, Docker Context `.` y su propio Dockerfile. No elegir la carpeta del módulo como contexto: necesita pom.xml raíz, shared-contracts y los POM de los módulos del reactor.

La etapa builder usa Maven 3.9.9/Java 21 y ejecuta `mvn -B -ntp -pl <module> -am package -DskipTests`. La etapa final copia únicamente el JAR correspondiente y el entrypoint, usa Java 21 JRE, usuario 10001 y heap limitado por porcentaje del contenedor. No copiar JARs manualmente ni definir Build Command o Start Command de un runtime Java nativo: seleccionar **Docker** y conservar el ENTRYPOINT del archivo. Las pruebas se ejecutan por separado con `./mvnw verify` antes de publicar.

`tools/render-entrypoint.sh` conserva URLs explícitas; si faltan, convierte referencias host:port en URLs HTTP. Construye JDBC desde PGHOST/PGPORT/PGDATABASE y ejecuta Java con exec para recibir SIGTERM. No imprime credenciales. `.dockerignore` excluye datos, .env, Git, logs y target.

Docker builds reproducibles desde la raíz backend, cuando Docker esté disponible:

```bash
docker build -f Dockerfile.inventory-service -t proxy-inventory .
docker build -f Dockerfile.order-service -t proxy-orders .
docker build -f Dockerfile.payment-service -t proxy-payments .
docker build -f Dockerfile.monitor-api -t proxy-monitor .
```

Estos comandos funcionan también en PowerShell desde la misma raíz. Docker no se instaló ni ejecutó en este entorno: la coherencia de archivos/contexto se comprueba localmente; un build Docker real queda pendiente.

## 4. Puerto y salud

Todos los módulos escuchan en `0.0.0.0` y `server.port=${PORT:<puerto local>}`. El Blueprint establece `PORT=10000` en cada contenedor; son procesos aislados, por lo que no hay conflicto. Si el Dashboard asigna otro PORT, Spring lo utiliza y las URLs privadas deben incluir el mismo puerto. No fijar localhost ni confundir EXPOSE con el puerto de escucha.

- Web Service monitor-api: configurar **Health Check Path `/actuator/health`**. Comprueba salud del proceso y datasource, sin detalles sensibles.
- Private Services: Render utiliza healthchecks **TCP**, no `healthCheckPath`. Cada negocio conserva `/actuator/health` para consulta HTTP desde la red privada. Un TCP abierto no prueba las operaciones: validarlas a través de monitor-api.
- El health de monitor-api no prueba automáticamente los tres servicios remotos. Ejecutar llamadas reales después de desplegar.

Fuentes: [puerto del Web Service](https://render.com/docs/web-services), [healthchecks y restricciones para privados](https://render.com/docs/health-checks).

## 5. Variables por recurso

Los tres Private Services solo necesitan `PORT=10000`. El Dockerfile define JAVA_TOOL_OPTIONS; no introducir secretos en esa variable. No asignar credenciales de DB a los negocios.

En monitor-api:

| Variable | Valor / origen |
| --- | --- |
| PORT | 10000 o el puerto configurado en Render |
| SPRING_PROFILES_ACTIVE | prod |
| CORS_ALLOWED_ORIGINS | origen HTTPS real de Vercel, sin slash final ni comodines; varios separados por comas sin espacios |
| INTERNAL_TIMEOUT_MS | 10000 en esta propuesta; configurable, sin reintentos de POST |
| SIMULATION_COOLDOWN_MS | 10000 |
| DB_MAX_POOL_SIZE | 5 |
| INVENTORY_SERVICE_URL | http://HOST_PRIVADO_REAL:PUERTO_REAL de inventory-service |
| ORDER_SERVICE_URL | http://HOST_PRIVADO_REAL:PUERTO_REAL de order-service |
| PAYMENT_SERVICE_URL | http://HOST_PRIVADO_REAL:PUERTO_REAL de payment-service |
| SPRING_DATASOURCE_URL | jdbc:postgresql://HOST_INTERNO_DB:5432/NOMBRE_DB?sslmode=require |
| SPRING_DATASOURCE_USERNAME | usuario de Render Postgres |
| SPRING_DATASOURCE_PASSWORD | secreto de Render Postgres, solo en Render |

Los valores HOST_PRIVADO_REAL, PUERTO_REAL, HOST_INTERNO_DB y NOMBRE_DB son marcadores, no URLs creadas. Copiarlos de los recursos reales. Nunca apuntar los clientes a URLs públicas de negocios si la red privada está disponible.

**Alternativa automática del Blueprint:** en lugar de las tres *_SERVICE_URL, usa INVENTORY_SERVICE_HOSTPORT, ORDER_SERVICE_HOSTPORT y PAYMENT_SERVICE_HOSTPORT mediante fromService/hostport. Para JDBC usa PGHOST, PGPORT y PGDATABASE mediante fromDatabase; usuario y password se inyectan directamente en sus variables Spring. El entrypoint agrega los prefijos http:// y jdbc:postgresql://. Una variable completa explícita tiene prioridad. No copiar el postgres:// que contiene usuario/password directamente a SPRING_DATASOURCE_URL.

En prod se requieren datasource y destinos; no hay fallback silencioso a H2 ni localhost. Flyway ejecuta V1__audit.sql, no ddl-auto=create. Pool máximo 5, mínimo 1. No habilitar consola H2. PostgreSQL interno admite TLS con sslmode=require; los certificados internos autofirmados no admiten verify-full. Restringir acceso externo de DB: el Blueprint usa ipAllowList vacío. Fuente: [conexión y TLS PostgreSQL](https://render.com/docs/postgresql-creating-connecting).

## 6. Creación manual paso a paso (solo tras autorización)

1. Autorizar primero el presupuesto. Publicar los cambios preparados en el repositorio backend con la autorización del usuario ya recibida; confirmar el commit main antes de crear servicios.
2. Entrar al workspace Render y elegir una misma región para los cinco recursos.
3. Crear Render Postgres proxy-audit-db, PostgreSQL 16, plan revisado y 1 GB de disco. Conservar host interno, puerto, database, user y password; restringir conexiones externas. No poner secretos en GitHub.
4. Crear inventory-service, order-service y payment-service como Private Service, runtime Docker, mismo repositorio backend y main, contexto raíz. Seleccionar Dockerfile de la tabla, plan autorizado, una instancia y PORT. Desactivar auto-deploy por ahora. Esperar que cada uno esté listo por TCP.
5. Copiar host:port privado de cada recurso desde el Dashboard; no inventar dominios ni omitir el puerto.
6. Crear monitor-api como Web Service Docker desde el mismo repo raíz, elegir Dockerfile.monitor-api, plan autorizado y una instancia. Configurar todas las variables de la sección anterior, perfil prod y health HTTP. Usar un CORS explícito provisional como http://localhost:5173 solo si todavía no hay dominio Vercel; sustituirlo por el origen real antes de validar frontend público.
7. Desplegar manualmente solo cuando esté autorizado. Revisar logs de arranque, migración Flyway y health. No registrar ni compartir contraseñas.
8. Copiar la URL HTTPS real de monitor-api y configurar VITE_API_BASE_URL en Vercel desde el repositorio frontend independiente. Valor = origen, sin /api. Reconstruir frontend al cambiar variable.
9. Autorizar dominio final Vercel en CORS y volver a desplegar configuración del gateway si Render lo requiere.
10. Completar las pruebas de producción abajo y registrar las dos URL reales para la entrega.

**Blueprint opcional:** render.paid.example.yaml representa los mismos cinco recursos y sus referencias privadas. CORS queda sync:false para suministrarlo manualmente. autoDeployTrigger:off evita futuros despliegues por push; **no evita el primer despliegue ni el coste al crear/sincronizar un Blueprint**. No se ha aplicado. No ejecutar creación o sincronización sin aprobación. Referencia: [especificación Render](https://render.com/docs/blueprint-spec).

## 7. Validación posterior en producción

1. GET /actuator/health público debe devolver UP, sin stack ni datos sensibles. Confirmar perfil prod y PostgreSQL por logs operativos del Dashboard, sin exponer secretos.
2. En una base nueva, GET /api/metrics/summary contiene los tres servicios con cero llamadas y tasas/promedio null. Polling no aumenta el total.
3. POST /api/services/inventory/getStock con params:[{productId:SKU-001}] devuelve stock y requestId; verificar exactamente un log con ese UUID y duración real. Utilizar el JSON completo válido de API_CONTRACT.md.
4. Crear/consultar pedido y procesar/consultar pago con datos ficticios. SIMULATED_FAILURE/503 es un resultado esperado del negocio y debe generar ERROR; no exigir un fallo por lote ni reintentar POST automáticamente.
5. Con otros operadores sin ejecutar negocios, medir total, POST /api/metrics/simulate-load y comprobar incremento exacto +50, requestedCalls=completedCalls=50, UUID distintos y métricas consistentes. No exigir cinco fallos: el 10% es probabilidad por pago válido.
6. Validar filtros combinados, fechas UTC inclusivas, paginación, recentCalls hasta 20 por servicio, gráfica y umbral errorRate >15. Las pruebas deterministas locales cubren los valores exactos.
7. Desde Vercel, verificar preflight CORS, operación manual, UUID en tabla y polling cada 3 segundos; el origen no autorizado no recibe permiso CORS. CORS no es autenticación.
8. Reiniciar únicamente monitor-api con autorización y confirmar que los logs permanecen. Los negocios en memoria no ofrecen persistencia de pedidos/pagos al reiniciarlos; esto no es fallo de PostgreSQL.
9. Revisar memoria, errores y latencia durante simulación. Verificar manejo tipado de 400/404/409/413 y, en pruebas controladas, 503/504. Los errores esperados no deben cerrar procesos.

**Pendientes reales:** aprobación de costes, publicación autorizada de esta adaptación, creación manual Render, build Docker real, PostgreSQL real, URLs, secretos privados y pruebas públicas. TEST_REPORT.md distingue lo ejecutado de lo pendiente. Ningún recurso se ha creado ni contratado.
