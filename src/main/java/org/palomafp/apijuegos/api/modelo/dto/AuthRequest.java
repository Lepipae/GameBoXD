package org.palomafp.apijuegos.api.modelo.dto;

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
public record AuthRequest(String nombre, String contrasenia) {
}
