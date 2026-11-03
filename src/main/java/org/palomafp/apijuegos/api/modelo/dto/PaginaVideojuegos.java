package org.palomafp.apijuegos.api.modelo.dto;

import java.util.List;

/**
 * Página de resultados del catálogo.
 *
 * <p>Formato pensado para que el frontend no tenga que descargar el catálogo
 * entero para pintar la primera pantalla: incluye solo los juegos de la página
 * pedida y los totales necesarios para saber si quedan más.</p>
 *
 * @param content       Juegos de esta página.
 * @param page          Número de página (empieza en 0).
 * @param size          Tamaño de página solicitado.
 * @param totalElements Total de juegos que cumplen el filtro.
 * @param totalPages    Total de páginas disponibles.
 * @author Andrés López
 */
public record PaginaVideojuegos(
        List<ResumenVideojuego> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Construye la página calculando el número total de páginas.
     *
     * @param content       Juegos de esta página.
     * @param page          Número de página.
     * @param size          Tamaño de página.
     * @param totalElements Total de coincidencias.
     * @return Página lista para serializar.
     */
    public static PaginaVideojuegos de(List<ResumenVideojuego> content, int page, int size, long totalElements) {
        int paginas = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PaginaVideojuegos(content, page, size, totalElements, paginas);
    }
}
