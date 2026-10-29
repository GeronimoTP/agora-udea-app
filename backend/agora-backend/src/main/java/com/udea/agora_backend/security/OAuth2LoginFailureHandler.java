package com.udea.agora_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Si Google rechaza el login o la cuenta no cumple las reglas (dominio @udea.edu.co, correo
 * asociado a otra cuenta), vuelve al login del frontend con un código de error en lugar de
 * mostrar la página de error del backend. Solo se envían códigos conocidos, nunca el detalle.
 */
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Set<String> CODIGOS_CONOCIDOS = Set.of("invalid_google_account", "account_conflict");

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String codigo = exception instanceof OAuth2AuthenticationException oauth
                && CODIGOS_CONOCIDOS.contains(oauth.getError().getErrorCode())
                ? oauth.getError().getErrorCode()
                : "login_fallido";
        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl.replaceAll("/+$", "") + "/auth/login?error=" + codigo);
    }
}
