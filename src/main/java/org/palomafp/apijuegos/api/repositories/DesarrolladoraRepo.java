package org.palomafp.apijuegos.api.repositories;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio para la clase Desarrolladora
 * @author Andrés López
 */
public interface DesarrolladoraRepo extends MongoRepository<Desarrolladora, String> {
    /**
     * Busca una desarrolladora por su id interno
     * @param miId Id interno
     * @return Desarrolladora encontrada o null
     */
    Desarrolladora findByMiId(int miId);

    /**
     * Resuelve varias desarrolladoras en una sola consulta.
     *
     * <p>Lo usa "Mi lista" para pintar el estudio de cada tarjeta sin una
     * petición por entrada.</p>
     *
     * @param ids Ids internos a buscar
     * @return Desarrolladoras encontradas (las que no existan no aparecen)
     */
    List<Desarrolladora> findByMiIdIn(Collection<Integer> ids);

    /**
     * Busca una desarrolladora por su nombre
     * @param nombre Nombre de la desarrolladora
     * @return Desarrolladora encontrada o null
     */
    Desarrolladora findByNombre(String nombre);

    /**
     * Elimina una desarrolladora por su id interno
     * @param miId Id interno
     */
    void deleteByMiId(int miId);
    
    /**
     * Encuentra la desarrolladora con el id interno mas alto
     * @return Desarrolladora con el id mas alto
     */
    Desarrolladora findTopByOrderByMiIdDesc();
    
    /**
     * Devuelve el ultimo id utilizado
     * @return Desarrolladora con el ultimo id
     */
    default Desarrolladora encontrarUltimoId() {
        return findTopByOrderByMiIdDesc();
    }
}
