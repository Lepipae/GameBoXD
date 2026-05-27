package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.repositories.EntradaListaRepo;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de EntradaLista
 * 
 * @author Andrés López
 */
@Service
public class EntradaListaService {

    @Autowired
    private EntradaListaRepo entradaListaRepo; // Repo de Entradalista

    @Autowired
    private VideojuegoRepo videojuegoRepo; // Repo de Videojuego

    /**
     * Actualiza la nota media de un videojuego basado en sus entradas de lista
     * 
     * @param idVideojuego Id del videojuego
     */
    private void actualizarNotaMedia(long idVideojuego) {
        List<EntradaLista> entradas = entradaListaRepo.findByIdVideojuego(idVideojuego);
        double notaMedia = 0.0;

        if (entradas != null && !entradas.isEmpty()) {
            double suma = 0.0;
            for (EntradaLista entrada : entradas) {
                suma += entrada.getNota();
            }
            notaMedia = suma / entradas.size();
            notaMedia = Math.round(notaMedia * 100.0) / 100.0;
        }

        Videojuego videojuego = videojuegoRepo.findByMiId(idVideojuego);
        if (videojuego != null) {
            videojuego.setNotaMedia(notaMedia);
            videojuegoRepo.save(videojuego);
        }
    }

    /**
     * Obtiene las entradas de la lista pertenecientes a un usuario
     * 
     * @param id Id del usuario
     * @return Lista de entradas del usuario
     */
    public List<EntradaLista> findByIdUsuario(int id) {
        return entradaListaRepo.findByIdUsuario(id);
    }

    /**
     * Obtiene las entradas de la lista pertenecientes a un videojuego
     * @param idVideojuego Id del videojuego
     * @return Lista de entradas del videojuego
     */
    public List<EntradaLista> findByIdVideojuego(long idVideojuego) {
        return entradaListaRepo.findByIdVideojuego(idVideojuego);
    }

    /**
     * Obtiene una entrada de la lista a partir de su id interno
     * 
     * @param id Id interno
     * @return EntradaLista encontrada o null
     */
    public EntradaLista findById(long id) {
        return entradaListaRepo.findByMiId(id);
    }

    /**
     * Borra una entrada a partir de su id interno
     * 
     * @param id Id interno
     */
    public void borrarEntrada(int id) {
        EntradaLista entrada = entradaListaRepo.findByMiId(id);
        if (entrada != null) {
            long idVideojuego = entrada.getIdVideojuego();
            entradaListaRepo.deleteByMiId(id);
            actualizarNotaMedia(idVideojuego);
        }
    }

    /**
     * Borra las entradas asociadas a un videojuego
     * 
     * @param idVideojuego Id del videojuego
     */
    public void borrarPorVideojuego(long idVideojuego) {
        entradaListaRepo.deleteByIdVideojuego(idVideojuego);
    }

    /**
     * Borra las entradas asociadas a un usuario
     * 
     * @param idUsuario Id del usuario
     */
    public void borrarPorUsuario(int idUsuario) {
        List<EntradaLista> entradas = entradaListaRepo.findByIdUsuario(idUsuario);
        entradaListaRepo.deleteByIdUsuario(idUsuario);
        if (entradas != null) {
            entradas.stream()
                    .map(EntradaLista::getIdVideojuego)
                    .distinct()
                    .forEach(this::actualizarNotaMedia);
        }
    }

    /**
     * Guarda una entrada en la base de datos
     * 
     * @param entradaLista Objeto a guardar
     * @return EntradaLista guardada
     */
    public EntradaLista guardar(EntradaLista entradaLista) {
        if (entradaLista.getId() == null) {
            EntradaLista ultimo = entradaListaRepo.encontrarUltimoId();
            long nuevoId = (ultimo != null) ? ultimo.getMiId() + 1 : 1;
            entradaLista.setMiId(nuevoId);
        }
        EntradaLista guardada = entradaListaRepo.save(entradaLista);
        actualizarNotaMedia(guardada.getIdVideojuego());
        return guardada;
    }
}
