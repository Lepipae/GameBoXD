package org.palomafp.apijuegos.api.repositories;

import org.palomafp.apijuegos.api.modelo.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio para la clase Usuario
 * @author Andrés López
 */
public interface UsuarioRepo extends MongoRepository<Usuario, String> {
    /**
     * Busca un usuario por su id interno
     * @param miId Id interno
     * @return Usuario encontrado o null
     */
    Usuario findByMiId(int miId);

    /**
     * Resuelve varios usuarios en una sola consulta.
     *
     * <p>Lo usa la ficha de un juego: las reseñas muestran el nombre del autor,
     * y resolverlas una a una costaba una petición por usuario distinto.</p>
     *
     * @param ids Ids internos a buscar
     * @return Usuarios encontrados (los que no existan no aparecen)
     */
    List<Usuario> findByMiIdIn(Collection<Integer> ids);

    /**
     * Busca un usuario por su nombre
     * @param nombre Nombre del usuario
     * @return Usuario encontrado o null
     */
    Usuario findByNombre(String nombre);

    /**
     * Elimina un usuario por su id interno
     * @param miId Id interno
     */
    void deleteByMiId(int miId);

    /**
     * Encuentra el usuario con el id interno mas alto
     * @return Usuario con el id mas alto
     */
    Usuario findTopByOrderByMiIdDesc();
    
    /**
     * Devuelve el ultimo id utilizado
     * @return Usuario con el ultimo id
     */
    default Usuario encontrarUltimoId() {
        return findTopByOrderByMiIdDesc();
    }
}
