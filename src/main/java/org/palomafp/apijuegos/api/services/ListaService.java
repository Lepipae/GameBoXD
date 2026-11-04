package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.dto.ListaCompleta;
import org.palomafp.apijuegos.api.modelo.dto.ResumenDesarrolladora;
import org.palomafp.apijuegos.api.modelo.dto.ResumenVideojuego;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Compone el contenido de "Mi lista" en una sola consulta lógica.
 *
 * <p>El frontend necesitaba tres peticiones para pintar una tarjeta: la lista
 * de entradas, los juegos de esas entradas y los estudios de esos juegos. Cada
 * una dependía de los ids de la anterior, así que la página encadenaba tres
 * viajes. Aquí se resuelven en el servidor y se devuelven juntos.</p>
 *
 * @author Andrés López
 */
@Service
public class ListaService {

    private final EntradaListaService entradaListaService;
    private final VideojuegoService videojuegoService;
    private final DesarrolladoraService desarrolladoraService;

    /**
     * Inyección por constructor.
     *
     * @param entradaListaService  Servicio de entradas de lista.
     * @param videojuegoService    Servicio de videojuegos (resúmenes por lote).
     * @param desarrolladoraService Servicio de desarrolladoras (lote).
     */
    public ListaService(EntradaListaService entradaListaService,
                        VideojuegoService videojuegoService,
                        DesarrolladoraService desarrolladoraService) {
        this.entradaListaService = entradaListaService;
        this.videojuegoService = videojuegoService;
        this.desarrolladoraService = desarrolladoraService;
    }

    /**
     * Devuelve las entradas del usuario junto con los juegos y estudios que
     * necesitan sus tarjetas.
     *
     * @param idUsuario Id interno del usuario.
     * @return Lista completa (posiblemente vacía).
     */
    public ListaCompleta obtener(int idUsuario) {
        List<EntradaLista> entradas = entradaListaService.findByIdUsuario(idUsuario);
        if (entradas == null || entradas.isEmpty()) {
            return new ListaCompleta(List.of(), List.of(), List.of());
        }

        List<Long> idsJuegos = entradas.stream()
                .map(EntradaLista::getIdVideojuego)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<ResumenVideojuego> juegos = videojuegoService.resumenPorIds(idsJuegos);

        List<Integer> idsDesarrolladoras = juegos.stream()
                .map(ResumenVideojuego::idDesarrolladora)
                .filter(id -> id > 0)
                .distinct()
                .toList();

        // Sin ids no se consulta la colección: ahorra una ida a Atlas cuando
        // ningún juego de la lista tiene estudio.
        List<ResumenDesarrolladora> desarrolladoras = idsDesarrolladoras.isEmpty()
                ? List.of()
                : desarrolladoraService.obtenerResumenesPorIds(idsDesarrolladoras);

        return new ListaCompleta(entradas, juegos, desarrolladoras);
    }
}
