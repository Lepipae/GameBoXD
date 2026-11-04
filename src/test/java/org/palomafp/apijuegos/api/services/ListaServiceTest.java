package org.palomafp.apijuegos.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.dto.ListaCompleta;
import org.palomafp.apijuegos.api.modelo.dto.ResumenDesarrolladora;
import org.palomafp.apijuegos.api.modelo.dto.ResumenVideojuego;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La composición de "Mi lista" debe resolver entradas, juegos y estudios con
 * una sola llamada del frontend y sin ids repetidos.
 */
class ListaServiceTest {

    @Mock
    private EntradaListaService entradaListaService;

    @Mock
    private VideojuegoService videojuegoService;

    @Mock
    private DesarrolladoraService desarrolladoraService;

    @InjectMocks
    private ListaService listaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static EntradaLista entrada(long idJuego) {
        EntradaLista e = new EntradaLista();
        e.setIdVideojuego(idJuego);
        return e;
    }

    @Test
    void usuarioSinEntradasNoConsultaJuegosNiEstudios() {
        when(entradaListaService.findByIdUsuario(1)).thenReturn(List.of());

        ListaCompleta lista = listaService.obtener(1);

        assertTrue(lista.entradas().isEmpty());
        assertTrue(lista.juegos().isEmpty());
        assertTrue(lista.desarrolladoras().isEmpty());
        verifyNoInteractions(videojuegoService, desarrolladoraService);
    }

    @Test
    void componeEntradasJuegosYEstudiosSinRepetirIds() {
        // Dos entradas del mismo juego: el lote de juegos recibe el id una vez.
        when(entradaListaService.findByIdUsuario(7))
                .thenReturn(List.of(entrada(10), entrada(10), entrada(20)));

        when(videojuegoService.resumenPorIds(List.of(10L, 20L))).thenReturn(List.of(
                new ResumenVideojuego("a", 10L, "Hollow Knight", null, 8.5, 100, List.of()),
                new ResumenVideojuego("b", 20L, "Celeste", null, 9.0, 100, List.of())));

        when(desarrolladoraService.obtenerResumenesPorIds(List.of(100)))
                .thenReturn(List.of(new ResumenDesarrolladora(100, "Team Cherry")));

        ListaCompleta lista = listaService.obtener(7);

        assertEquals(3, lista.entradas().size());
        assertEquals(2, lista.juegos().size());
        assertEquals(1, lista.desarrolladoras().size());
        assertEquals("Team Cherry", lista.desarrolladoras().get(0).nombre());
        verify(desarrolladoraService, times(1)).obtenerResumenesPorIds(List.of(100));
    }

    @Test
    void juegosSinDesarrolladoraNoSePiden() {
        when(entradaListaService.findByIdUsuario(7)).thenReturn(List.of(entrada(30)));
        when(videojuegoService.resumenPorIds(List.of(30L))).thenReturn(List.of(
                new ResumenVideojuego("c", 30L, "Sin estudio", null, 0.0, 0, List.of())));

        ListaCompleta lista = listaService.obtener(7);

        assertTrue(lista.desarrolladoras().isEmpty());
        verifyNoInteractions(desarrolladoraService);
    }
}
