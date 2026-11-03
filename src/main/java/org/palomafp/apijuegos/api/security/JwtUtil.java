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
import java.util.UUID;
import java.util.function.Function;

/**
 * Clase utilitaria para manejar la creación y validación de tokens JWT.
 * Se encarga de generar el token cuando un usuario hace login y de verificar
 * si un token es válido cuando hace una petición.
 * @author Andrés López
 */
@Component
public class JwtUtil {

    /** Vigencia del token: 30 minutos desde que se emite. */
    private static final Duration VALIDEZ = Duration.ofMinutes(30);

    /** Claim con el id interno del usuario (evita consultar la BD en cada petición). */
    public static final String CLAIM_MI_ID = "miId";

    /** Claim con el rol del usuario (permite reconstruir las autoridades sin BD). */
    public static final String CLAIM_ROL = "rol";

    /** Prefijo que Spring Security espera en las autoridades de tipo {@code ROLE_*}. */
    private static final String PREFIJO_ROL = "ROLE_";

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
     *
     * <p>Si el principal conoce el {@code miId} (caso normal), el token lleva
     * además el id y el rol. Con esos dos datos el filtro puede autenticar la
     * petición sin consultar MongoDB.</p>
     *
     * @param userDetails Objeto con la información del usuario autenticado.
     * @return Una cadena de texto que representa el token firmado.
     */
    public String generateToken(UserDetails userDetails) {
        if (userDetails instanceof UsuarioAutenticado autenticado) {
            return createToken(autenticado.getUsername(), autenticado.getMiId(), rolDe(autenticado));
        }
        // Token sin claims extra: se sigue emitiendo para no romper a quien
        // construya un UserDetails plano; el filtro caerá a la base de datos.
        return createToken(userDetails.getUsername(), null, null);
    }

    /**
     * Extrae el id interno del usuario del token, si lo lleva.
     *
     * @param token Token JWT.
     * @return Id interno, o {@code null} en tokens antiguos sin claim.
     */
    public Integer extractMiId(String token) {
        Object valor = extractClaim(token, claims -> claims.get(CLAIM_MI_ID));
        return valor instanceof Number numero ? numero.intValue() : null;
    }

    /**
     * Extrae el rol del usuario del token, si lo lleva.
     *
     * @param token Token JWT.
     * @return Nombre del rol (sin prefijo {@code ROLE_}), o {@code null}.
     */
    public String extractRol(String token) {
        Object valor = extractClaim(token, claims -> claims.get(CLAIM_ROL));
        return valor instanceof String texto ? texto : null;
    }

    /**
     * Nombre del rol del usuario a partir de sus autoridades.
     *
     * @param userDetails Principal autenticado.
     * @return Rol sin prefijo, o {@code null} si no tiene ninguno.
     */
    private static String rolDe(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(autoridad -> autoridad.startsWith(PREFIJO_ROL))
                .map(autoridad -> autoridad.substring(PREFIJO_ROL.length()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Método auxiliar encargado de la construcción interna del token con la librería JJWT.
     *
     * @param subject El nombre de usuario que será el sujeto del token.
     * @param miId    Id interno del usuario (puede ser {@code null}).
     * @param rol     Rol del usuario (puede ser {@code null}).
     * @return El token JWT en formato String.
     */
    private String createToken(String subject, Integer miId, String rol) {
        Instant ahora = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(VALIDEZ)));
        if (miId != null) {
            builder.claim(CLAIM_MI_ID, miId);
        }
        if (rol != null) {
            builder.claim(CLAIM_ROL, rol);
        }
        return builder.signWith(getSigningKey()).compact();
    }

    /**
     * Verifica que el token pertenece al usuario indicado y que no ha expirado.
     *
     * <p>Variante sin {@link UserDetails}: cuando el token ya trae los claims
     * suficientes no hace falta cargar el usuario de la base de datos.</p>
     *
     * @param token    Token de la petición.
     * @param username Nombre de usuario esperado.
     * @return true si el token es válido para ese usuario.
     */
    public boolean validateToken(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
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
