package org.palomafp.apijuegos.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * El servicio devolvia antes las autoridades vacias, por lo que el JWT se
 * validaba pero ningun hasRole() podia funcionar. Estas pruebas fijan el rol.
 */
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepo usuarioRepo;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Usuario usuarioConRol(String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setContrasenia("clave12345");
        usuario.setRol(rol);
        return usuario;
    }

    @Test
    @DisplayName("Un administrador recibe la autoridad ROLE_administrador")
    void mapeaRolAdministrador() {
        when(usuarioRepo.findByNombre("jefe"))
                .thenReturn(usuarioConRol("jefe", Rol.administrador));

        UserDetails detalles = customUserDetailsService.loadUserByUsername("jefe");

        assertEquals(1, detalles.getAuthorities().size());
        assertTrue(detalles.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_administrador")));
    }

    @Test
    @DisplayName("Un usuario normal recibe ROLE_usuarioNormal y no es administrador")
    void mapeaRolNormal() {
        when(usuarioRepo.findByNombre("normal"))
                .thenReturn(usuarioConRol("normal", Rol.usuarioNormal));

        UserDetails detalles = customUserDetailsService.loadUserByUsername("normal");

        assertEquals(1, detalles.getAuthorities().size());
        assertTrue(detalles.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_usuarioNormal")));
        assertTrue(detalles.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_administrador")));
    }

    @Test
    @DisplayName("Un usuario sin rol asignado cae en usuarioNormal en vez de quedarse sin permisos")
    void rolNuloUsaPorDefecto() {
        Usuario usuario = usuarioConRol("sindatos", null);
        when(usuarioRepo.findByNombre("sindatos")).thenReturn(usuario);

        UserDetails detalles = customUserDetailsService.loadUserByUsername("sindatos");

        assertEquals(1, detalles.getAuthorities().size());
        assertTrue(detalles.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_usuarioNormal")));
    }

    @Test
    @DisplayName("Un usuario inexistente lanza UsernameNotFoundException")
    void usuarioInexistente() {
        when(usuarioRepo.findByNombre("fantasma")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("fantasma"));
    }

    @Test
    @DisplayName("La contraseña encriptada se conserva para que el login funcione")
    void conservaHash() {
        Usuario usuario = usuarioConRol("normal", Rol.usuarioNormal);
        usuario.setContrasenia("$2a$10$hashfalsohashfalsohashfalso");
        when(usuarioRepo.findByNombre("normal")).thenReturn(usuario);

        UserDetails detalles = customUserDetailsService.loadUserByUsername("normal");

        assertEquals("$2a$10$hashfalsohashfalsohashfalso", detalles.getPassword());
    }
}
