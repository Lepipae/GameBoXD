package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.dto.UsuarioPublicoDTO;
import org.palomafp.apijuegos.api.services.UsuarioService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    public UsuarioPublicoDTO obtenerUsuarioPorId(@PathVariable int miId) {
        var usuario = usuarioService.obtenerPorMiId(miId);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("No existe ningun usuario con el id " + miId);
        }
        return new UsuarioPublicoDTO(usuario.getMiId(), usuario.getNombre(), usuario.getUrlImagen());
    }

    /**
     * Metodo que devuelve un usuario con el nombre indicado
     * @param nombre    Nombre del usuario que se busca
     * @return          Usuario con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public UsuarioPublicoDTO obtenerUsuarioPorNombre(@PathVariable String nombre) {
        var usuario = usuarioService.obtenerPorNombre(nombre);
        if (usuario == null) {
            throw new RecursoNoEncontradoException("No existe ningun usuario llamado " + nombre);
        }
        return new UsuarioPublicoDTO(usuario.getMiId(), usuario.getNombre(), usuario.getUrlImagen());
    }

    /**
     * Resuelve varios usuarios en una sola llamada ({@code ?ids=1,2,3}).
     *
     * <p>Lo usa la ficha de un juego para pintar el autor de cada reseña sin
     * lanzar una petición por usuario distinto.</p>
     *
     * @param ids Ids internos separados por comas.
     * @return Datos públicos de los usuarios encontrados.
     */
    @GetMapping("/lote")
    public ResponseEntity<List<UsuarioPublicoDTO>> lote(
            @RequestParam(name = "ids", required = false) String ids) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(usuarioService.obtenerPublicosPorIds(parsearIds(ids)));
    }

    /**
     * Convierte la lista de ids de la query string en enteros, ignorando los
     * valores no numéricos en lugar de devolver un 400.
     *
     * @param ids Texto {@code "1,2,3"} (puede ser {@code null}).
     * @return Lista de ids válidos.
     */
    private static List<Integer> parsearIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .map(valor -> {
                    try {
                        return Integer.parseInt(valor);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
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
    public Usuario guardar(@Valid @RequestBody Usuario usuario) {
        return usuarioService.guardar(usuario);
    }

}
