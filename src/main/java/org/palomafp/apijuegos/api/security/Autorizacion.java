package org.palomafp.apijuegos.api.security;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.services.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Reglas de propiedad de los datos del usuario, expuestas como bean para poder
 * usarlas desde las expresiones de {@code @PreAuthorize}.
 *
 * <p>Spring resuelve las expresiones como {@code @autorizacion.metodo(...)} gracias
 * al nombre del bean declarado en {@code @Component("autorizacion")}.</p>
 *
 * <p>La regla general es: un usuario solo accede a sus propios datos, y un
 * administrador puede acceder a los de cualquiera.</p>
 *
 * @author Andrés López
 */
@Component("autorizacion")
public class Autorizacion {

    /** Prefijo que Spring usa internamente para los roles. */
    private static final String PREFIJO_ROL = "ROLE_";

    /** Rol con permisos plenos. */
    private static final String ROL_ADMINISTRADOR = "administrador";

    /** Autoridad completa equivalente a {@link #ROL_ADMINISTRADOR}. */
    private static final String AUTORIDAD_ADMINISTRADOR = PREFIJO_ROL + ROL_ADMINISTRADOR;

    private final UsuarioService usuarioService;

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param usuarioService Servicio para resolver el usuario autenticado.
     */
    public Autorizacion(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Indica si la petición en curso la ha hecho un administrador.
     *
     * @param auth Autenticación de Spring Security (puede ser nula).
     * @return true si el usuario autenticado es administrador.
     */
    public boolean esAdministrador(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(autoridad -> AUTORIDAD_ADMINISTRADOR.equals(autoridad.getAuthority()));
    }

    /**
     * Comprueba si el usuario autenticado puede operar sobre los datos del
     * usuario con el id interno indicado.
     *
     * @param miId Id interno del usuario dueño de los datos.
     * @param auth Autenticación de Spring Security.
     * @return true si es el propio usuario o un administrador.
     */
    public boolean puedeOperarSobreUsuario(int miId, Authentication auth) {
        if (esAdministrador(auth)) {
            return true;
        }
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Usuario autenticado = usuarioService.obtenerPorNombre(auth.getName());
        return autenticado != null && autenticado.getMiId() == miId;
    }

    /**
     * Comprueba si el usuario autenticado puede modificar la entrada de lista
     * indicada, es decir, si la entrada es suya.
     *
     * @param entrada Entrada de lista; si es nula se deniega el acceso.
     * @param auth    Autenticación de Spring Security.
     * @return true si la entrada le pertenece o si el usuario es administrador.
     */
    public boolean puedeEditarEntrada(EntradaLista entrada, Authentication auth) {
        if (entrada == null) {
            return false;
        }
        return puedeOperarSobreUsuario(entrada.getIdUsuario(), auth);
    }
}
