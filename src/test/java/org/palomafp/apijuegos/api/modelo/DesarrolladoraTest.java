package org.palomafp.apijuegos.api.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DesarrolladoraTest {

    @Test
    void testConstructorVacioYSetters() {
        Desarrolladora dev = new Desarrolladora();
        dev.setId("1");
        dev.setMiId(10);
        dev.setNombre("DevTest");
        dev.setPais("España");
        dev.setUrlImagen("https://ejemplo.com/img.png");

        assertEquals("1", dev.getId());
        assertEquals(10, dev.getMiId());
        assertEquals("DevTest", dev.getNombre());
        assertEquals("España", dev.getPais());
        assertEquals("https://ejemplo.com/img.png", dev.getUrlImagen());
    }

    @Test
    void testConstructorConParametros() {
        Desarrolladora dev = new Desarrolladora();
        dev.setMiId(20);
        dev.setNombre("Dev2");
        dev.setPais("Francia");
        dev.setUrlImagen("https://ejemplo.com/img2.png");

        assertEquals(20, dev.getMiId());
        assertEquals("Dev2", dev.getNombre());
        assertEquals("Francia", dev.getPais());
        assertEquals("https://ejemplo.com/img2.png", dev.getUrlImagen());
    }

    @Test
    void testSetNombreInvalido() {
        Desarrolladora dev = new Desarrolladora();
        assertThrows(IllegalArgumentException.class, () -> dev.setNombre(null));
        assertThrows(IllegalArgumentException.class, () -> dev.setNombre("   "));
    }

    @Test
    void testSetPaisInvalido() {
        Desarrolladora dev = new Desarrolladora();
        assertThrows(IllegalArgumentException.class, () -> dev.setPais(null));
        assertThrows(IllegalArgumentException.class, () -> dev.setPais("   "));
    }

    @Test
    void testSetUrlImagenVaciaSeGuardaComoNull() {
        Desarrolladora dev = new Desarrolladora();
        dev.setUrlImagen(null);
        assertNull(dev.getUrlImagen());

        dev.setUrlImagen("   ");
        assertNull(dev.getUrlImagen());
    }

    @Test
    void testSetUrlImagenValidaSeGuarda() {
        Desarrolladora dev = new Desarrolladora();
        dev.setUrlImagen(" https://ejemplo.com/logo.png ");
        assertEquals("https://ejemplo.com/logo.png", dev.getUrlImagen());
    }

    @Test
    void testSetUrlImagenNoEsUrlSeRechaza() {
        Desarrolladora dev = new Desarrolladora();
        assertThrows(IllegalArgumentException.class, () -> dev.setUrlImagen("no-es-una-url"));
    }

    @Test
    void testValorHeredadoPlaceholderSeTrataComoAusencia() {
        Desarrolladora dev = new Desarrolladora();
        dev.setUrlImagen("placeholder");
        assertNull(dev.getUrlImagen());
    }
}
