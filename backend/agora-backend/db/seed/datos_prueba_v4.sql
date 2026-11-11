-- =====================================================================
-- Datos de prueba de la migración V4 (aprobación de semilleros, requisitos,
-- retiro voluntario y solicitudes de catálogo)
-- =====================================================================
-- Ejecutar DESPUÉS de V4__solicitudes_y_requisitos.sql y de datos_prueba.sql.
-- Usa los usuarios y semilleros de prueba (@seed.agora.test, SEED-...):
--
-- * SEED-06: semillero "Pendiente de aprobación" (líder estudiante24, tutor profesor2).
-- * SEED-07: semillero "Rechazado" con motivo (líder estudiante25, tutor profesor4).
-- * Una convocatoria abierta de SEED-02 con requisitos (dos obligatorios y uno opcional) y
--   dos postulaciones: una cumple todo y otra no, para ver la priorización.
-- * Una solicitud de retiro pendiente de un integrante de SEED-01.
-- * Solicitudes de catálogo: dos pendientes y una rechazada.
--
-- No se puede ejecutar dos veces. limpiar_datos_prueba.sql también borra estos datos.
-- =====================================================================

BEGIN;

DO $$
DECLARE
    ahora timestamptz := date_trunc('hour', now());
    hoy date := current_date;

    e_sem_pendiente integer;
    e_sem_rechazado integer;
    e_abierta integer;
    e_pendiente integer;
    e_sol_pendiente integer;
    e_sol_rechazada integer;

    v_admin integer;
    v_est24 integer;
    v_est25 integer;
    v_prof2 integer;
    v_prof4 integer;
    v_usuario_est01 integer;
    v_usuario_est02 integer;
    v_usuario_prof1 integer;
    v_sem01 integer;
    v_sem02 integer;
    v_programa integer;
    v_id integer;
    v_conv integer;
    v_req_texto integer;
    v_post integer;
    v_vinculacion integer;
    habilidades integer[];
    lineas integer[];
    aspirantes integer[];
BEGIN
    IF EXISTS (SELECT 1 FROM tbl_semilleros WHERE codigo_identificador = 'SEED-06') THEN
        RAISE EXCEPTION 'Los datos de prueba de V4 ya están cargados';
    END IF;

    SELECT e.id INTO e_sem_pendiente FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE c.nombre = 'Estado Semillero' AND e.nombre = 'Pendiente de aprobación';
    SELECT e.id INTO e_sem_rechazado FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE c.nombre = 'Estado Semillero' AND e.nombre = 'Rechazado';
    SELECT e.id INTO e_abierta FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE c.nombre = 'Estado Convocatoria' AND e.nombre = 'Abierta';
    SELECT e.id INTO e_pendiente FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE c.nombre = 'Estado Postulación' AND e.nombre = 'Pendiente';
    SELECT e.id INTO e_sol_pendiente FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE c.nombre = 'Estado Solicitud' AND e.nombre = 'Pendiente';
    SELECT e.id INTO e_sol_rechazada FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE c.nombre = 'Estado Solicitud' AND e.nombre = 'Rechazada';
    IF e_sem_pendiente IS NULL OR e_sol_pendiente IS NULL THEN
        RAISE EXCEPTION 'Falta ejecutar la migración V4__solicitudes_y_requisitos.sql';
    END IF;

    SELECT id INTO v_admin FROM tbl_usuarios WHERE email = 'admin@seed.agora.test';
    SELECT e.id INTO v_est24 FROM tbl_estudiantes e JOIN tbl_usuarios u ON u.id = e.id_usuario
    WHERE u.email = 'estudiante24@seed.agora.test';
    SELECT e.id INTO v_est25 FROM tbl_estudiantes e JOIN tbl_usuarios u ON u.id = e.id_usuario
    WHERE u.email = 'estudiante25@seed.agora.test';
    SELECT p.id INTO v_prof2 FROM tbl_profesores p JOIN tbl_usuarios u ON u.id = p.id_usuario
    WHERE u.email = 'profesor2@seed.agora.test';
    SELECT p.id INTO v_prof4 FROM tbl_profesores p JOIN tbl_usuarios u ON u.id = p.id_usuario
    WHERE u.email = 'profesor4@seed.agora.test';
    SELECT id INTO v_usuario_est01 FROM tbl_usuarios WHERE email = 'estudiante01@seed.agora.test';
    SELECT id INTO v_usuario_est02 FROM tbl_usuarios WHERE email = 'estudiante02@seed.agora.test';
    SELECT id INTO v_usuario_prof1 FROM tbl_usuarios WHERE email = 'profesor1@seed.agora.test';
    SELECT id INTO v_sem01 FROM tbl_semilleros WHERE codigo_identificador = 'SEED-01';
    SELECT id INTO v_sem02 FROM tbl_semilleros WHERE codigo_identificador = 'SEED-02';
    IF v_admin IS NULL OR v_est24 IS NULL OR v_sem01 IS NULL THEN
        RAISE EXCEPTION 'Falta ejecutar datos_prueba.sql';
    END IF;
    IF EXISTS (SELECT 1 FROM tbl_semilleros WHERE id_estudiante_lider IN (v_est24, v_est25)) THEN
        RAISE EXCEPTION 'estudiante24 o estudiante25 ya lideran un semillero';
    END IF;

    SELECT array_agg(id ORDER BY id) INTO habilidades FROM tbl_habilidades;
    SELECT array_agg(id ORDER BY id) INTO lineas FROM tbl_linea_investigacion;
    SELECT id INTO v_programa FROM tbl_programas_academicos ORDER BY id LIMIT 1;

    -- -----------------------------------------------------------------
    -- 1. Semilleros por aprobar y rechazado
    -- -----------------------------------------------------------------
    INSERT INTO tbl_semilleros (codigo_identificador, nombre, descripcion, id_estudiante_lider,
        id_programa, id_estado, created_at, fecha_solicitud)
    VALUES ('SEED-06', 'Semillero de Robótica Educativa',
        'Semillero de prueba pendiente de aprobación: diseño de robots de bajo costo para la enseñanza en colegios públicos.',
        v_est24, v_programa, e_sem_pendiente, ahora - interval '2 days', ahora - interval '2 days')
    RETURNING id INTO v_id;
    INSERT INTO tbl_semillero_x_linea_investigacion (id_semillero, id_linea_investigacion)
    SELECT v_id, l FROM unnest(lineas[1:2]) AS l;
    INSERT INTO tbl_profesor_x_semillero (id_semillero, id_profesor, fecha_asignacion)
    VALUES (v_id, v_prof2, hoy - 2);

    INSERT INTO tbl_semilleros (codigo_identificador, nombre, descripcion, id_estudiante_lider,
        id_programa, id_estado, created_at, fecha_solicitud, fecha_revision, id_usuario_revisor, motivo_rechazo)
    VALUES ('SEED-07', 'Semillero de Prueba Rechazado', 'Grupo de estudio.',
        v_est25, v_programa, e_sem_rechazado, ahora - interval '6 days', ahora - interval '6 days',
        ahora - interval '5 days', v_admin,
        'La descripción no explica el objetivo de investigación ni las actividades. Amplíala y vuelve a enviarlo.')
    RETURNING id INTO v_id;
    INSERT INTO tbl_semillero_x_linea_investigacion (id_semillero, id_linea_investigacion)
    SELECT v_id, l FROM unnest(lineas[3:3]) AS l;
    INSERT INTO tbl_profesor_x_semillero (id_semillero, id_profesor, fecha_asignacion)
    VALUES (v_id, v_prof4, hoy - 6);

    -- -----------------------------------------------------------------
    -- 2. Convocatoria con requisitos en SEED-02
    -- -----------------------------------------------------------------
    INSERT INTO tbl_convocatorias (titulo, descripcion, cupos_totales, cupos_disponibles,
        fecha_inicio, fecha_cierre, id_semillero, id_estado, created_at)
    VALUES ('Convocatoria con requisitos - Semillero de Biotecnología y Salud',
        'Convocatoria de prueba con requisitos obligatorios y opcionales.',
        2, 2, hoy - 3, hoy + 20, v_sem02, e_abierta, ahora - interval '4 days')
    RETURNING id INTO v_conv;
    INSERT INTO tbl_requisitos_convocatoria (id_convocatoria, id_habilidad, descripcion, obligatorio)
    VALUES (v_conv, habilidades[1], NULL, true);
    INSERT INTO tbl_requisitos_convocatoria (id_convocatoria, id_habilidad, descripcion, obligatorio)
    VALUES (v_conv, NULL, 'Haber aprobado el curso de Biología General', true)
    RETURNING id INTO v_req_texto;
    INSERT INTO tbl_requisitos_convocatoria (id_convocatoria, id_habilidad, descripcion, obligatorio)
    VALUES (v_conv, habilidades[2], NULL, false);

    -- Dos aspirantes que no integran SEED-02: el primero cumple todo, el segundo no
    SELECT array_agg(id ORDER BY id) INTO aspirantes FROM (
        SELECT e.id FROM tbl_estudiantes e JOIN tbl_usuarios u ON u.id = e.id_usuario
        WHERE u.email LIKE 'estudiante%@seed.agora.test'
          AND e.id NOT IN (v_est24, v_est25)
          AND NOT EXISTS (SELECT 1 FROM tbl_estudiante_x_semillero es
                          WHERE es.id_estudiante = e.id AND es.id_semillero = v_sem02 AND es.fecha_salida IS NULL)
        ORDER BY e.id LIMIT 2) t;

    INSERT INTO tbl_postulaciones (id_convocatoria, id_estudiante, respuesta_motivacion,
        disponibilidad_horas_semana, experiencia_previa, fecha_postulacion, id_estado, created_at)
    VALUES (v_conv, aspirantes[1], 'Quiero aplicar lo que aprendí en el laboratorio.', 10,
        'Monitor del laboratorio de biología.', hoy - 1, e_pendiente, ahora - interval '1 day')
    RETURNING id INTO v_post;
    INSERT INTO tbl_postulacion_x_habilidad (id_postulacion, id_habilidad)
    VALUES (v_post, habilidades[1]), (v_post, habilidades[2]);
    INSERT INTO tbl_postulacion_x_requisito (id_postulacion, id_requisito) VALUES (v_post, v_req_texto);

    INSERT INTO tbl_postulaciones (id_convocatoria, id_estudiante, respuesta_motivacion,
        disponibilidad_horas_semana, experiencia_previa, fecha_postulacion, id_estado, created_at)
    VALUES (v_conv, aspirantes[2], 'Me interesa la biotecnología aunque vengo de otra carrera.', 12,
        'Ninguna en el área.', hoy, e_pendiente, ahora)
    RETURNING id INTO v_post;
    INSERT INTO tbl_postulacion_x_habilidad (id_postulacion, id_habilidad)
    VALUES (v_post, habilidades[array_length(habilidades, 1)]);

    -- -----------------------------------------------------------------
    -- 4. Solicitud de retiro pendiente en SEED-01
    -- -----------------------------------------------------------------
    SELECT es.id INTO v_vinculacion FROM tbl_estudiante_x_semillero es
    WHERE es.id_semillero = v_sem01 AND es.fecha_salida IS NULL
      AND es.id_estudiante <> (SELECT id_estudiante_lider FROM tbl_semilleros WHERE id = v_sem01)
    ORDER BY es.id LIMIT 1;
    IF v_vinculacion IS NOT NULL THEN
        INSERT INTO tbl_solicitudes_retiro (id_estudiante_semillero, motivo, id_estado, fecha_solicitud)
        VALUES (v_vinculacion,
            'Este semestre tengo práctica profesional de tiempo completo y no podré cumplir con las reuniones.',
            e_sol_pendiente, ahora - interval '1 day');
    END IF;

    -- -----------------------------------------------------------------
    -- 5. Solicitudes de catálogo
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM tbl_habilidades WHERE lower(nombre) = 'visión por computador') THEN
        INSERT INTO tbl_solicitudes_catalogo (tipo, nombre, descripcion, justificacion,
            id_usuario_solicitante, id_estado, fecha_solicitud)
        VALUES ('HABILIDAD', 'Visión por computador', NULL,
            'Varios proyectos de IA usan procesamiento de imágenes y no hay una habilidad para declararlo.',
            v_usuario_est01, e_sol_pendiente, ahora - interval '3 days');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM tbl_linea_investigacion WHERE lower(nombre) = 'robótica educativa') THEN
        INSERT INTO tbl_solicitudes_catalogo (tipo, nombre, descripcion, id_area_especialidad, justificacion,
            id_usuario_solicitante, id_estado, fecha_solicitud)
        VALUES ('LINEA_INVESTIGACION', 'Robótica educativa',
            'Diseño y evaluación de robots como herramienta de enseñanza.',
            (SELECT id FROM tbl_areas_especialidad ORDER BY id LIMIT 1),
            'Hay un semillero en formación sobre el tema y ninguna línea lo describe.',
            v_usuario_prof1, e_sol_pendiente, ahora - interval '1 day');
    END IF;
    INSERT INTO tbl_solicitudes_catalogo (tipo, nombre, descripcion, justificacion,
        id_usuario_solicitante, id_estado, fecha_solicitud, fecha_respuesta, id_usuario_responde, comentario_respuesta)
    VALUES ('ENTIDAD_OTORGANTE', 'Club de lectura del barrio', 'Grupo de lectura comunitario.',
        'Nos dieron un diploma por participar.', v_usuario_est02, e_sol_rechazada,
        ahora - interval '8 days', ahora - interval '7 days', v_admin,
        'Las entidades otorgantes deben ser instituciones que reconozcan trabajos de investigación.');
END $$;

COMMIT;
