package org.palomafp.apijuegos.api.modelo.dto;

import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.modelo.EntradaLista;

import java.util.List;

/**
 * Contenido completo de "Mi lista" en una sola respuesta.
 *
 * <p>La página pedía las entradas, luego los juegos y luego las desarrolladoras:
 * tres viajes encadenados porque cada uno dependía de los ids del anterior.
 * Agruparlos ahorra dos idas y vueltas a la API y deja la lista lista con una
 * única petición.</p>
 *
 * <p>Los estudios viajan como {@link ResumenDesarrolladora} (id y nombre): la
 * tarjeta solo pinta el nombre, y los documentos completos pesaban cuatro veces
 * más.</p>
 *
 * @param entradas        Entradas de lista del usuario.
 * @param juegos          Resúmenes de los juegos referenciados.
 * @param desarrolladoras Estudios de esos juegos (solo id y nombre).
 * @author Andrés López
 */
public record ListaCompleta(
        List<EntradaLista> entradas,
        List<ResumenVideojuego> juegos,
        List<ResumenDesarrolladora> desarrolladoras
) {
}
