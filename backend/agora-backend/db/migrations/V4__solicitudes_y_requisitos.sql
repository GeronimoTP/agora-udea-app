-- Migración V4: decisiones de la reunión con el profesor.
-- Ejecutar en la base de datos (Supabase) ANTES de desplegar el backend que la usa:
-- con ddl-auto=validate, Hibernate no arranca si faltan estas tablas o columnas.
-- Es idempotente: puede ejecutarse más de una vez.
--
--   1. Aprobación de semilleros por el administrador: estados nuevos de semillero y
--      columnas de revisión en tbl_semilleros.
--   2. Requisitos de las convocatorias (habilidades del catálogo o texto libre,
--      obligatorios u opcionales) y los que el estudiante declara cumplir.
--   4. Solicitudes de retiro voluntario de un integrante.
--   5. Solicitudes de nuevos elementos del catálogo al administrador.
-- Los puntos 3 (sin cancelación en cascada entre semilleros), 6 (seguridad) y 7 (motor de
-- recomendación) no cambian el esquema.

BEGIN;

-- ==========================================
-- ESTADOS
-- ==========================================

-- Categoría compartida por las solicitudes de retiro y de catálogo
INSERT INTO tbl_categorias_estado (nombre, descripcion)
SELECT 'Estado Solicitud', 'Estados de una solicitud que alguien debe aprobar o rechazar'
WHERE NOT EXISTS (SELECT 1 FROM tbl_categorias_estado WHERE nombre = 'Estado Solicitud');

INSERT INTO tbl_estados (id_categoria_estado, nombre, descripcion)
SELECT c.id, v.nombre, v.descripcion
FROM tbl_categorias_estado c
CROSS JOIN (VALUES
    ('Estado Semillero', 'Pendiente de aprobación', 'Registrado; espera la aprobación del administrador'),
    ('Estado Semillero', 'Rechazado', 'El administrador rechazó el registro; puede corregirse y reenviarse'),
    ('Estado Solicitud', 'Pendiente', 'Esperando respuesta'),
    ('Estado Solicitud', 'Aprobada', 'Solicitud aprobada'),
    ('Estado Solicitud', 'Rechazada', 'Solicitud rechazada')
) AS v(categoria, nombre, descripcion)
WHERE c.nombre = v.categoria
  AND NOT EXISTS (
      SELECT 1 FROM tbl_estados e
      WHERE e.id_categoria_estado = c.id AND e.nombre = v.nombre);

-- ==========================================
-- 1. APROBACIÓN DE SEMILLEROS
-- ==========================================

-- Los semilleros que ya existen quedan como están (aprobados): estas columnas son nulas para ellos
ALTER TABLE tbl_semilleros
    ADD COLUMN IF NOT EXISTS fecha_solicitud timestamptz,
    ADD COLUMN IF NOT EXISTS fecha_revision timestamptz,
    ADD COLUMN IF NOT EXISTS id_usuario_revisor integer REFERENCES tbl_usuarios(id),
    ADD COLUMN IF NOT EXISTS motivo_rechazo varchar(1000);

-- ==========================================
-- 2. REQUISITOS DE LAS CONVOCATORIAS
-- ==========================================

-- Cada requisito es una habilidad del catálogo O un texto libre, nunca ambos
CREATE TABLE IF NOT EXISTS tbl_requisitos_convocatoria (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    id_convocatoria integer NOT NULL,
    id_habilidad integer,
    descripcion varchar(300),
    obligatorio boolean NOT NULL,
    CONSTRAINT tbl_requisitos_convocatoria_pkey PRIMARY KEY (id),
    CONSTRAINT tbl_requisitos_convocatoria_id_convocatoria_fkey
        FOREIGN KEY (id_convocatoria) REFERENCES public.tbl_convocatorias(id),
    CONSTRAINT tbl_requisitos_convocatoria_id_habilidad_fkey
        FOREIGN KEY (id_habilidad) REFERENCES public.tbl_habilidades(id),
    CONSTRAINT tbl_requisitos_convocatoria_tipo
        CHECK ((id_habilidad IS NULL) <> (descripcion IS NULL)),
    CONSTRAINT tbl_requisitos_convocatoria_habilidad_unica UNIQUE (id_convocatoria, id_habilidad)
);

-- Requisitos de texto que el estudiante declara cumplir al postularse. Los de habilidad no
-- se guardan aquí: se comparan con las habilidades declaradas en la postulación.
CREATE TABLE IF NOT EXISTS tbl_postulacion_x_requisito (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    id_postulacion integer NOT NULL,
    id_requisito integer NOT NULL,
    CONSTRAINT tbl_postulacion_x_requisito_pkey PRIMARY KEY (id),
    CONSTRAINT tbl_postulacion_x_requisito_id_postulacion_fkey
        FOREIGN KEY (id_postulacion) REFERENCES public.tbl_postulaciones(id),
    CONSTRAINT tbl_postulacion_x_requisito_id_requisito_fkey
        FOREIGN KEY (id_requisito) REFERENCES public.tbl_requisitos_convocatoria(id),
    CONSTRAINT tbl_postulacion_x_requisito_unico UNIQUE (id_postulacion, id_requisito)
);

-- ==========================================
-- 4. RETIRO VOLUNTARIO
-- ==========================================

CREATE TABLE IF NOT EXISTS tbl_solicitudes_retiro (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    id_estudiante_semillero integer NOT NULL,
    motivo varchar(1000) NOT NULL,
    id_estado integer NOT NULL,
    fecha_solicitud timestamptz NOT NULL,
    fecha_respuesta timestamptz,
    id_usuario_responde integer,
    comentario_respuesta varchar(1000),
    CONSTRAINT tbl_solicitudes_retiro_pkey PRIMARY KEY (id),
    CONSTRAINT tbl_solicitudes_retiro_id_estudiante_semillero_fkey
        FOREIGN KEY (id_estudiante_semillero) REFERENCES public.tbl_estudiante_x_semillero(id),
    CONSTRAINT tbl_solicitudes_retiro_id_estado_fkey
        FOREIGN KEY (id_estado) REFERENCES public.tbl_estados(id),
    CONSTRAINT tbl_solicitudes_retiro_id_usuario_responde_fkey
        FOREIGN KEY (id_usuario_responde) REFERENCES public.tbl_usuarios(id)
);

-- ==========================================
-- 5. SOLICITUDES DE CATÁLOGO
-- ==========================================

CREATE TABLE IF NOT EXISTS tbl_solicitudes_catalogo (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    tipo varchar(30) NOT NULL,
    nombre varchar(150) NOT NULL,
    descripcion varchar(1000),
    id_area_especialidad integer,
    justificacion varchar(1000) NOT NULL,
    id_usuario_solicitante integer NOT NULL,
    id_estado integer NOT NULL,
    fecha_solicitud timestamptz NOT NULL,
    fecha_respuesta timestamptz,
    id_usuario_responde integer,
    comentario_respuesta varchar(1000),
    -- ID del elemento creado al aprobar (en la tabla que indica "tipo")
    id_elemento_creado integer,
    CONSTRAINT tbl_solicitudes_catalogo_pkey PRIMARY KEY (id),
    CONSTRAINT tbl_solicitudes_catalogo_tipo CHECK (tipo IN (
        'HABILIDAD', 'LINEA_INVESTIGACION', 'AREA_ESPECIALIDAD',
        'ENTIDAD_OTORGANTE', 'TIPO_RECONOCIMIENTO')),
    CONSTRAINT tbl_solicitudes_catalogo_id_area_especialidad_fkey
        FOREIGN KEY (id_area_especialidad) REFERENCES public.tbl_areas_especialidad(id),
    CONSTRAINT tbl_solicitudes_catalogo_id_usuario_solicitante_fkey
        FOREIGN KEY (id_usuario_solicitante) REFERENCES public.tbl_usuarios(id),
    CONSTRAINT tbl_solicitudes_catalogo_id_estado_fkey
        FOREIGN KEY (id_estado) REFERENCES public.tbl_estados(id),
    CONSTRAINT tbl_solicitudes_catalogo_id_usuario_responde_fkey
        FOREIGN KEY (id_usuario_responde) REFERENCES public.tbl_usuarios(id)
);

COMMIT;

-- Verificación:
-- SELECT c.nombre AS categoria, e.nombre FROM tbl_estados e
-- JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
-- WHERE c.nombre IN ('Estado Semillero', 'Estado Solicitud') ORDER BY 1, 2;
