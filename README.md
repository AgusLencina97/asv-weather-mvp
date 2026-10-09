# ASV Weather

Aplicación web para consultar la previsión meteorológica del día siguiente en cualquier municipio de España, a partir de [AEMET OpenData](https://opendata.aemet.es/).

- **Backend**: Java 25 + Spring Boot 4, arquitectura hexagonal, autenticación JWT.
- **Frontend**: Angular 22 (componentes standalone, Signals, NgRx SignalStore) + Angular Material.

> **Nota sobre el enunciado:** el enunciado pide el backend en Node + Express. Por indicación expresa del proceso de selección, se ha desarrollado en **Java + Spring Boot**, manteniendo el mismo contrato REST.

Las respuestas a las preguntas del enunciado están en [RESPUESTAS.md](RESPUESTAS.md).

---

## Ejecución en local

### Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 25 |
| Node.js / npm | 24 LTS / 11 |
| API key de AEMET | Gratuita, se solicita en [opendata.aemet.es](https://opendata.aemet.es/centrodedescargas/altaUsuario) y llega por email |

Maven no es necesario: el proyecto incluye Maven Wrapper (`mvnw`).

### 1. Backend (puerto 8080)

```bash
cd backend
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
# Editar .env y pegar la API key de AEMET en AEMET_API_KEY
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Los secretos (API key de AEMET, credenciales y clave de firma del JWT) se leen de `backend/.env`, que está excluido del repositorio. La plantilla `.env.example` ya trae valores de desarrollo para todo salvo la API key. Si falta alguna variable, la aplicación no arranca e indica cuál es.

Documentación interactiva de la API (Swagger UI): <http://localhost:8080/swagger-ui.html>. Para probar los endpoints protegidos, obtener un token con `POST /api/v1/auth/login` y pegarlo en **Authorize**.

### 2. Frontend (puerto 4200)

```bash
cd frontend
npm ci
npm start
```

Abrir <http://localhost:4200> e iniciar sesión con las credenciales de `backend/.env` (por defecto `admin` / `admin123`). En desarrollo, el proxy de Angular (`proxy.conf.json`) redirige `/api` al backend, por lo que no hace falta configurar CORS para probar en local.

### Tests

```bash
cd backend && ./mvnw verify            # 45 tests: dominio, aplicación, adaptadores, web, seguridad y caché
cd frontend && npm test                # 88 tests (Vitest)
cd frontend && npm run test:coverage   # con informe de cobertura
```

Los tests no llaman a AEMET ni necesitan secretos reales. Se ejecutan automáticamente en cada push y pull request con GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml)).

---

## API

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Pública | Valida usuario y contraseña y devuelve un JWT |
| `GET` | `/api/v1/weather/municipalities?prefix=Ag` | JWT | Municipios cuyo nombre empieza por el prefijo |
| `GET` | `/api/v1/weather/prediction/{codigo}?unit=G_FAH` | JWT | Predicción del día siguiente. `unit` es opcional (`G_CEL` por defecto) |

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

curl -s "localhost:8080/api/v1/weather/municipalities?prefix=Ag" -H "Authorization: Bearer $TOKEN"
# [{"codigo":"35001","nombre":"Agaete"},{"codigo":"03002","nombre":"Agost"}, ...]

curl -s "localhost:8080/api/v1/weather/prediction/03002?unit=G_FAH" -H "Authorization: Bearer $TOKEN"
# {"fecha":"2026-10-10","mediaTemperatura":66.7,"unidadTemperatura":"G_FAH",
#  "probPrecipitacion":[{"probabilidad":5,"periodo":"00-06"},{"probabilidad":10,"periodo":"06-12"}, ...]}
```

Los errores siguen el formato estándar **RFC 9457 (Problem Details)**: `400` para parámetros inválidos, `401` sin token o con credenciales incorrectas, `404` si el municipio no existe y `502` si AEMET no responde. Nunca se exponen detalles internos.

---

## Arquitectura

### Backend: arquitectura hexagonal (puertos y adaptadores)

```
backend/src/main/java/com/grupoasv/weather
├── domain                  Núcleo de negocio, sin dependencias de frameworks
│   ├── model               Municipio, DailyForecast, WeatherPrediction, TemperatureUnit (conversión °C/°F)
│   ├── port/in             Casos de uso que ofrece la aplicación
│   ├── port/out            Lo que el dominio necesita del exterior (WeatherExternalPort)
│   └── exception
├── application/service     WeatherService: implementa los casos de uso
└── infrastructure
    ├── adapter/in/web      API REST: controladores, DTOs y manejo de errores
    ├── adapter/out/aemet   Cliente Feign y adaptador de AEMET (traduce su formato al dominio)
    ├── security            Emisión del JWT
    └── config              Seguridad, JWT, caché, reloj, OpenAPI, Feign
```

Las dependencias apuntan siempre hacia el dominio. Cambiar de proveedor meteorológico supone escribir un nuevo adaptador de `WeatherExternalPort`; añadir otro canal de entrada (por ejemplo, GraphQL o mensajería) no afecta a los casos de uso.

### Frontend

```
frontend/src/app
├── components              Presentación: login y pantalla del tiempo
├── stores                  WeatherStore (NgRx SignalStore): estado y lógica de la pantalla
├── services                Acceso HTTP (WeatherService, AuthService) y persistencia local (LastSelectionStorage)
├── guards, interceptors    Protección de rutas y envío del token
├── interfaces/models       Tipos del contrato de la API
├── utils                   Funciones puras: mensajes de error, icono del tiempo
└── config                  URL base de la API
```

Los componentes no llaman a HTTP directamente: delegan en el store, que coordina servicios y estado. Así la lógica se prueba sin renderizar la interfaz y la vista se mantiene simple.

---

## Decisiones técnicas

**Integración con AEMET**
- AEMET responde en dos pasos: una primera llamada devuelve un `estado` y una URL temporal (`datos`) con el contenido real. El adaptador resuelve ambos pasos y comprueba el `estado`, porque AEMET contesta HTTP 200 incluso cuando el municipio no existe (`"estado": 404`).
- La API key viaja en la cabecera `api_key`, no en la URL, para que no quede registrada en logs ni en mensajes de error.
- El "día siguiente" se busca por fecha y no por posición en el array: los ficheros de AEMET son pregenerados y empiezan en el día en que se elaboraron, que no siempre es hoy. La fecha se calcula en la zona horaria `Europe/Madrid`, independientemente de dónde se ejecute el servidor.
- AEMET da probabilidades de precipitación en tramos de 6 h para los dos primeros días del fichero y de 12 h o 24 h para los siguientes. Se devuelven los tramos de 6 h (como pide el enunciado) y, si no existen, los de la granularidad disponible más fina.

**Rendimiento y uso de AEMET**
- Caché Caffeine: el listado de municipios 24 h y las predicciones 1 h. La clave de la predicción es *municipio + fecha*, de modo que °C y °F comparten la misma entrada y la caché caduca sola al cambiar de día.
- `@Cacheable(sync = true)`: si varias peticiones encuentran la caché vacía a la vez, solo una llama a AEMET. Hay un test que lo verifica con peticiones concurrentes.

**Dominio y API**
- La conversión de unidades y el cálculo de la temperatura media están en el dominio (`TemperatureUnit`, `WeatherService`), no en el adaptador de AEMET.
- La búsqueda de municipios no distingue mayúsculas ni tildes y entiende los nombres que AEMET publica con el artículo al final ("A Coruña" encuentra "Coruña, A").
- Los DTOs de la API están separados del modelo de dominio, y la entrada se valida (código de municipio de 5 dígitos, unidad válida).
- La respuesta de predicción añade el campo `fecha` al contrato del enunciado, para que el cliente no tenga que deducir el día con su propio reloj.

**Seguridad** (no la pide el enunciado; se incluye como base para evolucionar el producto)
- Login que emite un JWT firmado (HS256, caduca en 1 h). El resto de endpoints lo validan con el soporte de *resource server* de Spring Security: API sin sesión, sin CSRF aplicable y con CORS limitado al origen del frontend.
- El mismo mensaje para usuario inexistente y contraseña incorrecta, para no revelar qué usuarios existen.
- Secretos fuera del repositorio; la aplicación no arranca si falta alguno o si la clave del JWT es demasiado corta.
- En el frontend, el token se guarda en `sessionStorage` con su caducidad, solo se envía a la propia API y un `401` cierra la sesión.

**Frontend**
- Estado con NgRx SignalStore. Búsqueda con *debounce*, `distinctUntilChanged` y `switchMap` para cancelar peticiones obsoletas. Indicadores de carga independientes para la búsqueda y la predicción.
- Se recuerda el último municipio y unidad elegidos (`localStorage`), y la predicción se carga automáticamente al volver (pregunta 3 del enunciado).
- TypeScript en modo `strict` y plantillas con `strictTemplates`.

---

## Posibles mejoras

- **Uso de AEMET a gran escala**: refresco programado de los municipios más consultados, limitador de peticiones hacia AEMET, servir la última predicción conocida si AEMET falla y caché distribuida (Redis) al escalar horizontalmente. Detallado en [RESPUESTAS.md](RESPUESTAS.md#4-carga-excesiva-sobre-aemet).
- **Usuarios y preferencias**: usuarios persistidos o un proveedor de identidad (Keycloak, Auth0) emitiendo tokens RS256, y preferencias guardadas en el backend para recordarlas entre dispositivos.
- **Producto**: icono a partir del estado del cielo de AEMET, geolocalización para proponer el municipio más cercano y más días de previsión.
- **Calidad y despliegue**: tests end-to-end (Playwright), Docker Compose para levantar todo con un comando y métricas (Micrometer) de las llamadas a AEMET.

---

## Uso de IA

Durante el desarrollo he utilizado asistentes de IA como apoyo para acelerar la implementación, revisar el código y redactar la documentación, con el conocimiento del proceso de selección. He revisado y validado cada cambio, ejecutándolo contra la API real de AEMET y con la batería de tests, y puedo explicar y defender cualquier decisión de la solución.
