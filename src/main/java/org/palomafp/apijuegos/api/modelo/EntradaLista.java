package org.palomafp.apijuegos.api.modelo;

import org.palomafp.apijuegos.api.modelo.enums.Estado;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Clase que representa una entrada en la lista de videojuegos de un usuario
 * @author Andrés López
 */
@Document(collection = "EntradaLista")
public class EntradaLista {
    @Id
    private String id;

    private long miId;
    private int horasJugadas;
    private double nota;
    private String resenya;
    private Estado estado;
    private long idVideojuego;
    private int idUsuario;

    /**
     * Constructor por defecto
     */
    public EntradaLista() {}

    /**
     * Constructor con parametros
     * @param miId          Id interno de la entrada
     * @param horasJugadas  Horas jugadas
     * @param nota          Nota del videojuego
     * @param resenya       Reseña escrita
     * @param estado        Estado actual del juego en la lista
     * @param idVideojuego  Id del videojuego asociado
     * @param idUsuario     Id del usuario asociado
     */
    public EntradaLista(long miId, int horasJugadas, double nota, String resenya, Estado estado, int idVideojuego, int idUsuario) {
        setMiId(miId);
        setHorasJugadas(horasJugadas);
        setNota(nota);
        setResenya(resenya);
        setEstado(estado);
        setIdVideojuego(idVideojuego);
        setIdUsuario(idUsuario);
    }

    /**
     * Obtiene el id del usuario
     * @return Id del usuario
     */
    public int getIdUsuario() {
        return idUsuario;
    }

    /**
     * Establece el id del usuario
     * @param idUsuario Id del usuario a establecer
     */
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    /**
     * Obtiene el id del videojuego
     * @return Id del videojuego
     */
    public long getIdVideojuego() {
        return idVideojuego;
    }

    /**
     * Establece el id del videojuego
     * @param idVideojuego Id del videojuego a establecer
     */
    public void setIdVideojuego(long idVideojuego) {
        this.idVideojuego = idVideojuego;
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
    public long getMiId() {
        return miId;
    }

    /**
     * Establece el id interno
     * @param miId Id interno a establecer
     */
    public void setMiId(long miId) {
        this.miId = miId;
    }

    /**
     * Obtiene las horas jugadas
     * @return Horas jugadas
     */
    public int getHorasJugadas() {
        return horasJugadas;
    }

    /**
     * Establece las horas jugadas
     * @param horasJugadas Horas a establecer
     */
    public void setHorasJugadas(int horasJugadas) {
        if (horasJugadas < 0) {
            throw new IllegalArgumentException("Horas jugadas negativas");
        }
        this.horasJugadas = horasJugadas;
    }

    /**
     * Obtiene la nota
     * @return Nota asignada
     */
    public double getNota() {
        return nota;
    }

    /**
     * Establece la nota
     * @param nota Nota a establecer
     */
    public void setNota(double nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("La nota debe de estar entre 0 y 10");
        }
        this.nota = nota;
    }

    /**
     * Obtiene la reseña
     * @return Reseña
     */
    public String getResenya() {
        return resenya;
    }

    /**
     * Establece la reseña
     * @param resenya Reseña a establecer
     */
    public void setResenya(String resenya) {
        this.resenya = resenya;
    }

    /**
     * Obtiene el estado
     * @return Estado en la lista
     */
    public Estado getEstado() {
        return estado;
    }

    /**
     * Establece el estado
     * @param estado Estado a establecer
     */
    public void setEstado(Estado estado) {
        this.estado = estado;
    }

}
