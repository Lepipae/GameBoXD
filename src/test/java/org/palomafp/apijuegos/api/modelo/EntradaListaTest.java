package org.palomafp.apijuegos.api.modelo;

import org.junit.jupiter.api.Test;
import org.palomafp.apijuegos.api.modelo.enums.Estado;

import static org.junit.jupiter.api.Assertions.*;

class EntradaListaTest {

    @Test
    void testConstructorVacioYSetters() {
        EntradaLista entrada = new EntradaLista();
        entrada.setId("1");
        entrada.setMiId(10L);
        entrada.setHorasJugadas(100);
        entrada.setNota(4.5);
        entrada.setResenya("Buena");
        entrada.setEstado(Estado.terminado);
        entrada.setIdVideojuego(1L);
        entrada.setIdUsuario(2);

        assertEquals("1", entrada.getId());
        assertEquals(10L, entrada.getMiId());
        assertEquals(100, entrada.getHorasJugadas());
        assertEquals(4.5, entrada.getNota());
        assertEquals("Buena", entrada.getResenya());
        assertEquals(Estado.terminado, entrada.getEstado());
        assertEquals(1L, entrada.getIdVideojuego());
        assertEquals(2, entrada.getIdUsuario());
    }

    @Test
    void testConstructorConParametros() {
        EntradaLista entrada = new EntradaLista(20L, 50, 3.0, "Regular", Estado.empezado, 5, 10);

        assertEquals(20L, entrada.getMiId());
        assertEquals(50, entrada.getHorasJugadas());
        assertEquals(3.0, entrada.getNota());
        assertEquals("Regular", entrada.getResenya());
        assertEquals(Estado.empezado, entrada.getEstado());
        assertEquals(5L, entrada.getIdVideojuego());
        assertEquals(10, entrada.getIdUsuario());
    }

    @Test
    void testHorasJugadasInvalidas() {
        EntradaLista entrada = new EntradaLista();
        assertThrows(IllegalArgumentException.class, () -> entrada.setHorasJugadas(-1));
    }

    @Test
    void testNotaInvalida() {
        EntradaLista entrada = new EntradaLista();
        assertThrows(IllegalArgumentException.class, () -> entrada.setNota(-0.1));
        assertThrows(IllegalArgumentException.class, () -> entrada.setNota(10.1));
    }
}
