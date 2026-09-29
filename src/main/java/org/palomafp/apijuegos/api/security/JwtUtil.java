package org.palomafp.apijuegos.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

/**
 * Clase utilitaria para manejar la creación y validación de tokens JWT.
 * Se encarga de generar el token cuando un usuario hace login y de verificar
 * si un token es válido cuando hace una petición.
 * @author Andrés López
 */
@Component
public class JwtUtil {

    /** Vigencia del token: 10 horas desde que se emite. */
    private static final Duration VALIDEZ = Duration.ofHours(10);

    // Clave secreta para firmar el token JWT. En un entorno de producción, esto debería estar en variables de entorno o application.properties.
    // Esta cadena debe tener al menos 256 bits (32 caracteres).
    @Value("${CLAVE_CIFRADO}")
    private String secretKey;

    /**
     * Obtiene la clave secreta generada a partir de la cadena de texto para firmar o validar tokens.
     * @return La clave criptográfica para JWT.
     */
    private SecretKey getSigningKey() {
        // El código anterior hacía encode base64 de los bytes y acto seguido los
        // decodificaba. Decodificar lo que se acaba de codificar devuelve los
        // bytes originales, así que el viaje de ida y vuelta era un no-op:
        // la clave firmada es exactamente la misma que la de antes.
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extrae el nombre de usuario (subject) contenido dentro del token JWT.
     * @param token Token proporcionado por el usuario.
     * @return Nombre de usuario que está dentro del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración contenida dentro del token JWT.
     * @param token Token proporcionado por el usuario.
     * @return Fecha en la que caduca el token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Método genérico para extraer información específica (claim) del token.
     * @param token El token JWT a procesar.
     * @param claimsResolver Función para procesar y extraer un claim en concreto.
     * @param <T> Tipo del dato devuelto.
     * @return Valor del claim solicitado.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Descifra el token y obtiene toda la información (claims) en su interior usando la clave secreta.
     * @param token Token proporcionado.
     * @return Objeto Claims con todos los datos del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si el token ha caducado.
     * @param token Token a comprobar.
     * @return true si la fecha de expiración es anterior al momento actual.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).toInstant().isBefore(Instant.now());
    }

    /**
     * Genera un nuevo token JWT a partir de los detalles de un usuario.
     * @param userDetails Objeto con la información del usuario autenticado.
     * @return Una cadena de texto que representa el token firmado.
     */
    public String generateToken(UserDetails userDetails) {
        return createToken(userDetails.getUsername());
    }

    /**
     * Método auxiliar encargado de la construcción interna del token con la librería JJWT.
     * @param subject El nombre de usuario que será el sujeto del token.
     * @return El token JWT en formato String.
     */
    private String createToken(String subject) {
        // El código anterior pasaba un Map de claims vacío. No aportaba nada
        // propio al token, asi que se ha quitado: JJWT genera la cabecera y la
        // firma por su cuenta.
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(VALIDEZ)))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Verifica que el token pertenece al usuario y que además no ha expirado.
     * @param token Token de la petición.
     * @param userDetails Datos del usuario encontrados en la base de datos.
     * @return true si el token es totalmente válido para ese usuario.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
