package org.palomafp.apijuegos.api.controladores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.dto.UsuarioPublicoDTO;
import org.palomafp.apijuegos.api.services.UsuarioService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void obtenerTodos() {
        Usuario u = new Usuario();
        when(usuarioService.obtenerTodos()).thenReturn(Arrays.asList(u));

        List<Usuario> result = usuarioController.obtenerTodos();
        assertEquals(1, result.size());
        verify(usuarioService, times(1)).obtenerTodos();
    }

    @Test
    void obtenerUsuarioPorId() {
        Usuario u = new Usuario();
        when(usuarioService.obtenerPorMiId(1)).thenReturn(u);

        UsuarioPublicoDTO result = usuarioController.obtenerUsuarioPorId(1);
        assertNotNull(result);
        verify(usuarioService, times(1)).obtenerPorMiId(1);
    }

    @Test
    void obtenerUsuarioPorNombre() {
        Usuario u = new Usuario();
        when(usuarioService.obtenerPorNombre("Test")).thenReturn(u);

        UsuarioPublicoDTO result = usuarioController.obtenerUsuarioPorNombre("Test");
        assertNotNull(result);
        verify(usuarioService, times(1)).obtenerPorNombre("Test");
    }

    @Test
    void borrarUsuarioPorMiId() {
        doNothing().when(usuarioService).borrarPorMiId(1);
        usuarioController.borrarUsuarioPorMiId(1);
        verify(usuarioService, times(1)).borrarPorMiId(1);
    }

    @Test
    void guardar() {
        Usuario u = new Usuario();
        when(usuarioService.guardar(u)).thenReturn(u);

        Usuario result = usuarioController.guardar(u);
        assertNotNull(result);
        verify(usuarioService, times(1)).guardar(u);
    }
}
