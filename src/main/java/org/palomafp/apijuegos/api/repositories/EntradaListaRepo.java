package org.palomafp.apijuegos.api.repositories;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.dto.ConteoJuego;
import org.springframework.data.mongodb.repository.Aggregation;
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
     * Devuelve el videojuego con más entradas de lista, contando en MongoDB.
     *
     * <p>Antes el servicio traía todas las entradas ({@code findAll()}) y las
     * agrupaba en memoria; con la colección creciendo eso era O(n) documentos
     * por cada visita a la portada. La agregación hace el trabajo en el
     * servidor, ordena también por id ascendente para que el empate sea
     * estable y devuelve como mucho una fila.</p>
     *
     * @return Lista con un único conteo (vacía si no hay entradas).
     */
    @Aggregation(pipeline = {
            "{ $group: { _id: '$idVideojuego', total: { $sum: 1 } } }",
            "{ $sort: { total: -1, _id: 1 } }",
            "{ $limit: 1 }",
            "{ $project: { _id: 0, idVideojuego: '$_id', total: 1 } }"
    })
    List<ConteoJuego> juegoConMasEntradas();
    
    /**
     * Devuelve la ultima entrada insertada
     * @return EntradaLista con el ultimo id
     */
    default EntradaLista encontrarUltimoId() {
        return findTopByOrderByMiIdDesc();
    }
}
