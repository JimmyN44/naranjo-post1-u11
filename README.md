# Post-contenido — Unidad 11: Buenas Prácticas y Patrones de Diseño

**Refactorización de un proyecto con SOLID, DTO y Strategy, y endurecimiento con manejo de errores, logging y Swagger**

Programación Web — Unidad 11 · Laboratorio post-contenido (Parte 1 y Parte 2)

## Descripción

Repositorio del laboratorio de la Unidad 11 de Programación Web. Contiene un único proyecto Maven Spring Boot (`pedidos/`), con paquete raíz `com.tienda.pedidos`, que parte de un sistema de gestión de pedidos de una tienda en línea **ya funcional pero mal diseñado**: un `PedidoController` con SQL concatenado, un método que concentra seis responsabilidades, una dependencia creada con `new`, manejo de errores que devuelve `null` y la entidad JPA expuesta directamente en la API (incluido el campo interno `notasInternas`).

- **Parte 1** refactoriza ese código aplicando arquitectura por capas, los principios SOLID (SRP y DIP), el patrón DAO con Spring Data JPA, DTOs de entrada y salida con mapeo explícito y el patrón Strategy para el cálculo del descuento por tipo de cliente.
- **Parte 2** endurece el mismo proyecto con manejo centralizado de excepciones (`@RestControllerAdvice`), logging estructurado con SLF4J y Logback, y documentación de la API con springdoc-openapi y Swagger UI.

## Objetivo

Transformar un controlador monolítico con *code smells* reales en una API REST por capas, mantenible y probada, que:

1. No contenga SQL escrito a mano ni colaboradores instanciados con `new`.
2. Nunca exponga `notasInternas` (ni en las respuestas ni en los logs).
3. Calcule el descuento según `tipoCliente` (REGULAR 0 %, VIP 15 %, MAYORISTA 25 %) y el IVA del 19 % sobre el subtotal con descuento.
4. Responda errores coherentes en JSON (404, 400 y 500) en lugar de `null` o `200` vacío.
5. Registre sus eventos con niveles adecuados en consola y en `logs/pedidos.log`.
6. Publique su contrato en Swagger UI.

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 (`java.version` y `release 17` del compilador) | Lenguaje |
| Spring Boot | 3.2.12 | Framework base |
| Maven / Maven Wrapper | 3.9.11 / wrapper 3.3.4 | Construcción (`mvnw`) |
| Spring Web | (Boot 3.2.12) | API REST |
| Spring Data JPA + Hibernate | Hibernate 6.4.10 | DAO (`PedidoRepository`) |
| H2 Database | 2.2.224 | Base de datos en memoria |
| Bean Validation (Hibernate Validator) | 8.0.1 | `@NotBlank`, `@NotNull`, `@Positive` |
| SLF4J + Logback | SLF4J 2.0.16 / Logback 1.4.14 | Logging |
| springdoc-openapi-starter-webmvc-ui | 2.3.0 | OpenAPI 3 y Swagger UI |
| JUnit 5, Mockito, MockMvc, AssertJ | (spring-boot-starter-test) | Pruebas automatizadas |

## Estructura de carpetas

```
naranjo-post1-u11/
├── README.md
├── .gitignore                         (excluye target/ y logs/)
├── capturas/                          (evidencias en PNG)
└── pedidos/
    ├── pom.xml
    ├── mvnw / mvnw.cmd / .mvn/wrapper/
    └── src/
        ├── main/
        │   ├── java/com/tienda/pedidos/
        │   │   ├── PedidosApplication.java              (@OpenAPIDefinition)
        │   │   ├── controller/
        │   │   │   └── PedidoController.java
        │   │   ├── service/
        │   │   │   ├── PedidoService.java               (interfaz)
        │   │   │   ├── PedidoServiceImpl.java           (implementación)
        │   │   │   ├── NotificacionService.java         (interfaz)
        │   │   │   └── NotificacionEmailServiceImpl.java
        │   │   ├── repository/
        │   │   │   └── PedidoRepository.java            (DAO, JpaRepository)
        │   │   ├── dto/
        │   │   │   ├── PedidoRequestDTO.java
        │   │   │   └── PedidoResponseDTO.java
        │   │   ├── entity/
        │   │   │   └── Pedido.java
        │   │   ├── strategy/
        │   │   │   ├── EstrategiaDescuento.java         (interfaz)
        │   │   │   ├── DescuentoRegular.java
        │   │   │   ├── DescuentoVip.java
        │   │   │   └── DescuentoMayorista.java
        │   │   └── exception/
        │   │       ├── ApiError.java
        │   │       ├── GlobalExceptionHandler.java
        │   │       └── RecursoNoEncontradoException.java
        │   └── resources/
        │       ├── application.properties
        │       └── logback-spring.xml
        └── test/java/com/tienda/pedidos/
            ├── PedidosApplicationTests.java
            ├── controller/PedidoControllerIntegrationTest.java
            ├── exception/GlobalExceptionHandlerTest.java
            ├── service/PedidoServiceImplTest.java
            └── strategy/EstrategiaDescuentoTest.java
```

## Arquitectura

### Diagrama de capas

```
 Cliente HTTP (curl / Postman / Swagger UI)
        │  JSON: PedidoRequestDTO                         ▲ JSON: PedidoResponseDTO | ApiError
        ▼                                                 │
┌──────────────────────────── CONTROLLER ──────────────────────────────┐
│ PedidoController   @RestController  /api/pedidos                     │
│   - recibe y valida (@Valid) ── delega ──► PedidoService (interfaz)  │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │ (excepciones suben sin try/catch)
┌──────────────────────────── SERVICE ─────────────▼───────────────────┐       ┌──────────── EXCEPTION ─────────────┐
│ PedidoServiceImpl implements PedidoService                           │       │ GlobalExceptionHandler             │
│   - elige estrategia, calcula descuento / IVA / total                │──────►│   @RestControllerAdvice            │
│   - persiste vía DAO, notifica, mapea Entity → ResponseDTO           │ lanza │   404 RecursoNoEncontradoException │
│   - registra logs SLF4J                                              │       │   400 MethodArgumentNotValid...    │
│                                                                      │       │   500 Exception (mensaje genérico) │
│ NotificacionService (interfaz) ◄── NotificacionEmailServiceImpl      │       │ ApiError {status,error,mensaje,    │
└───────┬──────────────────────────────────────┬───────────────────────┘       │          timestamp,path}           │
        │ Map<String, EstrategiaDescuento>     │                               └────────────────────────────────────┘
┌───────▼──────────── STRATEGY ───────────┐ ┌──▼──────────── REPOSITORY (DAO) ─────────────┐
│ EstrategiaDescuento (interfaz)          │ │ PedidoRepository                             │
│   @Component("REGULAR")  0 %            │ │   extends JpaRepository<Pedido, Long>        │
│   @Component("VIP")      15 %           │ │   findByClienteEmail(String)                 │
│   @Component("MAYORISTA") 25 %          │ └──┬───────────────────────────────────────────┘
└─────────────────────────────────────────┘    │ JPA / Hibernate (consultas parametrizadas)
                                       ┌───────▼──────── ENTITY ─────────┐      ┌────────── DTO ──────────────────┐
                                       │ Pedido  @Entity (tabla pedido)  │      │ PedidoRequestDTO  (entrada,     │
                                       │  incluye notasInternas          │      │   validaciones)                 │
                                       └───────┬─────────────────────────┘      │ PedidoResponseDTO (salida, SIN  │
                                               ▼                                │   notasInternas)                │
                                          H2 (memoria)                          └─────────────────────────────────┘
```

### Diagrama de clases (texto)

```
PedidoController
  - service : PedidoService
  + crear(PedidoRequestDTO) : PedidoResponseDTO          POST /api/pedidos      → 201
  + obtener(Long id)        : PedidoResponseDTO          GET  /api/pedidos/{id} → 200

«interface» PedidoService
  + crear(PedidoRequestDTO) : PedidoResponseDTO
  + buscarPorId(Long)       : PedidoResponseDTO
        △
        │ implements
PedidoServiceImpl
  - repo        : PedidoRepository
  - notificador : NotificacionService
  - estrategias : Map<String, EstrategiaDescuento>
  - toResponseDTO(Pedido) : PedidoResponseDTO           (mapeo explícito, privado)

«interface» NotificacionService                 «interface» EstrategiaDescuento
  + enviarConfirmacion(String, double)            + calcularDescuento(double) : double
        △                                               △            △             △
NotificacionEmailServiceImpl              DescuentoRegular   DescuentoVip   DescuentoMayorista

«interface» PedidoRepository extends JpaRepository<Pedido, Long>
  + findByClienteEmail(String) : List<Pedido>

Pedido (@Entity): id, clienteEmail, tipoCliente, subtotal, descuento, iva, total,
                  estado, notasInternas, fechaCreacion
PedidoRequestDTO : clienteEmail, tipoCliente, subtotal
PedidoResponseDTO: id, clienteEmail, tipoCliente, subtotal, descuento, iva, total, estado

GlobalExceptionHandler (@RestControllerAdvice) ──produce──► ApiError
RecursoNoEncontradoException extends RuntimeException
```

## Violaciones del código inicial y cómo se corrigieron

El código de partida (`PedidoController` con `JdbcTemplate`) presentaba estas violaciones:

| # | Violación en el código de partida | Corrección aplicada |
|---|---|---|
| 1 | **SRP violado:** `crearPedido` validaba, calculaba el descuento, construía el SQL, persistía, notificaba y registraba el log (seis responsabilidades en un bloque). | El controller solo recibe y delega. La validación pasa a Bean Validation en `PedidoRequestDTO` (`@Valid`), el cálculo a `PedidoServiceImpl` + Strategy, la persistencia a `PedidoRepository`, la notificación a `NotificacionService` y el log a SLF4J. |
| 2 | **DIP violado:** `new NotificacionEmailService()` dentro del controller. | Se creó la interfaz `NotificacionService` con su implementación `@Service NotificacionEmailServiceImpl`, inyectada por constructor. Ninguna clase instancia colaboradores con `new`. |
| 3 | **Sin capa de acceso a datos:** SQL `INSERT`/`SELECT` concatenado en el controller (vulnerable a inyección SQL). | `PedidoRepository extends JpaRepository<Pedido, Long>`: CRUD sin SQL manual; Hibernate genera consultas parametrizadas. |
| 4 | **Sin DTO, entidad expuesta:** se devolvía `Pedido` completo, incluido `notasInternas`. | `PedidoRequestDTO` (entrada) y `PedidoResponseDTO` (salida, sin `notasInternas`) con mapeo explícito en `toResponseDTO()`. |
| 5 | **Manejo de errores disperso:** `try/catch` con `System.out.println` que devolvía `null` → `200 OK` vacío para un id inexistente. | El servicio lanza `RecursoNoEncontradoException`; `GlobalExceptionHandler` responde `404` con `ApiError`. No queda ningún `try/catch` en el controller. |
| 6 | **Logging con `System.out.println`** y descuento con `if/else` embebido. | Logger SLF4J con niveles INFO/WARN/ERROR/DEBUG y placeholders `{}`; el `if/else` se reemplazó por el patrón Strategy. |

## Principios SOLID aplicados

### SRP — Principio de Responsabilidad Única
Cada clase tiene una sola razón para cambiar:

- `PedidoController`: traducir HTTP ↔ DTO y delegar (no contiene lógica de negocio).
- `PedidoServiceImpl`: orquestar el caso de uso (cálculo, persistencia, notificación y mapeo a DTO).
- `PedidoRepository`: acceso a datos.
- `DescuentoRegular` / `DescuentoVip` / `DescuentoMayorista`: una regla de descuento cada una.
- `NotificacionEmailServiceImpl`: el canal de notificación.
- `GlobalExceptionHandler`: traducir excepciones a respuestas HTTP.

### DIP — Principio de Inversión de Dependencias
Los módulos de alto nivel dependen de abstracciones, y Spring las inyecta **por constructor** (sin `@Autowired` en campos):

```java
public PedidoController(PedidoService service) { ... }                      // interfaz, no PedidoServiceImpl

public PedidoServiceImpl(PedidoRepository repo,                              // interfaz (Spring Data)
                         NotificacionService notificador,                    // interfaz
                         Map<String, EstrategiaDescuento> estrategias) {...} // interfaz
```

Gracias a esto, `PedidoServiceImplTest` prueba el servicio sin Spring ni base de datos, sustituyendo repositorio y notificación por mocks de Mockito.

## Patrón Strategy

El descuento varía según `tipoCliente`. Cada variante es una clase que implementa `EstrategiaDescuento` y se registra como bean con el nombre del tipo:

```java
@Component("REGULAR")   public class DescuentoRegular   implements EstrategiaDescuento { ... return 0.0; }
@Component("VIP")       public class DescuentoVip       implements EstrategiaDescuento { ... return subtotal * 0.15; }
@Component("MAYORISTA") public class DescuentoMayorista implements EstrategiaDescuento { ... return subtotal * 0.25; }
```

Spring puebla automáticamente un `Map<String, EstrategiaDescuento>` usando el nombre de cada bean como llave, y el servicio selecciona la estrategia **sin `switch` ni cadenas `if/else`**:

```java
EstrategiaDescuento estrategia = estrategias.getOrDefault(tipoCliente, estrategias.get("REGULAR"));
double descuento        = estrategia.calcularDescuento(dto.getSubtotal());
double baseConDescuento = dto.getSubtotal() - descuento;
double iva              = baseConDescuento * 0.19;
double total            = baseConDescuento + iva;
```

Valores para `subtotal = 100000`:

| tipoCliente | Descuento | IVA (19 %) | Total |
|---|---|---|---|
| REGULAR | 0 | 19000 | 119000 |
| VIP | 15000 | 16150 | 101150 |
| MAYORISTA | 25000 | 14250 | 89250 |

`tipoCliente` no distingue mayúsculas (`"vip"` → `VIP`). Un tipo desconocido usa REGULAR y queda registrado con un `WARN`. Agregar un tipo nuevo (por ejemplo CORPORATIVO) solo requiere una clase nueva anotada con `@Component("CORPORATIVO")`, sin modificar `PedidoServiceImpl` (OCP).

## Decisiones de diseño

Las cuatro primeras provienen de la guía del laboratorio; las demás se tomaron durante la implementación.

1. **JpaRepository en vez de mantener JdbcTemplate.** El SQL concatenado exponía el proyecto a inyección SQL y obligaba a mantener las sentencias sincronizadas con la entidad a mano. Spring Data JPA ofrece CRUD sin SQL y consultas derivadas (`findByClienteEmail`) con seguridad de tipos en tiempo de compilación.
2. **Strategy en vez de un segundo patrón creacional (Factory) para la conversión a DTO.** El descuento tiene una variación real de comportamiento y es candidato a crecer (OCP). La conversión entidad → DTO es un mapeo 1:1 sin lógica condicional, así que se resuelve con un método privado `toResponseDTO()` en el servicio, sin introducir una clase Factory/Mapper innecesaria (DRY, evitar sobre-ingeniería).
3. **El registro del descuento aplicado vive en el servicio, no en cada Strategy.** Un único mensaje INFO en `PedidoServiceImpl`, justo después de invocar la estrategia, da un solo punto de auditoría por pedido en lugar de dispersarlo en tres clases.
4. **`notasInternas` nunca se registra en los logs ni se expone en el DTO de respuesta.** Es información operativa de logística; los logs pueden compartirse con soporte o retenerse 30 días por la política de rotación. Los mensajes solo incluyen identificadores (id, clienteEmail, tipoCliente) y montos.
5. **`@NotNull` adicional en `subtotal`.** `@Positive` acepta `null`; sin `@NotNull`, un cuerpo sin subtotal provocaría un `NullPointerException` (500) en lugar de un 400 de validación.
6. **Manejadores adicionales en `GlobalExceptionHandler`.** El manejador genérico `Exception.class` convertía en 500 errores que son del cliente. Se añadieron manejadores para JSON mal formado y parámetros con tipo inválido (400), y para excepciones de Spring MVC que ya traen su código (ruta inexistente 404, método no soportado 405, media type no soportado 415). Todos responden con `ApiError`.
7. **`springdoc.override-with-generic-response=false`.** Sin esta propiedad, springdoc añadía el 404 del `@RestControllerAdvice` a todas las operaciones, incluido el POST (que nunca responde 404). Cada operación documenta solo las respuestas declaradas con `@ApiResponse`.

## Manejo centralizado de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) reemplaza el `try/catch` disperso del código de partida. Todas las respuestas de error usan el mismo cuerpo `ApiError`:

```json
{ "status": 404, "error": "Not Found", "mensaje": "...", "timestamp": "2026-11-14T03:29:31.758820796", "path": "/api/pedidos/999" }
```

| Excepción | HTTP | `mensaje` |
|---|---|---|
| `RecursoNoEncontradoException` | 404 | `Pedido con id {id} no encontrado.` |
| `MethodArgumentNotValidException` | 400 | Errores de validación `campo: mensaje` separados por `; ` |
| `HttpMessageNotReadableException` | 400 | `El cuerpo de la solicitud no es un JSON valido` |
| `MethodArgumentTypeMismatchException` | 400 | `El parametro 'id' tiene un formato invalido` |
| `NoResourceFoundException` / `HttpRequestMethodNotSupportedException` / `HttpMediaTypeNotSupportedException` | 404 / 405 / 415 | `Solicitud no soportada: {método} {ruta}` |
| `Exception` (cualquier otra) | 500 | `Error inesperado. Contactar soporte.` |

En el 500 el cliente nunca recibe el mensaje ni la traza de la excepción: la traza completa solo se escribe en el log con nivel ERROR.

## Logging con SLF4J y Logback

`PedidoServiceImpl`, `NotificacionEmailServiceImpl` y `GlobalExceptionHandler` usan `LoggerFactory.getLogger(...)` con placeholders `{}` (sin concatenación). No queda ningún `System.out.println` en el código de producción.

| Nivel | Uso | Ejemplo real |
|---|---|---|
| INFO | Operaciones exitosas | `Pedido creado exitosamente con id=2, tipoCliente=VIP, descuento=15000.0, total=101150.0` |
| WARN | Situaciones inusuales | `Pedido con id=999 no encontrado` · `Tipo de cliente desconocido tipoCliente=ORO, se aplica la estrategia REGULAR` · solicitudes inválidas |
| ERROR | Excepciones inesperadas (con traza) | `Error inesperado en method=GET, path=/api/pedidos/1` |
| DEBUG | Seguimiento técnico | `Buscando pedido con id=2` |

Configuración en `pedidos/src/main/resources/logback-spring.xml`:

- **CONSOLA** (`ConsoleAppender`): `%d{HH:mm:ss} %-5level %logger{30} - %msg%n`
- **ARCHIVO** (`RollingFileAppender`): `logs/pedidos.log` con `TimeBasedRollingPolicy` (rotación diaria `logs/pedidos.%d{yyyy-MM-dd}.log`, `maxHistory` de 30 días) y patrón `%d{yyyy-MM-dd HH:mm:ss} %-5level %logger - %msg%n`
- Logger `com.tienda.pedidos` en `DEBUG`; `root` en `INFO` con ambos appenders.

La ruta `logs/pedidos.log` es relativa al directorio de ejecución: al ejecutar desde `pedidos/` el archivo queda en `pedidos/logs/pedidos.log`. La carpeta `logs/` está excluida en `.gitignore`.

## Documentación con Swagger / OpenAPI

Dependencia `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0` y configuración en `application.properties`:

```properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.override-with-generic-response=false
```

| Anotación | Dónde |
|---|---|
| `@OpenAPIDefinition` | `PedidosApplication` — "API de Pedidos" v1.0 |
| `@Tag` | `PedidoController` — grupo "Pedidos: Creacion y consulta de pedidos" |
| `@Operation` | `crear` ("Crear un nuevo pedido") y `obtener` ("Obtener pedido por ID") |
| `@ApiResponse` | POST: 201, 400, 500 · GET: 200, 400, 404, 500 (errores con esquema `ApiError`) |
| `@Parameter` | `id` — "ID del pedido", ejemplo `1` |
| `@Schema` | `PedidoRequestDTO` (con `allowableValues` REGULAR/VIP/MAYORISTA), `PedidoResponseDTO` y `ApiError`, en español y con ejemplos |

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Especificación OpenAPI (JSON): <http://localhost:8080/api-docs>

## Requisitos

- JDK 17 o superior (`java -version`).
- No es necesario instalar Maven: el proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`), que descarga Maven 3.9.11.
- Conexión a internet en la primera compilación (descarga de dependencias).
- Puerto 8080 libre.
- Opcional: curl, Postman o un navegador para Swagger UI.

## Instalación y ejecución

```bash
git clone https://github.com/JimmyN44/naranjo-post1-u11.git
cd naranjo-post1-u11/pedidos
```

Compilar y ejecutar las pruebas automatizadas:

```bash
./mvnw clean test
```

Iniciar la aplicación:

```bash
./mvnw spring-boot:run
```

En Windows se usa `mvnw.cmd` en lugar de `./mvnw`. Alternativamente se puede empaquetar y ejecutar el JAR:

```bash
./mvnw clean package
java -jar target/pedidos-0.0.1-SNAPSHOT.jar
```

Con la aplicación en marcha:

- API: <http://localhost:8080/api/pedidos>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Logs: en consola y en `pedidos/logs/pedidos.log`
- Consola H2 (opcional): <http://localhost:8080/h2-console> con JDBC URL `jdbc:h2:mem:pedidosdb`, usuario `sa`, sin contraseña.

La base de datos es en memoria: los pedidos se pierden al detener la aplicación.

### Pruebas automatizadas

`./mvnw clean test` ejecuta 22 pruebas:

| Clase | Pruebas | Qué verifica |
|---|---|---|
| `EstrategiaDescuentoTest` | 3 | 0 %, 15 % y 25 % de cada estrategia |
| `PedidoServiceImplTest` | 7 | Cálculos por tipo (con mocks), `notasInternas` persistida, tipo desconocido → REGULAR, `RecursoNoEncontradoException` |
| `PedidoControllerIntegrationTest` | 10 | Map con las 3 estrategias, POST 201 por tipo sin `notasInternas`, GET 200, 404, 400 con mensajes, JSON mal formado, id no numérico, 405 |
| `GlobalExceptionHandlerTest` | 1 | 500 genérico sin exponer detalles internos |
| `PedidosApplicationTests` | 1 | Arranque del contexto |

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/pedidos` | Crea un pedido y aplica el descuento según `tipoCliente` | 201, 400, 500 |
| GET | `/api/pedidos/{id}` | Obtiene un pedido por su id | 200, 400, 404, 500 |

### POST /api/pedidos — crear pedido

```bash
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteEmail":"ana@correo.com","tipoCliente":"VIP","subtotal":100000}'
```

Respuesta `201 Created`:

```json
{"id":1,"clienteEmail":"ana@correo.com","tipoCliente":"VIP","subtotal":100000.0,"descuento":15000.0,"iva":16150.0,"total":101150.0,"estado":"CREADO"}
```

### GET /api/pedidos/{id} — consultar pedido

```bash
curl -i http://localhost:8080/api/pedidos/1
```

Respuesta `200 OK`: el mismo `PedidoResponseDTO` (nunca incluye `notasInternas`).

### Error 404 — pedido inexistente

```bash
curl -i http://localhost:8080/api/pedidos/999
```

```json
{"status":404,"error":"Not Found","mensaje":"Pedido con id 999 no encontrado.","timestamp":"2026-11-14T03:29:31.758820796","path":"/api/pedidos/999"}
```

### Error 400 — datos inválidos

```bash
curl -i -X POST http://localhost:8080/api/pedidos -H "Content-Type: application/json" -d '{}'
```

```json
{"status":400,"error":"Bad Request","mensaje":"clienteEmail: El email del cliente es obligatorio; subtotal: El subtotal es obligatorio; tipoCliente: El tipo de cliente es obligatorio","timestamp":"2026-11-14T03:29:31.838764527","path":"/api/pedidos"}
```

El orden de los errores dentro de `mensaje` lo determina Spring y puede variar entre ejecuciones.

## Capturas de evidencia

### Parte 1 — Refactorización con SOLID, DTO y Strategy

**Compilación exitosa**

![Compilación exitosa con Maven](capturas/01-compilacion-exitosa.png)

**Checkpoint 1 — DTOs y PedidoRepository (DAO)**

![PedidoRequestDTO, PedidoResponseDTO y PedidoRepository](capturas/02-dto-repository.png)

**Checkpoint 2 — PedidoService con DIP**

![PedidoServiceImpl con inyección por constructor y controller que delega](capturas/03-service-dip.png)

**Checkpoint 3 — Strategy con los tres descuentos**

![POST con REGULAR, VIP y MAYORISTA mostrando el descuento aplicado](capturas/04-strategy-descuentos.png)

### Parte 2 — Manejo de errores, logging y documentación

**Checkpoint 1 — Error 404 con ApiError**

![GET /api/pedidos/999 responde 404](capturas/05-error-404.png)

**Checkpoint 1 — Error 400 con errores de validación**

![POST /api/pedidos con {} responde 400](capturas/06-error-400.png)

**Checkpoint 2 — Logs en consola**

![Logs SLF4J en consola](capturas/07-logs-consola.png)

**Checkpoint 2 — Archivo logs/pedidos.log**

![Contenido de logs/pedidos.log](capturas/08-logs-archivo.png)

**Checkpoint 3 — Swagger UI**

![Swagger UI con los endpoints documentados](capturas/09-swagger-ui.png)
