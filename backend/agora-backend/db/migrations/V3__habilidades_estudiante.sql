-- Migración V3: habilidades en el perfil del estudiante.
-- Ejecutar en la base de datos (Supabase) ANTES de desplegar el backend que la usa:
-- con ddl-auto=validate, Hibernate no arranca si falta la tabla tbl_estudiante_x_habilidad.
-- Es idempotente: puede ejecutarse más de una vez.
--
-- Hasta ahora el motor de recomendación solo conocía las habilidades de un estudiante
-- por sus postulaciones, así que un estudiante nuevo recibía un valor neutral en el
-- factor de habilidades (40 % del puntaje). Con esta tabla las declara desde el registro.

BEGIN;

CREATE TABLE IF NOT EXISTS tbl_estudiante_x_habilidad (
    id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
    id_estudiante integer NOT NULL,
    id_habilidad integer NOT NULL,
    CONSTRAINT tbl_estudiante_x_habilidad_pkey PRIMARY KEY (id),
    CONSTRAINT tbl_estudiante_x_habilidad_id_estudiante_fkey
        FOREIGN KEY (id_estudiante) REFERENCES public.tbl_estudiantes(id),
    CONSTRAINT tbl_estudiante_x_habilidad_id_habilidad_fkey
        FOREIGN KEY (id_habilidad) REFERENCES public.tbl_habilidades(id),
    CONSTRAINT tbl_estudiante_x_habilidad_unica UNIQUE (id_estudiante, id_habilidad)
);

-- Estudiantes que ya se postularon: su perfil arranca con las habilidades que declararon
-- en sus postulaciones, para no perder lo que el motor ya sabía de ellos
INSERT INTO tbl_estudiante_x_habilidad (id_estudiante, id_habilidad)
SELECT DISTINCT p.id_estudiante, ph.id_habilidad
FROM tbl_postulacion_x_habilidad ph
JOIN tbl_postulaciones p ON p.id = ph.id_postulacion
ON CONFLICT (id_estudiante, id_habilidad) DO NOTHING;

COMMIT;

-- Verificación: estudiantes sin habilidades en el perfil (deberán elegirlas en /perfil)
-- SELECT e.id, u.nombre_completo
-- FROM tbl_estudiantes e JOIN tbl_usuarios u ON u.id = e.id_usuario
-- WHERE NOT EXISTS (SELECT 1 FROM tbl_estudiante_x_habilidad eh WHERE eh.id_estudiante = e.id);
