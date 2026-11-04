package org.palomafp.apijuegos.api.modelo.dto;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.dto.UsuarioPublicoDTO;

import java.util.List;

/**
 * Respuesta completa de la ficha de un juego.
 *
 * <p>Antes la página hacía tres peticiones: el juego, sus reseñas y (solo
 * cuando llegaba el juego) su desarrolladora. Las dos últimas dependían de la
 * primera, así que la ficha encadenaba viajes a Atlas y tardaba el doble de lo
 * necesario. Con este agregado la ficha se pinta con una sola petición.</p>
 *
 * <p>{@code autores} resuelve de una vez los nombres que necesitan las
 * tarjetas de reseña: la página pedía antes {@code /usuarios/lote} en cuanto
 * tenía las reseñas, otro viaje encadenado.</p>
 *
 * @param juego          Datos del videojuego.
 * @param desarrolladora Estudio que lo desarrolló (null si no tiene).
 * @param resenyas       Entradas de lista del juego (puede ser una lista vacía).
 * @param autores        Datos públicos de los autores de esas reseñas.
 * @author Andrés López
 */
public record FichaJuego(
        Videojuego juego,
        Desarrolladora desarrolladora,
        List<EntradaLista> resenyas,
        List<UsuarioPublicoDTO> autores
) {
}
