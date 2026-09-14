package com.udea.agora_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import  org.jspecify.annotations.NonNull ;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

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

        filterChain.doFilter(request, response);
    }
}
