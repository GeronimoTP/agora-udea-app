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

                .requestMatchers("/api/analitica/**").hasRole("Administrador")

                // Sesión propia: disponible para cualquier usuario autenticado
                .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()

                // Administración: gestión de cuentas, alta directa de perfiles
                // (los perfiles se crean por onboarding), listado global de postulaciones y cron manual
                .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("Administrador")
                .requestMatchers(HttpMethod.POST, "/api/estudiantes", "/api/profesores").hasRole("Administrador")
                .requestMatchers(HttpMethod.GET, "/api/postulaciones").hasRole("Administrador")
                .requestMatchers(HttpMethod.POST, "/api/convocatorias/procesar-expiradas").hasRole("Administrador")

                // Acciones del estudiante: postularse, ver recomendaciones y decidir sobre una oferta
                .requestMatchers(HttpMethod.POST, "/api/postulaciones").hasRole("Estudiante")
                .requestMatchers(HttpMethod.POST,
                    "/api/postulaciones/*/confirmar-aceptacion",
                    "/api/postulaciones/*/rechazar-estudiante").hasRole("Estudiante")
                .requestMatchers("/api/recomendaciones/**").hasRole("Estudiante")

                // Acciones del líder (estudiante líder o profesor del semillero); la propiedad
                // del semillero se valida en AutorizacionService
                .requestMatchers(HttpMethod.POST,
                    "/api/postulaciones/*/pre-aprobar",
                    "/api/postulaciones/*/rechazar-lider").hasAnyRole("Estudiante", "Profesor")

                // Crear semilleros y convocatorias (el liderazgo se valida en AutorizacionService)
                .requestMatchers(HttpMethod.POST, "/api/semilleros", "/api/convocatorias")
                    .hasAnyRole("Estudiante", "Profesor")

                // El resto de rutas protegidas valida propiedad o liderazgo en AutorizacionService

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