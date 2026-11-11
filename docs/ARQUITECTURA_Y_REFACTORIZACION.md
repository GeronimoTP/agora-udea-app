# Agora UdeA: arquitectura, auditoría y refactorización

Noviembre de 2026. Este documento tiene tres partes:

- **Arquitectura actual:** cómo está construido el sistema, sin juicio de valor.
- **Auditoría y refactorización:** qué se encontró al revisar los principios SOLID y las prácticas de clean code, qué se corrigió, cómo se verificó que nada se rompió y qué queda pendiente.
- **Evolución de la arquitectura:** opciones para cambiarla más adelante y cuál se recomienda. La arquitectura no se cambió en esta refactorización.

---

## 1. Arquitectura actual

### 1.1 Vista general

```
 Navegador
 ┌──────────────────────────────┐
 │ SPA React 19 + TanStack      │  Vercel (Nitro)
 │ Start / Router / Query       │
 └──────────────┬───────────────┘
                │ HTTPS + JSON, JWT en Authorization: Bearer
 ┌──────────────▼───────────────┐      ┌──────────────────┐
 │ API REST Spring Boot 4       │◄────►│ Google OAuth 2.0 │
 │ (monolito en capas)          │      └──────────────────┘
 │ Java 21, Docker              │  Render
 └──────────────┬───────────────┘
                │ JDBC (Hibernate, ddl-auto=validate)
 ┌──────────────▼───────────────┐
 │ PostgreSQL                   │  Supabase
 └──────────────────────────────┘
```

Es un **monolito en capas con frontend separado (SPA)**:

- **Un backend:** un solo proceso con una sola base de datos.
- **Autenticación sin sesión:** OAuth 2.0 con Google solo para identificar al usuario; después, cada petición lleva un JWT propio firmado con HS256.
- **Frontend:** consume la API REST.

### 1.2 Backend

Está organizado **por capa técnica** ("package by layer"):

| Paquete | Responsabilidad |
| --- | --- |
| `controller` | Endpoints REST: reciben DTOs validados, aplican la autorización de propiedad y delegan en un servicio. |
| `service` | Reglas de negocio y transacciones (`@Transactional`). Incluye los tres servicios de procesamiento del proyecto. |
| `service/analitica` | Fórmulas de indicadores (`CalculadoraIndicadores`) sobre los datos de un alcance. |
| `service/mapper` | Conversión de entidades a DTOs, un mapper por dominio. |
| `service/autorizacion` | Reglas de propiedad por recurso (perfil, semillero, postulación) e identidad del usuario autenticado. |
| `job` | Tareas programadas (`ExpiracionConvocatoriasJob`). |
| `repository` | Acceso a datos con Spring Data JPA (consultas derivadas y JPQL). |
| `model` | Entidades JPA (`tbl_*`) con algunos métodos de dominio. |
| `dto/request`, `dto/response` | Contratos de la API. |
| `dominio` | Nombres de estados y roles del catálogo. |
| `estructuras` | Estructuras de datos propias (`KdTree`). |
| `evento` | Eventos de aplicación (`SemillerosModificadosEvent`). |
| `security` | Filtro JWT, OAuth, CORS, reglas por rol. |
| `config` | OpenAPI y pesos configurables de los algoritmos (`Pesos*`, `@ConfigurationProperties`). |
| `exception` | Excepciones de negocio y manejador global (`@RestControllerAdvice`). |
| `util` | Utilidades sin estado (`Redondeo`). |

Una petición típica pasa por estas capas:

```
HTTP → JwtAuthenticationFilter (identidad y rol)
     → SecurityConfig (regla por rol)
     → Controller (@Valid DTO, service/autorizacion: ¿es su recurso?)
     → Service (@Transactional: reglas de negocio)
     → Repository (Spring Data JPA) → PostgreSQL
     ← Mapper / DTO de respuesta ← ManejadorExcepcionesGlobal si algo falla
```

Componentes transversales:

- **Autorización en dos niveles:** por rol en `SecurityConfig` y por propiedad en `service/autorizacion`. El segundo responde, por ejemplo, si el usuario es líder de ese semillero.
- **Tareas programadas** (`@Scheduled`, paquete `job`): una expira las ofertas vencidas y otra cierra las convocatorias que pasaron su fecha.
- **Configuración tipada:** los pesos de los tres algoritmos se leen de `application*.properties` y se validan al arrancar (cada grupo debe sumar 1).
- **Índice en memoria:** el árbol KD de semilleros similares. Se invalida con un evento después del commit y, como respaldo, cuando vence su vigencia.
- **Esquema externo:** Hibernate solo valida el esquema; los cambios se aplican con scripts SQL manuales (`db/migrations/V2`, `V3`).

Patrones presentes:

- **Arquitectura en capas.**
- **Repository** (Spring Data).
- **DTO.**
- **Mapper.**
- **Service Layer**, con lógica de tipo *transaction script*.
- **Observer:** eventos de Spring.
- **Builder:** Lombok.
- **Estructura de datos especializada:** árbol KD para búsqueda de vecinos más cercanos.

### 1.3 Frontend

Es una SPA con rutas basadas en archivos (TanStack Router). Las rutas que piden datos desactivan el SSR, porque el token vive en `localStorage`. Sus capas:

| Capa | Archivos | Responsabilidad |
| --- | --- | --- |
| Rutas | `src/routes/*.tsx` | Página, `loader` (carga previa y redirecciones), composición. |
| Consultas | `lib/api/queries.ts`, `lib/api/gestion.ts` | Claves de caché e invalidación (React Query). |
| Servicios | `lib/api/<dominio>.ts`: `semilleros`, `convocatorias`, `postulaciones`, `recomendaciones`, `analitica`, `perfiles`, `catalogos`, `gestion` | Llamadas a la API. Los módulos de vistas públicas adaptan los DTOs a tipos de vista; los de edición usan los DTOs tal cual. |
| Cliente HTTP | `lib/api/client.ts`, `lib/api/sesion.ts` | Token, tiempo de espera, manejo de 401, sesión en loaders. |
| Componentes | `components/common`, `components/gestion`, `components/layout`, `components/ui` (shadcn) | Interfaz reutilizable. |
| Utilidades | `lib/formato.ts`, `lib/utils.ts` | Formato de fechas y utilidades. |

### 1.4 Fortalezas y debilidades de la arquitectura actual

**Fortalezas**

- Es simple de entender, desplegar y operar con un equipo pequeño: un backend, una base de datos y un frontend estático.
- Las reglas de autorización están centralizadas y se aplican en el backend, no solo en la interfaz.
- Las operaciones críticas son transaccionales y usan bloqueo pesimista para que dos aceptaciones simultáneas no consuman el mismo cupo.
- Los servicios de procesamiento (recomendación, gestor de convocatorias y analítica) están identificados y separados del CRUD.

**Debilidades**

- **Organización por capa técnica.** Una funcionalidad (por ejemplo, postulaciones) está repartida en ocho paquetes (controlador, servicio, mapper, autorización, repositorio, modelo y DTOs de entrada y salida), y `service` tiene 27 clases (42 con sus subpaquetes) sin límites entre dominios.
- **Dominio mayormente anémico.** Las entidades son casi solo datos; las reglas viven en los servicios. Esta refactorización empezó a mover reglas al modelo.
- **Servicios acoplados a Spring Data JPA.** Probar una regla de negocio exige base de datos: solo `KdTree` tiene pruebas unitarias.
- **La analítica calcula en memoria.** `AnaliticaService` carga las tablas completas (`findAll`) y calcula los indicadores en Java. Son pocas consultas y con el volumen actual es lo más simple, pero con años de datos convendría agregar en SQL (ver 2.5).
- **Estado en memoria y tareas programadas en cada instancia.** Con más de una instancia del backend, las tareas se ejecutarían varias veces y el índice KD podría diferir entre instancias.
- **Migraciones manuales.** No hay Flyway ni Liquibase; el orden de los scripts depende de que alguien los ejecute.
- **El JWT vive en `localStorage`.** Es vulnerable si hubiera XSS.

---

## 2. Auditoría SOLID y refactorización

### 2.1 Cómo se verificó que nada se rompió

Antes de tocar el código se construyó una **prueba de caracterización** de la API:

- **Cobertura:** el script llama a 79 rutas de lectura con 6 usuarios (administrador, líder, profesor y tres estudiantes en situaciones distintas) y a un anónimo, antes y después de un flujo completo de escrituras:
  - postular, pre-aprobar y aceptar;
  - crear, editar, cerrar y eliminar convocatorias;
  - proyectos, publicaciones, autores y reconocimientos;
  - líneas del semillero, retiro de integrantes, edición de perfil;
  - intentos no autorizados.
- **Qué guarda:** el código HTTP y el cuerpo de las 988 respuestas, sin marcas de tiempo.
- **Entorno:** cada ejecución arranca una base PostgreSQL nueva con los datos de prueba, así que el resultado es determinista. Dos ejecuciones de la versión original dieron **0 diferencias**.

Después de cada paso de la refactorización se compararon las respuestas contra la versión original. Al final hay **123 diferencias, todas intencionales**:

| Diferencias | Causa |
| --- | --- |
| 96 | El JSON de líneas y estados ya no expone la entidad JPA, y las búsquedas `/buscar` ahora ordenan por nombre (mismo contenido). |
| 24 | El endpoint nuevo de indicadores por semillero (antes 403 o 404). |
| 2 | Un estudiante ya no puede crear un área ni renombrar un estado (antes 201 y 200; ahora 403). |
| 1 | El líder ve los indicadores de su semillero dentro del flujo de escritura. |

En una segunda ronda (mappers, consultas agregadas, división del gestor y de la autorización, pesos configurables y división del cliente HTTP del frontend) se generó una línea base nueva desde el commit anterior y se repitió la comparación: **0 diferencias en 988 casos**. Esa ronda no cambió el comportamiento.

Una tercera ronda (consultas por lote en el motor de recomendación, la priorización, los semilleros similares y los listados de postulaciones, publicaciones, proyectos, estudiantes y profesores) también dio **0 diferencias en 988 casos**.

Además:

- **Pruebas unitarias:** las 5 de `KdTreeTest` siguen pasando.
- **Frontend:** `tsc`, `eslint` (0 errores) y `vite build` pasan.
- **Interfaz:** se revisaron capturas de las pantallas afectadas en escritorio y móvil.
- **Indicadores por semillero:** se validaron contra consultas SQL directas.
- **Consultas SQL:** se contaron con `log_statement=all` en PostgreSQL, con los datos de prueba (5 semilleros, 30 estudiantes, 79 postulaciones, 14 publicaciones). Las cifras incluyen unas 8 sentencias fijas de autenticación y transacción. Después del cambio, ninguna crece con el número de elementos:

  | Endpoint | Antes | Después |
  | --- | --- | --- |
  | `GET /api/semilleros` | 52 | 15 |
  | `GET /api/recomendaciones/estudiante/{id}` | 54 | 22 |
  | `GET /api/recomendaciones/estudiante/{id}/similares` | 23 | 19 |
  | `GET /api/postulaciones/semillero/{id}/priorizacion` (2 aspirantes) | 24 | 20 |
  | `GET /api/postulaciones` | 97 | 12 |
  | `GET /api/postulaciones/semillero/{id}` | 40 | 18 |
  | `GET /api/publicaciones` | 73 | 9 |
  | `GET /api/proyectos` | 18 | 8 |
  | `GET /api/estudiantes` | 73 | 9 |
  | `GET /api/profesores` | 15 | 9 |

  La priorización mejora poco con 2 aspirantes porque antes hacía 4 consultas por aspirante: con 30 aspirantes habrían sido unas 136.

### 2.2 Hallazgos por principio

#### S: Responsabilidad única

| Hallazgo | Acción |
| --- | --- |
| `SemilleroService` (578 líneas) tenía cinco responsabilidades: CRUD, líneas, tutores, integrantes y liderazgo, además de la conversión a DTOs. | Se dividió en `SemilleroService` (datos, estado, líneas; 344 líneas), `MiembrosSemilleroService` (tutores, integrantes, liderazgo) y `SemilleroMapper` (DTOs). La regla "un estudiante lidera un solo semillero" pasó a `LiderazgoSemilleroService`. |
| `AnaliticaService` (393 líneas) mezclaba carga de datos, fórmulas y 9 DTOs anidados. | Se separó: `AnaliticaService` carga los datos (198 líneas), `CalculadoraIndicadores` aplica las fórmulas y los DTOs están en `dto/response/analitica`. |
| Había DTOs declarados dentro de servicios (`MotorRecomendacionService`, `PriorizacionCandidatosService`, `GestorConvocatoriasService`). | Se movieron a `dto/response`. |
| `SemilleroController` filtraba los integrantes activos. | El filtro está en el servicio (`obtenerIntegrantesActivos`) y se hace con una consulta. |
| Ocho servicios convertían sus entidades a DTOs en un método privado, y cinco repetían el `builder()` de sus asociaciones al crear y al listar. | Un mapper por dominio en `service/mapper` (`ConvocatoriaMapper`, `PostulacionMapper`, `ProyectoMapper`, `PublicacionMapper`, `ReconocimientoMapper`, `EstudianteMapper`, `ProfesorMapper`, `UsuarioMapper`). Cada conversión existe una sola vez. |
| `GestorConvocatoriasService` (414 líneas) mezclaba el flujo de doble vía, las tareas programadas, la expiración, la vinculación del admitido y la consulta de cupos. | El gestor quedó con el flujo y los cierres del líder (260 líneas). `ExpiracionPostulacionesService` marca `Expirada`, `ExpiracionConvocatoriasJob` tiene los crons, `MiembrosSemilleroService.incorporarAdmitido` vincula al admitido y `ConvocatoriaService.obtenerEstadoCupos` responde la consulta de cupos. |

#### O: Abierto/cerrado

| Hallazgo | Acción |
| --- | --- |
| Para calcular indicadores de otro alcance había que copiar las 8 fórmulas. | Las fórmulas operan sobre `DatosAnalitica`; un alcance nuevo solo necesita un cargador de datos. Así se agregó la vista por semillero sin tocar las fórmulas. |
| Agregar o renombrar un estado obligaba a editar 10 servicios con constantes propias. | Los nombres están una sola vez en `dominio`, y las preguntas sobre estado son métodos del modelo. |
| Ajustar un peso del motor, de la priorización o del árbol KD exigía editar y recompilar el servicio. | Los pesos son records `@ConfigurationProperties` (`PesosRecomendacion`, `PesosPriorizacion`, `PesosSemillerosSimilares`) con los mismos valores por defecto. Se cambian por propiedad o variable de entorno, y la aplicación no arranca si un grupo no suma 1. |

#### L: Sustitución de Liskov

No hay jerarquías propias que la violen. Las excepciones de negocio extienden `RuntimeException` de forma coherente, y los repositorios respetan el contrato de `JpaRepository`. No se requirió acción.

#### I: Segregación de interfaces

| Hallazgo | Acción |
| --- | --- |
| Para obtener un estado, seis servicios dependían de `EstadoRepository` con su propia copia de la consulta. | `BuscadorEstados` ofrece solo las búsquedas que necesitan (`dePostulacion`, `deConvocatoria`, `porIdEnCategoria`…). `EstadoService` quedó solo para el CRUD del catálogo. |
| `AutorizacionService` tenía 15 métodos públicos y 10 dependencias; cada controlador dependía de todo aunque usara uno o dos métodos. | Se dividió por recurso en `service/autorizacion`: `AutorizacionPerfilService`, `AutorizacionSemilleroService`, `AutorizacionPostulacionService` y `UsuarioAutenticadoService`. Cada controlador inyecta solo las que usa (ocho de diez usan una sola). |

#### D: Inversión de dependencias

| Hallazgo | Acción |
| --- | --- |
| Los controladores de áreas, líneas, habilidades y estados recibían y devolvían entidades JPA. La API quedaba atada al esquema de la base, permitía asignación masiva de campos y el JSON incluía `hibernateLazyInitializer`. | Usan `*RequestDTO` validados y `*ResponseDTO`. |
| Los servicios dependen de repositorios de Spring Data, que son abstracciones del framework y no del dominio. | Se mantuvo; ver la sección 3. |

### 2.3 Hallazgos de clean code

| Problema | Antes | Después |
| --- | --- | --- |
| Constantes de estado duplicadas (`ESTADO_*`) | 35 en 10 servicios | 0: clases de `dominio` |
| Copias del método `buscarEstado` | 6 | 1 (`BuscadorEstados`) |
| Copias de "¿tiene convocatoria abierta?" | 3 | 1 (`Convocatoria.recibePostulaciones` + `ConvocatoriaRepository.idsSemillerosRecibiendoPostulaciones`) |
| Redondeos `Math.round(x * 100.0) / 100.0` repetidos | 16 | `util.Redondeo` |
| `catch (Exception)` que devolvía 0 y ocultaba errores | 4 (motor de recomendación) | 0 |
| Números mágicos en los puntajes | `0.40`, `0.25`, `0.20`, `0.15`… en tres servicios | Configuración tipada y validada (`config/Pesos*`) |
| Consultas por elemento | 5 por semillero al listarlos; 1 o 2 por postulación, publicación (más 1 por autor), proyecto, estudiante y profesor; unas 8 por semillero en el motor; 4 por aspirante en la priorización | Número fijo: consultas por lote (`IN :ids`) y agregadas (`GROUP BY`), `aResponses` en los mappers y `default_batch_fetch_size` |
| Métodos privados de conversión a DTO | 8 servicios, con 5 `builder()` duplicados | 9 mappers en `service/mapper` |
| Métodos de servicio sin uso | 6 | 0 |
| Consultas de repositorio sin uso | 5 | 0 |
| Métodos heredados que creaban cuentas sin identidad de Google (riesgo de seguridad) | 3 | 0 |
| Búsquedas "por nombre" con `findAll()` y filtro en memoria | 3 | Consultas derivadas en la BD |
| Imports con comodín del propio proyecto | 21 | 0, más los imports sin uso eliminados |
| Sangría inconsistente | `EstudianteService` con 8 espacios por nivel | 4 espacios |
| Frontend: copias de `formatearFecha` | 4, tres con un error: mostraban el día anterior en Colombia | 1 (`lib/formato.ts`) |
| Frontend: bloque repetido de carga de sesión en loaders | 4 | `requerirSesion()` |
| Frontend: errores de lint (formato) | 605 | 0 |
| Frontend: `services.ts` con todos los dominios | 772 líneas | 7 módulos por dominio, el mayor de 195 líneas |
| Frontend: funciones de la API sin uso | 7 (`estudianteService` completo, `export default` y 5 métodos) | 0 |

### 2.4 Cambios de comportamiento

La refactorización buscó no cambiar el comportamiento. Estos son los únicos cambios visibles:

1. **Seguridad:** crear, editar o eliminar áreas, líneas, habilidades y estados ahora es solo del administrador. Antes cualquier usuario con sesión podía, por ejemplo, renombrar el estado "Abierta".
2. **Funcionalidad nueva:** pestaña **Indicadores** en la gestión del semillero, para líderes y tutores (`GET /api/analitica/semillero/{id}`).
3. **JSON de catálogos:**
   - Las líneas devuelven `idAreaEspecialidad` y `nombreAreaEspecialidad` en lugar del objeto `areaEspecialidad`.
   - Los estados devuelven `nombreCategoria` en lugar de `categoriaEstado`.
   - El frontend solo usa `id` y `nombre`, así que no le afecta.
4. **Búsquedas `/buscar` de catálogos:** ordenan por nombre.
5. **Fechas:** las fechas sin hora se muestran el día correcto.
6. **Mensaje de error unificado:** al elegir un estado de otra categoría, ahora dice "El estado 'X' no pertenece a la categoría 'Y'".

Después de la refactorización se implementaron las decisiones de la reunión con el profesor. Esos sí son cambios de reglas de negocio, documentados en `CONTEXTO.md`:

- Aprobación de semilleros por el administrador.
- Requisitos obligatorios u opcionales en las convocatorias.
- Aceptar una oferta ya no cancela las postulaciones a otros semilleros.
- Retiro voluntario con aprobación de un líder o tutor.
- Solicitudes de catálogo al administrador.
- Accesos a datos propios por token (`/me`, `/mias`) y listados sin correos.
- Factor "habilidades del equipo" en el motor de recomendación.

Requieren la migración V4. Se verificaron con 108 comprobaciones contra la API sobre una base con los datos de prueba.

### 2.5 Pendientes de refactorización

Ya se hicieron: consultas agregadas en el listado de semilleros, mappers del resto de servicios, división de `GestorConvocatoriasService` y de `AutorizacionService`, pesos configurables, división del cliente HTTP del frontend y consultas por lote en el motor de recomendación, la priorización y los demás listados.

Quedan, en orden de prioridad:

1. **Pruebas automatizadas:**
   - Llevar la prueba de caracterización al repositorio como prueba de integración con Testcontainers.
   - Agregar pruebas unitarias de `CalculadoraIndicadores` (es pura, no necesita base de datos).
2. **Analítica en SQL:** `AnaliticaService` carga tablas completas y calcula en memoria; la vista por semillero, además, vuelve a cargar toda la universidad para la referencia. Con el volumen actual responde rápido. Si crece, conviene calcular los totales con consultas agregadas o guardar en caché la referencia institucional por unos minutos.
3. **Estilo de tipos en el frontend:** hay dos formas, documentadas en la cabecera de cada módulo de `lib/api`. Las vistas públicas mapean el DTO a un tipo de vista; la edición (`gestion`, `perfiles`, `catalogos`) usa el DTO tal cual porque necesita los IDs y los nombres exactos de estado. Unificarlas en una sola implicaría reescribir los componentes de gestión, y hoy no lo justifica.

---

## 3. Evolución de la arquitectura

No se cambió la arquitectura. Estas son las opciones, de menor a mayor costo.

### Opción A: Monolito modular por funcionalidad (recomendada)

Se reorganizan los paquetes por dominio en lugar de por capa. Cada módulo contiene sus controladores, servicios, repositorios y DTOs:

```
com.udea.agora_backend
├── identidad        (usuarios, OAuth, JWT, perfiles)
├── catalogos        (áreas, líneas, habilidades, estados)
├── semilleros       (semilleros, miembros, proyectos, publicaciones, reconocimientos)
├── convocatorias    (convocatorias, postulaciones, gestor, priorización)   ← Servicio 2
├── recomendacion    (motor, árbol KD)                                       ← Servicio 1
├── analitica        (indicadores)                                           ← Servicio 3
└── compartido       (dominio, util, excepciones, seguridad base)
```

- **Límites verificados:** Spring Modulith comprueba en una prueba que un módulo solo use la API pública de otro. Por ejemplo, analítica lee de convocatorias, pero convocatorias no conoce a analítica.
- **Coherencia con el proyecto:** los tres servicios de procesamiento quedan como módulos visibles, que es como se presentan.
- **Bajo riesgo:** es mover archivos y ajustar visibilidad, sin reescribir lógica. Se puede hacer módulo por módulo, verificando con la prueba de caracterización.

### Opción B: Arquitectura hexagonal (puertos y adaptadores) en el núcleo

Se aplica solo donde hay reglas complejas: el flujo de postulaciones y cupos y el motor de recomendación.

- El dominio queda en clases Java puras que dependen de interfaces propias (puertos), por ejemplo `RepositorioPostulaciones` o `RelojDelSistema`.
- JPA y la web son adaptadores que implementan esos puertos.
- **Ganancia:** las reglas de negocio se prueban sin base de datos y quedan independientes del framework (DIP completo).
- **Costo:** más clases y duplicar modelos (dominio y entidad JPA). Tiene sentido después de la opción A, y solo en los módulos con lógica rica; para el CRUD no compensa.

### Opción C: Microservicios (no recomendada por ahora)

Separar recomendación, convocatorias y analítica en servicios desplegables independientes. Con un equipo pequeño, una sola base de datos compartida y la infraestructura gratuita de Render y Supabase, agregaría:

- comunicación en red y consistencia eventual en el flujo de cupos, que hoy es transaccional;
- más despliegues y más costo;

y no resolvería ningún problema actual. Solo se justificaría si un módulo, por ejemplo la analítica, necesitara escalar por separado.

### Mejoras de infraestructura, independientes de la opción elegida

- **Flyway:** migraciones versionadas que se aplican al arrancar, en lugar de scripts manuales.
- **ShedLock y caché compartida:** necesarios para más de una instancia del backend (tareas programadas e índice KD).
- **Cookies `HttpOnly`:** si el frontend y la API comparten dominio, el JWT podría viajar en una cookie `HttpOnly` + `SameSite` en lugar de `localStorage`.

**Recomendación:**

1. Adoptar la **opción A** con Spring Modulith.
2. Introducir Flyway y pruebas de integración con Testcontainers.
3. Evaluar la **opción B** solo para el módulo de convocatorias si el flujo de cupos sigue creciendo, por ejemplo con requisitos de convocatoria o aprobación de semilleros.
