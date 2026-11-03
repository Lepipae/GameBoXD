package org.palomafp.apijuegos.api.modelo.dto;

/**
 * Datos mínimos de una desarrolladora para un desplegable.
 *
 * <p>El formulario de "insertar" solo necesita el id interno y el nombre para
 * pintar las opciones; enviar país, logo e id de Mongo multiplicaba por seis
 * el peso de esa respuesta.</p>
 *
 * @param miId   Id interno de la desarrolladora.
 * @param nombre Nombre de la desarrolladora.
 * @author Andrés López
 */
public record ResumenDesarrolladora(Integer miId, String nombre) {
}
