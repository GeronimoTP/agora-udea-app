-- Migración V2: fecha de la oferta de una postulación.
-- Ejecutar en la base de datos (Supabase) ANTES de desplegar el backend que la usa:
-- con ddl-auto=validate, Hibernate no arranca si falta la columna fecha_pre_aprobacion.
-- Es idempotente: puede ejecutarse más de una vez.
--
-- La cancelación en cascada usa el estado existente "Cancelada en cascada" (categoría
-- "Estado Postulación"); no hace falta crear estados nuevos.

BEGIN;

-- Momento en que el líder pre-aprueba (emite la oferta). Permite medir el tiempo de
-- respuesta del estudiante y distinguir ofertas reales en la analítica.
ALTER TABLE tbl_postulaciones
    ADD COLUMN IF NOT EXISTS fecha_pre_aprobacion timestamptz;

COMMIT;

-- Verificación: deben existir todos los estados que usa el backend (por categoría).
-- Los nombres se repiten entre categorías, por eso el backend busca por categoría + nombre.
-- SELECT c.nombre AS categoria, e.nombre
-- FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
-- WHERE (c.nombre = 'Estado Postulación' AND e.nombre IN ('Pendiente', 'Pre-aprobada', 'Aceptada',
--            'Rechazada', 'Rechazada por Estudiante', 'Cancelada en cascada', 'Expirada'))
--    OR (c.nombre = 'Estado Convocatoria' AND e.nombre IN ('Abierta', 'Cerrada'))
--    OR (c.nombre = 'Estado Semillero' AND e.nombre = 'Activo')
--    OR (c.nombre = 'Estado Proyecto' AND e.nombre = 'En ejecución')
--    OR (c.nombre = 'Estado Usuario' AND e.nombre = 'Activo');
-- Debe devolver 12 filas.
