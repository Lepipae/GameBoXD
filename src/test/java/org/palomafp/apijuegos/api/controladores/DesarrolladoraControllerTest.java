package org.palomafp.apijuegos.api.controladores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.modelo.Desarrolladora;
import org.palomafp.apijuegos.api.services.DesarrolladoraService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class DesarrolladoraControllerTest {

    @Mock
    private DesarrolladoraService desarrolladoraService;

    @InjectMocks
    private DesarrolladoraController desarrolladoraController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void obtenerDesarrolladoras() {
        Desarrolladora d = new Desarrolladora();
        when(desarrolladoraService.obtenerDesarrolladoras()).thenReturn(Arrays.asList(d));

        List<Desarrolladora> result = desarrolladoraController.obtenerDesarrolladoras().getBody();
        assertEquals(1, result.size());
        verify(desarrolladoraService, times(1)).obtenerDesarrolladoras();
    }

    @Test
    void obtenerPorNombre() {
        Desarrolladora d = new Desarrolladora();
        when(desarrolladoraService.obtenerPorNombre("Test")).thenReturn(d);

        Desarrolladora result = desarrolladoraController.obtenerPorNombre("Test");
        assertNotNull(result);
        verify(desarrolladoraService, times(1)).obtenerPorNombre("Test");
    }

    @Test
    void obtenerPorId() {
        Desarrolladora d = new Desarrolladora();
        when(desarrolladoraService.obtenerPorId(1)).thenReturn(d);

        Desarrolladora result = desarrolladoraController.obtenerPorId(1).getBody();
        assertNotNull(result);
        verify(desarrolladoraService, times(1)).obtenerPorId(1);
    }

    @Test
    void guardar() {
        Desarrolladora d = new Desarrolladora();
        when(desarrolladoraService.guardar(d)).thenReturn(d);

        Desarrolladora result = desarrolladoraController.guardar(d);
        assertNotNull(result);
        verify(desarrolladoraService, times(1)).guardar(d);
    }

    @Test
    void borrarDesarrolladora() {
        doNothing().when(desarrolladoraService).borrarDesarrolladora(1);
        desarrolladoraController.borrarDesarrolladora(1);
        verify(desarrolladoraService, times(1)).borrarDesarrolladora(1);
    }
}
