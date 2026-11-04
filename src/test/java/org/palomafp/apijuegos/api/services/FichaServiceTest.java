package org.palomafp.apijuegos.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.dto.FichaJuego;
import org.palomafp.apijuegos.api.modelo.dto.UsuarioPublicoDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La ficha agrupada debe resolver juego, estudio, reseñas y autores de una vez.
 */
class FichaServiceTest {

    @Mock
    private VideojuegoService videojuegoService;

    @Mock
    private DesarrolladoraService desarrolladoraService;

    @Mock
    private EntradaListaService entradaListaService;

    @Mock
    private UsuarioService usuarioService;

    /** Caché real: así se comprueba que la segunda llamada no vuelve a componer. */
    @Spy
    private CacheTemporal cacheTemporal = new CacheTemporal();

    @InjectMocks
    private FichaService fichaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static EntradaLista resenya(int idUsuario) {
        EntradaLista e = new EntradaLista();
        e.setIdUsuario(idUsuario);
        e.setResenya("Buen juego");
        return e;
    }

    @Test
    void componeJuegoEstudioResenyasYAutores() {
        Videojuego juego = new Videojuego();
        juego.setMiId(42L);
        juego.setIdDesarrolladora(25);

        Desarrolladora dev = new Desarrolladora();
        dev.setMiId(25);
        dev.setNombre("Maxis");

        when(videojuegoService.obtenerPorId(42L)).thenReturn(juego);
        when(desarrolladoraService.obtenerPorId(25)).thenReturn(dev);
        when(entradaListaService.findByIdVideojuego(42L))
                .thenReturn(List.of(resenya(10), resenya(10), resenya(20)));
        when(usuarioService.obtenerPublicosPorIds(List.of(10, 20)))
                .thenReturn(List.of(
                        new UsuarioPublicoDTO(10, "Ana", null),
                        new UsuarioPublicoDTO(20, "Luis", null)));

        FichaJuego ficha = fichaService.obtener(42L);

        assertNotNull(ficha);
        assertEquals("Maxis", ficha.desarrolladora().getNombre());
        assertEquals(3, ficha.resenyas().size());
        assertEquals(2, ficha.autores().size());
        // Los ids de autor se deduplican antes de ir a la base de datos.
        verify(usuarioService, times(1)).obtenerPublicosPorIds(List.of(10, 20));
    }

    @Test
    void juegoInexistenteDevuelveNullSinConsultarElResto() {
        when(videojuegoService.obtenerPorId(99L)).thenReturn(null);

        assertNull(fichaService.obtener(99L));
        verifyNoInteractions(entradaListaService, usuarioService, desarrolladoraService);
    }

    @Test
    void juegoSinEstudioNiResenyasEsValido() {
        Videojuego juego = new Videojuego();
        juego.setMiId(7L);
        juego.setIdDesarrolladora(0);

        when(videojuegoService.obtenerPorId(7L)).thenReturn(juego);
        when(entradaListaService.findByIdVideojuego(7L)).thenReturn(List.of());

        FichaJuego ficha = fichaService.obtener(7L);

        assertNotNull(ficha);
        assertNull(ficha.desarrolladora());
        assertTrue(ficha.resenyas().isEmpty());
        assertTrue(ficha.autores().isEmpty());
        verifyNoInteractions(desarrolladoraService, usuarioService);
    }

    @Test
    void segundaPeticionSeSirveDeLaCache() {
        Videojuego juego = new Videojuego();
        juego.setMiId(5L);
        juego.setIdDesarrolladora(0);
        when(videojuegoService.obtenerPorId(5L)).thenReturn(juego);
        when(entradaListaService.findByIdVideojuego(5L)).thenReturn(List.of());

        fichaService.obtener(5L);
        fichaService.obtener(5L);

        verify(videojuegoService, times(1)).obtenerPorId(5L);
    }
}
