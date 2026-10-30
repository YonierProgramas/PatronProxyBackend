# Estado de producción

Repositorios oficiales: [backend](https://github.com/YonierProgramas/PatronProxyBackend) y [frontend](https://github.com/YonierProgramas/PatronProxyFrontend). El usuario autorizó nuevos commits y push, y después la variante gratuita con tres negocios como Web Services públicos protegidos. Cada carpeta conserva su Git, main y origin exclusivo; la carpeta superior no se inicializa.

Comprobaciones actuales: backend 32/32, frontend 20/20, compilaciones correctas, Blueprints validados con esquema oficial y entrypoint comprobado. No hay URL pública de aplicación verificada todavía. Los resultados HTTP locales previos de +50 llamadas permanecen en los informes; no se presentan como prueba productiva.

## Bloqueos concretos

1. La última consulta de integraciones muestra Render y Vercel sin instalar en ChatGPT; no hay CLI/sesión/token de despliegue local detectado. Completar instalación y autorización de ambas aplicaciones en ChatGPT con la cuenta que tiene acceso a YonierProgramas. Iniciar sesión únicamente en las páginas del proveedor no habilita las herramientas de esta sesión. Nunca compartir tokens ni contraseñas en el chat.
2. La restricción del plan se resolvió mediante aprobación explícita: cuatro Web Services Free y PostgreSQL Free temporal, con secreto entre gateway y negocios. El Blueprint activo render.yaml no contiene planes pagos. Falta desplegar y comprobarlo; no hay URL supuesta.
3. Vercel necesita VITE_API_BASE_URL con el origen real del gateway; CORS requiere el origen real de Vercel. No se inventan ni se publican URLs de marcador.

## Validación pendiente

Tras completar la conexión de los proveedores: crear los cuatro procesos Spring Boot y una base PostgreSQL, verificar salud y tres servicios en summary, operación manual con UUID, simulate-load con incremento exacto +50, persistencia tras reinicio del gateway y dashboard público con datos reales. Ver DEPLOYMENT.md del backend para variables, costes y pasos.

No se contrataron recursos ni se afirmó un despliegue exitoso sin evidencia.
