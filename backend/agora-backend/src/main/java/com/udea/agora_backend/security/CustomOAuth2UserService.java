package com.udea.agora_backend.security;

import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.model.OauthProveedor;
import com.udea.agora_backend.model.Rol;
import com.udea.agora_backend.repository.OauthProveedorRepository;
import com.udea.agora_backend.repository.RolRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;
    private final OauthProveedorRepository oauthProveedorRepository;
    private final RolRepository rolRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");
        String oauthId = oAuth2User.getAttribute("sub");

        if (email == null || !email.toLowerCase().endsWith("@udea.edu.co") || oauthId == null) {
            log.warn("Intento de login con correo no institucional: {}", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_account"),
                    "Debe usar una cuenta Google @udea.edu.co válida"
            );
        }

        if (usuarioRepository.findByOauthId(oauthId).isPresent()) {
            return oAuth2User;
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_conflict"),
                    "El correo ya está asociado a otra cuenta"
            );
        }

        OauthProveedor proveedor = oauthProveedorRepository.findByNombre("Google")
                .orElseThrow(() -> new IllegalStateException("No existe el proveedor OAuth 'Google'"));
        Rol rolPendiente = rolRepository.findByNombre("Pendiente")
                .orElseThrow(() -> new IllegalStateException("No existe el rol 'Pendiente'"));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setNombreCompleto(nombre == null ? email : nombre);
        nuevoUsuario.setProveedorOauth(proveedor);
        nuevoUsuario.setOauthId(oauthId);
        nuevoUsuario.setRol(rolPendiente);
        nuevoUsuario.setCreatedAt(ZonedDateTime.now());
        usuarioRepository.save(nuevoUsuario);

        log.info("Registrando nuevo usuario desde Google: {}", email);

        return oAuth2User;
    }
}