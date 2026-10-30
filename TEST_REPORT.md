# Informe de pruebas backend

Fecha: 30 de octubre de 2026 (America/Bogota). Java 21.0.13, Spring Boot 3.5.7, Maven 3.9.9. Se ejecutó con herramientas locales temporales, sin instalaciones globales ni Docker.

## Compilación final

Comando desde raíz backend: `./mvnw -B -Dmaven.repo.local=/tmp/proxy-tools/m2 clean verify`, con JAVA_HOME y MAVEN_USER_HOME temporales según docs/LOCAL_VERIFICATION.md. Resultado **BUILD SUCCESS**. Los seis proyectos del reactor terminaron SUCCESS; se generaron biblioteca compartida y los cuatro JARs ejecutables. Maven Wrapper oficial comprobado en ejecución.

| Suite | Pruebas | Fallos / errores / omitidas | Naturaleza |
| --- | ---: | --- | --- |
| InventoryServiceTest | 2 | 0 / 0 / 0 | Negocio real en memoria, validación y concurrencia |
| OrderServiceTest | 1 | 0 / 0 / 0 | Crear, consultar, listar y validar |
| PaymentServiceTest | 4 | 0 / 0 / 0 | Negocio real con fuente aleatoria controlada |
| AuditStoreTest | 4 | 0 / 0 / 0 | Spring + H2 + Flyway + JDBC reales |
| HttpServiceClientTest | 3 | 0 / 0 / 0 | Servidor HTTP local real para éxito/timeout; dobles de almacén para topología |
| LoggingProxyTest | 4 | 0 / 0 / 0 | Delegado y almacén Mockito; una llamada y una fila, propagación, UUID |
| MonitorWebTest | 5 | 0 / 0 / 0 | MockMvc, negocio simulado; UUID, CORS, validación, límite de cuerpo |
| SimulationServiceTest | 2 | 0 / 0 / 0 | Gateway doble, 50 invocaciones, continúa fallos, exclusión y cooldown |
| **Total** | **25** | **0 / 0 / 0** | Sin exigir porcentajes aleatorios exactos |

AuditStoreTest comprueba resumen vacío, fórmulas sobre éxitos y errores, promedio, exactamente 15% y valores superiores, últimas 20 por cada servicio, filtros combinados, límites inclusivos, orden, paginación y errores de persistencia por UUID duplicado. Los tests de Proxy verifican resultado por identidad, única delegación, final único, error propagado y ausencia de reintento cuando falla auditoría.

Se verificó sintaxis de scripts Python con py_compile y Bash con bash -n. Los scripts de inicio y parada se utilizaron en esta sesión. PowerShell no ejecutado.

## Integración HTTP real

Se ejecutó `python3 tools/manage.py start` y `python3 tools/smoke.py` contra cuatro JVM distintas en puertos 8080–8083. La auditoría H2 comenzó vacía. **PASS**:

- Resumen inicial total 0: productos precargados no generaron logs.
- getStock devolvió existencias y UUID localizado en la consulta de logs: exactamente una fila.
- Simulate-load completó 50 invocaciones de negocio reales: **49 éxitos, 1 error**.
- Total pasó de 1 a **51**; 51 requestId distintos.
- Distribución acumulada: inventario 18, pedidos 17, pagos 16.
- Métricas coherentes, duraciones no negativas, recentCalls cronológicas.
- Stock insuficiente devolvió 409 con requestId; operación desconocida 400 con requestId.
- Rango invertido devolvió 400 y cuerpo excesivo 413.

Evidencia original: [docs/SMOKE_RESULT.json](docs/SMOKE_RESULT.json). El resultado 49/1 es una observación, no una proporción exigida por el test. Los fallos deterministas de pagos se verifican por separado con FailureRandom.

La prueba HTTP no equivale a clics desde navegador. Se inició Vite y se obtuvo HTML HTTP 200 en 5173 y salud UP de la API. No hay automatización visual aprobada.

## Incidencias resueltas

Una reconstrucción con servicios activos falló al reemplazar el .jar.original bloqueado por el filesystem Windows. Se detuvieron exclusivamente los procesos del proyecto y se ejecutó clean verify con éxito. No se eliminaron pruebas para aprobar el build. Java informó advertencia de auto-attach Mockito; no fue fallo ni test omitido.

## Pendientes

Docker build, PostgreSQL real, Railway, Vercel, scripts PowerShell y QA de navegador. No había Docker/PostgreSQL/PowerShell/Chrome ejecutables ni cuentas de despliegue configuradas; no se instalaron ni inventaron resultados. La migración SQL está preparada para PostgreSQL, pero H2 no demuestra compatibilidad productiva por sí sola. Ver DEPLOYMENT.md y frontend/MANUAL_QA.md.

## Reinicio y operaciones adicionales reales

Se reiniciaron las cuatro JVM tras la compilación final. H2 conservó **53 logs**: 51 después del lote, más dos errores de negocio de la prueba de aceptación. Se verificaron listProducts, addStock (+3), removeStock (-3, restaura 30), createOrder y getOrder por UUID, processPayment y getPayment (ambos 200 en esta ejecución), y preflight CORS del origen local autorizado. Resultado **PASS**, sin exigir resultados aleatorios futuros idénticos. Evidencia: [docs/RESTART_RESULT.json](docs/RESTART_RESULT.json). Los datos de negocio sí se reinician, de acuerdo con el alcance en memoria.

Resumen extraído de los XML Surefire reales (sin copiar propiedades del entorno): [docs/TEST_RESULTS.json](docs/TEST_RESULTS.json).

Para repetir el smoke sin borrar auditoría previa, detén los servicios y utiliza una URL H2 de archivo nueva. Bash, desde backend:

```bash
export SPRING_DATASOURCE_URL="jdbc:h2:file:./data/acceptance-$(date +%s);DB_CLOSE_ON_EXIT=FALSE"
python3 tools/manage.py start
python3 tools/smoke.py
python3 tools/manage.py stop
unset SPRING_DATASOURCE_URL
```

PowerShell, desde backend:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./data/acceptance-' + (Get-Date -Format 'yyyyMMddHHmmss') + ';DB_CLOSE_ON_EXIT=FALSE'
py -3 tools/manage.py start
py -3 tools/smoke.py
py -3 tools/manage.py stop
Remove-Item Env:SPRING_DATASOURCE_URL
```

Cada ejecución genera llamadas reales y una base separada; no se borran registros para alterar resultados. No ejecutes el smoke simultáneamente con llamadas manuales, pues sus aserciones comprueban incrementos exactos.

## Verificación previa a GitHub (30 de octubre de 2026)

`./mvnw -B -Dmaven.repo.local=/tmp/proxy-tools/m2 verify`: **BUILD SUCCESS**, nuevamente **25 pruebas, cero fallos, errores u omisiones** y cuatro JARs ejecutables.

La primera aceptación de esta fase, con timeout por defecto de 3000 ms y Vitest concurrente, falló en el primer getStock: auditoría real `UPSTREAM_TIMEOUT`, duración 3128.048845 ms. Se conservó esa base y no se eliminó ni desactivó ninguna prueba. Para la repetición se utilizó otra base H2 vacía y la variable existente `INTERNAL_TIMEOUT_MS=10000`, apropiada para el arranque en frío de las cuatro JVM en este entorno WSL/OneDrive. El valor predeterminado de la aplicación no se cambió.

La repetición **PASS** comprobó operación manual, UUID único, lote +50, filtros y errores esperados; el lote observó **50 éxitos y 0 fallos**, resultado posible con probabilidad 0.10 por cada una de las 16 llamadas a pagos. Las pruebas de fallo de pagos y timeout siguen aprobadas con dependencias controladas. Evidencia: [docs/GITHUB_SMOKE_RESULT.json](docs/GITHUB_SMOKE_RESULT.json). Los cuatro procesos se detuvieron correctamente después de la prueba.

Se revisaron los archivos del índice Git: sin .env privados, credenciales, datos H2, logs, target, dist o node_modules. `.env.example` se conserva. Se normalizaron finales de línea con `.gitattributes` (Bash LF, Windows CMD/PowerShell CRLF en checkout) y se verificó `git diff --cached --check`.

## Adaptación Render y preparación de producción

El 30 de octubre de 2026 se ejecutó nuevamente Maven Wrapper verify: **BUILD SUCCESS, 25/25 pruebas**. Se generaron los cuatro JARs. Se verificó el Blueprint con el esquema JSON oficial de Render; cuatro Dockerfiles con contexto raíz, Maven -pl/-am, módulos compartidos y artefactos coherentes; PORT y binding 0.0.0.0; cuatro casos del entrypoint real con ejecutable Java doble (URLs derivadas, prioridad explícita, servicio sin DB y PGPORT requerido). El doble no es una imagen Docker ni una conexión PostgreSQL real.

Evidencia: docs/RENDER_CHECK_RESULT.json. Docker no está instalado: **no se construyeron imágenes Docker**. PostgreSQL y endpoints públicos todavía no se verificaron.

El Blueprint se conserva como **render.paid.example.yaml**, sin descubrimiento automático como render.yaml: describe la topología privada que requiere pago y **no se aplica** bajo la restricción vigente de no seleccionar recursos pagos. No se altera silenciosamente el taller para eludir el coste.
