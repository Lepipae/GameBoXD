package org.palomafp.apijuegos.api.modelo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Clase que representa a un usuario del sistema
 * @author Andrés López
 */
@Document(collection = "Users")
public class Usuario {
    @Id
    private String id;

    private int miId;
    private String nombre;
    private String urlImagen;

    /**
     * Contraseña del usuario (siempre guardada en formato BCrypt).
     *
     * WRITE_ONLY hace que Jackson la acepte al deserializar (registro) pero la
     * omita al serializar, de modo que el hash nunca viaja en las respuestas.
     * No se usa {@code @JsonIgnore} porque también bloquearía la entrada de la
     * contraseña y el registro dejaría de funcionar.
     *
     * El getter sigue disponible para Java: lo necesitan
     * {@code UsuarioService} y {@code CustomUserDetailsService} para autenticar.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasenia;

    private Rol rol;

    /**
     * Constructor por defecto
     */
    public Usuario() {
    }

    /**
     * Constructor con parametros
     * @param nombre        Nombre del usuario
     * @param urlImagen     URL de la imagen de perfil
     * @param contrasenia   Contraseña del usuario
     * @param rol           Rol del usuario en el sistema
     * @param miId          Id interno de la base de datos
     */
    public Usuario(String nombre, String urlImagen, String contrasenia, Rol rol, int miId) {
        setNombre(nombre);
        setUrlImagen(urlImagen);
        setContrasenia(contrasenia);
        setRol(rol);
        setMiId(miId);
    }

    /**
     * Establece el id interno
     * @param miId Id interno a establecer
     */
    public void setMiId(int miId) {
        this.miId = miId;
    }

    /**
     * Obtiene el id interno
     * @return Id interno
     */
    public int getMiId() {
        return miId;
    }

    /**
     * Obtiene el id de MongoDB
     * @return Id de MongoDB
     */
    public String getId() {
        return id;
    }

    /**
     * Establece el id de MongoDB
     * @param id Id a establecer
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del usuario
     * @return Nombre del usuario
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del usuario
     * @param nombre Nombre a establecer
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        this.nombre = nombre;
    }

    /**
     * Obtiene la url de la imagen de perfil
     *
     * <p>Los registros antiguos guardaban el texto {@code "placeholder"} en este
     * campo. Se devuelve como {@code null} para que ese valor heredado no llegue
     * a la API ni al frontend. La fila antigua se corrige sola la proxima vez que
     * se guarde el usuario.</p>
     *
     * @return URL de la imagen, o null si el usuario no tiene imagen
     */
    public String getUrlImagen() {
        return ValidadorUrlImagen.esAusente(urlImagen) ? null : urlImagen;
    }

    /**
     * Establece la url de la imagen de perfil
     * @param urlImagen URL a establecer
     */
    public void setUrlImagen(String urlImagen) {
        this.urlImagen = ValidadorUrlImagen.normalizar(urlImagen);
    }

    /**
     * Obtiene la contraseña
     * @return Contraseña
     */
    public String getContrasenia() {
        return contrasenia;
    }

    /**
     * Establece la contraseña
     * @param contrasenia Contraseña a establecer
     */
    public void setContrasenia(String contrasenia) {
        if (contrasenia == null || contrasenia.isBlank() || contrasenia.length() < 8) {
            throw new IllegalArgumentException("Contraseña no valida");
        }
        this.contrasenia = contrasenia;
    }

    /**
     * Obtiene el rol del usuario
     * @return Rol del usuario
     */
    public Rol getRol() {
        return rol;
    }

    /**
     * Establece el rol del usuario
     * @param rol Rol a establecer
     */
    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
