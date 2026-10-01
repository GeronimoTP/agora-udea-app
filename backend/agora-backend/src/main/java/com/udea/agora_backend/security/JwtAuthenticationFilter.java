package com.udea.agora_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import  org.jspecify.annotations.NonNull ;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.udea.agora_backend.repository.UsuarioRepository;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String correoUsuario;

        // Si no hay token o no empieza con "Bearer ", ignorar y seguir
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token (quitando "Bearer ")
        jwt = authHeader.substring(7);
        
        try {
            correoUsuario = jwtService.extraerCorreo(jwt);
            
            // Si el correo existe en el token y no hay autenticación actual en el contexto
            if (correoUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtService.validarToken(jwt)) {
                    // Crear objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            correoUsuario,
                            null,
                            Collections.emptyList() // Aquí irían los roles (ej. ROLE_ESTUDIANTE)
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Guardar el usuario en el contexto de seguridad de Spring
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                }
            }
        } catch (Exception e) {
            logger.error("Error validando el token JWT: " + e.getMessage());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = obtenerEmail(authentication);
        boolean perfilPendiente = email != null && usuarioRepository.findByEmail(email)
                .map(usuario -> "Pendiente".equalsIgnoreCase(usuario.getRol().getNombre()))
                .orElse(false);

        if (perfilPendiente && !permiteCompletarPerfil(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Debes completar tu perfil antes de usar esta funcionalidad");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        }
        if (principal instanceof String email && email.contains("@")) {
            return email;
        }
        return null;
    }

    private boolean permiteCompletarPerfil(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && (
                "/api/auth/onboarding/estudiante".equals(path)
                        || "/api/auth/onboarding/profesor".equals(path))) {
            return true;
        }

        return "GET".equals(method) && (
                "/api/catalogos/programas-academicos".equals(path)
                        || "/api/lineas-investigacion".equals(path)
                        || "/api/areas-especialidad".equals(path));
    }
}
