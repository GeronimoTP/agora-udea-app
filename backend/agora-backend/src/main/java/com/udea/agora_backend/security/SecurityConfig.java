package com.udea.agora_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    // Catálogos que solo el administrador puede crear, editar o eliminar
    private static final String[] CATALOGOS_ADMINISTRABLES = {
        "/api/areas-especialidad", "/api/areas-especialidad/**",
        "/api/lineas-investigacion", "/api/lineas-investigacion/**",
        "/api/habilidades", "/api/habilidades/**",
        "/api/estados", "/api/estados/**"
    };

    // Orígenes autorizados para llamar a la API desde el navegador (separados por coma)
    @Value("${app.cors.allowed-origins}")
    private String origenesPermitidos;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/api/public/**",
                    "/",
                    "/error",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Login de pruebas: público, pero apagado si no hay clave (ver LoginPruebasService)
                .requestMatchers("/api/auth/login-pruebas").permitAll()

                // Analítica de un semillero: la ven sus líderes, tutores y el administrador
                // (AutorizacionSemilleroService). Debe ir antes de la regla general de analítica.
                .requestMatchers(HttpMethod.GET, "/api/analitica/semillero/*").authenticated()
                .requestMatchers("/api/analitica/**").hasRole("Administrador")

                // Sesión propia: disponible para cualquier usuario autenticado
                .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()

                // Administración: gestión de cuentas, alta directa de perfiles
                // (los perfiles se crean por onboarding), listado global de postulaciones y cron manual
                .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("Administrador")
                .requestMatchers(HttpMethod.POST, "/api/estudiantes", "/api/profesores").hasRole("Administrador")
                .requestMatchers(HttpMethod.GET, "/api/postulaciones").hasRole("Administrador")
                .requestMatchers(HttpMethod.POST, "/api/convocatorias/procesar-expiradas").hasRole("Administrador")
                // Perfiles por ID de usuario: solo administración (cada quien usa /me)
                .requestMatchers(HttpMethod.GET, "/api/estudiantes/usuario/*", "/api/profesores/usuario/*")
                    .hasRole("Administrador")

                // Aprobación de semilleros (el reenvío de uno rechazado es de sus líderes)
                .requestMatchers(HttpMethod.GET, "/api/semilleros/pendientes-aprobacion").hasRole("Administrador")
                .requestMatchers(HttpMethod.POST, "/api/semilleros/*/aprobar", "/api/semilleros/*/rechazar")
                    .hasRole("Administrador")

                // Solicitudes de catálogo: estudiantes y profesores proponen, el administrador decide
                .requestMatchers(HttpMethod.GET, "/api/solicitudes-catalogo/mias").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/solicitudes-catalogo").hasAnyRole("Estudiante", "Profesor")
                .requestMatchers("/api/solicitudes-catalogo", "/api/solicitudes-catalogo/**").hasRole("Administrador")
                // Alta directa de entidades otorgantes y tipos de reconocimiento
                .requestMatchers(HttpMethod.POST, "/api/catalogos/**").hasRole("Administrador")

                // Retiro voluntario: lo pide el integrante; la propiedad se valida en el servicio
                .requestMatchers(HttpMethod.POST, "/api/solicitudes-retiro").hasRole("Estudiante")

                // Catálogos: cualquier usuario autenticado los lee, solo el administrador los modifica.
                // Renombrar o borrar un estado o una línea rompería convocatorias, recomendaciones y analítica.
                .requestMatchers(HttpMethod.POST, CATALOGOS_ADMINISTRABLES).hasRole("Administrador")
                .requestMatchers(HttpMethod.PUT, CATALOGOS_ADMINISTRABLES).hasRole("Administrador")
                .requestMatchers(HttpMethod.PATCH, CATALOGOS_ADMINISTRABLES).hasRole("Administrador")
                .requestMatchers(HttpMethod.DELETE, CATALOGOS_ADMINISTRABLES).hasRole("Administrador")

                // Acciones del estudiante: postularse, ver recomendaciones y decidir sobre una oferta.
                // Lo propio va por rutas /me y /mias: el estudiante sale del token, no de la URL
                .requestMatchers(HttpMethod.POST, "/api/postulaciones").hasRole("Estudiante")
                .requestMatchers(HttpMethod.GET, "/api/postulaciones/mias", "/api/postulaciones/mias/**").hasRole("Estudiante")
                .requestMatchers(HttpMethod.POST,
                    "/api/postulaciones/*/confirmar-aceptacion",
                    "/api/postulaciones/*/rechazar-estudiante").hasRole("Estudiante")
                .requestMatchers("/api/recomendaciones/**").hasRole("Estudiante")

                // Acciones del líder (estudiante líder o profesor del semillero); la propiedad
                // del semillero se valida en AutorizacionSemilleroService
                .requestMatchers(HttpMethod.POST,
                    "/api/postulaciones/*/pre-aprobar",
                    "/api/postulaciones/*/rechazar-lider").hasAnyRole("Estudiante", "Profesor")

                // Registrar semilleros (quedan pendientes de aprobación) y crear convocatorias
                // (el liderazgo se valida en AutorizacionSemilleroService)
                .requestMatchers(HttpMethod.POST, "/api/semilleros", "/api/convocatorias")
                    .hasAnyRole("Estudiante", "Profesor")

                // El resto de rutas protegidas valida propiedad o liderazgo en service/autorizacion

                .anyRequest().authenticated()
            )
            // La API responde 401 sin sesión o con un token inválido/vencido; sin esto, Spring
            // redirige a Google y el frontend solo ve un error de red
            .exceptionHandling(excepciones -> excepciones
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler)
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS solo para el frontend desplegado (APP_CORS_ORIGINS o, por defecto, APP_FRONTEND_URL).
    // El token viaja en el header Authorization, así que no se permiten cookies entre orígenes.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
            .map(String::trim)
            .map(origen -> origen.replaceAll("/+$", ""))
            .filter(origen -> !origen.isEmpty())
            .toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}