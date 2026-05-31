package org.palomafp.apijuegos.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.repositories.EntradaListaRepo;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.palomafp.apijuegos.api.modelo.Videojuego;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntradaListaServiceTest {

    @Mock
    private EntradaListaRepo entradaListaRepo;

    @Mock
    private VideojuegoRepo videojuegoRepo;

    @InjectMocks
    private EntradaListaService entradaListaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findByIdUsuario() {
        EntradaLista e = new EntradaLista();
        when(entradaListaRepo.findByIdUsuario(1)).thenReturn(Arrays.asList(e));

        List<EntradaLista> result = entradaListaService.findByIdUsuario(1);
        assertEquals(1, result.size());
        verify(entradaListaRepo, times(1)).findByIdUsuario(1);
    }

    @Test
    void findById() {
        EntradaLista e = new EntradaLista();
        e.setMiId(1L);
        when(entradaListaRepo.findByMiId(1L)).thenReturn(e);

        EntradaLista result = entradaListaService.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getMiId());
    }

    @Test
    void guardarNuevoExito() {
        EntradaLista nuevo = new EntradaLista();

        EntradaLista ultimo = new EntradaLista();
        ultimo.setMiId(5L);

        when(entradaListaRepo.encontrarUltimoId()).thenReturn(ultimo);
        when(entradaListaRepo.save(any(EntradaLista.class))).thenReturn(nuevo);

        entradaListaService.guardar(nuevo);
        assertEquals(6L, nuevo.getMiId());
        verify(entradaListaRepo).save(nuevo);
    }

    @Test
    void guardarNuevoSinUltimo() {
        EntradaLista nuevo = new EntradaLista();

        when(entradaListaRepo.encontrarUltimoId()).thenReturn(null);
        when(entradaListaRepo.save(any(EntradaLista.class))).thenReturn(nuevo);

        entradaListaService.guardar(nuevo);
        assertEquals(1L, nuevo.getMiId());
        verify(entradaListaRepo).save(nuevo);
    }

    @Test
    void guardarExistente() {
        EntradaLista existente = new EntradaLista();
        existente.setId("1");

        when(entradaListaRepo.save(any(EntradaLista.class))).thenReturn(existente);

        entradaListaService.guardar(existente);
        verify(entradaListaRepo, never()).encontrarUltimoId();
        verify(entradaListaRepo).save(existente);
    }

    @Test
    void borrarEntrada() {
        EntradaLista e = new EntradaLista();
        e.setIdVideojuego(1L);
        when(entradaListaRepo.findByMiId(1L)).thenReturn(e);
        doNothing().when(entradaListaRepo).deleteByMiId(1L);
        entradaListaService.borrarEntrada(1);
        verify(entradaListaRepo, times(1)).deleteByMiId(1L);
    }

    @Test
    void borrarPorVideojuego() {
        doNothing().when(entradaListaRepo).deleteByIdVideojuego(1L);
        entradaListaService.borrarPorVideojuego(1L);
        verify(entradaListaRepo, times(1)).deleteByIdVideojuego(1L);
    }
}
