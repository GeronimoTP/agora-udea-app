-- Migración V2: fecha de la oferta y estado de cancelación en cascada.
-- Ejecutar en la base de datos (Supabase) ANTES de desplegar el backend que la usa:
-- con ddl-auto=validate, Hibernate no arranca si falta la columna fecha_pre_aprobacion.
-- Es idempotente: puede ejecutarse más de una vez.

BEGIN;

-- Momento en que el líder pre-aprueba (emite la oferta). Permite medir el tiempo de
-- respuesta del estudiante y distinguir ofertas reales en la analítica.
ALTER TABLE tbl_postulaciones
    ADD COLUMN IF NOT EXISTS fecha_pre_aprobacion timestamptz;

-- Estado para las postulaciones que se liberan en cascada cuando el estudiante acepta
-- otra oferta. Se crea en la misma categoría que "Pendiente" (estados de postulación).
INSERT INTO tbl_estados (id_categoria_estado, nombre, descripcion)
SELECT e.id_categoria_estado,
       'Cancelada',
       'Postulación liberada automáticamente porque el estudiante aceptó otra oferta'
FROM tbl_estados e
WHERE e.nombre = 'Pendiente'
  AND NOT EXISTS (SELECT 1 FROM tbl_estados WHERE lower(nombre) = 'cancelada');

COMMIT;

-- Verificación: deben existir todos los estados que usa el backend.
-- SELECT nombre FROM tbl_estados
-- WHERE nombre IN ('Pendiente', 'Pre-aprobada', 'Aceptada', 'Rechazada', 'Rechazada por Estudiante',
--                  'Cancelada', 'Expirada', 'Abierta', 'Cerrada', 'Activo');
