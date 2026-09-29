package org.palomafp.apijuegos.api.excepciones;

/**
 * Se lanza cuando se pide un recurso que no existe.
 *
 * <p>Los servicios devuelven {@code null} cuando no encuentran nada, para no
 * obligar a los llamantes a capturar excepciones. Es el controlador el que
 * convierte ese {@code null} en esta excepción, y el manejador de errores la
 * traduce a un <b>404 Not Found</b>. Antes de este cambio esos endpoints
 * respondían 200 con el cuerpo vacío.</p>
 *
 * @author Andrés López
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje que se mostrará al cliente.
     *
     * @param mensaje Descripción legible de lo que no se encontró.
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
