# Entrega de código en GitHub

## Repositorios oficiales

- Backend: https://github.com/YonierProgramas/PatronProxyBackend
- Frontend: https://github.com/YonierProgramas/PatronProxyFrontend

Cada proyecto tiene su propio repositorio Git y rama `main`, con sus archivos directamente en la raíz. La carpeta superior no es un repositorio del taller. Render y Vercel permanecen sin despliegue verificado; ver DEPLOYMENT_STATUS.md para los bloqueos de conexión y plan.

Origen exclusivo de este proyecto: `https://github.com/YonierProgramas/PatronProxyBackend.git`.

El usuario autorizó inicialización, commits descriptivos y push normal a `main` si pasan las comprobaciones principales. No se permite force push ni reescribir historial. Si el remoto contiene commits, se conserva su historial y se integran antes de publicar.

Los archivos de proyecto están directamente en la raíz, sin otra carpeta contenedora. `.gitignore` excluye `.env` y variantes privadas, credenciales, claves, datos locales y artefactos; conserva `.env.example` sin secretos.

Verificación antes de publicar: Maven Wrapper verify, 25 pruebas y cuatro JARs ejecutables. Los resultados se registran en TEST_REPORT.md. La confirmación del push se realiza comparando el SHA local con `refs/heads/main` del remoto oficial.

El código fuente no representa un despliegue: la publicación de aplicaciones en Render y Vercel depende de resolver conexión y plan conforme a DEPLOYMENT_STATUS.md. QA visual, PostgreSQL/Docker, PowerShell y comprobación de URL públicas permanecen como verificaciones separadas.
