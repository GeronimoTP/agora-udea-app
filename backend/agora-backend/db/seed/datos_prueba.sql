-- =====================================================================
-- Datos de prueba de Agora UdeA
-- =====================================================================
-- Crea usuarios, semilleros, convocatorias, postulaciones, proyectos,
-- publicaciones y reconocimientos coherentes con las reglas de negocio,
-- para que la analítica y todas las vistas muestren información.
--
-- * Todos los usuarios tienen oauth_id 'seed-...' y correo @seed.agora.test:
--   solo entran por el login de pruebas (APP_LOGIN_PRUEBAS_CLAVE), nunca por Google.
-- * Todos los semilleros tienen código 'SEED-...'.
-- * Las fechas son relativas al día en que se ejecuta.
-- * Usa los catálogos existentes (programas, líneas, áreas, estados, roles...).
--   Solo crea habilidades, tipos de reconocimiento, entidades otorgantes,
--   roles de autor y roles de proyecto si esas tablas están vacías.
-- * No toca los usuarios reales. Se deshace con limpiar_datos_prueba.sql.
--
-- Ejecutar completo en el editor SQL de Supabase. Si algo falla, no queda nada a medias.
-- =====================================================================

BEGIN;

CREATE FUNCTION pg_temp.estado(categoria text, nombre_estado text) RETURNS integer AS $$
DECLARE
    v_id integer;
BEGIN
    SELECT e.id INTO v_id
    FROM tbl_estados e
    JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE lower(c.nombre) = lower(categoria) AND lower(e.nombre) = lower(nombre_estado);
    IF v_id IS NULL THEN
        RAISE EXCEPTION 'No existe el estado "%" en la categoría "%"', nombre_estado, categoria;
    END IF;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql;

-- Elemento i (desde 1) de un arreglo, de forma circular
CREATE FUNCTION pg_temp.elegir(arr integer[], i integer) RETURNS integer AS $$
    SELECT arr[1 + ((i - 1) % array_length(arr, 1))];
$$ LANGUAGE sql IMMUTABLE;

CREATE FUNCTION pg_temp.postular(
    p_convocatoria integer, p_estudiante integer, p_estado integer, p_fecha date,
    p_pre_aprobacion timestamptz, p_decision timestamptz, p_habilidades integer[]
) RETURNS integer AS $$
DECLARE
    v_id integer;
BEGIN
    INSERT INTO tbl_postulaciones (id_convocatoria, id_estudiante, respuesta_motivacion,
        disponibilidad_horas_semana, experiencia_previa, fecha_postulacion,
        fecha_decision_estudiante, id_estado, created_at, fecha_pre_aprobacion)
    VALUES (p_convocatoria, p_estudiante,
        'Me interesa aportar al semillero y fortalecer mi formación en investigación.',
        4 + (p_estudiante % 9), 'Proyectos de curso y trabajos de aula relacionados.',
        p_fecha, p_decision, p_estado, p_fecha::timestamptz + interval '10 hours', p_pre_aprobacion)
    RETURNING id INTO v_id;

    INSERT INTO tbl_postulacion_x_habilidad (id_postulacion, id_habilidad)
    SELECT DISTINCT v_id, h FROM unnest(p_habilidades) AS h;
    RETURN v_id;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    hoy date := current_date;
    ahora timestamptz := date_trunc('hour', now());

    nombres_estudiantes text[] := ARRAY[
        'Valentina Restrepo', 'Santiago Gómez', 'Mariana Zapata', 'Juan Pablo Ospina', 'Isabella Cardona',
        'Samuel Henao', 'Sara Montoya', 'Mateo Arango', 'Daniela Valencia', 'Tomás Giraldo',
        'Luciana Ríos', 'Emilio Vélez', 'Gabriela Correa', 'Nicolás Muñoz', 'Antonella Duque',
        'Martín Castaño', 'Salomé Jaramillo', 'Jerónimo Agudelo', 'Manuela Echeverri', 'Simón Posada',
        'Laura Betancur', 'Andrés Quintero', 'Camila Londoño', 'David Mejía', 'Paula Álvarez'];
    nombres_lideres text[] := ARRAY[
        'Alejandra Tobón', 'Felipe Uribe', 'Natalia Herrera', 'Sebastián Marín', 'Juliana Rendón'];
    nombres_profesores text[] := ARRAY[
        'Carolina Bedoya', 'Jorge Iván Salazar', 'Beatriz Elena Cano', 'Hernán Darío Pineda', 'Lucía Fernanda Mora'];
    nombres_semilleros text[] := ARRAY[
        'Semillero de Inteligencia Artificial Aplicada', 'Semillero de Biotecnología y Salud',
        'Semillero de Energías Renovables', 'Semillero de Estudios Sociales y Territorio',
        'Semillero de Ciencia de Datos Educativa'];

    v_google integer;
    v_rol_est integer;
    v_rol_prof integer;
    v_rol_admin integer;

    e_usuario_activo integer := pg_temp.estado('Estado Usuario', 'Activo');
    e_usuario_inactivo integer := pg_temp.estado('Estado Usuario', 'Inactivo');
    e_semillero_activo integer := pg_temp.estado('Estado Semillero', 'Activo');
    e_abierta integer := pg_temp.estado('Estado Convocatoria', 'Abierta');
    e_cerrada integer := pg_temp.estado('Estado Convocatoria', 'Cerrada');
    e_cancelada integer := pg_temp.estado('Estado Convocatoria', 'Cancelada');
    e_pendiente integer := pg_temp.estado('Estado Postulación', 'Pendiente');
    e_pre_aprobada integer := pg_temp.estado('Estado Postulación', 'Pre-aprobada');
    e_aceptada integer := pg_temp.estado('Estado Postulación', 'Aceptada');
    e_rechazada integer := pg_temp.estado('Estado Postulación', 'Rechazada');
    e_rechazada_est integer := pg_temp.estado('Estado Postulación', 'Rechazada por Estudiante');
    e_cascada integer := pg_temp.estado('Estado Postulación', 'Cancelada en cascada');
    e_expirada integer := pg_temp.estado('Estado Postulación', 'Expirada');
    e_proyecto_ejecucion integer := pg_temp.estado('Estado Proyecto', 'En ejecución');
    e_proyecto_otro integer;

    programas integer[];
    lineas integer[];
    areas_linea integer[];
    areas integer[];
    habilidades integer[];
    tipos_rec integer[];
    entidades integer[];
    roles_autor integer[];
    roles_proyecto integer[];

    est integer[] := '{}';       -- id de tbl_estudiantes de estudiante01..25
    est_usuario integer[] := '{}';
    lider integer[] := '{}';     -- id de tbl_estudiantes de lider1..5
    lider_usuario integer[] := '{}';
    prof integer[] := '{}';
    prof_usuario integer[] := '{}';
    sem integer[] := '{}';
    conv_hist integer[] := '{}';
    conv_rec integer[] := '{}';
    conv_abierta integer[] := '{}';

    v_id integer;
    v_usuario integer;
    v_proyecto integer;
    v_publicacion integer;
    i integer;
    s integer;
    sig integer;    -- semillero siguiente (circular)
    a integer; b integer; c integer; d integer;  -- estudiantes del grupo de cada semillero
BEGIN
    IF EXISTS (SELECT 1 FROM tbl_usuarios WHERE oauth_id LIKE 'seed-%')
        OR EXISTS (SELECT 1 FROM tbl_semilleros WHERE codigo_identificador LIKE 'SEED-%') THEN
        RAISE EXCEPTION 'Ya hay datos de prueba cargados. Ejecuta primero limpiar_datos_prueba.sql';
    END IF;

    SELECT id INTO v_google FROM tbl_oauth_proveedor WHERE nombre = 'Google';
    SELECT id INTO v_rol_est FROM tbl_roles WHERE nombre = 'Estudiante';
    SELECT id INTO v_rol_prof FROM tbl_roles WHERE nombre = 'Profesor';
    SELECT id INTO v_rol_admin FROM tbl_roles WHERE nombre = 'Administrador';
    IF v_google IS NULL OR v_rol_est IS NULL OR v_rol_prof IS NULL OR v_rol_admin IS NULL THEN
        RAISE EXCEPTION 'Faltan el proveedor Google o los roles Estudiante/Profesor/Administrador';
    END IF;

    -- Catálogos que el script solo crea si están vacíos
    IF NOT EXISTS (SELECT 1 FROM tbl_habilidades) THEN
        INSERT INTO tbl_habilidades (nombre) VALUES ('Python'), ('Análisis de datos'),
            ('Redacción científica'), ('Machine Learning'), ('Estadística'),
            ('Trabajo de campo'), ('Diseño experimental'), ('Programación web');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM tbl_tipos_reconocimiento) THEN
        INSERT INTO tbl_tipos_reconocimiento (nombre, descripcion) VALUES
            ('Premio', 'Premio otorgado en evento o convocatoria'),
            ('Mención de honor', 'Mención destacada a un trabajo de investigación'),
            ('Categorización', 'Clasificación oficial del semillero o grupo');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM tbl_entidades_otorgantes) THEN
        INSERT INTO tbl_entidades_otorgantes (nombre, descripcion) VALUES
            ('Minciencias', 'Ministerio de Ciencia, Tecnología e Innovación'),
            ('Universidad de Antioquia', 'Vicerrectoría de Investigación'),
            ('RedCOLSI', 'Red Colombiana de Semilleros de Investigación');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM tbl_roles_autor) THEN
        INSERT INTO tbl_roles_autor (nombre, descripcion) VALUES
            ('Autor principal', 'Responsable principal de la publicación'),
            ('Coautor', 'Participa en la elaboración de la publicación');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM tbl_roles_estudiante_proyecto) THEN
        INSERT INTO tbl_roles_estudiante_proyecto (nombre, descripcion) VALUES
            ('Investigador principal', 'Coordina las actividades del proyecto'),
            ('Auxiliar de investigación', 'Apoya la ejecución del proyecto');
    END IF;

    SELECT array_agg(id ORDER BY id) INTO programas FROM tbl_programas_academicos;
    SELECT array_agg(id ORDER BY id), array_agg(id_area_especialidad ORDER BY id)
        INTO lineas, areas_linea FROM tbl_linea_investigacion;
    SELECT array_agg(id ORDER BY id) INTO areas FROM tbl_areas_especialidad;
    SELECT array_agg(id ORDER BY id) INTO habilidades FROM tbl_habilidades;
    SELECT array_agg(id ORDER BY id) INTO tipos_rec FROM tbl_tipos_reconocimiento;
    SELECT array_agg(id ORDER BY id) INTO entidades FROM tbl_entidades_otorgantes;
    SELECT array_agg(id ORDER BY id) INTO roles_autor FROM tbl_roles_autor;
    SELECT array_agg(id ORDER BY id) INTO roles_proyecto FROM tbl_roles_estudiante_proyecto;
    IF programas IS NULL OR lineas IS NULL OR areas IS NULL THEN
        RAISE EXCEPTION 'Se necesitan programas académicos, líneas de investigación y áreas de especialidad';
    END IF;

    -- Estado de proyectos terminados: preferir uno tipo "Finalizado"; si no, otro de la categoría
    SELECT e.id INTO e_proyecto_otro
    FROM tbl_estados e JOIN tbl_categorias_estado c ON c.id = e.id_categoria_estado
    WHERE lower(c.nombre) = 'estado proyecto' AND e.id <> e_proyecto_ejecucion
    ORDER BY (e.nombre ILIKE 'final%' OR e.nombre ILIKE 'termin%' OR e.nombre ILIKE 'complet%') DESC, e.id
    LIMIT 1;
    e_proyecto_otro := coalesce(e_proyecto_otro, e_proyecto_ejecucion);

    -- ---------------------------------------------------------------
    -- Usuarios
    -- ---------------------------------------------------------------
    INSERT INTO tbl_usuarios (nombre_completo, email, id_proveedor_oauth, oauth_id, id_rol, created_at)
    VALUES ('Administrador de Pruebas', 'admin@seed.agora.test', v_google, 'seed-admin', v_rol_admin,
            ahora - interval '220 days');

    FOR i IN 1..5 LOOP
        INSERT INTO tbl_usuarios (nombre_completo, email, id_proveedor_oauth, oauth_id, id_rol, created_at)
        VALUES (nombres_profesores[i], 'profesor' || i || '@seed.agora.test', v_google,
                'seed-profesor-' || i, v_rol_prof, ahora - interval '210 days')
        RETURNING id INTO v_usuario;
        INSERT INTO tbl_profesores (id_usuario, id_programa, created_at)
        VALUES (v_usuario, pg_temp.elegir(programas, i), ahora - interval '210 days')
        RETURNING id INTO v_id;
        prof := prof || v_id;
        prof_usuario := prof_usuario || v_usuario;
        -- Áreas alineadas con las líneas de su semillero, para el motor de recomendación
        INSERT INTO tbl_profesor_x_area_especialidad (id_profesor, id_area_especialidad)
        SELECT v_id, area FROM (SELECT DISTINCT unnest(ARRAY[
            pg_temp.elegir(areas_linea, 2 * i - 1), pg_temp.elegir(areas, i + 1)]) AS area) t;
    END LOOP;

    FOR i IN 1..5 LOOP
        INSERT INTO tbl_usuarios (nombre_completo, email, id_proveedor_oauth, oauth_id, id_rol, created_at)
        VALUES (nombres_lideres[i], 'lider' || i || '@seed.agora.test', v_google,
                'seed-lider-' || i, v_rol_est, ahora - interval '200 days')
        RETURNING id INTO v_usuario;
        INSERT INTO tbl_estudiantes (id_usuario, id_programa, semestre, created_at)
        VALUES (v_usuario, pg_temp.elegir(programas, i), 6 + (i % 4), ahora - interval '200 days')
        RETURNING id INTO v_id;
        lider := lider || v_id;
        lider_usuario := lider_usuario || v_usuario;
        INSERT INTO tbl_estudiante_x_linea_investigacion (id_estudiante, id_linea_investigacion)
        SELECT v_id, l FROM (SELECT DISTINCT unnest(ARRAY[
            pg_temp.elegir(lineas, 2 * i - 1), pg_temp.elegir(lineas, 2 * i)]) AS l) t;
    END LOOP;

    FOR i IN 1..25 LOOP
        INSERT INTO tbl_usuarios (nombre_completo, email, id_proveedor_oauth, oauth_id, id_rol, created_at)
        VALUES (nombres_estudiantes[i], 'estudiante' || lpad(i::text, 2, '0') || '@seed.agora.test', v_google,
                'seed-estudiante-' || lpad(i::text, 2, '0'), v_rol_est, ahora - interval '190 days')
        RETURNING id INTO v_usuario;
        -- Programas rotados respecto a los semilleros: genera vinculaciones interdisciplinarias
        INSERT INTO tbl_estudiantes (id_usuario, id_programa, semestre, created_at)
        VALUES (v_usuario, pg_temp.elegir(programas, i + (i % 2)), 1 + (i % 10), ahora - interval '190 days')
        RETURNING id INTO v_id;
        est := est || v_id;
        est_usuario := est_usuario || v_usuario;
        INSERT INTO tbl_estudiante_x_linea_investigacion (id_estudiante, id_linea_investigacion)
        SELECT v_id, l FROM (SELECT DISTINCT unnest(ARRAY[
            pg_temp.elegir(lineas, i), pg_temp.elegir(lineas, i + 3)]) AS l) t;
    END LOOP;

    -- ---------------------------------------------------------------
    -- Semilleros, líneas y tutores
    -- ---------------------------------------------------------------
    FOR s IN 1..5 LOOP
        INSERT INTO tbl_semilleros (codigo_identificador, nombre, descripcion, id_estudiante_lider,
            id_programa, id_estado, created_at)
        VALUES ('SEED-' || lpad(s::text, 2, '0'), nombres_semilleros[s],
            'Semillero de prueba: espacio de formación en investigación con proyectos, publicaciones y convocatorias abiertas a estudiantes de pregrado.',
            lider[s], pg_temp.elegir(programas, s), e_semillero_activo, ahora - interval '180 days')
        RETURNING id INTO v_id;
        sem := sem || v_id;

        INSERT INTO tbl_semillero_x_linea_investigacion (id_semillero, id_linea_investigacion)
        SELECT v_id, l FROM (SELECT DISTINCT unnest(ARRAY[
            pg_temp.elegir(lineas, 2 * s - 1), pg_temp.elegir(lineas, 2 * s)]) AS l) t;

        INSERT INTO tbl_profesor_x_semillero (id_semillero, id_profesor, fecha_asignacion)
        VALUES (v_id, prof[s], hoy - 180);
        IF s IN (1, 3) THEN
            INSERT INTO tbl_profesor_x_semillero (id_semillero, id_profesor, fecha_asignacion)
            VALUES (v_id, prof[s % 5 + 1], hoy - 90);
        END IF;
    END LOOP;

    -- ---------------------------------------------------------------
    -- Convocatorias: histórica y reciente (cerradas) y una abierta por semillero
    -- ---------------------------------------------------------------
    FOR s IN 1..5 LOOP
        INSERT INTO tbl_convocatorias (titulo, descripcion, cupos_totales, cupos_disponibles,
            fecha_inicio, fecha_cierre, id_semillero, id_estado, created_at)
        VALUES ('Convocatoria ' || extract(year FROM hoy - 150) || ' - ' || nombres_semilleros[s],
            'Primera convocatoria para vincular estudiantes al semillero.',
            3, 3, hoy - 150, hoy - 120, sem[s], e_cerrada, (hoy - 152)::timestamptz)
        RETURNING id INTO v_id;
        conv_hist := conv_hist || v_id;

        INSERT INTO tbl_convocatorias (titulo, descripcion, cupos_totales, cupos_disponibles,
            fecha_inicio, fecha_cierre, id_semillero, id_estado, created_at)
        VALUES ('Segunda convocatoria - ' || nombres_semilleros[s],
            'Ampliación del equipo para los proyectos en ejecución.',
            3, 3, hoy - 70, hoy - 40, sem[s], e_cerrada, (hoy - 72)::timestamptz)
        RETURNING id INTO v_id;
        conv_rec := conv_rec || v_id;

        INSERT INTO tbl_convocatorias (titulo, descripcion, cupos_totales, cupos_disponibles,
            fecha_inicio, fecha_cierre, id_semillero, id_estado, created_at)
        VALUES ('Convocatoria abierta - ' || nombres_semilleros[s],
            'Buscamos estudiantes motivados para unirse a nuestras líneas de investigación.',
            4, 4, hoy - 7, hoy + 23, sem[s], e_abierta, (hoy - 8)::timestamptz)
        RETURNING id INTO v_id;
        conv_abierta := conv_abierta || v_id;
    END LOOP;

    -- ---------------------------------------------------------------
    -- Postulaciones
    -- Grupo del semillero s: estudiantes a, b, c, d = 4(s-1)+1 .. 4(s-1)+4.
    -- Estudiantes 21..25 no pertenecen a ningún semillero (postulantes actuales).
    -- ---------------------------------------------------------------
    FOR s IN 1..5 LOOP
        sig := s % 5 + 1;
        a := 4 * (s - 1) + 1; b := a + 1; c := a + 2; d := a + 3;

        -- Convocatoria histórica: a y b aceptan (respuestas entre 6 h y 3 días),
        -- c declina la oferta, d es rechazado por el líder
        PERFORM pg_temp.postular(conv_hist[s], est[a], e_aceptada, hoy - 145,
            (hoy - 135)::timestamptz + interval '9 hours',
            (hoy - 135)::timestamptz + interval '9 hours' + (6 * s) * interval '1 hour',
            ARRAY[pg_temp.elegir(habilidades, a), pg_temp.elegir(habilidades, a + 2)]);
        PERFORM pg_temp.postular(conv_hist[s], est[b], e_aceptada, hoy - 144,
            (hoy - 135)::timestamptz + interval '9 hours',
            (hoy - 132)::timestamptz + interval '15 hours',
            ARRAY[pg_temp.elegir(habilidades, b), pg_temp.elegir(habilidades, s)]);
        PERFORM pg_temp.postular(conv_hist[s], est[c], e_rechazada_est, hoy - 143,
            (hoy - 134)::timestamptz + interval '10 hours',
            (hoy - 133)::timestamptz + interval '18 hours',
            ARRAY[pg_temp.elegir(habilidades, c)]);
        PERFORM pg_temp.postular(conv_hist[s], est[d], e_rechazada, hoy - 142, NULL, NULL,
            ARRAY[pg_temp.elegir(habilidades, d)]);
        -- El estudiante "a" del semillero siguiente también se postuló aquí: al aceptar en su
        -- semillero, esta postulación se canceló en cascada (con oferta en semilleros pares)
        PERFORM pg_temp.postular(conv_hist[s], est[4 * (sig - 1) + 1], e_cascada, hoy - 146,
            CASE WHEN s % 2 = 0 THEN (hoy - 136)::timestamptz + interval '11 hours' END, NULL,
            ARRAY[pg_temp.elegir(habilidades, s + 3)]);
        -- El estudiante "c" del semillero siguiente recibió oferta y no respondió a tiempo
        PERFORM pg_temp.postular(conv_hist[s], est[4 * (sig - 1) + 3], e_expirada, hoy - 141,
            (hoy - 125)::timestamptz + interval '8 hours', NULL,
            ARRAY[pg_temp.elegir(habilidades, s + 1), pg_temp.elegir(habilidades, s + 4)]);

        -- Convocatoria reciente: c y d entran al semillero; el postulante 20+s no respondió
        PERFORM pg_temp.postular(conv_rec[s], est[c], e_aceptada, hoy - 66,
            (hoy - 55)::timestamptz + interval '9 hours',
            (hoy - 55)::timestamptz + interval '21 hours',
            ARRAY[pg_temp.elegir(habilidades, c), pg_temp.elegir(habilidades, c + 1)]);
        PERFORM pg_temp.postular(conv_rec[s], est[d], e_aceptada, hoy - 65,
            (hoy - 54)::timestamptz + interval '10 hours',
            (hoy - 52)::timestamptz + interval '8 hours',
            ARRAY[pg_temp.elegir(habilidades, d), pg_temp.elegir(habilidades, s + 2)]);
        PERFORM pg_temp.postular(conv_rec[s], est[20 + s], e_expirada, hoy - 64,
            (hoy - 50)::timestamptz + interval '9 hours', NULL,
            ARRAY[pg_temp.elegir(habilidades, 20 + s)]);
        PERFORM pg_temp.postular(conv_rec[s], est[4 * (sig - 1) + 2], e_rechazada, hoy - 63, NULL, NULL,
            ARRAY[pg_temp.elegir(habilidades, s + 5)]);
        -- Sin oferta y vencida: no cuenta como oferta en la retención
        PERFORM pg_temp.postular(conv_rec[s], est[20 + sig], e_expirada, hoy - 62, NULL, NULL,
            ARRAY[pg_temp.elegir(habilidades, s + 6)]);

        -- Convocatoria abierta: trabajo pendiente para líderes y ofertas para estudiantes
        PERFORM pg_temp.postular(conv_abierta[s], est[20 + s], e_pendiente, hoy - 5, NULL, NULL,
            ARRAY[pg_temp.elegir(habilidades, 20 + s), pg_temp.elegir(habilidades, s)]);
        PERFORM pg_temp.postular(conv_abierta[s], est[20 + sig], e_pre_aprobada, hoy - 6,
            (hoy - 2)::timestamptz + interval '10 hours', NULL,
            ARRAY[pg_temp.elegir(habilidades, 20 + sig)]);
        PERFORM pg_temp.postular(conv_abierta[s], est[4 * (sig - 1) + 1], e_pendiente, hoy - 4, NULL, NULL,
            ARRAY[pg_temp.elegir(habilidades, s + 2)]);
        PERFORM pg_temp.postular(conv_abierta[s], est[4 * (sig % 5) + 2], e_rechazada, hoy - 6, NULL, NULL,
            ARRAY[pg_temp.elegir(habilidades, s + 4)]);
    END LOOP;

    -- Convocatoria cancelada en el semillero 5: lo pendiente y lo pre-aprobado pasó a Expirada
    INSERT INTO tbl_convocatorias (titulo, descripcion, cupos_totales, cupos_disponibles,
        fecha_inicio, fecha_cierre, id_semillero, id_estado, created_at)
    VALUES ('Convocatoria de apoyo a evento (cancelada)',
        'Convocatoria cancelada por cambio en la agenda del evento.',
        2, 2, hoy - 30, hoy + 10, sem[5], e_cancelada, (hoy - 31)::timestamptz)
    RETURNING id INTO v_id;
    PERFORM pg_temp.postular(v_id, est[22], e_expirada, hoy - 28, NULL, NULL,
        ARRAY[pg_temp.elegir(habilidades, 3)]);
    PERFORM pg_temp.postular(v_id, est[24], e_expirada, hoy - 27,
        (hoy - 20)::timestamptz + interval '9 hours', NULL, ARRAY[pg_temp.elegir(habilidades, 5)]);

    -- Cupos disponibles = totales - aceptadas
    UPDATE tbl_convocatorias cv
    SET cupos_disponibles = cv.cupos_totales - (
        SELECT count(*) FROM tbl_postulaciones p WHERE p.id_convocatoria = cv.id AND p.id_estado = e_aceptada)
    WHERE cv.id_semillero = ANY (sem);

    -- Cada postulación aceptada vincula al estudiante desde el día en que aceptó
    INSERT INTO tbl_estudiante_x_semillero (id_semillero, id_estudiante, fecha_ingreso, fecha_salida, id_estado)
    SELECT cv.id_semillero, p.id_estudiante, p.fecha_decision_estudiante::date, NULL, e_usuario_activo
    FROM tbl_postulaciones p JOIN tbl_convocatorias cv ON cv.id = p.id_convocatoria
    WHERE cv.id_semillero = ANY (sem) AND p.id_estado = e_aceptada;

    -- Un integrante retirado del semillero 1 (estudiante04)
    UPDATE tbl_estudiante_x_semillero
    SET fecha_salida = hoy - 10, id_estado = e_usuario_inactivo
    WHERE id_semillero = sem[1] AND id_estudiante = est[4];

    -- ---------------------------------------------------------------
    -- Proyectos, integrantes, publicaciones y autores
    -- ---------------------------------------------------------------
    FOR s IN 1..5 LOOP
        a := 4 * (s - 1) + 1; b := a + 1; c := a + 2;

        -- Proyecto terminado con dos publicaciones
        INSERT INTO tbl_proyectos (titulo, descripcion, id_semillero, presupuesto_asignado,
            fecha_inicio, fecha_fin, id_estado, created_at)
        VALUES ('Diagnóstico inicial - ' || nombres_semilleros[s],
            'Proyecto de arranque del semillero: revisión del estado del arte y diagnóstico.',
            sem[s], 3000000 + 500000 * s, hoy - 170, hoy - 20, e_proyecto_otro, (hoy - 171)::timestamptz)
        RETURNING id INTO v_proyecto;
        INSERT INTO tbl_estudiante_x_proyecto (id_proyecto, id_estudiante, id_rol_proyecto, fecha_vinculacion)
        VALUES (v_proyecto, lider[s], pg_temp.elegir(roles_proyecto, 1), hoy - 170),
               (v_proyecto, est[a], pg_temp.elegir(roles_proyecto, 2), hoy - 130),
               (v_proyecto, est[b], pg_temp.elegir(roles_proyecto, 2), hoy - 130);

        FOR i IN 1..2 LOOP
            INSERT INTO tbl_publicaciones (titulo, url_documento, fecha_publicacion, id_proyecto, created_at)
            VALUES (CASE i WHEN 1 THEN 'Estado del arte: ' ELSE 'Resultados preliminares: ' END
                        || nombres_semilleros[s],
                    'https://example.org/publicaciones/seed-' || s || '-' || i || '.pdf',
                    hoy - 60 + 25 * i, v_proyecto, (hoy - 60 + 25 * i)::timestamptz)
            RETURNING id INTO v_publicacion;
            INSERT INTO tbl_publicacion_x_usuario (id_publicacion, id_usuario, id_rol_autor, fecha_asociacion)
            VALUES (v_publicacion, lider_usuario[s], pg_temp.elegir(roles_autor, 1), hoy - 60 + 25 * i),
                   (v_publicacion, est_usuario[CASE i WHEN 1 THEN a ELSE b END],
                        pg_temp.elegir(roles_autor, 2), hoy - 60 + 25 * i),
                   (v_publicacion, prof_usuario[s], pg_temp.elegir(roles_autor, 2), hoy - 60 + 25 * i);
        END LOOP;

        -- Proyecto en ejecución; en semilleros impares ya tiene una publicación
        INSERT INTO tbl_proyectos (titulo, descripcion, id_semillero, presupuesto_asignado,
            fecha_inicio, fecha_fin, id_estado, created_at)
        VALUES ('Proyecto aplicado - ' || nombres_semilleros[s],
            'Desarrollo de una solución aplicada a partir de los hallazgos del diagnóstico.',
            sem[s], 5000000 + 750000 * s, hoy - 45, hoy + 240, e_proyecto_ejecucion, (hoy - 46)::timestamptz)
        RETURNING id INTO v_proyecto;
        INSERT INTO tbl_estudiante_x_proyecto (id_proyecto, id_estudiante, id_rol_proyecto, fecha_vinculacion)
        VALUES (v_proyecto, lider[s], pg_temp.elegir(roles_proyecto, 1), hoy - 45),
               (v_proyecto, est[c], pg_temp.elegir(roles_proyecto, 2), hoy - 40);
        IF s % 2 = 1 THEN
            INSERT INTO tbl_publicaciones (titulo, url_documento, fecha_publicacion, id_proyecto, created_at)
            VALUES ('Avance metodológico: ' || nombres_semilleros[s],
                    'https://example.org/publicaciones/seed-' || s || '-3.pdf', hoy - 5, v_proyecto,
                    (hoy - 5)::timestamptz)
            RETURNING id INTO v_publicacion;
            INSERT INTO tbl_publicacion_x_usuario (id_publicacion, id_usuario, id_rol_autor, fecha_asociacion)
            VALUES (v_publicacion, est_usuario[c], pg_temp.elegir(roles_autor, 1), hoy - 5),
                   (v_publicacion, prof_usuario[s], pg_temp.elegir(roles_autor, 2), hoy - 5);
        END IF;

        -- Reconocimientos: uno por semillero y otro en los pares
        INSERT INTO tbl_reconocimientos_semilleros (id_semillero, id_tipo_reconocimiento, titulo, descripcion,
            id_entidad_otorgante, fecha_obtencion, url_certificado, created_at)
        VALUES (sem[s], pg_temp.elegir(tipos_rec, s), 'Mejor ponencia en encuentro regional',
            'Reconocimiento a la ponencia del proyecto de diagnóstico.', pg_temp.elegir(entidades, s),
            hoy - 30, 'https://example.org/certificados/seed-' || s || '-1.pdf', (hoy - 29)::timestamptz);
        IF s % 2 = 0 THEN
            INSERT INTO tbl_reconocimientos_semilleros (id_semillero, id_tipo_reconocimiento, titulo, descripcion,
                id_entidad_otorgante, fecha_obtencion, url_certificado, created_at)
            VALUES (sem[s], pg_temp.elegir(tipos_rec, s + 1), 'Reconocimiento a la trayectoria del semillero',
                'Distinción por la producción académica del semillero.', pg_temp.elegir(entidades, s + 1),
                hoy - 90, 'https://example.org/certificados/seed-' || s || '-2.pdf', (hoy - 89)::timestamptz);
        END IF;
    END LOOP;

    RAISE NOTICE 'Datos de prueba creados: 1 admin, 5 profesores, 30 estudiantes, 5 semilleros';
END;
$$;

COMMIT;
