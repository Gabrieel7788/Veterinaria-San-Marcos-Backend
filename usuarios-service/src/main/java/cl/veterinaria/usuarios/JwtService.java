package cl.veterinaria.usuarios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey clave;

    public JwtService(
            @Value("${jwt.secret}") String secretoBase64) {

        this.clave = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(secretoBase64)
        );
    }

    public String generarToken(Usuario usuario) {

        Instant ahora = Instant.now();

        return Jwts.builder()
                .issuer("veterinaria-san-marcos")
                .subject(usuario.getId().toString())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(
                    ahora.plus(30, ChronoUnit.MINUTES)
                ))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public Long validarYObtenerId(String token) {

        var datos = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer("veterinaria-san-marcos")
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(datos.getSubject());
    }

}
