package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.modelo.dto.ResumenDesarrolladora;
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
    private final CacheTemporal cacheTemporal;

    /** Clave de caché del listado ligero de desplegables. */
    private static final String CLAVE_OPCIONES = "desarrolladoras:opciones";

    /** Las desarrolladoras cambian solo al escribir: 5 minutos. */
    private static final java.time.Duration TTL_OPCIONES = java.time.Duration.ofMinutes(5);

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param desarrolladoraRepo Repositorio de desarrolladoras.
     * @param cacheTemporal      Caché del listado de desplegables.
     */
    public DesarrolladoraService(DesarrolladoraRepo desarrolladoraRepo, CacheTemporal cacheTemporal) {
        this.desarrolladoraRepo = desarrolladoraRepo;
        this.cacheTemporal = cacheTemporal;
    }

    /**
     * Obtiene todas las desarrolladoras almacenadas
     * @return Lista de desarrolladoras
     */
    public List<Desarrolladora> obtenerDesarrolladoras() {
        return desarrolladoraRepo.findAll();
    }

    /**
     * Listado ligero de desarrolladoras para desplegables.
     *
     * <p>Devuelve solo id interno y nombre: el formulario de insertar no
     * necesita país ni logo, y son dos tercios del peso de la respuesta.</p>
     *
     * @return Desarrolladoras con solo {@code miId} y {@code nombre}.
     */
    public List<ResumenDesarrolladora> obtenerOpciones() {
        return cacheTemporal.obtener(CLAVE_OPCIONES, TTL_OPCIONES, () ->
                desarrolladoraRepo.findSoloIdYNombre().stream()
                        .map(dev -> new ResumenDesarrolladora(dev.getMiId(), dev.getNombre()))
                        .toList());
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
     * Resuelve varias desarrolladoras de una sola vez.
     *
     * @param ids Ids internos pedidos (se ignoran los {@code null}).
     * @return Desarrolladoras encontradas.
     */
    public List<Desarrolladora> obtenerPorIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Integer> unicos = ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .limit(200)
                .toList();
        if (unicos.isEmpty()) {
            return List.of();
        }
        return desarrolladoraRepo.findByMiIdIn(unicos);
    }

    /**
     * Resuelve varias desarrolladoras devolviendo solo id y nombre.
     *
     * <p>Es lo que necesita "Mi lista": pinta el nombre del estudio bajo cada
     * tarjeta y no usa país ni logo. La proyección la aplica MongoDB, así que
     * tampoco viajan esos campos por la red.</p>
     *
     * @param ids Ids internos pedidos (se ignoran los {@code null}).
     * @return Pares {@code miId}/{@code nombre} de las desarrolladoras encontradas.
     */
    public List<ResumenDesarrolladora> obtenerResumenesPorIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Integer> unicos = ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .limit(200)
                .toList();
        if (unicos.isEmpty()) {
            return List.of();
        }
        return desarrolladoraRepo.findSoloIdYNombrePorIds(unicos).stream()
                .map(dev -> new ResumenDesarrolladora(dev.getMiId(), dev.getNombre()))
                .toList();
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
        Desarrolladora guardada = desarrolladoraRepo.save(desarrolladora);
        // El desplegable de "insertar" acaba de cambiar.
        cacheTemporal.invalidar("desarrolladoras:");
        return guardada;
    }

    /**
     * Borra una desarrolladora a partir de su id interno
     * @param id Id interno
     */
    public void borrarDesarrolladora(int id) {
        desarrolladoraRepo.deleteByMiId(id);
        cacheTemporal.invalidar("desarrolladoras:");
    }
}
