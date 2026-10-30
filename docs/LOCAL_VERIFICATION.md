# Entorno de verificación de esta sesión

Fecha: 30 de octubre de 2026, zona del usuario America/Bogota. Workspace inicialmente vacío. No había Java o Node Linux ejecutables; el npm Windows disponible desde Bash fallaba por integración WSL. No se cambió WSL ni se instaló nada global.

Se descargaron herramientas temporales en `/tmp/proxy-tools`, fuera de los proyectos publicables:

- Java Temurin 21.0.13+9.
- Node 22.16.0 y npm 10.9.2.
- Maven 3.9.9; Wrapper oficial 3.3.4 only-script.
- Cache de dependencias Maven y npm.

No se debe depender de `/tmp` después de reiniciar o cambiar de entorno. Para el usuario Windows se requieren Java 21 y Node 22.12+ accesibles desde PowerShell; para WSL, herramientas Linux. Los scripts no instalan ni cambian configuraciones del computador.

Comandos usados para verificación backend, con herramientas temporales:

```bash
export JAVA_HOME=/tmp/proxy-tools/jdk-21.0.13+9
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME=/tmp/proxy-tools/maven-wrapper-home
export MAVEN_OPTS='-Xmx384m -XX:ActiveProcessorCount=2'
./mvnw -B -Dmaven.repo.local=/tmp/proxy-tools/m2 clean verify
python3 tools/manage.py start
python3 tools/smoke.py
python3 tools/manage.py stop
```

Frontend (desde su propia carpeta):

```bash
export PATH="/tmp/proxy-tools/node-v22.16.0-linux-x64/bin:$PATH"
npm install --cache /tmp/proxy-tools/npm-cache --no-audit --no-fund
npm test
npm run build
VITE_API_BASE_URL=http://localhost:8080 npm run dev -- --port 5173 --strictPort
```

El lockfile generado permite utilizar npm ci en instalaciones posteriores. Se sirvió Vite por HTTP y se comprobó su HTML; esto no constituye QA visual ni interacción real de navegador. No se encontró Docker, PostgreSQL, PowerShell Linux ni Chrome/Chromium ejecutables, y no se instalaron. Pruebas de esas infraestructuras quedan pendientes. No se solicitaron cuentas ni credenciales porque el código y preparación local podían completarse independientemente.

Durante una recompilación con los JARs en ejecución, el filesystem Windows bloqueó el reemplazo de un archivo .jar.original. Se detuvieron únicamente los cuatro procesos verificados del proyecto y se volvió a ejecutar clean verify. Los scripts ahora esperan la parada antes de permitir reconstrucción. Las pruebas nunca se eliminaron para ocultar fallos.
