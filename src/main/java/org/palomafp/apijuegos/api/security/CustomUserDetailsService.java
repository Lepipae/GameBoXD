package org.palomafp.apijuegos.api.security;

import org.jspecify.annotations.NullMarked;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Servicio encargado de proveer a Spring Security la información del usuario
 * cargándola desde nuestra propia base de datos de MongoDB.
 * Implementa la interfaz UserDetailsService requerida por el entorno de Spring.
 * @author Andrés López
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /** Prefijo que Spring Security espera en las autoridades de tipo {@code ROLE_*}. */
    private static final String PREFIJO_ROL = "ROLE_";

    private final UsuarioRepo usuarioRepo;

    /**
     * Inyección por constructor: la dependencia es {@code final} y la clase no
     * puede existir con un repositorio nulo.
     *
     * @param usuarioRepo Repositorio de usuarios.
     */
    public CustomUserDetailsService(UsuarioRepo usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    /**
     * Llamado automáticamente por Spring Security durante el proceso
     * de autenticación para comprobar si el usuario existe.
     * @param username Nombre del usuario ingresado.
     * @return Objeto UserDetails que usa Spring internamente.
     * @throws UsernameNotFoundException Si el usuario no existe en la base de datos.
     */
    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscamos el usuario en nuestro repositorio usando su nombre
        Usuario usuario = usuarioRepo.findByNombre(username);

        if (usuario == null) {
            // Lanzamos error si no lo encontramos
            throw new UsernameNotFoundException("Usuario no encontrado con el nombre: " + username);
        }

        // Traducimos el rol del usuario a una autoridad de Spring Security con el prefijo "ROLE_",
        // que es el que espera hasRole('administrador') en los @PreAuthorize de los controladores.
        // Sin este paso el token se validaria pero no autorizaria nada.
        // Un usuario sin rol guardado cae a 'usuarioNormal' en vez de quedarse sin permisos.
        Rol rol = Objects.requireNonNullElse(usuario.getRol(), Rol.usuarioNormal);
        var autoridad = new SimpleGrantedAuthority(PREFIJO_ROL + rol.name());

        // Devolvemos una instancia de "User" de Spring Security con nombre, contraseña y su rol.
        return new User(usuario.getNombre(), usuario.getContrasenia(), List.of(autoridad));
    }
}
