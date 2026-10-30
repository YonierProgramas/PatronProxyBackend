# Matriz de cumplimiento

“Implementado” describe código presente, no despliegue ni QA de navegador aprobado. Resultados exactos y límites en TEST_REPORT.md y el informe frontend. Numeración corresponde a las 31 secciones del documento fuente.

| Requisito | Archivo / componente | Prueba / evidencia | Estado |
| --- | --- | --- | --- |
| 1. Tres servicios, Proxy y observabilidad | módulos Spring, frontend | smoke.py y tests por módulo | Implementado; repositorios oficiales definidos, URL de producción pendientes |
| 2. Proyecto nuevo y alcance académico | carpetas independientes, pagos ficticios | revisión de estructura y dependencias | Implementado, sin integraciones ajenas |
| 3. Dos proyectos independientes | pom.xml, package.json, README por proyecto | compilaciones separadas | Git independiente en cada proyecto; orígenes oficiales |
| 4. Código inglés y UI español | clases Java, DTO, componentes React | tests de traducción y revisión | Implementado |
| 5. Java 21, Maven Wrapper, React/TS/Vite | pom.xml, mvnw, package-lock.json | Maven verify, tsc y Vite build | Ver informes de ejecución |
| 6. Cuatro procesos y biblioteca | módulos y aplicaciones main separadas | arranque y cuatro healthchecks reales | Verificado localmente |
| 7. Contrato Proxy exacto y composición | MicroserviceProxy, LoggingProxy, ServiceGateway | LoggingProxyTest y HttpServiceClientTest | Verificado: una delegación y una fila |
| 8. Operaciones reales y concurrencia | InventoryService, OrderService, PaymentService | InventoryServiceTest, OrderServiceTest, PaymentServiceTest | Verificado unitariamente; flujo HTTP real |
| 9. Seis rutas públicas y respuestas | MonitorController, ErrorHandler | MonitorWebTest y smoke.py | Implementado, contrato documentado |
| 10. Auditoría y requestId | LoggingProxy, RequestFilter, AuditLog | UUID, duración, success/error, propagación HTTP | Verificado en tests y smoke |
| 11. H2, PostgreSQL, migraciones | AuditStore, V1__audit.sql, perfiles | AuditStoreTest con H2 real | H2 verificado; PostgreSQL pendiente |
| 12. Fórmulas, null, recent20 por servicio | AuditStore.summary, ServiceCards | AuditStoreTest, dashboard.test.tsx | Implementado y probado |
| 13. Filtros y paginación backend | AuditStore.logs, AuditTable | pruebas combinadas, límites y páginas | Implementado y probado |
| 14. Probabilidad 0.10 inyectable | PaymentService, FailureRandom | PaymentServiceTest con fuente controlada | Verificado determinista; fallo real observado |
| 15. 50 llamadas por Proxy, continúa errores | SimulationService | SimulationServiceTest, smoke.py +50 | Verificado; 17/17/16, concurrencia 1 |
| 16. Frontend independiente y origen API | src/api/client.ts, .env.example | build y tests API | Implementado; variable productiva pendiente |
| 17. Tarjetas y Recharts | ServiceCards, DurationChart | tests de tarjetas y umbral; build | Implementado; QA visual pendiente |
| 18. Tabla, detalles y filtros | AuditTable | dashboard.test.tsx | Implementado y probado por componente |
| 19. Polling 3000 ms y cancelación | useDashboard | polling.test.tsx | Implementado; resultados en informe frontend |
| 20. Botón carga y operación manual | Dashboard, OperationPanel | actions.test.tsx y HTTP real | Implementado; clic en navegador pendiente |
| 21. Responsive y estados | global.css y componentes | tests vacíos/conexión; MANUAL_QA | Implementado; visual manual pendiente |
| 22. CORS, límites, salud, timeouts | CorsConfiguration, RequestFilter, HttpServiceClient | MonitorWebTest, timeout real, smoke 413 | Implementado; sin login, datos ficticios |
| 23. Windows y WSL, inicio seguro | tools/manage.py, start/stop .sh/.ps1 | arranque WSL y verificación sintáctica Python/Bash | WSL verificado; PowerShell pendiente |
| 24. Render por módulo y PostgreSQL | cuatro Dockerfiles, DEPLOYMENT.md | revisión del contexto raíz | Preparado; Docker/Render/PostgreSQL sin ejecutar |
| 25. Vercel y orden CORS | vercel.json, frontend/DEPLOYMENT.md | build local de producción | Preparado; publicación pendiente |
| 26. Pruebas backend | ocho clases de pruebas + smoke.py | Maven verify y TEST_REPORT | Resultados reales documentados |
| 27. Pruebas frontend y flujo | cuatro suites Vitest, smoke.py | TEST_REPORT frontend/backend | HTTP real verificado; navegador/público pendiente |
| 28. Documentación y sustentación | README, API_CONTRACT, PATTERN_PROXY, DEMO | revisión de documentos y Mermaid | Creado, datos académicos pendientes |
| 29. Archivos publicables y exclusiones | .gitignore, wrapper, lockfile | inspección de archivos | Repositorios oficiales configurados, exclusiones revisadas |
| 30. Fases de implementación | estructura, código, tests y documentos | informes reales | Trabajo local realizado; pendientes explícitos |
| 31. Informe final y entrega | FINAL_REPORT.md | referencias a evidencias | Informe preparado; entrega pública incompleta |

## Condiciones de entrega todavía pendientes

- Nombre del autor y profesor (no inventados).
- Repositorios oficiales: PatronProxyBackend y PatronProxyFrontend, bajo YonierProgramas. Commits y push autorizados; su resultado se confirma con los SHA de main.
- Cuenta/proyecto Render, PostgreSQL, variables privadas, cuatro servicios y API pública.
- Cuenta/proyecto Vercel, variable URL real y dominio autorizado en CORS.
- Prueba PostgreSQL y Docker, scripts Windows y QA visual en navegador.
- URL pública del frontend y API verificadas con operación UUID y lote +50.

No se puede considerar el taller completamente entregado sin los dos repositorios remotos y ambas URL reales verificadas.

## Variante Render Free aprobada explícitamente

El usuario autorizó cuatro Web Services Free, con los tres negocios públicos protegidos por un secreto compartido, ante la ausencia de Private Services gratuitos. Se mantienen procesos y Proxy; no es un cambio silencioso de arquitectura. El Blueprint activo render.yaml usa únicamente plan free; PostgreSQL Free vence a los 30 días. La prueba productiva sigue pendiente de conexión de los proveedores y URL reales.
