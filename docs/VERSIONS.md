# Versiones y fuentes

Selección estable compatible con Java 21; no se utilizan SNAPSHOT. No se afirma que las versiones fijadas sean siempre las más nuevas. Revisar parches antes de exposición pública prolongada.

- Spring Boot 3.5.7: artefacto oficial confirmado en [Maven Central](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/3.5.7/spring-boot-starter-parent-3.5.7.pom). [Compatibilidad oficial de la línea 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
- Java 21 Temurin: [Adoptium API oficial](https://api.adoptium.net/v3/info/available_releases). Herramienta temporal usada para verificar, sin instalación global.
- Maven 3.9.9 y Wrapper: [distribución Maven Central](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/), [Wrapper oficial y only-script](https://maven.apache.org/wrapper/).
- React 19: [versiones oficiales](https://react.dev/versions).
- Vite 7: [requisitos Node oficiales](https://vite.dev/guide/), Node 22.12+ compatible; se utilizó Node 22.16.0 temporal.
- Frontend usa rangos estables en package.json; package-lock.json fija las versiones resueltas, sin SNAPSHOT ni prerelease solicitado. `npm ci` reproduce el árbol.
- Recharts: [documentación](https://recharts.github.io/en-US/guide/).
- Vitest y Testing Library: [Vitest](https://vitest.dev/guide/), [Testing Library React](https://testing-library.com/docs/react-testing-library/intro/).

Los resultados de compilación y pruebas de estas versiones están en TEST_REPORT.md. No confundir compatibilidad documentada con prueba real de despliegue.

## Versiones frontend resueltas

| Paquete | Versión del lockfile |
| --- | --- |
| react | 19.3.1 |
| react-dom | 19.3.1 |
| recharts | 3.10.1 |
| typescript | 5.9.3 |
| vite | 7.3.7 |
| vitest | 3.2.7 |
| @vitejs/plugin-react | 5.2.0 |
| @testing-library/react | 16.3.3 |
