package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de Usuario
 * @author Andrés López
 */
@Service
public class UsuarioService {

    /** Prefijo con el que BCrypt marca sus hashes; sirve para no cifrar dos veces. */
    private static final String PREFIJO_BCRYPT = "$2a$";

    private final UsuarioRepo usuarioRepo;
    // Inyectamos el PasswordEncoder (definido en SecurityConfig) para poder encriptar contraseñas
    private final PasswordEncoder passwordEncoder;
    private final EntradaListaService entradaListaService;

    /**
     * Inyección por constructor: las tres dependencias quedan {@code final}.
     *
     * @param usuarioRepo       Repositorio de usuarios.
     * @param passwordEncoder   Codificador de contraseñas (BCrypt).
     * @param entradaListaService Servicio de entradas de lista, para la cascada de borrado.
     */
    public UsuarioService(UsuarioRepo usuarioRepo,
                          PasswordEncoder passwordEncoder,
                          EntradaListaService entradaListaService) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.entradaListaService = entradaListaService;
    }

    /**
     * Obtiene todos los usuarios almacenados en la base de datos
     * @return Lista de todos los usuarios
     */
    public List<Usuario> obtenerTodos() {
        return usuarioRepo.findAll();
    }

    /**
     * Obtiene un usuario a partir de su id interno
     * @param id Id interno del usuario
     * @return Usuario encontrado o null
     */
    public Usuario obtenerPorMiId(int id) {
        return usuarioRepo.findByMiId(id);
    }

    /**
     * Obtiene un usuario a partir de su id de MongoDB
     * @param id Id de MongoDB del usuario
     * @return Usuario encontrado o null
     */
    public Usuario obtenerPorId(String id) {
        return usuarioRepo.findById(id).orElse(null);
    }

    /**
     * Obtiene un usuario a partir de su nombre
     * @param nombre Nombre del usuario a buscar
     * @return Usuario encontrado o null
     */
    public Usuario obtenerPorNombre(String nombre) {
        return usuarioRepo.findByNombre(nombre);
    }

    /**
     * Guarda un usuario en la base de datos verificando que el nombre no exista
     * @param usuario Usuario a guardar
     * @return Usuario guardado
     * @throws RecursoDuplicadoException si el usuario ya existe
     */
    public Usuario guardar(Usuario usuario) {
        // Encriptamos la contraseña del usuario antes de proceder a guardarlo.
        // Verificamos que exista y que no esté ya encriptada (BCrypt suele empezar por "$2a$")
        if (usuario.getContrasenia() != null && !usuario.getContrasenia().startsWith(PREFIJO_BCRYPT)) {
            usuario.setContrasenia(passwordEncoder.encode(usuario.getContrasenia()));
        }

        if (usuario.getId() == null) {
            Usuario ultimo = usuarioRepo.encontrarUltimoId();
            usuario.setMiId(ultimo != null ? ultimo.getMiId() + 1 : 1);
        }

        // Antes esto estaba repartido en dos ramas (nuevo / actualización) y repetía
        // la búsqueda por nombre. La diferencia real entre ambas es que al actualizar
        // no se considera duplicado el propio usuario: por eso la condición mira si
        // el registro encontrado es el mismo que se está guardando.
        Usuario previo = usuarioRepo.findByNombre(usuario.getNombre());
        boolean esOtro = previo != null
                && (usuario.getId() == null || !previo.getId().equals(usuario.getId()));
        if (esOtro) {
            throw new RecursoDuplicadoException("Ya existe un usuario con ese nombre");
        }

        return usuarioRepo.save(usuario);
    }

    /**
     * Borra un usuario a partir de su id interno
     * @param id Id interno del usuario
     */
    public void borrarPorMiId(int id) {
        entradaListaService.borrarPorUsuario(id);
        usuarioRepo.deleteByMiId(id);
    }
}
