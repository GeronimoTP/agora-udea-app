# Contexto del Proyecto Agora UdeA

## Propósito
Agora UdeA administra semilleros de investigación de la Universidad de Antioquia: perfiles de estudiantes y profesores, líneas y áreas de investigación, convocatorias, postulaciones, proyectos, publicaciones, reconocimientos y analítica institucional.

Este documento describe el código y flujo revisados. Cuando exista una diferencia entre esta guía y la implementación, prevalece el código actual; actualiza la guía junto con el cambio.

## Estructura
- `backend/agora-backend`: API Spring Boot, entidades JPA, repositorios, servicios, controladores y seguridad.
- `frontend`: aplicación React con TanStack Start, TanStack Router y React Query.
- `Dockerfile` y `docker-compose.yml`: ejecución local del backend y PostgreSQL.
- `frontend/src/routeTree.gen.ts`: archivo generado; no editar manualmente.
- `frontend/src/routes/README.md`: convenciones de rutas TanStack.
- `frontend/AGENTS.md`: advertencia de Lovable; no reescribir historia publicada.

## Tecnologías
- Backend: Java 21, Spring Boot 4.1.1, Spring MVC, Spring Security, OAuth2 Client, Spring Data JPA, PostgreSQL, Lombok, JJWT y springdoc.
- Frontend: React 19, TypeScript, TanStack Start/Router/Query, Vite 8 y Nitro.
- Producción: backend en Render; frontend en Vercel; base de datos PostgreSQL/Supabase.
- El frontend usa `apiRequest` en `frontend/src/lib/api/client.ts` para enviar `Authorization: Bearer ...` desde `localStorage.access_token`. Parsea JSON cuando la respuesta es `application/json` y texto plano en otro caso (las acciones de postulación devuelven texto).
- El proyecto compila con Java 21 (`pom.xml`). Si VS Code reporta "Flexible Constructor Bodies" sobre `@RequiredArgsConstructor` es un falso positivo de la extensión Red Hat Java (1.57); `./mvnw compile` es la referencia. No subir `java.version`.

## Modelo de Datos
Las entidades JPA se mapean a tablas `tbl_*`. Los grupos principales son:

- Identidad y roles: `tbl_usuarios`, `tbl_roles`, `tbl_oauth_proveedor`.
- Personas y oferta académica: `tbl_estudiantes`, `tbl_profesores`, `tbl_facultades`, `tbl_programas_academicos`.
- Investigación: `tbl_semilleros`, `tbl_proyectos`, `tbl_convocatorias`, `tbl_postulaciones`, `tbl_publicaciones`, `tbl_reconocimientos_semilleros`.
- Catálogos: `tbl_estados`, `tbl_categorias_estado`, `tbl_areas_especialidad`, `tbl_linea_investigacion`, `tbl_habilidades`, `tbl_entidades_otorgantes`, `tbl_tipos_reconocimiento`.
- Asociaciones: líneas de investigación de estudiantes/semilleros, profesores de semilleros, estudiantes de semilleros/proyectos, habilidades de postulaciones y usuarios de publicaciones.
- Roles de autoría: `tbl_roles_autor`. Roles de estudiante en proyectos: `tbl_roles_estudiante_proyecto`. Estos no son roles de cuenta.

Relaciones centrales:
- `Usuario` tiene proveedor OAuth, `oauthId` único, rol y fecha de creación obligatorios.
- `Estudiante` y `Profesor` tienen relación uno-a-uno con `Usuario`; ambos requieren programa académico.
- Un estudiante requiere semestre. Las asociaciones a líneas de investigación se guardan en su tabla de vínculo.
- Un semillero tiene estudiante líder, programa y estado; sus líneas y profesores se asocian mediante tablas de vínculo.
- Una convocatoria pertenece a un semillero y tiene cupos totales/disponibles, fechas y estado.
- Una postulación enlaza estudiante y convocatoria; sus habilidades son asociaciones aparte.
- Un proyecto pertenece a un semillero; una publicación pertenece a un proyecto; la autoría enlaza publicación, usuario y rol de autor.
- Las fechas de creación se almacenan como `ZonedDateTime`/`timestamptz` según las entidades.

En producción `spring.jpa.hibernate.ddl-auto=validate`: Hibernate valida el esquema, no lo crea ni lo migra. Cambios de esquema requieren SQL/migración controlada. Verifica que las FK de perfiles apunten desde `tbl_estudiantes.id_usuario` y `tbl_profesores.id_usuario` hacia `tbl_usuarios.id`, no al revés.

## Servicios del sistema
El proyecto se organiza en tres servicios de procesamiento: cada uno toma datos del sistema y produce información nueva que no existe guardada en la BD.

| Servicio | Clases | Toma | Produce |
| --- | --- | --- | --- |
| 1. Motor de recomendación | `MotorRecomendacionService`, `RecomendacionController` | Perfil del estudiante, habilidades de sus postulaciones, líneas y áreas, tutores y convocatorias vigentes | Puntaje de afinidad 0–100 y ranking de semilleros para cada estudiante |
| 2. Gestor de convocatorias | `GestorConvocatoriasService`, `PriorizacionCandidatosService`, `PlazoOfertasService` | Postulaciones, cupos, fechas, historial de ofertas de cada estudiante | Flujo de doble vía (ofertas, cascada, cierre, expiración) y **priorización de candidatos** 0–100 por convocatoria |
| 3. Analítica institucional | `AnaliticaService`, `AnaliticaController` | Todas las postulaciones, proyectos, publicaciones, reconocimientos y vinculaciones | 8 indicadores: demanda, tiempo de respuesta, retención de ofertas, conversión proyecto-publicación, participación estudiantil, reconocimientos, habilidades emergentes e interdisciplinariedad |

Las reglas de cada servicio están en "Recomendaciones", "Postulaciones y cupos" (incluida "Priorización de candidatos") y "Analítica".

## Reglas de Negocio

### Cuentas y perfiles
- El acceso Google exige correo terminado en `@udea.edu.co` y usa el atributo `sub` de Google como `oauthId` estable.
- El proveedor guardado en `tbl_oauth_proveedor` debe tener el nombre `Google`.
- Roles de cuenta (`tbl_roles`): `Estudiante`, `Profesor` y `Administrador`.
- No existe rol de cuenta `Pendiente`. No lo agregues ni lo uses para controlar onboarding.
- El estado `Pendiente` sí existe como estado de una postulación; no confundirlo con un rol de usuario.
- El login con Google **no** guarda nada en `tbl_usuarios`. La cuenta se persiste solo cuando se completa el formulario de perfil, junto con su rol definitivo.
- Un estudiante debe elegir programa, semestre >= 1 y al menos una línea de investigación.
- Un profesor debe elegir programa y al menos un área de especialidad.
- El usuario, el perfil y sus asociaciones se crean en una transacción.
- Un usuario solo puede tener un perfil de estudiante o uno de profesor. Evita crear filas duplicadas.
- El backend obtiene email, `oauthId` y nombre de claims firmados del token temporal; no confía en esos datos recibidos en el body.
- Página `/perfil` (estudiante y profesor; el administrador no tiene perfil): el estudiante cambia programa, semestre (>= 1) y líneas de investigación (mínimo 1); el profesor, programa y áreas de especialidad (mínimo 1). Usa `PUT /api/estudiantes/{id}` y `PUT /api/profesores/{id}`, que reemplazan líneas/áreas en una sola transacción, y solo el dueño del perfil puede editarlo. El correo y el nombre vienen de Google y no se editan. Se llega desde "Mi perfil" en el encabezado (`AppShell`).
- El semestre debe actualizarse cada periodo: el motor de recomendación y la priorización de candidatos usan el programa y las líneas del perfil.

### Cuenta administrador
- El administrador no tiene perfil de estudiante ni de profesor y no pasa por onboarding.
- Se crea manualmente en la BD. Como el `sub` de Google no se conoce de antemano, se inserta con un `oauth_id` provisional:
  ```sql
  INSERT INTO tbl_usuarios (nombre_completo, email, id_proveedor_oauth, oauth_id, id_rol, created_at)
  VALUES (
    'Administrador Agora',
    '<correo-admin>@udea.edu.co',
    (SELECT id FROM tbl_oauth_proveedor WHERE nombre = 'Google'),
    'pendiente-vinculacion-admin',
    (SELECT id FROM tbl_roles WHERE nombre = 'Administrador'),
    now()
  );
  ```
- En su primer login, si no existe usuario con ese `sub` pero sí un usuario `Administrador` con el mismo correo y Google marca `email_verified=true`, `OAuth2LoginSuccessHandler` reemplaza el `oauth_id` provisional por el `sub` real. Esta excepción aplica solo al rol `Administrador`; para el resto, `CustomOAuth2UserService` rechaza un correo asociado a otro `sub`.
- Con perfil de rol `Administrador`, el login emite el JWT normal y el frontend lo envía a `/analitica`.

### Login de pruebas y datos de prueba
- `POST /api/auth/login-pruebas` (`{correo, clave}`) emite un JWT normal sin pasar por Google. `GET` del mismo endpoint devuelve `{habilitado}` y el formulario "Acceso de pruebas" de `/auth/login` solo aparece si está habilitado. Ambos son públicos en `SecurityConfig`.
- `LoginPruebasService` lo habilita solo si `APP_LOGIN_PRUEBAS_CLAVE` (`app.login-pruebas.clave`) tiene al menos 16 caracteres; sin ella responde 404. Compara la clave en tiempo constante y solo acepta usuarios con `oauth_id` que empieza por `seed-`: una cuenta real creada por Google nunca entra por aquí. Vaciar la variable antes de la entrega.
- `backend/agora-backend/db/seed/datos_prueba.sql` carga 1 administrador, 5 profesores, 5 estudiantes líderes, 25 estudiantes y 5 semilleros (`SEED-01`…`SEED-05`) con convocatorias, postulaciones en todos los estados, vinculaciones, proyectos, publicaciones y reconocimientos coherentes con las reglas, para que la analítica tenga datos. Los correos son `@seed.agora.test` (`admin@`, `profesor1..5@`, `lider1..5@`, `estudiante01..25@`). Usa los catálogos existentes por nombre (sin IDs fijos), las fechas son relativas al día de ejecución y no se puede ejecutar dos veces. `limpiar_datos_prueba.sql` lo deshace.
- El menú (`AppShell`) tiene "Cerrar sesión": borra `access_token` y limpia la caché de React Query para que el siguiente usuario no vea datos del anterior.
- Los datos de prueba crean ofertas `Pre-aprobada` con fecha relativa al día de carga: con el plazo de respuesta (3 días por defecto) expiran poco después. Para probar ofertas, pre-aprueba desde la app como líder.

### Experiencia por rol (frontend)
`AppShell` es un menú lateral fijo a la izquierda (desde `lg`; en pantallas pequeñas se abre con un botón en la barra superior) con la navegación, "Mi semillero", el nombre y rol del usuario, "Mi perfil" y "Cerrar sesión". El contenido ocupa el resto del ancho hasta 1600 px (`max-w-[1600px]`), y las cuadrículas agregan columnas en `xl`/`2xl`. El login (`/auth/login`) divide la pantalla: panel institucional verde a la izquierda (oculto en pantallas pequeñas) y formulario a la derecha.

La navegación (`AppShell`) y las rutas se filtran con `tipoPerfil(sesion)` de `frontend/src/lib/api/queries.ts` (`ADMINISTRADOR`, `ESTUDIANTE`, `PROFESOR`):

| Ruta | Estudiante | Profesor | Administrador |
| --- | --- | --- | --- |
| `/` Tablero | Convocatorias, sus postulaciones, ofertas pendientes con su fecha límite y recomendaciones. "Semilleros en los que participas" si integra uno o varios. Sin semillero liderado: opción "Registrar semillero". Líder: sección "Semilleros que lideras" | Convocatorias y "Semilleros que lideras" (con "Registrar otro semillero"). Sin semilleros: mensaje que explica cómo vincularse, con "Registrar semillero" y "Explorar semilleros" | Redirige a `/analitica` |
| `/semilleros/nuevo` | Registrar semillero (solo si aún no lidera uno) | Registrar semillero eligiendo al estudiante líder; queda como tutor (puede registrar varios) | Redirige a `/` |
| `/gestion/$idSemillero` | Gestión completa si es el estudiante líder | Gestión completa si es tutor | Redirige a `/` |
| `/semilleros` y detalle | Ver y postularse. En un semillero que integra ve "Eres integrante" y en uno que lidera no puede postularse (el detalle explica el motivo) | Solo ver | Solo ver |
| `/postulaciones` | "Mis postulaciones": aceptar/declinar ofertas pre-aprobadas antes de su fecha límite. Si es líder de un semillero, también "Postulaciones recibidas" (pestañas) | "Postulaciones recibidas" ordenadas por la priorización del Servicio 2 (posición, puntaje, desglose y observaciones); pre-aprobar se desactiva cuando las ofertas vigentes igualan los cupos | Redirige a `/` |
| `/perfil` | Editar programa, semestre y líneas | Editar programa y áreas | Redirige a `/` |
| `/compatibilidad` | Ranking del motor de recomendación | Redirige a `/` | Redirige a `/` |
| `/analitica` | Redirige a `/` | Redirige a `/` | KPIs institucionales |

La gestión (`/gestion/$idSemillero?seccion=...`) tiene pestañas: Información (datos, líneas, estado, transferir liderazgo, eliminar), Convocatorias (crear, editar, cerrar, cancelar, eliminar), Integrantes y tutores (retirar integrantes, agregar/quitar tutores), Proyectos (CRUD, estado, integrantes con rol), Publicaciones (CRUD y autores) y Reconocimientos (CRUD). Se entra desde "Semilleros que lideras" en el tablero, el ítem "Mi semillero" del menú (si lidera exactamente uno) o el botón "Gestionar" del detalle público. Usa los DTOs del backend sin mapear (`frontend/src/lib/api/gestion.ts`) y todas sus consultas cuelgan de la clave `["gestion", id]`.

Ocultar en el frontend no es seguridad: cada restricción tiene su regla equivalente en el backend.

### Autorización (backend)
- `JwtAuthenticationFilter` carga el rol del usuario desde la BD y lo expone como autoridad `ROLE_<nombre del rol>` (por ejemplo `ROLE_Administrador`). El token de onboarding no recibe autoridades.
- `SecurityConfig`:
  - `/api/analitica/**` → `Administrador`.
  - `POST /api/postulaciones`, `POST /api/postulaciones/*/confirmar-aceptacion`, `POST /api/postulaciones/*/rechazar-estudiante`, `/api/recomendaciones/**` → `Estudiante`.
  - `POST /api/postulaciones/*/pre-aprobar`, `POST /api/postulaciones/*/rechazar-lider` → `Estudiante` o `Profesor`, y además el usuario debe ser líder del semillero de la postulación (ver abajo).
  - `POST /api/semilleros`, `POST /api/convocatorias` → `Estudiante` o `Profesor`.
  - Solo `Administrador`: `/api/usuarios/**` (excepto `GET /api/usuarios/me`), `POST /api/estudiantes`, `POST /api/profesores` (los perfiles se crean por onboarding), `GET /api/postulaciones` (listado global) y `POST /api/convocatorias/procesar-expiradas` (también expira ofertas vencidas).
  - Resto de rutas no públicas → autenticación, más las reglas de propiedad de `AutorizacionService`.
  - Sin token, o con un token inválido o vencido, `/api/**` responde 401 (no redirige a Google).
- Líderes de semillero: `LiderazgoSemilleroService` considera líder al estudiante `estudianteLider` del semillero y a los profesores vinculados en `tbl_profesor_x_semillero`. `GET /api/usuarios/me` devuelve en `idSemillerosLiderados` la unión de ambos casos.
- `AutorizacionService` concentra las reglas de propiedad (403 si no se cumplen):

| Acción | Quién |
| --- | --- |
| Editar o eliminar el semillero, cambiar su estado, sus líneas, profesores y estudiantes vinculados | Líder del semillero |
| Crear, editar, cerrar, cancelar o eliminar convocatorias | Líder del semillero |
| Crear, editar, cambiar estado o eliminar proyectos y vincular estudiantes a ellos | Líder del semillero del proyecto |
| Crear, editar o eliminar publicaciones y gestionar sus autores | Líder del semillero del proyecto de la publicación |
| Crear, editar o eliminar reconocimientos | Líder del semillero |
| Retirar integrantes y transferir el liderazgo | Líder del semillero |
| Ver postulaciones de un semillero o convocatoria, y su priorización de candidatos; pre-aprobar o rechazar | Líder del semillero |
| Crear, editar, retirar una postulación y sus habilidades; aceptar o declinar una oferta | El estudiante postulado |
| Ver una postulación concreta | El estudiante postulado, un líder del semillero o el administrador |
| Ver postulaciones o recomendaciones de un estudiante | Ese estudiante |
| Editar o eliminar un perfil y sus líneas/áreas | El dueño del perfil |
| Crear semillero | Un estudiante solo como líder de sí mismo; un profesor con cualquier líder, y queda vinculado como tutor |

- Liderar un semillero es una relación, no un rol: el estudiante líder conserva el rol `Estudiante` y su vista de líder sale de `idSemillerosLiderados` (`esEstudianteLider` en `queries.ts`).
- Si la base de datos cambia el nombre de un rol, las reglas `hasRole(...)` dejan de coincidir: mantener la capitalización exacta.

### OAuth y JWT
1. El frontend inicia OAuth redirigiendo a `/oauth2/authorization/google` en el backend.
2. `CustomOAuth2UserService` valida dominio y `sub`; no crea una fila incompleta.
3. `OAuth2LoginSuccessHandler` determina si la cuenta está completa (perfil de estudiante/profesor, o rol `Administrador`):
   - Cuenta completa: emite JWT normal.
   - Perfil ausente: emite token `token_type=onboarding` y redirige a `/oauth2/redirect` con `onboarding=true`.
   - En ambos casos el token va en el **fragmento** de la URL (`/oauth2/redirect#token=...&onboarding=...`) y la sesión HTTP del intercambio con Google se invalida.
   - Si Google o las reglas rechazan la cuenta, `OAuth2LoginFailureHandler` redirige a `/auth/login?error=<código>` (`invalid_google_account`, `account_conflict` o `login_fallido`) y el login muestra el mensaje.
4. El frontend lee el token del fragmento, lo borra de la barra de direcciones (`history.replaceState`), lo guarda y dirige al usuario a selección de rol (`/auth/select-role`) y completar perfil (`/auth/complete-profile`).
5. Los endpoints de onboarding verifican el token temporal, crean el perfil y devuelven `accessToken` normal. El frontend reemplaza el token temporal por ese JWT y carga la página principal.
6. El filtro JWT limita el token de onboarding a:
   - `GET /api/catalogos/programas-academicos`
   - `GET /api/lineas-investigacion`
   - `GET /api/areas-especialidad`
   - `POST /api/auth/onboarding/estudiante`
   - `POST /api/auth/onboarding/profesor`

`jwt.expiration` es el tiempo de vida del JWT normal (producción: 86.400.000 ms). El token de onboarding tiene `jwt.onboarding.expiration`, con 1.800.000 ms por defecto. `JwtService` espera `JWT_SECRET` en Base64 y firma HS256.

El fragmento (`#`) no se envía a ningún servidor ni aparece en el header `Referer`, así que el token no queda en logs de acceso de Vercel ni de terceros. Se guarda en `localStorage`; no lo escribas en logs. Si la API responde 401 a una petición con token, `apiRequest` lo borra y redirige a `/auth/login?error=sesion_expirada` (excepto en `/api/auth/*`, que manejan su propio 401).

### Semilleros y estados
- Crear un semillero (`POST /api/semilleros`, `SemilleroRequestDTO`):
  - Requiere código identificador único (mayúsculas, números y guiones), nombre (5–150), descripción, programa, al menos una línea de investigación y `idProfesores` con al menos un profesor ya registrado (si lo crea un profesor, él se añade solo).
  - Un estudiante lidera como máximo un semillero (también al cambiar de líder en la edición).
  - Nace en estado `Activo` (categoría "Estado Semillero"), sin aprobación del administrador por ahora.
  - El formulario `/semilleros/nuevo` lista los profesores de `GET /api/profesores` y los filtra en el cliente por nombre/correo, facultad, programa y área de especialidad.
  - Si lo usa un profesor, además elige al estudiante líder entre `GET /api/estudiantes` (sin los que ya lideran un semillero, según `idEstudianteLider` de `/api/semilleros`); los demás tutores son opcionales porque él queda vinculado. Un profesor puede tutorar varios semilleros sin límite.
  - La vinculación de un profesor como tutor no requiere su aceptación: lo incluye quien registra el semillero o un líder desde la gestión.
- Un semillero debe conservar al menos un profesor tutor y al menos una línea de investigación.
- El líder estudiantil solo cambia con `POST /api/semilleros/{id}/transferir-liderazgo/{idEstudiante}`: el nuevo líder debe ser integrante activo y no liderar otro semillero; el saliente queda como integrante activo. El `PUT` del semillero rechaza un cambio de líder.
- No se puede pausar ni inactivar un semillero con convocatorias `Abierta`.
- Eliminar un semillero solo es posible sin convocatorias, proyectos, integrantes ni reconocimientos; si tiene historial se inactiva.
- Un estudiante puede ser integrante activo de **varios semilleros a la vez**. `GET /api/usuarios/me` devuelve sus membresías activas en `idSemillerosIntegrante`; el tablero las lista y el detalle del semillero muestra "Eres integrante". Solo no puede postularse a un semillero que ya integra o que lidera. Aceptar una oferta sigue cancelando en cascada sus demás postulaciones en proceso.
- Integrantes (`tbl_estudiante_x_semillero`): activo = `fecha_salida` nula. `POST /api/semilleros/{id}/estudiantes/{idEstudiante}/retirar` pone fecha de salida hoy y estado `Inactivo` (categoría "Estado Usuario"). Un retirado puede volver a postularse y, si acepta una oferta, su vinculación se reactiva. `totalIntegrantes`, el chequeo "ya perteneces" y las recomendaciones solo cuentan integrantes activos.
- Proyectos: `fechaFin` >= `fechaInicio`; no cambian de semillero; solo el estudiante líder o integrantes activos se vinculan a ellos; no se eliminan si tienen publicaciones (al eliminar se borran sus vínculos con estudiantes).
- Publicaciones: solo se mueven a otro proyecto del mismo semillero; los autores deben ser el líder, un integrante activo o un tutor del semillero; al eliminar se borran sus autorías.
- Reconocimientos: no cambian de semillero; `urlCertificado` debe empezar por http(s).
- `GET /api/estados/categoria?nombre=Estado Semillero` lista los estados de una categoría (el frontend no usa IDs fijos).
- Cambiar el estado de un semillero o de un proyecto exige un estado de su categoría.
- Las convocatorias abiertas usan el estado `Abierta`; al crear una convocatoria, el cupo disponible inicia igual al total.
- Crear una convocatoria exige semillero `Activo`, `fechaInicio` >= hoy y `fechaCierre` >= `fechaInicio` y >= hoy.
- Editar una convocatoria:
  - No puede cambiar de semillero.
  - `cuposTotales` no puede ser menor que los cupos ya asignados; `cuposDisponibles` se recalcula como total − asignados.
  - Si los cupos disponibles quedan en 0 pasa a `Cerrada`; si estaba `Cerrada`, vuelve a tener cupos y su cierre no ha pasado, se reabre como `Abierta`.
- Una convocatoria con postulaciones no se elimina (422); se cierra o se cancela.
- El líder puede cerrar (`POST /api/convocatorias/{id}/cerrar` → `Cerrada`) o cancelar (`/{id}/cancelar` → `Cancelada`) una convocatoria `Abierta`. En ambos casos lo `Pendiente` o `Pre-aprobada` pasa a `Expirada` y los cupos asignados se conservan. Una convocatoria cancelada no se edita.
- El cron de expiración solo cierra convocatorias `Abierta` vencidas (no toca las canceladas).
- Los nombres de estado se repiten entre categorías (`Activo` en Usuario y Semillero, `Expirada` en Postulación y Convocatoria, `Cancelada` es de Convocatoria). Toda búsqueda de estado usa categoría + nombre: `EstadoRepository.findByCategoriaEstadoNombreIgnoreCaseAndNombreIgnoreCase` con las constantes de `CategoriasEstado`. No buscar solo por nombre.
- `GET /api/convocatorias/activas` usa el ID de estado `Abierta` y cupos disponibles > 0. El nombre de la ruta es histórico; el nombre guardado en `tbl_estados` es `Abierta`, no `Activa`.
- Los nombres de estado están en `tbl_estados`, ligados a `tbl_categorias_estado`. Al cambiar estados, revisa también la categoría y los nombres exactos usados por los servicios.

### Postulaciones y cupos
- No se permite más de una postulación del mismo estudiante a la misma convocatoria.
- Se permiten postulaciones activas simultáneas a distintas convocatorias.
- Para postularse, `PostulacionService.crear` exige que la convocatoria esté en estado `Abierta`, tenga cupos disponibles, que la fecha actual esté entre `fechaInicio` y `fechaCierre`, que el estudiante no sea el líder del semillero y que no sea ya integrante activo de él (sí puede integrar otros). Si no se cumple, lanza `ErrorLogicaNegocioException` (422).
- Una postulación solo se edita o retira (incluidas sus habilidades) mientras está `Pendiente`; así retirarla nunca altera cupos.
- El estudiante decide libremente sobre una oferta `Pre-aprobada`: puede aceptarla o rechazarla (por ejemplo, si prefiere otra oferta).
- Crear una postulación (`POST /api/postulaciones`, `PostulacionRequestDTO`) requiere `idConvocatoria`, `idEstudiante`, `respuestaMotivacion`, `disponibilidadHorasSemana` (>= 2), `experienciaPrevia` e `idHabilidades` (al menos una, de `/api/habilidades`). El formulario del detalle de semillero envía exactamente esos campos.
- Flujo de doble vía (todas las acciones son `POST`):
  - `Pendiente` → el líder (estudiante líder o profesor del semillero) puede pre-aprobar (`/{id}/pre-aprobar` → `Pre-aprobada`) o rechazar (`/{id}/rechazar-lider` → `Rechazada`).
  - `Pre-aprobada` → el estudiante confirma (`/{id}/confirmar-aceptacion` → `Aceptada`) o rechaza (`/{id}/rechazar-estudiante` → `Rechazada por Estudiante`).
- Pre-aprobar exige que la convocatoria siga `Abierta` y vigente, y registra `fechaPreAprobacion` (momento de la oferta). Pre-aprobar o rechazar no descuenta cupos.
- **Límite de ofertas**: las ofertas vigentes (`Pre-aprobada` dentro de su plazo) de una convocatoria no pueden superar sus cupos disponibles. Pre-aprobar bloquea la convocatoria (`PESSIMISTIC_WRITE`), expira primero las ofertas vencidas de esa convocatoria y responde 422 si ya hay tantas ofertas vigentes como cupos. Así, toda oferta aceptada a tiempo tiene cupo. Si el líder reduce los cupos al editar la convocatoria, las ofertas ya emitidas se conservan pero no se emiten nuevas hasta que haya espacio.
- **Plazo de respuesta**: el estudiante tiene `app.ofertas.dias-respuesta` días (por defecto 3, variable `APP_OFERTAS_DIAS_RESPUESTA`) desde `fechaPreAprobacion` para aceptar o declinar. `PostulacionResponseDTO.fechaLimiteRespuesta` lo expone y el frontend lo muestra. Una oferta vencida no se acepta ni se declina (422); el cron `app.scheduler.ofertas-vencidas.cron` (por defecto cada hora) la pasa a `Expirada` conservando `fechaPreAprobacion`, y libera el espacio para otro aspirante. El cierre de la convocatoria también expira las ofertas pendientes aunque no haya vencido su plazo.
- Confirmar aceptación, en una sola transacción:
  1. Bloquea la fila de la convocatoria (`findByIdParaActualizarCupos`, `PESSIMISTIC_WRITE`) para que dos aceptaciones simultáneas no consuman el mismo cupo.
  2. Exige que la convocatoria siga vigente y con cupos, y descuenta uno.
  3. Vincula al estudiante al semillero (`tbl_estudiante_x_semillero`, estado `Activo` de "Estado Usuario", porque la vinculación no tiene categoría propia; ver `CategoriasEstado.VINCULACION`) si no lo estaba.
  4. Marca las demás postulaciones activas del estudiante como `Cancelada en cascada`. `Rechazada por Estudiante` queda solo para la decisión explícita del estudiante.
  5. Si los cupos llegan a cero, la convocatoria pasa a `Cerrada` y lo que quedaba `Pendiente` o `Pre-aprobada` en ella pasa a `Expirada`.
- `fechaDecisionEstudiante` solo se registra cuando el estudiante acepta o declina; los rechazos del líder, la cascada y las expiraciones no la tocan.
- Un cron diario, configurable con `app.scheduler.convocatorias-expiradas.cron` (por defecto 01:00), cierra convocatorias expiradas y marca como `Expirada` lo que quedó sin resolver, también en convocatorias que ya estaban `Cerradas`.
- Estados de postulación (categoría "Estado Postulación" en `tbl_estados`): `Pendiente`, `Pre-aprobada`, `Aceptada`, `Rechazada`, `Rechazada por Estudiante`, `Cancelada en cascada`, `Expirada`. El frontend los refleja 1:1 (`PENDIENTE`, `PRE_APROBADA`, ..., `CANCELADA`).
### Priorización de candidatos (Servicio 2)
`GET /api/postulaciones/semillero/{idSemillero}/priorizacion` (solo líderes del semillero) puntúa de 0 a 100 cada postulación en proceso (`Pendiente` o `Pre-aprobada`) y la ordena dentro de su convocatoria (`posicion` 1 = mejor; empates por orden de llegada):
- Afinidad temática, 35%: líneas del estudiante que coinciden con las del semillero, sobre el total de líneas del semillero.
- Habilidades, 25%: habilidades declaradas en la postulación que el semillero ya usa (las de sus postulaciones `Aceptada`). Sin ese historial, valor neutral 50.
- Disponibilidad, 20%: horas semanales declaradas; 12 h o más dan el 100.
- Compromiso, 20%: promedio de cómo respondió el estudiante a ofertas anteriores (aceptó 100, aceptó otra y esta se canceló en cascada 75, declinó 50, la dejó expirar 0). Sin historial, valor neutral 50.

La respuesta incluye `cuposDisponibles`, `ofertasVigentes` y observaciones para el líder ("Tiene otra oferta vigente en …", "Ya integra N semillero(s)", "Sin historial de ofertas anteriores"). El frontend la combina con las postulaciones recibidas: muestra posición, puntaje, desglose y observaciones, y ordena primero las que siguen en proceso. El puntaje orienta la decisión; el líder sigue decidiendo.

- La columna `tbl_postulaciones.fecha_pre_aprobacion` se crea con `backend/agora-backend/db/migrations/V2__oferta_y_cancelacion_postulaciones.sql`, que debe ejecutarse antes de desplegar.

### Recomendaciones
`MotorRecomendacionService` solo considera semilleros `Activo` que el estudiante no lidera y a los que no pertenece. Los ordena por puntaje ponderado:
- Habilidades: 40%.
- Coincidencia con áreas de especialidad de tutores: 25%.
- Disponibilidad: 20%. Tiene una convocatoria `Abierta`, ya iniciada, no vencida y con cupos.
- Proximidad temática y de programa: 15%. Internamente es 75% coincidencia de líneas de investigación y 25% mismo programa académico.

El puntaje se limita a 0–100. Las habilidades del estudiante salen de sus postulaciones previas, porque el perfil no las guarda. Si el semillero no tiene historial de postulaciones, o el estudiante aún no se ha postulado a nada, el factor de habilidades usa un valor neutral de 50 en lugar de penalizar.

### Analítica
- Tiempo de respuesta del estudiante: va de `fechaPreAprobacion` a `fechaDecisionEstudiante`, solo en postulaciones `Aceptada` o `Rechazada por Estudiante`.
- Retención de ofertas: solo cuenta ofertas reales.
  - Aceptadas.
  - Rechazadas: `Rechazada por Estudiante` y `Cancelada en cascada` con oferta previa.
  - Expiradas: con oferta previa.
  - Las postulaciones que nunca recibieron oferta no cuentan.
- Interdisciplinariedad: se calcula sobre `tbl_estudiante_x_semillero`, que ahora se llena al aceptar una oferta.
- Una oferta que expira por plazo de respuesta cuenta como "Expirada con oferta previa" en la retención; no entra en el tiempo de respuesta, porque el estudiante no decidió.
- Las convocatorias no tienen habilidades requeridas en el modelo; el frontend oculta esa sección si la lista viene vacía.

## API Principal
- OAuth/onboarding: `/oauth2/authorization/google`, `/api/auth/onboarding/estudiante`, `/api/auth/onboarding/profesor`.
- Sesión: `GET /api/usuarios/me` (incluye `rol`, `idEstudiante`, `idProfesor`, `idSemillerosLiderados` e `idSemillerosIntegrante`).
- Catálogos: `/api/catalogos/*`, `/api/areas-especialidad`, `/api/lineas-investigacion`, `/api/habilidades`, `/api/estados`.
- Perfiles: `/api/estudiantes`, `/api/profesores`.
- Semilleros: `/api/semilleros`. El DTO incluye `nombreFacultad`, `totalProyectos`, `totalPublicaciones`, `totalIntegrantes` y `convocatoriaAbierta`.
- Proyectos y publicaciones de un semillero: `/api/proyectos/semillero/{id}`, `/api/publicaciones/semillero/{id}`.
- Convocatorias: `/api/convocatorias`, `/api/convocatorias/activas`, `/api/convocatorias/semillero/{id}`.
- Postulaciones: `/api/postulaciones`, `/api/postulaciones/estudiante/{id}`, `/api/postulaciones/semillero/{id}`, `/api/postulaciones/semillero/{id}/priorizacion` y las acciones del flujo de doble vía. El DTO incluye `fechaLimiteRespuesta`.
- Recomendaciones (solo estudiante): `/api/recomendaciones/estudiante/{id}`.
- Analítica (solo administrador): `/api/analitica/dashboard` y KPIs bajo `/api/analitica/*`.

Los servicios frontend adaptan los DTO del backend a tipos de vista con funciones `mapear*` en `services.ts`. Toda respuesta debe pasar por un mapeador: no tipar la respuesta cruda del backend con un tipo de vista. Al cambiar un endpoint, verifica ambos lados (ruta, método HTTP y cuerpo). El registro heredado por email (`AuthController`, `/api/auth/register/**`, `/api/auth/check-email`) se eliminó: el único alta de cuentas es el onboarding OAuth con identidad firmada.

## Seguridad
- **CORS**: configuración única en `SecurityConfig.corsConfigurationSource`, solo para `/api/**` y solo para los orígenes de `app.cors.allowed-origins` (`APP_CORS_ORIGINS`, separados por coma; por defecto `APP_FRONTEND_URL`). Sin cookies entre orígenes (`allowCredentials=false`). Los controladores ya no usan `@CrossOrigin`; no lo vuelvas a agregar. Si el frontend se abre desde otro dominio (por ejemplo, una URL de vista previa de Vercel), agrégalo a `APP_CORS_ORIGINS`.
- **Token fuera de la URL visible**: el callback OAuth entrega el JWT en el fragmento y el frontend lo borra de la barra de direcciones; la sesión HTTP del login con Google se invalida.
- **401 en la API**: `/api/**` responde 401 en lugar de redirigir a Google; el frontend cierra la sesión vencida y vuelve al login.
- **Errores de login**: `OAuth2LoginFailureHandler` devuelve al frontend solo códigos conocidos, nunca el detalle de la excepción.
- **Errores 500**: el cliente recibe un mensaje genérico; el detalle se registra en el log del servidor (sin `printStackTrace`).
- **Swagger**: apagado en producción (`springdoc.*.enabled`) salvo `APP_SWAGGER_HABILITADO=true`.
- **Actuator** (si se agrega la dependencia): solo `health`, sin detalles.
- **Registro heredado eliminado**: no hay endpoints que creen cuentas sin la metadata OAuth.
- Pendiente conocido: el JWT vive en `localStorage` (accesible a JavaScript). La defensa es no introducir XSS: no usar `dangerouslySetInnerHTML` con datos de usuarios.

## Convenciones Técnicas para Agentes
- Backend: seguir controller → service → repository → entity; validar requests con Jakarta Validation y usar DTOs para la API.
- Persistencia de varias entidades de un mismo flujo: usar `@Transactional`.
- Las asociaciones JPA `LAZY` deben cargarse en consulta o transacción antes de leerlas fuera del servicio. Para `Usuario.rol`, usar consultas `findWithRol...` con `@EntityGraph`.
- Mantener nombres de dominio en español y respetar nombres/capitalización de catálogos, estados y roles.
- No añadir acceso a datos demo ni IDs fijos. El usuario actual se obtiene con `/api/usuarios/me`.
- Frontend: rutas file-based en `frontend/src/routes`; `index.tsx` es `/`; `__root.tsx` es el único root layout. No editar `routeTree.gen.ts`.
- Toda ruta cuyo `loader` consulte la API usa `ssr: false`: el token vive en `localStorage` y en el servidor no existe, así que una carga directa (recargar o abrir un enlace) fallaría. Aplica a `/`, `/semilleros`, `/semilleros/$idSemillero`, `/postulaciones`, `/compatibilidad`, `/gestion/$idSemillero`, `/semilleros/nuevo`, `/perfil`, `/analitica` y `/oauth2/redirect`.
- Mantener `apiRequest` para llamadas HTTP: añade Bearer token, no envía cookies y usa timeout configurable.
- Para mostrar u ocultar funcionalidades por rol, usar `tipoPerfil`, `esAdministrador`, `esEstudiante` y `esProfesor` de `queries.ts`, y añadir la regla equivalente en `SecurityConfig`.
- No relajar restricciones de DB para ocultar datos obligatorios ausentes. Mantener relaciones y completar entidades correctamente.
- No subir secretos, tokens, correos de prueba ni URLs que incluyan JWT.
- El esquema productivo es externo. No cambiar `ddl-auto` a `update`/`create` en producción.
- CORS se configura solo en `SecurityConfig`; no uses `@CrossOrigin` en controladores.
- Un endpoint nuevo bajo `/api/**` necesita su regla en `SecurityConfig` o en `AutorizacionService`; por defecto exige autenticación.

## Configuración y Comandos
Variables esperadas por el backend:
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `JWT_SECRET`
- `APP_FRONTEND_URL`
- `APP_LOGIN_PRUEBAS_CLAVE` (opcional, >= 16 caracteres): habilita el login de pruebas.
- `APP_CORS_ORIGINS` (opcional): orígenes permitidos separados por coma; por defecto `APP_FRONTEND_URL`.
- `APP_OFERTAS_DIAS_RESPUESTA` (opcional, por defecto 3): plazo para responder una oferta.
- `APP_SWAGGER_HABILITADO` (opcional, por defecto `false` en producción): expone Swagger.

Frontend:
- `VITE_API_URL` debe apuntar al backend desplegado.
- `VITE_API_TIMEOUT` configura timeout HTTP; `VITE_DEBUG_API=true` habilita logs de desarrollo.

Comandos de validación:
```bash
cd backend/agora-backend
./mvnw -q -DskipTests compile
./mvnw test          # requiere las variables anteriores (al menos JWT_SECRET y la BD)

cd frontend
npx tsc --noEmit
```
