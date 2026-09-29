package org.palomafp.apijuegos.api.modelo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class VideojuegoTest {

    @Test
    void testConstructorVacioYSetters() {
        Videojuego vj = new Videojuego();
        vj.setId("1");
        vj.setMiId(10L);
        vj.setNombre("Juego 1");
        vj.setDescripcion("Desc");
        vj.setUrlImagen("https://ejemplo.com/img.jpg");
        vj.setNotaMedia(4.5);
        vj.setIdDesarrolladora(2);
        
        ArrayList<String> tags = new ArrayList<>();
        tags.add("Accion");
        vj.setTags(tags);

        assertEquals("1", vj.getId());
        assertEquals(10L, vj.getMiId());
        assertEquals("Juego 1", vj.getNombre());
        assertEquals("Desc", vj.getDescripcion());
        assertEquals("https://ejemplo.com/img.jpg", vj.getUrlImagen());
        assertEquals(4.5, vj.getNotaMedia());
        assertEquals(2, vj.getIdDesarrolladora());
        assertEquals(1, vj.getTags().size());
    }

    @Test
    void testConstructorConParametros() {
        ArrayList<String> tags = new ArrayList<>();
        tags.add("Aventura");
        Videojuego vj = new Videojuego("2", 20L, "Juego 2", "Desc 2", "https://ejemplo.com/img2.jpg", 3.0, tags, 5);

        assertEquals("2", vj.getId());
        assertEquals(20L, vj.getMiId());
        assertEquals("Juego 2", vj.getNombre());
        assertEquals("Desc 2", vj.getDescripcion());
        assertEquals("https://ejemplo.com/img2.jpg", vj.getUrlImagen());
        assertEquals(3.0, vj.getNotaMedia());
        assertEquals(5, vj.getIdDesarrolladora());
        assertEquals(1, vj.getTags().size());
    }

    @Test
    void testSetNombreInvalido() {
        Videojuego vj = new Videojuego();
        assertThrows(IllegalArgumentException.class, () -> vj.setNombre(null));
        assertThrows(IllegalArgumentException.class, () -> vj.setNombre("   "));
    }
}
