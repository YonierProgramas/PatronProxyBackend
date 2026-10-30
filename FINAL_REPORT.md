# Informe final de implementación local

30 de octubre de 2026. **Implementación local construida y verificada; entrega pública todavía pendiente.**

1. **Ubicación.** Backend: `C:\Users\Jhonier\OneDrive\Documentos\Proxy\microservices-monitor-backend`. Frontend: `C:\Users\Jhonier\OneDrive\Documentos\Proxy\microservices-monitor-frontend`. En WSL, prefijo `/mnt/c/Users/Jhonier/OneDrive/Documentos/Proxy/`. Dos repositorios Git independientes con rama main y orígenes oficiales exclusivos; la carpeta superior no se inicializa.
2. **Arquitectura.** React → monitor-api → LoggingProxy por servicio → cliente HTTP → tres procesos Spring Boot de negocio. Solo monitor-api guarda auditoría. H2/Flyway local y perfil PostgreSQL productivo preparado. shared-contracts es biblioteca, no despliegue.
3. **Archivos principales.** pom raíz y por módulo; MicroserviceProxy.java; LoggingProxy.java; HttpServiceClient.java; ServiceGateway.java; InventoryService.java; OrderService.java; PaymentService.java; AuditStore.java; MonitorController.java; SimulationService.java; V1__audit.sql. Frontend: src/pages/Dashboard.tsx, src/hooks/useDashboard.ts, src/api/client.ts y componentes.
4. **LoggingProxy.** Implementa contrato exacto y recibe delegado por composición. Registra inicio, delega una vez, mide con System.nanoTime, persiste una fila final SUCCESS/ERROR y devuelve el resultado o propaga ServiceException. UUID se propaga al servicio por header. Persistencia fallida se informa explícitamente y no reintenta negocio. Diagramas en PATTERN_PROXY.md.
5. **Operaciones.** Inventario: listProducts, getStock, addStock, removeStock. Pedidos: createOrder, getOrder, listOrders. Pagos: processPayment, getPayment. Cantidades/IDs/importe validados, BigDecimal, almacenamiento concurrente en memoria y fallos de pagos con probabilidad 0.10 por llamada válida.
6. **Endpoints.** POST /api/services/inventory/{operation}, POST /api/services/orders/{operation}, POST /api/services/payments/{operation}, GET /api/metrics/summary, GET /api/metrics/logs, POST /api/metrics/simulate-load. GET /actuator/health en cada proceso. Ejemplos y errores en API_CONTRACT.md.
7. **Pruebas reales.** Backend 25/25; frontend 20/20 en una ejecución completa final de cuatro suites (además de las ejecuciones históricas documentadas). Integración HTTP real: base vacía, operación manual con UUID y lote exactamente +50; 49 éxitos y 1 fallo de pago observado inicialmente; la nueva aceptación previa a GitHub también pasó con 50 éxitos y 0 errores, usando timeout configurable de 10 s para el arranque en frío. Reinicio conservó 53 logs; mutaciones de inventario, pedido, pago y CORS reales pasaron. Tests con mocks se distinguen de HTTP/H2 reales en ambos TEST_REPORT.
8. **Build backend.** Maven Wrapper ejecutado con `clean verify`: BUILD SUCCESS, seis módulos SUCCESS y cuatro JARs ejecutables. La recompilación se hizo con servicios detenidos para evitar bloqueo de JAR en Windows.
9. **TypeScript/frontend.** Modo estricto y build Vite correctos; dist generado con chunks separados de aplicación, React y gráficas. No requiere backend disponible para compilar; URL de API sí se requiere para usarlo.
10. **Ejecución exacta.** Requiere Java 21, Python 3.10+ y Node 22.12+ en sus entornos. Desde PowerShell:

```powershell
Set-Location 'C:\Users\Jhonier\OneDrive\Documentos\Proxy\microservices-monitor-backend'
.\mvnw.cmd -B verify
.\tools\start.ps1
# Otra terminal:
Set-Location 'C:\Users\Jhonier\OneDrive\Documentos\Proxy\microservices-monitor-frontend'
npm ci
# Crear solo si no existe; no sobreescribir configuración propia
if (-not (Test-Path .env.local)) { Copy-Item .env.example .env.local }
.\tools\start.ps1
```

Abrir http://localhost:5173. Frontend se detiene con Ctrl+C; backend con tools/stop.ps1 desde su carpeta. Bash/WSL separado:

```bash
cd '/mnt/c/Users/Jhonier/OneDrive/Documentos/Proxy/microservices-monitor-backend'
./mvnw -B verify
bash tools/start.sh
# Otra terminal:
cd '/mnt/c/Users/Jhonier/OneDrive/Documentos/Proxy/microservices-monitor-frontend'
npm ci
if [ ! -f .env.local ]; then cp .env.example .env.local; fi
bash tools/start.sh
```

Parada backend: `bash tools/stop.sh`. Logs en backend/.runtime/<servicio>.log. Herramientas temporales usadas aquí y export JAVA_HOME/PATH en docs/LOCAL_VERIFICATION.md; no depender de /tmp tras reinicios. Scripts no instalan herramientas ni matan procesos por puerto.

11. **Limitaciones.** Negocio efímero al reiniciar, auditoría persistente sin retención, sin autenticación, limitador de simulación por instancia, sin transacciones distribuidas. Solo datos ficticios. QA visual, PowerShell, Docker y PostgreSQL real pendientes. No se ha certificado seguridad mediante npm audit. No confundir una ejecución HTTP local con despliegue.
12. **GitHub y próximos pasos.** Repositorios oficiales: [Backend](https://github.com/YonierProgramas/PatronProxyBackend) y [Frontend](https://github.com/YonierProgramas/PatronProxyFrontend). El usuario autorizó commits y push normal a main tras las comprobaciones. Ver GITHUB_DELIVERY.md y TEST_REPORT.md. Render y Vercel no se despliegan todavía: la fase posterior requiere cuatro servicios backend, PostgreSQL privado, variables, API pública, frontend y CORS. Ambos DEPLOYMENT.md detallan esos pasos; no se crearon recursos de pago.
13. **Matriz de cumplimiento.** [docs/COMPLIANCE.md](docs/COMPLIANCE.md) relaciona las 31 secciones con archivos, pruebas y estado. Sustentación en docs/DEMO.md, arquitectura y secuencias Mermaid en README y PATTERN_PROXY.
14. **Datos pendientes.** Nombre del autor y profesor, cuenta/proyecto Render y PostgreSQL, variables privadas, cuenta/proyecto Vercel, URL pública de API y frontend y comprobación pública con UUID/+50. No se inventó ningún dato o URL.

El taller no está completamente entregado mientras falten ambos repositorios remotos y las URL de producción verificadas. Los procesos de verificación se detienen al terminar para liberar memoria.
