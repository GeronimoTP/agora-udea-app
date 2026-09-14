package com.udea.agora_backend.security;

import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");

        // 1. Validar dominio institucional
        if (email == null || !email.endsWith("@udea.edu.co")) {
            log.warn("Intento de login con correo no institucional: {}", email);
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_domain"), "Debe usar un correo @udea.edu.co");
        }

        // 2. Buscar usuario en BD o crearlo si no existe
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isEmpty()) {
            log.info("Registrando nuevo usuario desde Google: {}", email);
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setEmail(email);
            nuevoUsuario.setNombreCompleto(nombre);
            // Aquí puedes asignar roles por defecto o marcarlo como pendiente de completar perfil
            usuarioRepository.save(nuevoUsuario);
        }

        return oAuth2User;
    }
}