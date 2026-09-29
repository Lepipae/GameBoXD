package org.palomafp.apijuegos.api.modelo;

import java.util.regex.Pattern;

/**
 * Validador de las URLs de imagen de los modelos.
 *
 * <p>Antes, {@code Desarrolladora} y {@code Usuario} guardaban el texto literal
 * {@code "placeholder"} en el campo {@code urlImagen} cuando el usuario no
 * aportaba ninguna imagen. Eso obligaba al frontend a tratar ese texto como un
 * caso especial, porque un campo de URL no debería contener una palabra.</p>
 *
 * <p>La regla nueva es:</p>
 * <ul>
 *   <li>Sin imagen (nulo, vacío o solo espacios) se guarda como {@code null}.</li>
 *   <li>Una URL http(s) válida se guarda tal cual.</li>
 *   <li>Cualquier otra cosa se rechaza con un 400 en vez de almacenarse.</li>
 * </ul>
 *
 * <p>El valor heredado {@code "placeholder"} se sigue aceptando como "sin imagen"
 * para que los registros antiguos puedan volver a guardarse sin romperse, y para
 * que la limpieza de los datos sea gradual.</p>
 *
 * @author Andrés López
 */
public final class ValidadorUrlImagen {

    /** Valor heredado que el backend escribia en el campo URL. */
    public static final String VALOR_HEREDADO = "placeholder";

    /** Solo se admiten http y https: cualquier otro esquema no es una imagen web. */
    private static final Pattern URL_HTTP = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);

    /**
     * Constructor privado: es una utilidad de métodos estáticos.
     */
    private ValidadorUrlImagen() {
    }

    /**
     * Normaliza y valida una URL de imagen.
     *
     * @param urlImagen Valor recibido del cliente; puede ser nulo o vacío.
     * @return La URL normalizada, o {@code null} si no se ha indicado ninguna imagen.
     * @throws IllegalArgumentException Si el valor no es una URL http(s) válida.
     */
    public static String normalizar(String urlImagen) {
        if (urlImagen == null) {
            return null;
        }

        var limpia = urlImagen.trim();

        // Sin imagen, o el valor heredado que se guardaba antes: se trata como ausencia.
        if (limpia.isEmpty() || VALOR_HEREDADO.equalsIgnoreCase(limpia)) {
            return null;
        }

        if (!URL_HTTP.matcher(limpia).matches()) {
            throw new IllegalArgumentException("""
                    La URL de la imagen debe empezar por http:// o https:// \
                    (valor recibido: %s)""".formatted(limpia));
        }

        return limpia;
    }

    /**
     * Indica si una URL almacenada debe exponerse al cliente como ausencia de imagen.
     *
     * <p>Los getters de los tres modelos delegan aquí, en vez de repetir la misma
     * comprobación del literal heredado en cada clase: era la única forma de que
     * {@code "placeholder"} no saliera nunca en las respuestas, y tres copias
     * invitan a que una se quede sin actualizar.</p>
     *
     * <p>Una cadena en blanco también cuenta como ausencia. El setter nunca guarda
     * una, pero si quedara alguna escrita directamente en la base de datos,
     * devolverla tal cual haría que el frontend intentara cargar una imagen inexistente.</p>
     *
     * @param urlImagen Valor tal y como está en el campo del modelo.
     * @return true si no hay imagen que enseñar.
     */
    public static boolean esAusente(String urlImagen) {
        if (urlImagen == null) {
            return true;
        }
        var limpia = urlImagen.trim();
        return limpia.isEmpty() || VALOR_HEREDADO.equalsIgnoreCase(limpia);
    }
}
