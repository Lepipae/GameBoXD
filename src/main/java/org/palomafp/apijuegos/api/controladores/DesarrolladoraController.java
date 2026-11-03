package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.services.DesarrolladoraService;
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
 * Controlador de la clase desarrolladora que interactua con la clase
 * desarrolladoraService para insertar eliminar y revisar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/desarrolladoras")
public class DesarrolladoraController {

    private final DesarrolladoraService desarrolladoraService; // El servicio de desarrolladora

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param desarrolladoraService Servicio de desarrolladoras.
     */
    public DesarrolladoraController(DesarrolladoraService desarrolladoraService) {
        this.desarrolladoraService = desarrolladoraService;
    }

    /**
     * Metodo que devuelve todas las desarrolladoras en la base de datos
     * @return  Lista conteniendo todas las desarrolladoras
     */
    @GetMapping
    public ResponseEntity<List<Desarrolladora>> obtenerDesarrolladoras() {
        // El listado completo (16,8 KB) lo pide el desplegable de "insertar";
        // al ser un catálogo casi estático se sirve con caché pública.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(desarrolladoraService.obtenerDesarrolladoras());
    }

    /**
     * Resuelve varias desarrolladoras en una sola llamada ({@code ?ids=1,2,3}).
     *
     * @param ids Ids internos separados por comas.
     * @return Desarrolladoras encontradas.
     */
    @GetMapping("/lote")
    public ResponseEntity<List<Desarrolladora>> lote(
            @RequestParam(name = "ids", required = false) String ids) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(desarrolladoraService.obtenerPorIds(parsearIds(ids)));
    }

    /**
     * Convierte la lista de ids de la query string en enteros, ignorando los
     * valores no numéricos.
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
     * Metodo que devuelve una desarrolladora con el nombre indicado
     * @param nombre    Nombre de la desarrolladora que se busca
     * @return          Desarrolladora con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public Desarrolladora obtenerPorNombre(@PathVariable String nombre) {
        var desarrolladora = desarrolladoraService.obtenerPorNombre(nombre);
        if (desarrolladora == null) {
            throw new RecursoNoEncontradoException("No existe ninguna desarrolladora llamada " + nombre);
        }
        return desarrolladora;
    }

    /**
     * Metodo que devuelve una desarrolladora asociado a su id interno
     * @param id    Id interno de la base de datos
     * @return      Desarrolladora asociada a ese id
     */
    @GetMapping("/id/{id}")
    public Desarrolladora obtenerPorId(@PathVariable int id) {
        var desarrolladora = desarrolladoraService.obtenerPorId(id);
        if (desarrolladora == null) {
            throw new RecursoNoEncontradoException("No existe ninguna desarrolladora con el id " + id);
        }
        return desarrolladora;
    }

    /**
     * Metodo para guardar una desarrolladora en la base de datos
     * @param desarrolladora    Objeto desarrolladora a guardar
     * @return                  Objeto guardado
     */
    @PostMapping
    public Desarrolladora guardar(@Valid @RequestBody Desarrolladora desarrolladora) {
        return desarrolladoraService.guardar(desarrolladora);
    }

    /**
     * Metodo para eliminar una desarrolladora de la base de datos
     * @param id    Id de la desarrolladora que se quiere borrar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('administrador')")
    public void borrarDesarrolladora(@PathVariable int id) {
        desarrolladoraService.borrarDesarrolladora(id);
    }
}
