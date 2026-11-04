package org.palomafp.apijuegos.api.controladores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.dto.FichaJuego;
import org.palomafp.apijuegos.api.services.FichaService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La ficha agrupada sustituye a tres peticiones encadenadas del detalle.
 */
class FichaControllerTest {

    @Mock
    private FichaService fichaService;

    @InjectMocks
    private FichaController fichaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void devuelveJuegoDesarrolladoraYResenyasEnUnaRespuesta() {
        Videojuego juego = new Videojuego();
        juego.setMiId(7L);
        juego.setNombre("Celeste");

        Desarrolladora dev = new Desarrolladora();
        dev.setMiId(3);
        dev.setNombre("Extremely OK Games");

        EntradaLista resenya = new EntradaLista();
        resenya.setIdUsuario(10);

        var autor = new org.palomafp.apijuegos.api.modelo.dto.UsuarioPublicoDTO(10, "Ana", null);

        when(fichaService.obtener(7L)).thenReturn(
                new FichaJuego(juego, dev, List.of(resenya), List.of(autor)));

        FichaJuego ficha = fichaController.obtener(7L).getBody();

        assertNotNull(ficha);
        assertEquals("Celeste", ficha.juego().getNombre());
        assertEquals("Extremely OK Games", ficha.desarrolladora().getNombre());
        assertEquals(1, ficha.resenyas().size());
        assertEquals(1, ficha.autores().size());
        assertEquals("Ana", ficha.autores().get(0).nombre());
        verify(fichaService, times(1)).obtener(7L);
    }

    @Test
    void juegoInexistenteLanzaNoEncontrado() {
        when(fichaService.obtener(99L)).thenReturn(null);

        assertThrows(RecursoNoEncontradoException.class, () -> fichaController.obtener(99L));
    }

    @Test
    void juegoSinDesarrolladoraEsValido() {
        Videojuego juego = new Videojuego();
        juego.setMiId(8L);
        when(fichaService.obtener(8L)).thenReturn(new FichaJuego(juego, null, List.of(), List.of()));

        FichaJuego ficha = fichaController.obtener(8L).getBody();

        assertNotNull(ficha);
        assertNull(ficha.desarrolladora());
        assertTrue(ficha.resenyas().isEmpty());
        assertTrue(ficha.autores().isEmpty());
    }
}
