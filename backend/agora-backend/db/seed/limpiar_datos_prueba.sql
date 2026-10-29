-- =====================================================================
-- Elimina los datos creados por datos_prueba.sql
-- =====================================================================
-- Borra los usuarios con oauth_id 'seed-...' y los semilleros 'SEED-...',
-- con todo lo que cuelga de ellos. También borra lo que usuarios reales
-- hayan hecho sobre esos datos (por ejemplo, postularse a una convocatoria
-- de prueba). Los catálogos se conservan.
-- Si un semillero real quedó liderado por un estudiante de prueba, el script
-- falla y no borra nada: transfiere antes el liderazgo.
-- =====================================================================

BEGIN;

CREATE TEMP TABLE seed_usuarios ON COMMIT DROP AS
    SELECT id FROM tbl_usuarios WHERE oauth_id LIKE 'seed-%';
CREATE TEMP TABLE seed_estudiantes ON COMMIT DROP AS
    SELECT id FROM tbl_estudiantes WHERE id_usuario IN (SELECT id FROM seed_usuarios);
CREATE TEMP TABLE seed_profesores ON COMMIT DROP AS
    SELECT id FROM tbl_profesores WHERE id_usuario IN (SELECT id FROM seed_usuarios);
CREATE TEMP TABLE seed_semilleros ON COMMIT DROP AS
    SELECT id FROM tbl_semilleros WHERE codigo_identificador LIKE 'SEED-%';
CREATE TEMP TABLE seed_proyectos ON COMMIT DROP AS
    SELECT id FROM tbl_proyectos WHERE id_semillero IN (SELECT id FROM seed_semilleros);
CREATE TEMP TABLE seed_publicaciones ON COMMIT DROP AS
    SELECT id FROM tbl_publicaciones WHERE id_proyecto IN (SELECT id FROM seed_proyectos);
CREATE TEMP TABLE seed_postulaciones ON COMMIT DROP AS
    SELECT p.id FROM tbl_postulaciones p
    JOIN tbl_convocatorias c ON c.id = p.id_convocatoria
    WHERE c.id_semillero IN (SELECT id FROM seed_semilleros)
       OR p.id_estudiante IN (SELECT id FROM seed_estudiantes);

DELETE FROM tbl_publicacion_x_usuario
WHERE id_publicacion IN (SELECT id FROM seed_publicaciones)
   OR id_usuario IN (SELECT id FROM seed_usuarios);
DELETE FROM tbl_publicaciones WHERE id IN (SELECT id FROM seed_publicaciones);
DELETE FROM tbl_estudiante_x_proyecto
WHERE id_proyecto IN (SELECT id FROM seed_proyectos)
   OR id_estudiante IN (SELECT id FROM seed_estudiantes);
DELETE FROM tbl_proyectos WHERE id IN (SELECT id FROM seed_proyectos);
DELETE FROM tbl_reconocimientos_semilleros WHERE id_semillero IN (SELECT id FROM seed_semilleros);

DELETE FROM tbl_postulacion_x_habilidad WHERE id_postulacion IN (SELECT id FROM seed_postulaciones);
DELETE FROM tbl_postulaciones WHERE id IN (SELECT id FROM seed_postulaciones);
DELETE FROM tbl_convocatorias WHERE id_semillero IN (SELECT id FROM seed_semilleros);

DELETE FROM tbl_estudiante_x_semillero
WHERE id_semillero IN (SELECT id FROM seed_semilleros)
   OR id_estudiante IN (SELECT id FROM seed_estudiantes);
DELETE FROM tbl_profesor_x_semillero
WHERE id_semillero IN (SELECT id FROM seed_semilleros)
   OR id_profesor IN (SELECT id FROM seed_profesores);
DELETE FROM tbl_semillero_x_linea_investigacion WHERE id_semillero IN (SELECT id FROM seed_semilleros);
DELETE FROM tbl_semilleros WHERE id IN (SELECT id FROM seed_semilleros);

DELETE FROM tbl_estudiante_x_linea_investigacion WHERE id_estudiante IN (SELECT id FROM seed_estudiantes);
DELETE FROM tbl_estudiante_x_habilidad WHERE id_estudiante IN (SELECT id FROM seed_estudiantes);
DELETE FROM tbl_profesor_x_area_especialidad WHERE id_profesor IN (SELECT id FROM seed_profesores);
DELETE FROM tbl_estudiantes WHERE id IN (SELECT id FROM seed_estudiantes);
DELETE FROM tbl_profesores WHERE id IN (SELECT id FROM seed_profesores);
DELETE FROM tbl_usuarios WHERE id IN (SELECT id FROM seed_usuarios);

COMMIT;
