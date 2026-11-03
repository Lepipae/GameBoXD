package org.palomafp.apijuegos.api.services;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caché en memoria de respuestas de lectura costosas.
 *
 * <p>La latencia de esta API la marca el viaje a MongoDB Atlas (~60-250 ms por
 * consulta). Las lecturas de catálogo que alimentan la portada (hero,
 * recomendaciones) se piden en cada visita y su resultado cambia muy poco, así
 * que recalcularlas en cada petición es tirar tiempo de espera del usuario.</p>
 *
 * <p>Cada entrada tiene su propio tiempo de vida y se puede invalidar por
 * prefijo cuando se escribe en la colección de la que sale. Si dos peticiones
 * piden la misma clave a la vez, como mucho se calcula dos veces: no se
 * bloquea a nadie esperando a otro hilo.</p>
 *
 * @author Andrés López
 */
@Component
public class CacheTemporal {

    /**
     * Entrada guardada: valor + instante de expiración.
     *
     * @param valor    Resultado cacheado (puede ser una lista vacía, no null).
     * @param expiraEn Momento (epoch ms) a partir del cual ya no vale.
     * @param <T>      Tipo del valor.
     */
    private record Entrada<T>(T valor, long expiraEn) {
    }

    /** Almacén de entradas por clave. */
    private final Map<String, Entrada<?>> entradas = new ConcurrentHashMap<>();

    /**
     * Devuelve el valor cacheado de una clave o lo calcula si no existe o caducó.
     *
     * @param clave    Identificador de la entrada (se invalida por prefijo).
     * @param ttl      Tiempo de vida de la entrada.
     * @param calculo  Cómo obtener el valor si no está en caché.
     * @param <T>      Tipo del valor.
     * @return Valor cacheado o recién calculado.
     */
    public <T> T obtener(String clave, Duration ttl, Supplier<T> calculo) {
        long ahora = System.currentTimeMillis();

        Entrada<?> guardada = entradas.get(clave);
        if (guardada != null && guardada.expiraEn() > ahora) {
            @SuppressWarnings("unchecked")
            T valor = (T) guardada.valor();
            return valor;
        }

        T valor = calculo.get();
        // No se cachea null: significaría "no hay datos todavía" y bloquearía
        // la aparición del primer resultado durante todo el TTL. Además
        // ConcurrentHashMap no admite valores nulos.
        if (valor != null) {
            entradas.put(clave, new Entrada<>(valor, ahora + ttl.toMillis()));
        }
        return valor;
    }

    /**
     * Borra todas las entradas cuya clave empiece por alguno de los prefijos.
     *
     * <p>Se llama al guardar o borrar: una escritura invalida tanto el listado
     * como cualquier variante (por ejemplo, las recomendaciones de cada
     * tamaño).</p>
     *
     * @param prefijos Prefijos de clave a descartar.
     */
    public void invalidar(String... prefijos) {
        if (prefijos == null || prefijos.length == 0) {
            entradas.clear();
            return;
        }
        entradas.keySet().removeIf(clave -> {
            for (String prefijo : prefijos) {
                if (prefijo != null && clave.startsWith(prefijo)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Número de entradas guardadas (incluidas las caducadas). Útil en tests.
     *
     * @return Tamaño actual del almacén.
     */
    public int tamano() {
        return entradas.size();
    }
}
