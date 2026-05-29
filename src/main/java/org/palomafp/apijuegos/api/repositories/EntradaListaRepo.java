package org.palomafp.apijuegos.api.repositories;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio para la clase EntradaLista
 * @author Andrés López
 */
public interface EntradaListaRepo extends MongoRepository<EntradaLista, String> {
    /**
     * Busca las entradas de la lista pertenecientes a un usuario
     * @param id Id del usuario
     * @return Lista de entradas del usuario
     */
    List<EntradaLista> findByIdUsuario(int id);

    /**
     * Busca una entrada de lista para un usuario y videojuego específicos
     * @param idUsuario Id del usuario
     * @param idVideojuego Id del videojuego
     * @return EntradaLista encontrada o null
     */
    EntradaLista findByIdUsuarioAndIdVideojuego(int idUsuario, long idVideojuego);

    /**
     * Busca una entrada de la lista por su id interno
     * @param id Id interno
     * @return EntradaLista encontrada o null
     */
    EntradaLista findByMiId(long id);

    /**
     * Elimina una entrada por su id interno
     * @param id Id interno
     */
    void deleteByMiId(long id);

    /**
     * Elimina las entradas asociadas a un videojuego
     * @param idVideojuego Id del videojuego
     */
    void deleteByIdVideojuego(long idVideojuego);

    /**
     * Elimina las entradas asociadas a un usuario
     * @param idUsuario Id del usuario
     */
    void deleteByIdUsuario(int idUsuario);

    /**
     * Busca las entradas de la lista pertenecientes a un videojuego
     * @param idVideojuego Id del videojuego
     * @return Lista de entradas del videojuego
     */
    List<EntradaLista> findByIdVideojuego(long idVideojuego);


    /**
     * Encuentra la entrada con el id interno mas alto
     * @return EntradaLista con el id mas alto
     */
    EntradaLista findTopByOrderByMiIdDesc();
    
    /**
     * Devuelve la ultima entrada insertada
     * @return EntradaLista con el ultimo id
     */
    default EntradaLista encontrarUltimoId() {
        return findTopByOrderByMiIdDesc();
    }
}
