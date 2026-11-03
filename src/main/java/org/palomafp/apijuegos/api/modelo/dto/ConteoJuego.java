package org.palomafp.apijuegos.api.modelo.dto;

/**
 * Resultado de contar entradas de lista por videojuego.
 *
 * <p>Es la proyección que devuelve la agregación del hero: en lugar de traer
 * las 42 entradas a Java para contarlas, MongoDB agrupa, ordena y devuelve ya
 * el juego ganador.</p>
 *
 * @param idVideojuego Id público del videojuego.
 * @param total        Número de entradas de lista que lo referencian.
 * @author Andrés López
 */
public record ConteoJuego(Long idVideojuego, long total) {
}
