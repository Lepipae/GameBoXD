package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.repositories.EntradaListaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de EntradaLista
 * @author Andrés López
 */
@Service
public class EntradaListaService {

    @Autowired
    private EntradaListaRepo entradaListaRepo; //Repo de Entradalista

    /**
     * Obtiene las entradas de la lista pertenecientes a un usuario
     * @param id Id del usuario
     * @return Lista de entradas del usuario
     */
    public List<EntradaLista> findByIdUsuario(int id) {
        return entradaListaRepo.findByIdUsuario(id);
    }

    /**
     * Obtiene una entrada de la lista a partir de su id interno
     * @param id Id interno
     * @return EntradaLista encontrada o null
     */
    public EntradaLista findById(long id) {
        return entradaListaRepo.findByMiId(id);
    }

    /**
     * Borra una entrada a partir de su id interno
     * @param id Id interno
     */
    public void borrarEntrada(int id) {
        entradaListaRepo.deleteByMiId(id);
    }

    /**
     * Borra las entradas asociadas a un videojuego
     * @param idVideojuego Id del videojuego
     */
    public void borrarPorVideojuego(long idVideojuego) {
        entradaListaRepo.deleteByIdVideojuego(idVideojuego);
    }

    /**
     * Borra las entradas asociadas a un usuario
     * @param idUsuario Id del usuario
     */
    public void borrarPorUsuario(int idUsuario) {
        entradaListaRepo.deleteByIdUsuario(idUsuario);
    }

    /**
     * Guarda una entrada en la base de datos
     * @param entradaLista Objeto a guardar
     * @return EntradaLista guardada
     */
    public EntradaLista guardar(EntradaLista entradaLista) {
        if (entradaLista.getId() == null) {
            EntradaLista ultimo = entradaListaRepo.encontrarUltimoId();
            long nuevoId = (ultimo != null) ? ultimo.getMiId() + 1 : 1;
            entradaLista.setMiId(nuevoId);
        }
        return entradaListaRepo.save(entradaLista);
    }
}
