package com.udea.agora_backend.security;

import com.udea.agora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String oauthId = oAuth2User.getAttribute("sub");

        if (email == null || !email.toLowerCase(Locale.ROOT).endsWith("@udea.edu.co") || oauthId == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_account"),
                    "Debe usar una cuenta Google @udea.edu.co válida"
            );
        }

        boolean correoAsociadoAOtraCuenta = usuarioRepository.findByEmail(email)
            .filter(usuario -> !oauthId.equals(usuario.getOauthId()))
            .isPresent();
        if (correoAsociadoAOtraCuenta) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("account_conflict"),
                "El correo ya está asociado a otra cuenta OAuth"
            );
        }

        return oAuth2User;
    }
}