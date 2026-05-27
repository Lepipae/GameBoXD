package org.palomafp.apijuegos.api.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Clase que representa a una desarrolladora de videojuegos
 * 
 * @author Andrés López
 */
@Document(collection = "Desarrolladoras")
public class Desarrolladora {
    @Id
    private String id;

    private int miId;
    private String urlImagen;
    private String nombre;
    private String pais;

    /**
     * Constructor por defecto
     */
    public Desarrolladora() {
    }

    /**
     * Constructor con parametros
     * 
     * @param id        Id de MongoDB
     * @param pais      Pais de origen de la desarrolladora
     * @param nombre    Nombre de la desarrolladora
     * @param urlImagen URL de la imagen o logo
     * @param miId      Id interno de la base de datos
     */
    public Desarrolladora(String id, String pais, String nombre, String urlImagen, int miId) {
        this.id = id;
        setPais(pais);
        setNombre(nombre);
        setUrlImagen(urlImagen);
        setMiId(miId);
    }

    /**
     * Establece el id de MongoDB
     * 
     * @param id Id a establecer
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtiene el id de MongoDB
     * 
     * @return Id de MongoDB
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene el id interno
     * 
     * @return Id interno
     */
    public int getMiId() {
        return miId;
    }

    /**
     * Establece el id interno
     * 
     * @param miId Id interno a establecer
     */
    public void setMiId(int miId) {
        this.miId = miId;
    }

    /**
     * Obtiene la url de la imagen
     * 
     * @return URL de la imagen
     */
    public String getUrlImagen() {
        return urlImagen;
    }

    /**
     * Establece la url de la imagen
     * 
     * @param urlImagen URL a establecer
     */
    public void setUrlImagen(String urlImagen) {
        if (urlImagen == null || urlImagen.isBlank()) {
            this.urlImagen = "placeholder";
        } else {
            this.urlImagen = urlImagen;
        }
    }

    /**
     * Obtiene el nombre de la desarrolladora
     * 
     * @return Nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de la desarrolladora
     * 
     * @param nombre Nombre a establecer
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Nombre no valido");
        }
        this.nombre = nombre;
    }

    /**
     * Obtiene el pais de la desarrolladora
     * 
     * @return Pais
     */
    public String getPais() {
        return pais;
    }

    /**
     * Establece el pais de la desarrolladora
     * 
     * @param pais Pais a establecer
     */
    public void setPais(String pais) {
        if (pais == null || pais.isBlank()) {
            throw new IllegalArgumentException("El pais es necesario");
        }
        this.pais = pais;
    }
    // comentario para probar el push de actions 6 :)
}
