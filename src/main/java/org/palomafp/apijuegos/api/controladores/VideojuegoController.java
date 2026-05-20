package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.services.VideojuegoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de la clase videojuego que interactua con la clase
 * videojuegoService para insertar eliminar y revisar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/videojuegos")
public class VideojuegoController {
    @Autowired
    private VideojuegoService videojuegoService;

    /**
     * Metodo que devuelve todos los videojuegos en la base de datos
     * @return  Lista conteniendo todos los videojuegos
     */
    @GetMapping
    public List<Videojuego> obtenerTodos() {
        return videojuegoService.obtenerTodos();
    }

    /**
     * Metodo que devuelve un videojuego asociado a su id interno
     * @param miId  Id interno de la base de datos
     * @return      Videojuego asociado a ese id
     */
    @GetMapping("/{miId}")
    public Videojuego obtenerPorId(@PathVariable long miId) {
        return videojuegoService.obtenerPorId(miId);
    }

    /**
     * Metodo que devuelve un videojuego con el nombre indicado
     * @param nombre    Nombre del videojuego que se busca
     * @return          Videojuego con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public Videojuego obtenerPorNombre(@PathVariable String nombre) {
        return videojuegoService.obtenerPorNombre(nombre);
    }

    /**
     * Metodo que devuelve una lista de videojuegos por sus tags
     * @param tag   Tag o categoria a buscar
     * @return      Lista de videojuegos con ese tag
     */
    @GetMapping("/tag/{tag}")
    public List<Videojuego> obtenerPorTags(@PathVariable String tag) {
        return videojuegoService.obtenerPorTags(tag);
    }

    /**
     * Metodo que devuelve los videojuegos de una desarrolladora especifica
     * @param desarrollador Id interno de la desarrolladora
     * @return              Lista de videojuegos creados por esa desarrolladora
     */
    @GetMapping("/desarrolladora/{desarrollador}")
    public List<Videojuego> obtenerPorDesarrolladora(@PathVariable int desarrollador) {
        return videojuegoService.obtenerPorDesarrolladora(desarrollador);
    }

    /**
     * Metodo para guardar un videojuego en la base de datos
     * @param videojuego    Objeto videojuego a guardar
     * @return              Objeto guardado
     */
    @PostMapping
    public Videojuego guardar(@RequestBody Videojuego videojuego) {
        return videojuegoService.guardar(videojuego);
    }

    /**
     * Metodo para eliminar un videojuego de la base de datos
     * @param miId  Id del videojuego que se quiere borrar
     */
    @DeleteMapping("/{miId}")
    public void borrarVideojuego(@PathVariable long miId) {
        videojuegoService.borrarVideojuego(miId);
    }

}
