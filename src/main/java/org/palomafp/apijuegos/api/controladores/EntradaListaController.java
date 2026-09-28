package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.security.Autorizacion;
import org.palomafp.apijuegos.api.services.EntradaListaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private Autorizacion autorizacion;

    /**
     * Metodo que devuelve todas las entradas asociadas a un usuario
     *
     * <p>Solo el propio usuario puede ver su lista; un administrador puede ver cualquiera.
     * Esta comprobacion se hace con {@code @PreAuthorize} porque la decisión depende solo
     * del id de la ruta.</p>
     *
     * @param id    Id del usuario del que queremos las entradas de lista
     * @return      Lista que contiene todas las entradas de un usuario
     */
    @GetMapping("/{miId}")
    @PreAuthorize("@autorizacion.puedeOperarSobreUsuario(#id, authentication)")
    public List<EntradaLista> findByIdUsuario(@PathVariable("miId") int id) {
        return entradaListaService.findByIdUsuario(id);
    }

    /**
     * Metodo que devuelve todas las entradas asociadas a un videojuego
     * @param idJuego Id del videojuego del que queremos las entradas
     * @return      Lista que contiene todas las entradas de ese videojuego
     */
    @GetMapping("/juego/{idJuego}")
    public List<EntradaLista> findByIdVideojuego(@PathVariable("idJuego") long idJuego) {
        return entradaListaService.findByIdVideojuego(idJuego);
    }

    /**
     * Metodo que devuelve una entradaLista basado en su id interno
     *
     * <p>La pertenencia de la entrada solo se puede comprobar tras leerla de la base de
     * datos, por eso aquí se resuelve de forma explicita en lugar de en la anotación.</p>
     *
     * @param id    Id de la entrada
     * @param auth  Usuario autenticado que hace la petición
     * @return      Entrada asociada a ese id
     */
    @GetMapping("/id/{id}")
    public EntradaLista findById(@PathVariable("id") long id, Authentication auth) {
        EntradaLista entrada = entradaListaService.findById(id);
        if (!autorizacion.puedeEditarEntrada(entrada, auth)) {
            throw new AccessDeniedException("Esa entrada no pertenece a tu lista");
        }
        return entrada;
    }

    /**
     * Metodo que elimina una entrada de la lista basado en el id interno
     *
     * <p>Solo se puede borrar una entrada propia. Antes se comprobaba el id interno
     * sin más, por lo que cualquier usuario autenticado podia vaciar la lista ajena.</p>
     *
     * @param id    Id de la entrada
     * @param auth  Usuario autenticado que hace la petición
     */
    @DeleteMapping("/{miId}")
    public void borrarEntrada(@PathVariable("miId") int id, Authentication auth) {
        EntradaLista entrada = entradaListaService.findById(id);
        if (!autorizacion.puedeEditarEntrada(entrada, auth)) {
            throw new AccessDeniedException("Esa entrada no pertenece a tu lista");
        }
        entradaListaService.borrarEntrada(id);
    }

    /**
     * Metodo para guardar una nueva entrada en la lista
     *
     * <p>El id de usuario del cuerpo debe ser el del propio usuario, para que nadie
     * pueda escribir reseñas en la lista de otro. Se valida con {@code @PreAuthorize}
     * porque la decisión depende solo del cuerpo de la petición.</p>
     *
     * @param entradaLista  Nuevo objeto entradaLista
     * @return              Objeto guardado
     */
    @PostMapping
    @PreAuthorize("@autorizacion.puedeOperarSobreUsuario(#entradaLista.idUsuario, authentication)")
    public EntradaLista guardar(@RequestBody EntradaLista entradaLista) {
        return entradaListaService.guardar(entradaLista);
    }


}
