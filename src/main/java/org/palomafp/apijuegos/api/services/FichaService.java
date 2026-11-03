package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.dto.FichaJuego;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Compone la respuesta de la ficha de un juego en una sola llamada.
 *
 * <p>La página de detalle necesitaba tres peticiones encadenadas: el juego,
 * sus reseñas y su desarrolladora (esta última no se podía pedir hasta conocer
 * el {@code idDesarrolladora}). Cada eslabón es un viaje a Atlas de ~200 ms, así
 * que la ficha tardaba el doble de lo que tarda este agregado.</p>
 *
 * @author Andrés López
 */
@Service
public class FichaService {

    /**
     * Las reseñas y la nota media cambian al puntuar, así que la ficha se cachea
     * poco: lo justo para absorber recargas y el "atrás" del navegador.
     */
    private static final Duration TTL_FICHA = Duration.ofSeconds(30);

    private final VideojuegoService videojuegoService;
    private final DesarrolladoraService desarrolladoraService;
    private final EntradaListaService entradaListaService;
    private final CacheTemporal cacheTemporal;

    /**
     * Inyección por constructor.
     *
     * @param videojuegoService    Servicio de videojuegos.
     * @param desarrolladoraService Servicio de desarrolladoras.
     * @param entradaListaService  Servicio de entradas de lista (reseñas).
     * @param cacheTemporal        Caché de la ficha por id.
     */
    public FichaService(VideojuegoService videojuegoService,
                        DesarrolladoraService desarrolladoraService,
                        EntradaListaService entradaListaService,
                        CacheTemporal cacheTemporal) {
        this.videojuegoService = videojuegoService;
        this.desarrolladoraService = desarrolladoraService;
        this.entradaListaService = entradaListaService;
        this.cacheTemporal = cacheTemporal;
    }

    /**
     * Devuelve el juego, su desarrolladora y sus reseñas.
     *
     * <p>La clave de caché usa el prefijo {@code videojuegos:} a propósito: las
     * escrituras de videojuegos y de entradas de lista ya invalidan ese prefijo,
     * así que una nueva reseña no deja la ficha con datos viejos.</p>
     *
     * @param miId Id interno del videojuego.
     * @return Ficha completa, o {@code null} si el juego no existe.
     */
    public FichaJuego obtener(long miId) {
        return cacheTemporal.obtener("videojuegos:ficha:" + miId, TTL_FICHA, () -> {
            Videojuego juego = videojuegoService.obtenerPorId(miId);
            if (juego == null) {
                return null;
            }

            Desarrolladora desarrolladora = juego.getIdDesarrolladora() > 0
                    ? desarrolladoraService.obtenerPorId(juego.getIdDesarrolladora())
                    : null;

            List<EntradaLista> resenyas = entradaListaService.findByIdVideojuego(miId);

            return new FichaJuego(juego, desarrolladora, resenyas == null ? List.of() : resenyas);
        });
    }
}
