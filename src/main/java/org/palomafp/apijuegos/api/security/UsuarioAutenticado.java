package org.palomafp.apijuegos.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Principal de Spring Security que lleva el id interno del usuario.
 *
 * <p>Antes, cada comprobación de propiedad ({@code @autorizacion...}) resolvía
 * el usuario por nombre contra MongoDB. Con este principal el dato ya viaja en
 * la autenticación, así que autorizar una petición deja de costar una consulta
 * a la base de datos.</p>
 *
 * @author Andrés López
 */
public class UsuarioAutenticado extends User {

    /** Id público del usuario (el que usa el frontend y las entradas de lista). */
    private final int miId;

    /**
     * Construye el principal autenticado.
     *
     * @param username    Nombre de usuario.
     * @param password    Contraseña (vacía cuando se reconstruye desde un JWT).
     * @param authorities Autoridades concedidas.
     * @param miId        Id interno del usuario.
     */
    public UsuarioAutenticado(String username, String password,
                              Collection<? extends GrantedAuthority> authorities, int miId) {
        super(username, password, authorities);
        this.miId = miId;
    }

    /**
     * Id interno del usuario autenticado.
     *
     * @return Id público del usuario.
     */
    public int getMiId() {
        return miId;
    }
}
