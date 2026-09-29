package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de Usuario
 * @author Andrés López
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepo usuarioRepo;

    // Inyectamos el PasswordEncoder (definido en SecurityConfig) para poder encriptar contraseñas
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntradaListaService entradaListaService;

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
     * @throws IllegalArgumentException si el usuario ya existe
     */
    public Usuario guardar(Usuario usuario) {
        // Encriptamos la contraseña del usuario antes de proceder a guardarlo.
        // Verificamos que exista y que no esté ya encriptada (BCrypt suele empezar por "$2a$")
        if (usuario.getContrasenia() != null && !usuario.getContrasenia().startsWith("$2a$")) {
            usuario.setContrasenia(passwordEncoder.encode(usuario.getContrasenia()));
        }

        if (usuario.getId() == null) {
            Usuario ultimo = usuarioRepo.encontrarUltimoId();
            int nuevoId = (ultimo != null) ? ultimo.getMiId() + 1 : 1;
            usuario.setMiId(nuevoId);

            Usuario viejo1 = usuarioRepo.findByNombre(usuario.getNombre());
            if (viejo1 != null) {
                throw new RecursoDuplicadoException("Ya existe un usuario con ese nombre");
            }
        } else {
            Usuario viejo1 = usuarioRepo.findByNombre(usuario.getNombre());
            if (viejo1 != null && !viejo1.getId().equals(usuario.getId())) {
                throw new RecursoDuplicadoException("Ya existe un usuario con ese nombre");
            }
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
