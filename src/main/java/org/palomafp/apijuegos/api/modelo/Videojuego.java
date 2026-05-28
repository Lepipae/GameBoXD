package org.palomafp.apijuegos.api.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa a un videojuego
 * @author Andrés López
 */
@Document(collection = "Videojuegos")
public class Videojuego {
    @Id
    private String id;

    private Long miId;
    private String nombre;
    private String descripcion;
    private String urlImagen;
    private double notaMedia;
    private int idDesarrolladora;
    private List<String> tags;

    /**
     * Constructor por defecto
     */
    public Videojuego(){}

    /**
     * Constructor con parametros
     * @param id                Id de MongoDB
     * @param miId              Id interno de la base de datos
     * @param nombre            Nombre del videojuego
     * @param descripcion       Descripcion o sinopsis
     * @param urlImagen         URL de la portada
     * @param notaMedia         Nota media del juego
     * @param tags              Lista de categorias o tags
     * @param idDesarrolladora  Id de la desarrolladora
     */
    public Videojuego(String id, Long miId, String nombre, String descripcion, String urlImagen, double notaMedia, List<String> tags, int idDesarrolladora) {
        this.id = id;
        setMiId(miId);
        setNombre(nombre);
        setDescripcion(descripcion);
        setUrlImagen(urlImagen);
        setNotaMedia(notaMedia);
        setTags(tags);
        setIdDesarrolladora(idDesarrolladora);
    }

    /**
     * Establece el id de la desarrolladora
     * @param idDesarrolladora Id de la desarrolladora a establecer
     */
    public void setIdDesarrolladora(int idDesarrolladora) {
        this.idDesarrolladora = idDesarrolladora;
    }

    /**
     * Obtiene el id de la desarrolladora
     * @return Id de la desarrolladora
     */
    public int getIdDesarrolladora() {
        return idDesarrolladora;
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
     * Obtiene el id interno
     * @return Id interno
     */
    public Long getMiId() {
        return miId;
    }

    /**
     * Establece el id interno
     * @param miId Id interno a establecer
     */
    public void setMiId(Long miId) {
        this.miId = miId;
    }

    /**
     * Obtiene el nombre del videojuego
     * @return Nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del videojuego
     * @param nombre Nombre a establecer
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es requerido");
        }
        this.nombre = nombre;
    }

    /**
     * Obtiene la descripcion del videojuego
     * @return Descripcion
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Establece la descripcion del videojuego
     * @param descripcion Descripcion a establecer
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Obtiene la url de la portada
     * @return URL de la imagen
     */
    public String getUrlImagen() {
        return urlImagen;
    }

    /**
     * Establece la url de la portada
     * @param urlImagen URL a establecer
     */
    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    /**
     * Obtiene la nota media
     * @return Nota media
     */
    public double getNotaMedia() {
        return notaMedia;
    }

    /**
     * Establece la nota media
     * @param notaMedia Nota a establecer
     */
    public void setNotaMedia(double notaMedia) {
        this.notaMedia = notaMedia;
    }

    /**
     * Obtiene la lista de tags
     * @return Tags o categorias
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Establece la lista de tags
     * @param tags Tags a establecer
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
