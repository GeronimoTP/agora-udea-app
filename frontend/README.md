# Semillero Connect

Ya cree el backend para mi proyecto Agora UdeA. Te voy a dar contexto del proyecto:


1. DESCRIPCIÓN GENERAL DEL PROYECTO

El proyecto consiste en el desarrollo de una plataforma web centralizada para la 

gestión, difusión y vinculación de estudiantes a los semilleros de investigación 

de la Universidad de Antioquia.

El sistema busca resolver la dispersión de la información académica y facilitar 

el proceso de postulación y selección de aspirantes a semilleros, permitiendo a 

los lideres de semilleros publicar convocatorias, proyectos y productos académicos (papers), 

mientras que los estudiantes pueden descubrir oportunidades alineadas a su perfil 

e intereses académicos. Por su parte, la administración institucional puede analizar métricas de rendimiento.

3. ESTRUCTURA DE USUARIOS Y ROLES

1. Estudiante:

   - Explora y busca semilleros mediante filtros y algoritmos de recomendación.

   - Consulta proyectos y publicaciones académicas asociadas.

   - Postula a convocatorias abiertas completando su perfil de interés.

   - Consulta el estado de sus postulaciones.

2. Líder de Semillero (Docente / Estudiante Investigador):

   - Registra y administra la información de su semillero (Inscripción sujeta a aprobación).

   - Gestiona proyectos de investigación y asocia estudiantes a cada proyecto.

   - Registra publicaciones (papers, enlaces DOI, documentos en la nube).

   - Crea, abre y cierra convocatorias de vinculación.

   - Evalúa (acepta/rechaza) postulaciones recibidas.

   - Consulta estadísticas específicas del semillero.

3. Administrador Institucional (Director de Investigación / Admin):

   - Accede al dashboard de analítica institucional con KPIs globales.

5. SERVICIOS DE LÓGICA DE NEGOCIO (ETAPA 3)

Servicio 1: Ponderación de Factores Basada en el Modelo de Datos

Para elevar el motor de recomendación de un simple filtro estático a un algoritmo de afinidad inteligente (0% a 100%), puedes estructurar una fórmula de puntuación ponderada utilizando los catálogos normalizados que ya diseñaste:

1. Coincidencia de Habilidades Clave (Peso: 40%)

Qué evalúa: Cruza las habilidades que el estudiante registró en su perfil o postulaciones (tbl_habilidades) frente al perfil técnico requerido o histórico de los proyectos del semillero.

Impacto: Garantiza que el estudiante no solo "quiera" estar en el semillero, sino que tenga las capacidades técnicas (ej. Python, Redacción Académica, Modelado de Datos) para aportar desde el primer día.

2. Afinidad por Áreas de Especialidad del Profesor (Peso: 25%)

Qué evalúa: Compara los intereses del estudiante con las especialidades oficiales de los profesores tutores de ese grupo (tbl_profesor_x_area_especialidad).

Impacto: Conecta la línea de investigación real del líder con la curiosidad académica del alumno, asegurando mentoría experta.

3. Disponibilidad de Convocatorias Abiertas (Boost de Urgencia: 20%)

Qué evalúa: Filtra o prioriza a los semilleros que posean una convocatoria activa en curso (tbl_convocatorias) con cupos disponibles (cupos_disponibles > 0).

Impacto: Evita recomendar semilleros que tengan excelentes afinidades teóricas pero cuyas convocatorias estén cerradas, orientando al estudiante hacia opciones donde pueda postularse de inmediato.

4. Proximidad Temática y de Programa (Peso: 15%)

Qué evalúa: Analiza la coincidencia entre los tags de interés del estudiante y la facultad o programa académico de adscripción.

Impacto: Permite descubrir semilleros afines incluso fuera de la facultad directa del estudiante, fomentando la investigación interdisciplinaria en la universidad.

Salida del Algoritmo

El servicio procesará estos cuatro vectores en una consulta SQL optimizada (o script de backend) para arrojar un JSON estructurado con el ranking de afinidad:

{

  "id_semillero": 4,

  "nombre": "Semillero de Inteligencia Artificial Aplicada",

  "porcentaje_match": 92.5,

  "factores_coincidencia": ["Python", "Machine Learning", "Convocatoria Activa"]

}

Servicio 2: Gestor Automatizado de Convocatorias y Resoluciones en Cascada

Descripción: Este servicio ejecuta el control de reglas de negocio para la gestión del ciclo de vida de las convocatorias y coordina el flujo de selección de doble vía (pre-aprobación del semillero y decisión final del estudiante).

Postulación Concurrente y Resolución en Cascada: Elimina la restricción de exclusividad. Permite que un estudiante mantenga múltiples postulaciones activas simultáneamente. Cuando un estudiante recibe una o varias ofertas de admisión y acepta una de ellas (registrando la fecha_decision_estudiante), el backend dispara un evento automático que actualiza y rechaza (o cancela) todas las demás postulaciones activas de ese estudiante en otras convocatorias, liberando esos procesos.

Gestión Dinámica de Cupos (Doble Vía): El descuento de cupos no se ejecuta solo con la pre-aprobación del líder, sino cuando el proceso se consolida (el estudiante confirma su aceptación). En ese momento, el servicio descuenta en tiempo real un cupo disponible. Cuando el contador de cupos de la convocatoria llega a 0, el sistema actualiza automáticamente su estado a "Cerrada", bloqueando nuevas postulaciones.

Cierre Automatizado y Expiración de Ofertas: Proceso en segundo plano (Cron Job) que compara diariamente la fecha del sistema contra la fecha_cierre de cada convocatoria abierta. Al cumplirse el plazo límite, el servicio realiza dos acciones:

Transiciona automáticamente el estado de la convocatoria a "Cerrada".

Si existen ofertas de admisión que se mantuvieron sin respuesta por parte del estudiante hasta esa fecha, las marca automáticamente como "Expiradas" o "Rechazadas".

Servicio 3: Módulo de Analítica e Indicadores de Impacto Institucional (Dashboard)

1. Atracción y Eficiencia de Vinculación (Convocatorias y Postulaciones)

Tasa de Demanda por Semillero (Presión sobre cupos):

Cálculo: Total de postulaciones recibidas / Total de cupos ofertados.

Tablas involucradas: tbl_convocatorias (cupos_totales) + conteo en tbl_postulaciones.

Valor de negocio: Permite a la facultad saber cuáles son los semilleros más populares y si se necesitan abrir más cupos en ciertas líneas de investigación.

Tiempo Promedio de Respuesta del Estudiante:

Cálculo: Diferencia en horas/días entre la fecha en que el líder actualiza la postulación a "Pre-aprobada" y la fecha_decision_estudiante.

Tablas involucradas: tbl_postulaciones.

Valor de negocio: Mide la agilidad del proceso. Si el tiempo es muy alto, los estudiantes están dudando o el sistema no está notificando bien.

Tasa de Retención vs. Rechazo de Ofertas:

Cálculo: Porcentaje de postulaciones con estado "Rechazado por Estudiante" o "Expirado" frente a las "Pre-aprobadas".

Valor de negocio: Indica si los estudiantes prefieren otras ofertas en la resolución en cascada.

2. Productividad Científica y Operativa (Proyectos y Publicaciones)

Índice de Conversión Proyecto-Publicación:

Cálculo: Promedio de registros en tbl_publicaciones generados por cada registro en tbl_proyectos que tenga un estado finalizado.

Tablas involucradas: tbl_proyectos, tbl_publicaciones, tbl_estados.

Valor de negocio: Mide qué tan efectivos son los proyectos para generar entregables reales (papers, ponencias), no solo intenciones.

Índice de Participación Estudiantil en Autorías:

Cálculo: Porcentaje de autores de publicaciones que son estudiantes frente al total de autores (identificados mediante el catálogo de roles de autoría).

Tablas involucradas: tbl_publicaciones, catálogo de roles de autor, tbl_usuarios.

Valor de negocio: Demuestra si los semilleros están dando el crédito académico adecuado a los estudiantes o si los profesores concentran la autoría principal.

3. Impacto Institucional y Prestigio (Reconocimientos)

Distribución del Impacto (Local vs. Nacional vs. Internacional):

Cálculo: Conteo y agrupación de reconocimientos filtrados por la tbl_entidades_otorgantes y tbl_tipos_reconocimiento.

Tablas involucradas: tbl_reconocimientos_semilleros, tablas de diccionarios de reconocimientos.

Valor de negocio: Permite a la UdeA reportar a entidades como Minciencias qué porcentaje de sus galardones provienen de redes externas vs. concursos internos.

4. Demografía y Capacidades (Perfiles y Habilidades)

Mapa de Habilidades Emergentes (Skill Trend):

Cálculo: Conteo de las habilidades (de la tabla tbl_habilidades) más frecuentes asociadas a las postulaciones que terminaron en estado "Aceptado".

Tablas involucradas: tbl_postulaciones, tbl_postulacion_x_habilidad, tbl_habilidades.

Valor de negocio: Le dice a la Universidad qué herramientas técnicas (ej. Python, Análisis de Datos, Redacción) están demandando realmente los grupos de investigación hoy en día.

Índice de Interdisciplinariedad:

Cálculo: Porcentaje de estudiantes vinculados a un semillero cuyo id_programa (Facultad/Programa) es diferente al id_programa del profesor líder.

Tablas involucradas: tbl_estudiantes, tbl_profesores, tbl_semillero_x_profesor, tbl_programas_academicos.

Valor de negocio: Mide la colaboración cruzada (ej. un estudiante de Ingeniería de Sistemas trabajando en un semillero de Medicina).

6. FUNCIONALIDADES BASE DEL SISTEMA

Módulo de Seguridad y Perfiles

1.Autenticación Unificada (OAuth) y Autorización: Acceso seguro mediante proveedores externos (ej. Google/Microsoft institucional) y control de acceso basado en roles (Estudiante, Profesor, Administrador) usando JWT.

2.Gestión de Perfiles 1:1 Avanzados: Creación y edición de perfiles únicos vinculados a programas académicos, permitiendo a los profesores gestionar múltiples áreas de especialidad (catálogo finito).

3.Catálogos Vivos de Habilidades e Intereses (Folksonomía): Sistema dinámico para que los estudiantes etiqueten sus perfiles con "tags" temáticos y "habilidades clave", permitiendo el crecimiento orgánico de los diccionarios del sistema.

Módulo de Gestión de Semilleros

4. Administración del Ciclo de Vida del Semillero: Creación y edición de la información del grupo, controlada por un catálogo de estados categorizados (ej. Activo, En pausa).

5. Sistema de Co-tutoría y Gobernanza: Capacidad de asignar y gestionar múltiples profesores (N:M) como líderes o co-tutores de un mismo semillero.

6. Trazabilidad de Reconocimientos (Nuevo): Módulo para que los semilleros registren sus logros, premios y galardones, estandarizados mediante diccionarios de "Tipos de Reconocimiento" y "Entidades Otorgantes".

Módulo de Convocatorias y Motor de Postulación

7. Configuración Parametrizada de Convocatorias: Creación de ofertas con fechas de apertura/cierre exactas, gestión dinámica de cupos y estado de la convocatoria.

8. Postulación Concurrente: Capacidad del estudiante para aplicar a múltiples semilleros simultáneamente utilizando su perfil estandarizado y habilidades clave.

9. Flujo de Selección de Doble Vía: Panel interactivo donde el líder revisa, filtra (por habilidades/tags) y emite una pre-aprobación, delegando la decisión final de aceptación al estudiante.

10. Resolución en Cascada y Cierre Automático: Servicio en segundo plano (Cron) que procesa el rechazo automático de las demás postulaciones de un estudiante cuando este acepta una oferta, y que cierra convocatorias o expira postulaciones al cumplirse las fechas límite.

Módulo de Producción Investigativa

11. Gestión de Proyectos de Investigación: Trazabilidad de proyectos internos del semillero, incluyendo fechas, control de presupuesto asignado, estados y vinculación de estudiantes/profesores bajo roles de trabajo específicos.

12. Repositorio de Producción Académica: Registro de publicaciones, artículos y entregables, con enlaces externos y asignación normalizada de roles de autoría para los participantes.

Módulo de Inteligencia y Analítica

13. Algoritmo de Matchmaking: Sistema de recomendación que cruza las etiquetas (tags) y habilidades de los estudiantes con las líneas de investigación de los semilleros para sugerir convocatorias afines.

14. Dashboard de Analítica y KPIs: Tableros de datos que consumen los catálogos estandarizados para generar reportes institucionales (ej. tiempos de respuesta de estudiantes, reconocimientos por entidad, distribución de habilidades, semilleros por programa).

El proyecto esta alojado en mi repositorio agora-udea-app

Necesito que crees el frontend dentro de la carpeta frontend, usando como tecnologia React.js y revisando el backend para que se hagan las conexiones, ademas, sigue estas instrucciones:

Actúa como un Desarrollador Frontend Senior y Diseñador UI/UX especializado en aplicaciones web institucionales y académicas. Necesito que diseñes y desarrolles el frontend para "Agora UdeA", un sistema de gestión de semilleros de investigación de la Universidad de Antioquia.

### 1. Lineamientos de Diseño y Estética (Obligatorio)

- Estilo académico, sobrio, formal y minimalista. Nada de colores pastel, degradados llamativos o estilos genéricos típicos de IA.

- Inspiración visual: Basado en la identidad institucional de la Universidad de Antioquia. Utiliza una paleta de colores corporativa basada en azul institucional oscuro (ej. #003366 o similar para barras de navegación y elementos principales), blancos, grises neutros claros para fondos, y un azul de acento sobrio para llamadas a la acción.

- TIPOGRAFÍA Y TEXTOS: Textos limpios, claros y profesionales. Jerarquía tipográfica muy legible (sans-serif formal).

- RESTRICCIÓN ABSOLUTA: Queda totalmente prohibido el uso de emojis en cualquier componente de la interfaz (botones, títulos, menús o alertas). Si se requieren iconos, utiliza únicamente iconos vectoriales limpios y minimalistas (ej. Lucide React o SVG planos).

### 2. Estructura y Vistas Principales

Diseña las vistas necesarias para un sistema de gestión de semilleros:

- Dashboard principal con indicadores institucionales y accesos rápidos.

- Módulo de visualización y postulación a semilleros de investigación.

- Vista de gestión y matching de compatibilidad para estudiantes y tutores.

- Panel de analítica institucional con tablas de datos claras y ordenadas.

### 3. Integración con el Backend

- El diseño debe estructurarse de manera modular, separando componentes, páginas y servicios de API.

- Prepara la capa de servicios (ej. usando Axios o Fetch) con variables de entorno listas (`NEXT_PUBLIC_API_URL` o similar) para conectarse de forma limpia y directa al backend en Spring Boot ya existente.

- Define interfaces/tipos de TypeScript claros que se mapeen fácilmente con los DTOs del backend (estudiantes, semilleros, postulaciones, etc.).

Por favor, entrega la estructura de carpetas sugerida y el código modular y limpio de los componentes principales.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/b4b489ef-267b-4f70-9010-436d011ee6c5).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
