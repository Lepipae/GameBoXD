package org.palomafp.apijuegos.api.controladores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.security.Autorizacion;
import org.palomafp.apijuegos.api.services.EntradaListaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EntradaListaControllerTest {

    @Mock
    private EntradaListaService entradaListaService;

    @Mock
    private Autorizacion autorizacion;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EntradaListaController entradaListaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findByIdUsuario() {
        EntradaLista e = new EntradaLista();
        when(entradaListaService.findByIdUsuario(1)).thenReturn(Arrays.asList(e));

        List<EntradaLista> result = entradaListaController.findByIdUsuario(1);
        assertEquals(1, result.size());
        verify(entradaListaService, times(1)).findByIdUsuario(1);
    }

    @Test
    void findById() {
        EntradaLista e = new EntradaLista();
        when(entradaListaService.findById(1L)).thenReturn(e);
        when(autorizacion.puedeEditarEntrada(e, authentication)).thenReturn(true);

        EntradaLista result = entradaListaController.findById(1L, authentication);
        assertNotNull(result);
        verify(entradaListaService, times(1)).findById(1L);
    }

    /**
     * Una entrada que pertenece a otro usuario no debe poder consultarse.
     */
    @Test
    void findByIdRechazaEntradaAjena() {
        EntradaLista e = new EntradaLista();
        when(entradaListaService.findById(1L)).thenReturn(e);
        when(autorizacion.puedeEditarEntrada(e, authentication)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> entradaListaController.findById(1L, authentication));
    }

    @Test
    void borrarEntrada() {
        EntradaLista e = new EntradaLista();
        when(entradaListaService.findById(1L)).thenReturn(e);
        when(autorizacion.puedeEditarEntrada(e, authentication)).thenReturn(true);
        doNothing().when(entradaListaService).borrarEntrada(1);

        entradaListaController.borrarEntrada(1, authentication);

        verify(entradaListaService, times(1)).borrarEntrada(1);
    }

    /**
     * Regresion de seguridad: antes cualquier usuario autenticado (o ninguno) podia
     * borrar la entrada de otra persona solo con conocer su id.
     */
    @Test
    void borrarEntradaRechazaEntradaAjena() {
        EntradaLista e = new EntradaLista();
        when(entradaListaService.findById(1L)).thenReturn(e);
        when(autorizacion.puedeEditarEntrada(e, authentication)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> entradaListaController.borrarEntrada(1, authentication));
        verify(entradaListaService, never()).borrarEntrada(anyInt());
    }

    @Test
    void guardar() {
        EntradaLista e = new EntradaLista();
        when(entradaListaService.guardar(e)).thenReturn(e);

        EntradaLista result = entradaListaController.guardar(e);
        assertNotNull(result);
        verify(entradaListaService, times(1)).guardar(e);
    }
}
