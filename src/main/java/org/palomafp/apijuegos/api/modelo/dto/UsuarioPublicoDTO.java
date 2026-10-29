package org.palomafp.apijuegos.api.modelo.dto;

/**
 * DTO público de un usuario: solo expone los datos no sensibles necesarios para
 * mostrar perfiles, reseñas y listas (id interno, nombre y avatar).
 *
 * <p>Se devuelve en los endpoints públicos {@code GET /api/usuarios/{miId}} y
 * {@code GET /api/usuarios/nombre/{nombre}} para no filtrar credenciales ni rol.</p>
 *
 * @param miId      Id interno del usuario.
 * @param nombre    Nombre del usuario.
 * @param urlImagen URL de la imagen de perfil (puede ser null).
 * @author Andrés López
 */
public record UsuarioPublicoDTO(int miId, String nombre, String urlImagen) {
}