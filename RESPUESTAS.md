# Respuestas a las preguntas del enunciado

## 1. Listado de municipios lento

> *Las llamadas al servicio para obtener el listado de municipios son bastante lentas. ¿Cómo mejoraríamos el tiempo de respuesta, teniendo en cuenta que el listado no cambia con frecuencia?*

Como el listado es grande (más de 8.000 municipios), lento de obtener (dos peticiones a AEMET) y casi estático, lo adecuado es **no pedírselo a AEMET en cada búsqueda, sino tenerlo ya en nuestro lado**.

**Implementado:**
- **Caché en memoria (Caffeine) de 24 horas** sobre el adaptador de AEMET. La primera búsqueda descarga el listado completo (en nuestras pruebas, unos 1,7 s) y las siguientes filtran en memoria en milisegundos. El filtrado por prefijo se hace en nuestro backend, así que todas las búsquedas comparten la misma entrada de caché.
- **`@Cacheable(sync = true)`**: si llegan varias búsquedas a la vez con la caché vacía, solo una descarga el listado y el resto espera ese resultado.

**Siguientes pasos:**
- **Precarga al arrancar** (`ApplicationReadyEvent`) para que ningún usuario pague la primera descarga.
- **Refresco en segundo plano** (`refreshAfterWrite` de Caffeine o una tarea `@Scheduled` nocturna): se sigue sirviendo el listado anterior mientras se descarga el nuevo y, si AEMET falla, se conserva el que había en lugar de dejar la caché vacía.
- **Persistencia** del listado en base de datos, fichero o Redis, para que sobreviva a reinicios y lo compartan varias instancias del backend. Con una base de datos, la búsqueda por prefijo puede apoyarse en un índice.

## 2. Por qué AEMET devuelve una URL en lugar de los datos

> *El servicio para recuperar la predicción de un municipio devuelve una URL a la que hacer la consulta. ¿Por qué motivo piensas que AEMET lo ha implementado así?*

La primera llamada no devuelve la predicción, sino un pequeño JSON con un `estado`, una URL `datos` con el contenido y otra `metadatos` con su descripción. Creo que responde a separar **el control de acceso** de **la entrega de los datos**:

- **El contenido está pregenerado.** El JSON de la predicción incluye un campo `elaborado` (por ejemplo, `"2026-10-08T20:55:08"`), y comprobamos que AEMET regenera esos ficheros periódicamente (el de la noche ya no era el mismo a la mañana siguiente). Se sirve como un fichero (`text/plain; charset=ISO-8859-15`), no como una respuesta calculada al momento. Servir ficheros estáticos es muy barato y se puede delegar en servidores de ficheros o una CDN.
- **La primera llamada es ligera y es la que se controla.** Ahí se valida la API key, se contabiliza la cuota de cada usuario y se aplican los límites de uso. La URL de `datos` no necesita la API key, así que la descarga pesada puede salir de una infraestructura distinta y escalar por separado.
- **Deja la puerta abierta a procesos asíncronos.** Para consultas costosas, el mismo contrato permitiría generar el resultado en diferido y devolver una URL que estará disponible después, sin mantener la conexión abierta.
- **Separa datos y metadatos:** el cliente que ya conoce el formato no descarga la descripción en cada petición.

Consecuencias en nuestra implementación: el adaptador resuelve los dos pasos, comprueba el `estado` del primero (AEMET responde HTTP 200 incluso cuando el municipio no existe) y cachea el contenido ya transformado, nunca la URL.

## 3. Recordar el último municipio seleccionado

> *Les gustaría que se recordara el último municipio seleccionado y que automáticamente les cargue la predicción del día siguiente. ¿Cómo lo podríamos hacer?*

**Implementado en el frontend:**
- `LastSelectionStorage` guarda en `localStorage` el último municipio y la unidad elegidos cada vez que cambian. No es información sensible, así que este almacenamiento es adecuado.
- Al abrir la pantalla, el `WeatherStore` lo restaura en su hook `onInit`, muestra el municipio en el buscador y carga la predicción automáticamente.
- Los datos guardados se validan al leerlos: si están corruptos o en un formato antiguo, se ignoran sin romper la aplicación.

**Evolución:** `localStorage` es por navegador. Para que la preferencia siga al usuario entre dispositivos, se guardaría en el backend asociada a su identidad, que ya conocemos por el `sub` del JWT (por ejemplo `GET/PUT /api/v1/users/me/preferences`). Solo cambiaría la implementación de `LastSelectionStorage`: el store y los componentes no se enteran. Como mejora adicional, se podría proponer el municipio más cercano con la geolocalización del navegador, porque el listado de AEMET incluye la latitud y longitud de cada municipio.

## 4. Carga excesiva sobre AEMET

> *Estamos causando una carga excesiva en el servicio de AEMET. Debemos buscar una solución que afecte lo mínimo posible a nuestros usuarios. ¿Qué podríamos hacer? ¿Cómo implementarías la solución?*

La clave es que **el número de llamadas a AEMET no dependa del número de usuarios, sino del número de datos distintos**: hay unos 8.000 municipios y la predicción cambia unas pocas veces al día. En desarrollo comprobamos que AEMET corta con un 429 tras unas pocas llamadas seguidas e indica *"Vuelva a intentarlo el próximo minuto"*. Por eso reintentar dentro de la misma petición no sirve: la solución es **reducir y suavizar** las llamadas.

**Ya implementado:**
1. **Caché compartida de predicciones** (1 hora, clave *municipio + fecha*). Mil usuarios consultando Valencia generan una sola llamada por hora, y pedir °C o °F no duplica llamadas porque la conversión se hace después de la caché.
2. **`sync = true`**: si llegan muchas peticiones a la vez para el mismo municipio con la caché vacía, solo una va a AEMET. Hay un test que lo verifica: sin esta opción, 5 peticiones simultáneas generaban 5 llamadas; con ella, 1.
3. **Caché del listado de municipios** (24 horas).

**Cómo lo completaría, de menor a mayor esfuerzo:**
1. **Ajustar la caducidad a la frecuencia real de actualización** de AEMET. El campo `elaborado` permite saber cuándo se generó cada predicción y no volver a pedirla antes de tiempo.
2. **Cabeceras `Cache-Control`** en nuestras respuestas, para que navegadores y una CDN reutilicen las predicciones sin llegar siquiera a nuestro backend.
3. **Limitador de peticiones hacia AEMET** (Resilience4j `RateLimiter` o Bucket4j) configurado por debajo de su límite. Garantiza que nunca lo superamos, sea cual sea el tráfico.
4. **Servir la última predicción conocida si AEMET falla o nos limita** (*stale-if-error*): una segunda caché de mayor duración con la última respuesta válida. El usuario ve una predicción y un aviso de "actualizada a las HH:mm" en lugar de un error.
5. **Refresco programado** (`@Scheduled`) de los municipios más consultados, repartido en el tiempo y después de las horas en que AEMET publica. El tráfico de usuarios deja de generar llamadas a AEMET: solo lee de nuestra caché.
6. **Caché distribuida (Redis)** al escalar a varias instancias, para que N instancias no multipliquen las llamadas.
7. **Métricas y alertas** (Micrometer) del número de llamadas a AEMET y de las respuestas 429, para detectar el problema antes de que AEMET nos avise.

Los puntos 1 a 3 se pueden aplicar en horas y ya eliminan el exceso. Los puntos 4 y 5 son los que hacen que el usuario no note nada aunque AEMET limite el servicio.
