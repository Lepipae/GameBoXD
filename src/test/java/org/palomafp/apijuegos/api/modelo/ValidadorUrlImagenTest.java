package org.palomafp.apijuegos.api.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del validador de URL de imagen.
 *
 * <p>{@link ValidadorUrlImagen#esAusente(String)} es la pieza que decide qué valor
 * sale en el campo {@code urlImagen} de los tres modelos. Si deja pasar un valor,
 * el literal heredado vuelve a aparecer en las respuestas de la API.</p>
 */
class ValidadorUrlImagenTest {

    @Test
    @DisplayName("Sin imagen: null, vacio, espacios o el literal heredado")
    void detectaValoresSinImagen() {
        assertTrue(ValidadorUrlImagen.esAusente(null));
        assertTrue(ValidadorUrlImagen.esAusente(""));
        assertTrue(ValidadorUrlImagen.esAusente("   "));
        assertTrue(ValidadorUrlImagen.esAusente("placeholder"));
        assertTrue(ValidadorUrlImagen.esAusente("  PLACEHOLDER  "));
    }

    @Test
    @DisplayName("Una URL real no se considera ausencia de imagen")
    void unaUrlRealNoEsAusencia() {
        assertFalse(ValidadorUrlImagen.esAusente("https://ejemplo.com/logo.png"));
        assertFalse(ValidadorUrlImagen.esAusente("http://ejemplo.com/logo.png"));
    }

    @Test
    @DisplayName("normalizar devuelve null cuando no hay imagen")
    void normalizarSinImagenDevuelveNull() {
        assertNull(ValidadorUrlImagen.normalizar(null));
        assertNull(ValidadorUrlImagen.normalizar("   "));
        assertNull(ValidadorUrlImagen.normalizar("placeholder"));
    }

    @Test
    @DisplayName("normalizar recorta los espacios de una URL valida")
    void normalizarRecortaEspacios() {
        assertTrue(ValidadorUrlImagen.normalizar("  https://ejemplo.com/a.png  ")
                .equals("https://ejemplo.com/a.png"));
    }

    @Test
    @DisplayName("Un esquema que no es http(s) se rechaza en vez de guardarse")
    void esquemaNoSoportadoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> ValidadorUrlImagen.normalizar("ftp://ejemplo.com/a.png"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorUrlImagen.normalizar("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorUrlImagen.normalizar("no-es-una-url"));
    }
}
