package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.services.DesarrolladoraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de la clase desarrolladora que interactua con la clase
 * desarrolladoraService para insertar eliminar y revisar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/desarrolladoras")
public class DesarrolladoraController {

    @Autowired
    private DesarrolladoraService desarrolladoraService;    // El servicio de desarrolladora

    /**
     * Metodo que devuelve todas las desarrolladoras en la base de datos
     * @return  Lista conteniendo todas las desarrolladoras
     */
    @GetMapping
    public List<Desarrolladora> obtenerDesarrolladoras() {
        return desarrolladoraService.obtenerDesarrolladoras();
    }

    /**
     * Metodo que devuelve una desarrolladora con el nombre indicado
     * @param nombre    Nombre de la desarrolladora que se busca
     * @return          Desarrolladora con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public Desarrolladora obtenerPorNombre(@PathVariable String nombre) {
        return desarrolladoraService.obtenerPorNombre(nombre);
    }

    /**
     * Metodo que devuelve una desarrolladora asociado a su id interno
     * @param id    Id interno de la base de datos
     * @return      Desarrolladora asociada a ese id
     */
    @GetMapping("/id/{id}")
    public Desarrolladora obtenerPorId(@PathVariable int id) {
        return desarrolladoraService.obtenerPorId(id);
    }

    /**
     * Metodo para guardar una desarrolladora en la base de datos
     * @param desarrolladora    Objeto desarrolladora a guardar
     * @return                  Objeto guardado
     */
    @PostMapping
    public Desarrolladora guardar(@RequestBody Desarrolladora desarrolladora) {
        return desarrolladoraService.guardar(desarrolladora);
    }

    /**
     * Metodo para eliminar una desarrolladora de la base de datos
     * @param id    Id de la desarrolladora que se quiere borrar
     */
    @DeleteMapping("/{id}")
    public void borrarDesarrolladora(@PathVariable int id) {
        desarrolladoraService.borrarDesarrolladora(id);
    }
}
