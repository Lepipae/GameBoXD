package org.palomafp.apijuegos.api.modelo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.palomafp.apijuegos.api.modelo.enums.Rol;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void testConstructorVacioYSetters() {
        Usuario usuario = new Usuario();
        usuario.setId("123");
        usuario.setMiId(1);
        usuario.setNombre("TestUser");
        usuario.setUrlImagen("http://imagen.com");
        usuario.setContrasenia("12345678");
        usuario.setRol(Rol.usuarioNormal);

        assertEquals("123", usuario.getId());
        assertEquals(1, usuario.getMiId());
        assertEquals("TestUser", usuario.getNombre());
        assertEquals("http://imagen.com", usuario.getUrlImagen());
        assertEquals("12345678", usuario.getContrasenia());
        assertEquals(Rol.usuarioNormal, usuario.getRol());
    }

    @Test
    void testConstructorConParametros() {
        Usuario usuario = new Usuario("TestUser", "http://imagen.com", "12345678", Rol.administrador, 2);

        assertEquals("TestUser", usuario.getNombre());
        assertEquals("http://imagen.com", usuario.getUrlImagen());
        assertEquals("12345678", usuario.getContrasenia());
        assertEquals(Rol.administrador, usuario.getRol());
        assertEquals(2, usuario.getMiId());
    }

    @Test
    void testSetNombreInvalido() {
        Usuario usuario = new Usuario();
        assertThrows(IllegalArgumentException.class, () -> usuario.setNombre(null));
        assertThrows(IllegalArgumentException.class, () -> usuario.setNombre("   "));
    }

    @Test
    void testSetUrlImagenVaciaAsignaPlaceholder() {
        Usuario usuario = new Usuario();
        usuario.setUrlImagen(null);
        assertEquals("placeholder", usuario.getUrlImagen());

        usuario.setUrlImagen("  ");
        assertEquals("placeholder", usuario.getUrlImagen());
    }

    @Test
    void testSetContraseniaInvalida() {
        Usuario usuario = new Usuario();
        assertThrows(IllegalArgumentException.class, () -> usuario.setContrasenia(null));
        assertThrows(IllegalArgumentException.class, () -> usuario.setContrasenia("   "));
        assertThrows(IllegalArgumentException.class, () -> usuario.setContrasenia("1234567")); // longitud < 8
    }

    /**
     * Regresion de seguridad: el hash de la contrasena no debe salir jamas
     * en las respuestas de la API (GET /api/usuarios era publico).
     */
    @Test
    void testContraseniaNoSeSerializa() throws Exception {
        Usuario usuario = new Usuario("TestUser", "http://imagen.com", "12345678", Rol.usuarioNormal, 1);

        String json = new ObjectMapper().writeValueAsString(usuario);

        assertFalse(json.contains("contrasenia"),
                "La respuesta de la API no debe incluir el campo contrasenia: " + json);
        assertFalse(json.contains("12345678"), "La contrasena se ha filtrado en el JSON: " + json);
        // El resto de datos publicos deben seguir saliendo
        assertTrue(json.contains("TestUser"));
    }

    /**
     * El campo es WRITE_ONLY, no IGNORE: debe seguir aceptandose al registrarse.
     */
    @Test
    void testContraseniaSiSeDeserializa() throws Exception {
        String json = "{\"nombre\":\"TestUser\",\"urlImagen\":\"http://imagen.com\","
                + "\"contrasenia\":\"12345678\",\"rol\":\"usuarioNormal\"}";

        Usuario usuario = new ObjectMapper().readValue(json, Usuario.class);

        assertEquals("12345678", usuario.getContrasenia());
    }
}
