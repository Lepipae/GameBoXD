package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.services.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de la clase usuario que interactua con la clase
 * usuarioService para insertar eliminar y revisar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param usuarioService Servicio de usuarios.
     */
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Metodo que devuelve todos los usuarios en la base de datos
     * @return  Lista conteniendo todos los usuarios
     */
    @GetMapping
    @PreAuthorize("hasRole('administrador')")
    public List<Usuario> obtenerTodos() {
        return usuarioService.obtenerTodos();
    }

    /**
     * Metodo que devuelve un usuario asociado a su id interno
     * @param miId  Id interno de la base de datos
     * @return      Usuario asociado a ese id
     */
    @GetMapping("/{miId}")
    public Usuario obtenerUsuarioPorId(@PathVariable int miId) {
        var usuario = usuarioService.obtenerPorMiId(miId);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("No existe ningun usuario con el id " + miId);
        }
        return usuario;
    }

    /**
     * Metodo que devuelve un usuario con el nombre indicado
     * @param nombre    Nombre del usuario que se busca
     * @return          Usuario con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public Usuario obtenerUsuarioPorNombre(@PathVariable String nombre) {
        var usuario = usuarioService.obtenerPorNombre(nombre);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("No existe ningun usuario llamado " + nombre);
        }
        return usuario;
    }

    /**
     * Metodo para eliminar un usuario de la base de datos
     * @param miId  Id interno del usuario que se quiere borrar
     */
    @DeleteMapping("/{miId}")
    @PreAuthorize("hasRole('administrador')")
    public void borrarUsuarioPorMiId(@PathVariable int miId) {
        usuarioService.borrarPorMiId(miId);
    }

    /**
     * Metodo para guardar un usuario en la base de datos
     * @param usuario   Objeto usuario a guardar
     * @return          Objeto guardado
     */
    @PostMapping
    public Usuario guardar(@RequestBody Usuario usuario) {
        return usuarioService.guardar(usuario);
    }

}
