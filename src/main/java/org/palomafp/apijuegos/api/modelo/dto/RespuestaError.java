package org.palomafp.apijuegos.api.modelo.dto;

import java.time.Instant;

/**
 * Cuerpo común de todas las respuestas de error de la API.
 *
 * <p>Antes de existir este tipo, {@code ManejadorErrores} devolvía
 * {@code ResponseEntity<Map<String, Object>}}: el mismo JSON, pero sin contrato.
 * Nada impedía escribir {@code cuerpo.put("erro", ...)} o devolver un mapa con
 * claves distintas según el manejador, y el frontend no tenía forma de saber a qué
 * atenerse. Como {@code record}, la forma de la respuesta queda fijada por el
 * compilador y es idéntica para los ocho códigos HTTP.</p>
 *
 * @param error     Mensaje legible para el cliente.
 * @param status    Código HTTP devuelto.
 * @param timestamp Momento de la respuesta en ISO-8601, para que el cliente pueda
 *                  correlacionarla con los registros del servidor.
 * @author Andrés López
 */
public record RespuestaError(String error, int status, String timestamp) {

    /**
     * Crea la respuesta sellando el instante actual.
     *
     * @param mensaje Mensaje legible para el cliente.
     * @param status  Código HTTP devuelto.
     * @return Respuesta de error con la marca de tiempo puesta.
     */
    public static RespuestaError de(String mensaje, int status) {
        return new RespuestaError(mensaje, status, Instant.now().toString());
    }
}
