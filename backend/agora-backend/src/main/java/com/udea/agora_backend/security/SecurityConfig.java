package com.udea.agora_backend.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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

                // Administración: gestión de cuentas, registro heredado, alta directa de perfiles
                // (los perfiles se crean por onboarding), listado global de postulaciones y cron manual
                .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("Administrador")
                .requestMatchers("/api/auth/register/**").hasRole("Administrador")
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
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
            )

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}