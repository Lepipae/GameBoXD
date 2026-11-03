package org.palomafp.apijuegos.api.modelo.dto;

import org.palomafp.apijuegos.api.modelo.Videojuego;

import java.util.List;

/**
 * Proyección ligera de un videojuego para listados, búsquedas y recomendaciones.
 *
 * <p>El catálogo guarda la {@code descripcion} de cada juego (hasta 5000
 * caracteres). Devolver el documento completo en un listado multiplicaba el JSON
 * por diez sin que ninguna tarjeta lo usara: la portada solo necesita nombre,
 * imagen, nota y desarrolladora. Con este resumen, una página de 24 juegos pasa
 * de ~100 KB a ~2 KB.</p>
 *
 * @param id                Id de MongoDB (no se usa en el frontend, se mantiene por compatibilidad).
 * @param miId              Id público del juego.
 * @param nombre            Nombre del juego.
 * @param urlImagen         Portada (puede ser {@code null}).
 * @param notaMedia         Nota media de la comunidad.
 * @param idDesarrolladora  Id de la desarrolladora.
 * @param tags              Etiquetas del juego.
 * @author Andrés López
 */
public record ResumenVideojuego(
        String id,
        Long miId,
        String nombre,
        String urlImagen,
        double notaMedia,
        int idDesarrolladora,
        List<String> tags
) {

    /**
     * Construye el resumen a partir del documento completo.
     *
     * @param juego Videojuego origen.
     * @return Resumen listo para serializar.
     */
    public static ResumenVideojuego de(Videojuego juego) {
        return new ResumenVideojuego(
                juego.getId(),
                juego.getMiId(),
                juego.getNombre(),
                juego.getUrlImagen(),
                juego.getNotaMedia(),
                juego.getIdDesarrolladora(),
                juego.getTags()
        );
    }
}
