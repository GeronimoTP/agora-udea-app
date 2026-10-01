package com.udea.agora_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.onboarding.expiration:1800000}")
    private long onboardingExpiration;

    // Genera el token usando el correo del usuario
    public String generarToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
            .claim("token_type", "access")
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

        public String generarTokenOnboarding(String email, String oauthId, String nombreCompleto) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .setSubject(email)
            .claim("token_type", "onboarding")
            .claim("oauth_id", oauthId)
            .claim("nombre_completo", nombreCompleto)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + onboardingExpiration))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
        }

    // Extrae el correo (subject) del token
    public String extraerCorreo(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extraerOAuthId(String token) {
        return extraerClaims(token).get("oauth_id", String.class);
    }

    public String extraerNombreCompleto(String token) {
        return extraerClaims(token).get("nombre_completo", String.class);
    }

    public boolean esTokenOnboarding(String token) {
        try {
            return validarToken(token)
                    && "onboarding".equals(extraerClaims(token).get("token_type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    // Valida si el token es correcto y no ha expirado
    public boolean validarToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extraerClaims(token));
    }

    private Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
