package org.palomafp.apijuegos.api.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) para manejar las peticiones de inicio de sesión (Login).
 * Contiene las credenciales enviadas por el usuario.
 *
 * <p>Es un {@code record}: no necesita constructor vacío, setters ni campos privados
 * declarados a mano. Jackson lo deserializa a través del constructor canónico, así que
 * el acceso es por accesores ({@code nombre()}) y el objeto es inmutable.</p>
 *
 * @param nombre      Nombre de usuario proporcionado para el inicio de sesión.
 * @param contrasenia Contraseña proporcionada para el inicio de sesión.
 * @author Andrés López
 */
public record AuthRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(min = 8, max = 100) String contrasenia) {
}
