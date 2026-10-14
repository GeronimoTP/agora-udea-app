package com.udea.agora_backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor // Importante para inyectar JwtService
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService; // Inyectamos el servicio creador de tokens
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String oauthId = oAuth2User.getAttribute("sub");

        Usuario usuario = usuarioRepository.findWithRolByOauthId(oauthId)
            .orElseGet(() -> vincularAdministrador(email, oauthId));
        // El administrador no tiene perfil de estudiante/profesor y no pasa por onboarding
        boolean perfilCompleto = usuario != null && (
            "Administrador".equals(usuario.getRol().getNombre())
                || estudianteRepository.findByUsuarioId(usuario.getId()).isPresent()
                || profesorRepository.findByUsuarioId(usuario.getId()).isPresent());
        boolean onboarding = !perfilCompleto;
        String nombre = oAuth2User.getAttribute("name");
        String token = onboarding
            ? jwtService.generarTokenOnboarding(email, oauthId, nombre == null ? email : nombre)
            : jwtService.generarToken(email);

        String targetUrl = frontendUrl.replaceAll("/+$", "")
            + "/oauth2/redirect?token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8)
            + "&onboarding=" + onboarding;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // La cuenta administrador se crea manualmente en la BD sin conocer el `sub` de Google.
    // En su primer inicio de sesión se vincula por el correo verificado por Google.
    private Usuario vincularAdministrador(String email, String oauthId) {
        return usuarioRepository.findWithRolByEmail(email)
            .filter(usuario -> "Administrador".equals(usuario.getRol().getNombre()))
            .map(usuario -> {
                usuario.setOauthId(oauthId);
                return usuarioRepository.save(usuario);
            })
            .orElse(null);
    }
}
