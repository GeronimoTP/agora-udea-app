package com.udea.agora_backend.service;

import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Inicio de sesión sin Google para probar la app con usuarios precargados por el script de datos.
 * Solo funciona si APP_LOGIN_PRUEBAS_CLAVE tiene al menos 16 caracteres, y solo para usuarios cuyo
 * oauth_id empieza por "seed-": las cuentas reales creadas por Google nunca entran por aquí.
 * Vaciar la variable antes de la entrega.
 */
@Slf4j
@Service
public class LoginPruebasService {

    public static final String PREFIJO_OAUTH_PRUEBAS = "seed-";
    private static final int LONGITUD_MINIMA_CLAVE = 16;
    private static final String CREDENCIALES_INVALIDAS = "Correo o clave de pruebas inválidos";

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final JwtService jwtService;
    private final byte[] clave;

    public LoginPruebasService(UsuarioRepository usuarioRepository,
                               EstudianteRepository estudianteRepository,
                               ProfesorRepository profesorRepository,
                               JwtService jwtService,
                               @Value("${app.login-pruebas.clave:}") String clave) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorRepository = profesorRepository;
        this.jwtService = jwtService;
        if (clave.length() >= LONGITUD_MINIMA_CLAVE) {
            this.clave = clave.getBytes(StandardCharsets.UTF_8);
            log.warn("Login de pruebas HABILITADO: vaciar APP_LOGIN_PRUEBAS_CLAVE antes de la entrega");
        } else {
            this.clave = null;
            if (!clave.isEmpty()) {
                log.warn("Login de pruebas deshabilitado: la clave debe tener al menos {} caracteres",
                    LONGITUD_MINIMA_CLAVE);
            }
        }
    }

    public boolean estaHabilitado() {
        return clave != null;
    }

    @Transactional(readOnly = true)
    public String iniciarSesion(String correo, String claveRecibida) {
        if (!estaHabilitado()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // Comparación en tiempo constante para no filtrar la clave por tiempos de respuesta
        boolean claveValida = MessageDigest.isEqual(clave, claveRecibida.getBytes(StandardCharsets.UTF_8));
        Usuario usuario = usuarioRepository.findWithRolByEmail(correo.trim())
            .filter(u -> u.getOauthId().startsWith(PREFIJO_OAUTH_PRUEBAS))
            .orElse(null);
        if (!claveValida || usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        boolean perfilCompleto = "Administrador".equals(usuario.getRol().getNombre())
            || estudianteRepository.findByUsuarioId(usuario.getId()).isPresent()
            || profesorRepository.findByUsuarioId(usuario.getId()).isPresent();
        if (!perfilCompleto) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                "El usuario de prueba no tiene perfil de estudiante ni de profesor");
        }

        log.info("Login de pruebas como usuario {}", usuario.getId());
        return jwtService.generarToken(usuario.getEmail());
    }
}
