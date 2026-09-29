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

    private Integer miId;
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
    public Desarrolladora(String id, String pais, String nombre, String urlImagen, Integer miId) {
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
    public Integer getMiId() {
        return miId;
    }

    /**
     * Establece el id interno
     * 
     * @param miId Id interno a establecer
     */
    public void setMiId(Integer miId) {
        this.miId = miId;
    }

    /**
     * Obtiene la url de la imagen
     *
     * <p>Los registros antiguos guardaban el texto {@code "placeholder"} en este
     * campo. Se devuelve como {@code null} para que ese valor heredado no llegue
     * a la API ni al frontend. La fila antigua se corrige sola la proxima vez que
     * se guarde la desarrolladora.</p>
     *
     * @return URL de la imagen, o null si no tiene logo
     */
    public String getUrlImagen() {
        if (urlImagen == null || ValidadorUrlImagen.VALOR_HEREDADO.equalsIgnoreCase(urlImagen.trim())) {
            return null;
        }
        return urlImagen;
    }

    /**
     * Establece la url de la imagen
     * 
     * @param urlImagen URL a establecer
     */
    public void setUrlImagen(String urlImagen) {
        this.urlImagen = ValidadorUrlImagen.normalizar(urlImagen);
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
