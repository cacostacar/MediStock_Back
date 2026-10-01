package br.com.fiap.medistockbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${medistock.jwt.secret}")
    private String segredoBase64;

    @Value("${medistock.jwt.expiracao-minutos}")
    private long expiracaoMinutos;

    private SecretKey chave() {
        byte[] bytes = Base64.getDecoder().decode(segredoBase64);
        return Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(String emailInstitucional) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMinutos * 60 * 1000);

        return Jwts.builder()
                .subject(emailInstitucional)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave())
                .compact();
    }

    public long getExpiracaoMinutos() {
        return expiracaoMinutos;
    }

    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean tokenValido(String token, String emailEsperado) {
        String email = extrairEmail(token);
        return email.equalsIgnoreCase(emailEsperado) && !tokenExpirado(token);
    }

    private boolean tokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
