# Guía de sustentación

1. Arrancar los cuatro servicios y frontend siguiendo ambos README. Con base nueva el resumen debe contener cero llamadas; los productos precargados no son logs. No borrar la base histórica para fingir una ejecución nueva: usar una ruta H2 nueva si hace falta una demostración desde cero.
2. Abrir “Probar operación”, Inventario, Consultar existencias, SKU-001 y Ejecutar. Mostrar response y requestId.
3. Localizar ese UUID en la tabla. Pulsar fila o botón +, expandir inputParams y respuesta JSON. Mostrar una única entrada final.
4. Mostrar LoggingProxy.java: implementación del Subject, composición, llamada única, System.nanoTime y store.save fuera del catch. Seguir HttpServiceClient hasta InventoryService para explicar proceso remoto real.
5. Pulsar Simular carga, observar botón deshabilitado y resumen. Comparar total anterior con +50 finalizados si la DB funciona. Explicar distribución 17/17/16 y batchId distinto de cada requestId.
6. Filtrar Pagos y Error. Si un lote no produjo fallo, explicar probabilidad y repetir después del cooldown; no prometer exactamente cinco errores. Mostrar origen de fallo en PaymentService con fuente aleatoria inyectable.
7. Expandir un ERROR: código, mensaje y stack resumido. Explicar que la operación fallida no guarda pago exitoso.
8. Explicar tasas: éxitos y errores divididos por total, promedio de todas las llamadas; umbral de atención estrictamente mayor a 15. Sin datos equivale a null.
9. Mostrar gráfica: series por servicio y últimas 20 de cada uno; filtros únicamente de la tabla. Polling cada 3 s consulta métricas, no negocios.
10. Mostrar pruebas deterministas y sus resultados reales en TEST_REPORT. Distinguir tests con dobles, H2 real y HTTP real. Indicar los pendientes de infraestructura y entrega pública.

Profesor y autor: completar por el estudiante. No hay integrantes de equipo: taller individual.
