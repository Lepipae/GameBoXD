package org.palomafp.apijuegos.api.excepciones;

/**
 * Se lanza cuando se intenta crear un recurso que ya existe
 * (un juego con el mismo nombre, un usuario con el mismo nombre o una
 * entrada de lista repetida para el mismo juego).
 *
 * <p>Hereda de {@link IllegalArgumentException} para no romper el contrato que ya
 * usan los servicios y sus pruebas, pero el manejador de errores la traduce a
 * un <b>409 Conflict</b> en lugar del 400 que le tocaría por ser de validación.</p>
 *
 * @author Andrés López
 */
public class RecursoDuplicadoException extends IllegalArgumentException {

    /**
     * Crea la excepción con el mensaje que se mostrará al cliente.
     *
     * @param mensaje Descripción legible del conflicto.
     */
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
