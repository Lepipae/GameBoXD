package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.repositories.EntradaListaRepo;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio que gestiona la logica de negocio de EntradaLista
 *
 * @author Andrés López
 */
@Service
public class EntradaListaService {

    private final EntradaListaRepo entradaListaRepo; // Repo de EntradaLista
    private final VideojuegoRepo videojuegoRepo;       // Repo de Videojuego

    /**
     * Inyección por constructor: ambas dependencias quedan {@code final}.
     *
     * @param entradaListaRepo Repositorio de entradas de lista.
     * @param videojuegoRepo    Repositorio de videojuegos, para recalcular la nota media.
     */
    public EntradaListaService(EntradaListaRepo entradaListaRepo, VideojuegoRepo videojuegoRepo) {
        this.entradaListaRepo = entradaListaRepo;
        this.videojuegoRepo = videojuegoRepo;
    }

    /**
     * Actualiza la nota media de un videojuego basado en sus entradas de lista
     *
     * <p>Solo entran en la media las notas estrictamente mayores que 0: una entrada
     * sin puntuar no debe arrastrar la nota del juego hacia abajo. El resultado se
     * redondea a dos decimales para no arrastrar errores de coma flotante al JSON.</p>
     *
     * @param idVideojuego Id del videojuego
     */
    private void actualizarNotaMedia(long idVideojuego) {
        // Optional por el contrato del repositorio: findByIdVideojuego puede devolver
        // null, y antes estaba protegido con un if que ocupaba medio método.
        var notasPuestas = Optional.ofNullable(entradaListaRepo.findByIdVideojuego(idVideojuego))
                .orElseGet(List::of)
                .stream()
                .map(EntradaLista::getNota)
                .filter(nota -> nota > 0.0)
                .toList();

        double notaMedia = notasPuestas.isEmpty()
                ? 0.0
                : Math.round(notasPuestas.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0) / 100.0;

        Videojuego videojuego = videojuegoRepo.findByMiId(idVideojuego);
        if (videojuego != null) {
            videojuego.setNotaMedia(notaMedia);
            videojuegoRepo.save(videojuego);
        }
    }

    /**
     * Obtiene las entradas de la lista pertenecientes a un usuario
     *
     * @param id Id del usuario
     * @return Lista de entradas del usuario
     */
    public List<EntradaLista> findByIdUsuario(int id) {
        return entradaListaRepo.findByIdUsuario(id);
    }

    /**
     * Obtiene las entradas de la lista pertenecientes a un videojuego
     * @param idVideojuego Id del videojuego
     * @return Lista de entradas del videojuego
     */
    public List<EntradaLista> findByIdVideojuego(long idVideojuego) {
        return entradaListaRepo.findByIdVideojuego(idVideojuego);
    }

    /**
     * Devuelve el videojuego al que pertenecen más entradas de lista (el más
     * "reseñado" de cara al hero del index).
     *
     * <p>Cuentan todas las entradas, aunque no lleven texto de reseña: una
     * entrada con solo nota u horas ya es un jugador que ha traté ese juego.
     * En empate se queda con el menor id de juego para que el resultado sea
     * estable entre consultas (mismos datos, mismo héroe).</p>
     *
     * @return El juego con más entradas, o null si todavía no hay ninguna.
     */
    public Videojuego juegoConMasEntradas() {
        List<EntradaLista> entradas = entradaListaRepo.findAll();
        if (entradas == null || entradas.isEmpty()) {
            return null;
        }

        Map<Long, Long> entradasPorJuego = new HashMap<>();
        for (EntradaLista entrada : entradas) {
            entradasPorJuego.merge(entrada.getIdVideojuego(), 1L, Long::sum);
        }

        long idLider = -1;
        long maximo = -1;
        for (Map.Entry<Long, Long> cuenta : entradasPorJuego.entrySet()) {
            if (cuenta.getValue() > maximo || (cuenta.getValue() == maximo && cuenta.getKey() < idLider)) {
                maximo = cuenta.getValue();
                idLider = cuenta.getKey();
            }
        }

        return videojuegoRepo.findByMiId(idLider);
    }

    /**
     * Obtiene una entrada de la lista a partir de su id interno
     * @param id Id interno
     * @return EntradaLista encontrada o null
     */
    public EntradaLista findById(long id) {
        return entradaListaRepo.findByMiId(id);
    }

    /**
     * Borra una entrada a partir de su id interno
     *
     * @param id Id interno
     */
    public void borrarEntrada(int id) {
        EntradaLista entrada = entradaListaRepo.findByMiId(id);
        if (entrada != null) {
            long idVideojuego = entrada.getIdVideojuego();
            entradaListaRepo.deleteByMiId(id);
            actualizarNotaMedia(idVideojuego);
        }
    }

    /**
     * Borra las entradas asociadas a un videojuego
     *
     * @param idVideojuego Id del videojuego
     */
    public void borrarPorVideojuego(long idVideojuego) {
        entradaListaRepo.deleteByIdVideojuego(idVideojuego);
    }

    /**
     * Borra las entradas asociadas a un usuario
     *
     * @param idUsuario Id del usuario
     */
    public void borrarPorUsuario(int idUsuario) {
        // Se leen ANTES de borrar, porque hay que recalcular la media de cada juego
        // afectado y, una vez borradas, ya no sabríamos cuáles son.
        var idsAfectados = Optional.ofNullable(entradaListaRepo.findByIdUsuario(idUsuario))
                .orElseGet(List::of)
                .stream()
                .map(EntradaLista::getIdVideojuego)
                .distinct()
                .toList();

        entradaListaRepo.deleteByIdUsuario(idUsuario);

        // El borrado va antes del recálculo a propósito: actualizarNotaMedia vuelve a
        // leer las entradas del juego, y si se calculara con las entradas aún
        // presentes la notaMedia del videojuego se quedaría con el valor viejo.
        idsAfectados.forEach(this::actualizarNotaMedia);
    }

    /**
     * Guarda una entrada en la base de datos
     * @param entradaLista Objeto a guardar
     * @return EntradaLista guardada
     * @throws RecursoDuplicadoException si el usuario ya tiene ese juego en su lista
     */
    public EntradaLista guardar(EntradaLista entradaLista) {
        // Normalizar ID vacío a null para evitar problemas en MongoDB
        if (entradaLista.getId() != null && entradaLista.getId().isBlank()) {
            entradaLista.setId(null);
        }

        // Comprobación de duplicados robusta en memoria
        boolean duplicada = Optional.ofNullable(entradaListaRepo.findByIdUsuario(entradaLista.getIdUsuario()))
                .orElseGet(List::of)
                .stream()
                .anyMatch(existente -> existente.getIdVideojuego() == entradaLista.getIdVideojuego()
                        && (entradaLista.getId() == null || !existente.getId().equals(entradaLista.getId())));

        if (duplicada) {
            throw new RecursoDuplicadoException("El usuario ya tiene este juego en su lista");
        }

        if (entradaLista.getId() == null) {
            EntradaLista ultimo = entradaListaRepo.encontrarUltimoId();
            entradaLista.setMiId(ultimo != null ? ultimo.getMiId() + 1 : 1);
        }

        EntradaLista guardada = entradaListaRepo.save(entradaLista);
        actualizarNotaMedia(guardada.getIdVideojuego());
        return guardada;
    }
}
