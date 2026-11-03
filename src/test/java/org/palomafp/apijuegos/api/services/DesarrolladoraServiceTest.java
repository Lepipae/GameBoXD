package org.palomafp.apijuegos.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.repositories.DesarrolladoraRepo;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DesarrolladoraServiceTest {

    @Mock
    private DesarrolladoraRepo desarrolladoraRepo;

    /** Caché real: el listado de desplegables se cachea y se invalida al escribir. */
    @Spy
    private CacheTemporal cacheTemporal = new CacheTemporal();

    @InjectMocks
    private DesarrolladoraService desarrolladoraService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void obtenerDesarrolladoras() {
        Desarrolladora d1 = new Desarrolladora();
        when(desarrolladoraRepo.findAll()).thenReturn(Arrays.asList(d1));

        List<Desarrolladora> result = desarrolladoraService.obtenerDesarrolladoras();
        assertEquals(1, result.size());
        verify(desarrolladoraRepo, times(1)).findAll();
    }

    @Test
    void obtenerPorNombre() {
        Desarrolladora d = new Desarrolladora();
        d.setNombre("Dev");
        when(desarrolladoraRepo.findByNombre("Dev")).thenReturn(d);

        Desarrolladora result = desarrolladoraService.obtenerPorNombre("Dev");
        assertNotNull(result);
        assertEquals("Dev", result.getNombre());
    }

    @Test
    void obtenerPorId() {
        Desarrolladora d = new Desarrolladora();
        d.setMiId(1);
        when(desarrolladoraRepo.findByMiId(1)).thenReturn(d);

        Desarrolladora result = desarrolladoraService.obtenerPorId(1);
        assertNotNull(result);
        assertEquals(1, result.getMiId());
    }

    @Test
    void guardarNuevoExito() {
        Desarrolladora d = new Desarrolladora();
        Desarrolladora ultimo = new Desarrolladora();
        ultimo.setMiId(5);

        when(desarrolladoraRepo.encontrarUltimoId()).thenReturn(ultimo);
        when(desarrolladoraRepo.save(any(Desarrolladora.class))).thenReturn(d);

        desarrolladoraService.guardar(d);
        assertEquals(6, d.getMiId());
        verify(desarrolladoraRepo).save(d);
    }

    @Test
    void guardarNuevoSinUltimo() {
        Desarrolladora d = new Desarrolladora();

        when(desarrolladoraRepo.encontrarUltimoId()).thenReturn(null);
        when(desarrolladoraRepo.save(any(Desarrolladora.class))).thenReturn(d);

        desarrolladoraService.guardar(d);
        assertEquals(1, d.getMiId());
        verify(desarrolladoraRepo).save(d);
    }

    @Test
    void guardarExistente() {
        Desarrolladora d = new Desarrolladora();
        d.setId("1");

        when(desarrolladoraRepo.save(any(Desarrolladora.class))).thenReturn(d);

        desarrolladoraService.guardar(d);
        verify(desarrolladoraRepo, never()).encontrarUltimoId();
        verify(desarrolladoraRepo).save(d);
    }

    @Test
    void borrarDesarrolladora() {
        doNothing().when(desarrolladoraRepo).deleteByMiId(1);
        desarrolladoraService.borrarDesarrolladora(1);
        verify(desarrolladoraRepo, times(1)).deleteByMiId(1);
    }
}
