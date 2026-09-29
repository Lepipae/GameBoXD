package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.repositories.DesarrolladoraRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que gestiona la logica de negocio de Desarrolladora
 * @author Andrés López
 */
@Service
public class DesarrolladoraService {

    private final DesarrolladoraRepo desarrolladoraRepo;

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param desarrolladoraRepo Repositorio de desarrolladoras.
     */
    public DesarrolladoraService(DesarrolladoraRepo desarrolladoraRepo) {
        this.desarrolladoraRepo = desarrolladoraRepo;
    }

    /**
     * Obtiene todas las desarrolladoras almacenadas
     * @return Lista de desarrolladoras
     */
    public List<Desarrolladora> obtenerDesarrolladoras() {
        return desarrolladoraRepo.findAll();
    }

    /**
     * Obtiene una desarrolladora a partir de su nombre
     * @param nombre Nombre de la desarrolladora
     * @return Desarrolladora encontrada o null
     */
    public Desarrolladora obtenerPorNombre(String nombre) {
        return desarrolladoraRepo.findByNombre(nombre);
    }

    /**
     * Obtiene una desarrolladora a partir de su id interno
     * @param id Id interno
     * @return Desarrolladora encontrada o null
     */
    public Desarrolladora obtenerPorId(int id) {
        return desarrolladoraRepo.findByMiId(id);
    }

    /**
     * Guarda una desarrolladora en la base de datos
     * @param desarrolladora Objeto a guardar
     * @return Desarrolladora guardada
     */
    public Desarrolladora guardar(Desarrolladora desarrolladora) {
        if (desarrolladora.getId() == null) {
            var ultimo = desarrolladoraRepo.encontrarUltimoId();
            desarrolladora.setMiId(ultimo != null ? ultimo.getMiId() + 1 : 1);
        }
        return desarrolladoraRepo.save(desarrolladora);
    }

    /**
     * Borra una desarrolladora a partir de su id interno
     * @param id Id interno
     */
    public void borrarDesarrolladora(int id) {
        desarrolladoraRepo.deleteByMiId(id);
    }
}
