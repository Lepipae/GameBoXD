package org.palomafp.apijuegos.api.controladores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.services.VideojuegoService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VideojuegoControllerTest {

    @Mock
    private VideojuegoService videojuegoService;

    @InjectMocks
    private VideojuegoController videojuegoController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void obtenerTodos() {
        Videojuego v = new Videojuego();
        when(videojuegoService.obtenerTodos()).thenReturn(Arrays.asList(v));

        List<Videojuego> result = videojuegoController.obtenerTodos();
        assertEquals(1, result.size());
        verify(videojuegoService, times(1)).obtenerTodos();
    }

    @Test
    void obtenerPorId() {
        Videojuego v = new Videojuego();
        when(videojuegoService.obtenerPorId(1L)).thenReturn(v);

        Videojuego result = videojuegoController.obtenerPorId(1L);
        assertNotNull(result);
        verify(videojuegoService, times(1)).obtenerPorId(1L);
    }

    @Test
    void obtenerPorNombre() {
        Videojuego v = new Videojuego();
        when(videojuegoService.obtenerPorNombre("Test")).thenReturn(v);

        Videojuego result = videojuegoController.obtenerPorNombre("Test");
        assertNotNull(result);
        verify(videojuegoService, times(1)).obtenerPorNombre("Test");
    }

    @Test
    void obtenerPorTags() {
        Videojuego v = new Videojuego();
        when(videojuegoService.obtenerPorTags("Tag")).thenReturn(Arrays.asList(v));

        List<Videojuego> result = videojuegoController.obtenerPorTags("Tag");
        assertEquals(1, result.size());
        verify(videojuegoService, times(1)).obtenerPorTags("Tag");
    }

    @Test
    void obtenerPorDesarrolladora() {
        Videojuego v = new Videojuego();
        when(videojuegoService.obtenerPorDesarrolladora(1)).thenReturn(Arrays.asList(v));

        List<Videojuego> result = videojuegoController.obtenerPorDesarrolladora(1);
        assertEquals(1, result.size());
        verify(videojuegoService, times(1)).obtenerPorDesarrolladora(1);
    }

    @Test
    void guardar() {
        Videojuego v = new Videojuego();
        when(videojuegoService.guardar(v)).thenReturn(v);

        Videojuego result = videojuegoController.guardar(v);
        assertNotNull(result);
        verify(videojuegoService, times(1)).guardar(v);
    }

    @Test
    void borrarVideojuego() {
        doNothing().when(videojuegoService).borrarVideojuego(1L);
        videojuegoController.borrarVideojuego(1L);
        verify(videojuegoService, times(1)).borrarVideojuego(1L);
    }

    /**
     * El endpoint del hero devuelve el juego con más entradas de la lista.
     */
    @Test
    void obtenerMasResenyado() {
        Videojuego v = new Videojuego();
        v.setMiId(9L);
        when(videojuegoService.obtenerMasResenyado()).thenReturn(v);

        Videojuego result = videojuegoController.obtenerMasResenyado();

        assertNotNull(result);
        assertEquals(9L, result.getMiId());
        verify(videojuegoService, times(1)).obtenerMasResenyado();
    }

    /**
     * Sin ninguna entrada de lista no hay hero: 404 con mensaje claro en vez
     * de un cuerpo null silencioso.
     */
    @Test
    void obtenerMasResenyadoSinDatosLanzaNoEncontrado() {
        when(videojuegoService.obtenerMasResenyado()).thenReturn(null);

        assertThrows(RecursoNoEncontradoException.class,
                () -> videojuegoController.obtenerMasResenyado());
    }
}
