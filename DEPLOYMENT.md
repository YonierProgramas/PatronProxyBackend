# Preparación para Railway

El usuario proporcionó y autorizó los repositorios GitHub oficiales y su publicación en main. No autorizó despliegues: Railway y Vercel se configuran en una fase posterior. Costos y planes deben revisarse antes de crear recursos; no se promete alojamiento gratuito.

## Repositorio y build

Repositorio backend oficial: https://github.com/YonierProgramas/PatronProxyBackend. Su raíz contiene pom.xml y shared-contracts. No usar Proxy como raíz del repositorio. Configurar cuatro servicios Railway desde **ese mismo repositorio** y root directory del repo; cada Dockerfile requiere el contexto completo.

| Servicio Railway | Dockerfile | Start | Puerto local / Railway | Variables | Dependencia |
| --- | --- | --- | --- | --- | --- |
| inventory-service | Dockerfile.inventory-service | ENTRYPOINT java -jar app.jar | 8081 / PORT | PORT opcional | ninguna |
| order-service | Dockerfile.order-service | ENTRYPOINT java -jar app.jar | 8082 / PORT | PORT opcional | ninguna |
| payment-service | Dockerfile.payment-service | ENTRYPOINT java -jar app.jar | 8083 / PORT | PORT opcional | ninguna |
| monitor-api | Dockerfile.monitor-api | ENTRYPOINT java -jar app.jar | 8080 / PORT | perfil, URLs, CORS, datasource | los tres servicios y PostgreSQL |

Configurar `RAILWAY_DOCKERFILE_PATH` con el archivo de la fila o seleccionarlo en la configuración del servicio. Build reproducible por módulo: `mvn -B -pl <module> -am package -DskipTests` dentro del builder. Pruebas deben ejecutarse antes en CI/local con `./mvnw verify`. Los Dockerfiles no requieren JARs copiados manualmente. Imágenes Java 21, proceso no root. `PORT` controla el servidor; todos los healthchecks usan `/actuator/health`, timeout de arranque sugerido 120 s. Puerto del dominio debe coincidir con el servidor. Si red privada requiere IPv6, comprobar binding dual-stack del runtime/proveedor.

Servicios de negocio: usar red privada, sin dominios públicos. Definir sus URLs en monitor-api con esquema HTTP y puerto efectivo; no asumir que hostname privado incluye el puerto. No usar localhost en producción. No apuntar a servicios externos.

## monitor-api producción

```text
SPRING_PROFILES_ACTIVE=prod
PORT=<puerto asignado>
INVENTORY_SERVICE_URL=http://<host privado inventario>:<puerto>
ORDER_SERVICE_URL=http://<host privado pedidos>:<puerto>
PAYMENT_SERVICE_URL=http://<host privado pagos>:<puerto>
CORS_ALLOWED_ORIGINS=https://<dominio frontend real>
SPRING_DATASOURCE_URL=jdbc:postgresql://<PGHOST privado>:<PGPORT>/<PGDATABASE>
SPRING_DATASOURCE_USERNAME=<PGUSER>
SPRING_DATASOURCE_PASSWORD=<PGPASSWORD secreto>
INTERNAL_TIMEOUT_MS=3000
SIMULATION_COOLDOWN_MS=10000
```

Las dos últimas son opcionales. Nunca copiar secretos al repositorio o variables VITE_. Usar referencias del proveedor a PGHOST, PGPORT, PGDATABASE, PGUSER y PGPASSWORD. Una URL `postgresql://usuario:clave@host:puerto/db` **no es** automáticamente una URL JDBC: construir `jdbc:postgresql://host:puerto/db` y suministrar usuario/contraseña por separado. Ajustar `sslmode` a la documentación del proveedor y a la red utilizada; no desactivar TLS si el proveedor lo exige. Sin puerto público de DB innecesario. Flyway migra V1 al arrancar; H2 no se usa en prod.

Publicar solo monitor-api con HTTPS. Verificar health y resumen vacío mediante URL real antes de configurar frontend. El endpoint es académico sin login; usar únicamente datos ficticios. Límite de simulación por proceso: mantener una réplica para la demostración. Almacenes de negocio efímeros; PostgreSQL solo conserva auditoría.

## Orden de integración

1. Backend y DB disponibles en Railway; verificar servicios privados y API pública.
2. Establecer URL HTTPS real de monitor-api en VITE_API_BASE_URL de Vercel.
3. Desplegar repositorio frontend independiente.
4. Autorizar origen real de Vercel en CORS_ALLOWED_ORIGINS, separado por comas si hay varios; sin comodines.
5. Operación manual desde frontend público, localizar UUID, simular carga y comprobar +50 logs, errores de pago y filtros.

**Pendientes:** cuenta/proyecto Railway, PostgreSQL y sus secretos, cuenta Vercel, dominios públicos reales, comprobación PostgreSQL, Docker build y flujo público. Ninguna URL ficticia representa un despliegue.

Fuentes oficiales de preparación: [Dockerfiles en Railway](https://docs.railway.com/builds/dockerfiles) y [repositorios compartidos](https://docs.railway.com/deployments/monorepo). Consultadas para confirmar RAILWAY_DOCKERFILE_PATH y el contexto raíz; no prueban un despliegue de este proyecto.
