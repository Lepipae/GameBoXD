package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de Videojuego
 * @author Andrés López
 */
@Service
public class VideojuegoService {

    private final VideojuegoRepo videojuegoRepo;
    private final EntradaListaService entradaListaService;

    /**
     * Inyección por constructor: ambas dependencias quedan {@code final}.
     *
     * @param videojuegoRepo    Repositorio de videojuegos.
     * @param entradaListaService Servicio de entradas de lista, para la cascada de borrado.
     */
    public VideojuegoService(VideojuegoRepo videojuegoRepo, EntradaListaService entradaListaService) {
        this.videojuegoRepo = videojuegoRepo;
        this.entradaListaService = entradaListaService;
    }

    /**
     * Metodo que devuelve todos los videojuegos de la base de datos
     * @return  Lista de todos los juegos
     */
    public List<Videojuego> obtenerTodos() {
        return videojuegoRepo.findAll();
    }

    /**
     * Metodo que obtiene un videojuego asociado a su id interno
     * @param idVideojuego  id interno
     * @return  Videojuego asociado al id
     */
    public Videojuego obtenerPorId(long idVideojuego) {
        return videojuegoRepo.findByMiId(idVideojuego);
    }

    /**
     * Obtiene un videojuego a partir de su nombre
     * @param nombreVideojuego Nombre del videojuego
     * @return Videojuego encontrado o null
     */
    public Videojuego obtenerPorNombre(String nombreVideojuego) {
        return videojuegoRepo.findByNombre(nombreVideojuego);
    }

    /**
     * Obtiene una lista de videojuegos a partir de un tag o categoria
     * @param tag Categoria a buscar
     * @return Lista de videojuegos con el tag
     */
    public List<Videojuego> obtenerPorTags(String tag) {
        return videojuegoRepo.findByTags(tag);
    }

    /**
     * Obtiene una lista de videojuegos creados por una desarrolladora
     * @param desarrolladora Id interno de la desarrolladora
     * @return Lista de videojuegos de la desarrolladora
     */
    public List<Videojuego> obtenerPorDesarrolladora(int desarrolladora) {
        return videojuegoRepo.findByIdDesarrolladora(desarrolladora);
    }

    /**
     * Guarda un videojuego en la base de datos
     * @param videojuego Objeto a guardar
     * @return Videojuego guardado
     * @throws RecursoDuplicadoException si el videojuego ya existe
     */
    public Videojuego guardar(Videojuego videojuego) {
        if (videojuego.getId() == null) {
            Videojuego ultimo = videojuegoRepo.encontrarUltimoId();
            videojuego.setMiId(ultimo != null ? ultimo.getMiId() + 1 : 1);
        }

        // Igual que en UsuarioService: al actualizar, el registro con el mismo
        // nombre solo es duplicado si es OTRO juego distinto del que se guarda.
        Videojuego previo = videojuegoRepo.findByNombre(videojuego.getNombre());
        boolean esOtro = previo != null
                && (videojuego.getId() == null || !previo.getId().equals(videojuego.getId()));
        if (esOtro) {
            throw new RecursoDuplicadoException("Ya existe un videojuego con ese nombre");
        }

        return videojuegoRepo.save(videojuego);
    }

    /**
     * Borra un videojuego a partir de su id interno y elimina sus entradas asociadas
     * @param id Id interno del videojuego
     */
    public void borrarVideojuego(long id) {
        entradaListaService.borrarPorVideojuego(id);
        videojuegoRepo.deleteByMiId(id);
    }
}
