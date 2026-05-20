package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.services.EntradaListaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de la clase EntradaLista que interactua con la clase entradaListaService para
 * guardar eliminar y consultar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/lista")
public class EntradaListaController {
    @Autowired
    private EntradaListaService entradaListaService;    // Servicio de la clase entradalista

    /**
     * Metodo que devuelve todas las entradas asociadas a un usuario
     * @param id    Id del usuario del que queremos las entradas de lista
     * @return      Lista que contiene todas las entradas de un usuario
     */
    @GetMapping("/{miId}")
    public List<EntradaLista> findByIdUsuario(@PathVariable("miId") int id) {
        return entradaListaService.findByIdUsuario(id);
    }

    /**
     * Metodo que devuelve una entradaLista basado en su id interno
     * @param id    Id de la entrada
     * @return      Entrada asociada a ese id
     */
    @GetMapping("/id/{id}")
    public EntradaLista findById(@PathVariable("id") long id) {
        return entradaListaService.findById(id);
    }

    /**
     * Metodo que elimina una entrada de la lista basado en el id interno
     * @param id    Id de la entrada
     */
    @DeleteMapping("/{miId}")
    public void borrarEntrada(@PathVariable("miId") int id) {
        entradaListaService.borrarEntrada(id);
    }

    /**
     * Metodo para guardar una nueva entrada en la lista
     * @param entradaLista  Nuevo objeto entradaLista
     * @return              Objeto guardado
     */
    @PostMapping
    public EntradaLista guardar(@RequestBody EntradaLista entradaLista) {
        return entradaListaService.guardar(entradaLista);
    }


}
