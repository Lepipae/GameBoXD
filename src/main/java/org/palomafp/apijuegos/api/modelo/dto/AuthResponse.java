package org.palomafp.apijuegos.api.modelo.dto;

/**
 * Data Transfer Object (DTO) para manejar las respuestas de inicio de sesión exitoso.
 * Contiene el token JWT generado para que el cliente lo utilice en futuras peticiones.
 *
 * <p>Como {@code record}, el token se expone con {@code jwt()} y no puede cambiarse
 * después de emitido.</p>
 *
 * @param jwt Token JWT generado tras una autenticación exitosa.
 * @author Andrés López
 */
public record AuthResponse(String jwt) {
}
