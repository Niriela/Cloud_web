package com.cloudweb.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.cloudweb.repository.ReglesGestionRepository;
import com.cloudweb.entity.ReglesGestion;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private final ReglesGestionRepository reglesGestionRepository;

    public JwtTokenProvider(ReglesGestionRepository reglesGestionRepository) {
        this.reglesGestionRepository = reglesGestionRepository;
    }

    public String generateToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenFromEmail(userPrincipal.getEmail());
    }

    public String generateTokenFromEmail(String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        long expirationMs = resolveExpirationMs();
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserNameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    private long resolveExpirationMs() {
        try {
            ReglesGestion regle = reglesGestionRepository
                    .findByLibelle("Duree_vie_session")
                    .orElse(null);
            if (regle == null || regle.getValeur() == null) {
                return jwtExpirationMs;
            }

            long minutes = Long.parseLong(regle.getValeur());
            if (minutes <= 0) {
                return jwtExpirationMs;
            }

            return minutes * 60_000L;
        } catch (Exception ex) {
            log.warn("Failed to resolve session duration from Regles_gestion: {}", ex.getMessage());
            return jwtExpirationMs;
        }
    }
}
