package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de Videojuego
 * @author Andrés López
 */
@Service
public class VideojuegoService {

    @Autowired
    private VideojuegoRepo videojuegoRepo;

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
     * @throws IllegalArgumentException si el videojuego ya existe
     */
    public Videojuego guardar(Videojuego videojuego) {
        if (videojuego.getId() == null) {
            Videojuego ultimo = videojuegoRepo.encontrarUltimoId();
            long nuevoId = (ultimo != null) ? ultimo.getMiId() + 1 : 1;
            videojuego.setMiId(nuevoId);

            Videojuego videojuegoViejo = videojuegoRepo.findByNombre(videojuego.getNombre());
            if (videojuegoViejo != null) {
                throw new RecursoDuplicadoException("Ya existe un videojuego con ese nombre");
            }
        } else {
            Videojuego videojuegoViejo = videojuegoRepo.findByNombre(videojuego.getNombre());
            if (videojuegoViejo != null && !videojuegoViejo.getId().equals(videojuego.getId())) {
                throw new RecursoDuplicadoException("Ya existe un videojuego con ese nombre");
            }
        }
        return videojuegoRepo.save(videojuego);
    }

    @Autowired
    private EntradaListaService entradaListaService;

    /**
     * Borra un videojuego a partir de su id interno y elimina sus entradas asociadas
     * @param id Id interno del videojuego
     */
    public void borrarVideojuego(long id) {
        entradaListaService.borrarPorVideojuego(id);
        videojuegoRepo.deleteByMiId(id);
    }
}
