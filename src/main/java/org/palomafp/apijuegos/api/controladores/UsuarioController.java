package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de la clase usuario que interactua con la clase
 * usuarioService para insertar eliminar y revisar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    /**
     * Metodo que devuelve todos los usuarios en la base de datos
     * @return  Lista conteniendo todos los usuarios
     */
    @GetMapping
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
        return usuarioService.obtenerPorMiId(miId);
    }

    /**
     * Metodo que devuelve un usuario con el nombre indicado
     * @param nombre    Nombre del usuario que se busca
     * @return          Usuario con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public Usuario obtenerUsuarioPorNombre(@PathVariable String nombre) {
        return usuarioService.obtenerPorNombre(nombre);
    }

    /**
     * Metodo para eliminar un usuario de la base de datos
     * @param miId  Id del usuario que se quiere borrar
     */
    @DeleteMapping("/{miId}")
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
