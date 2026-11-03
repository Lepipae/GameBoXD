package org.palomafp.apijuegos.api.services;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Descarga, redimensiona y cachea las portadas de los juegos.
 *
 * <p>Las 198 portadas del catálogo viven en SteamGridDB como PNG de 600x900,
 * y pesan ~730 KB cada una: cuatro tarjetas en la portada eran ~2,9 MB, más
 * que todas las respuestas de la API juntas. Servidas a 300 px y en JPEG
 * bajan a ~30 KB (24 veces menos) sin diferencia visible en una tarjeta.</p>
 *
 * <p>Por seguridad solo se aceptan URLs del CDN de SteamGridDB: el endpoint es
 * público y no debe convertirse en un proxy abierto (SSRF).</p>
 *
 * @author Andrés López
 */
@Service
public class ImagenService {

    /** Único host aceptado: es el que usan todas las portadas del catálogo. */
    private static final String HOST_PERMITIDO = "cdn2.steamgriddb.com";

    /** Ancho mínimo aceptado, en píxeles. */
    private static final int ANCHO_MINIMO = 80;

    /** Ancho máximo aceptado, en píxeles (una ficha usa 600). */
    private static final int ANCHO_MAXIMO = 800;

    /** Ancho por defecto: suficiente para una tarjeta a 2x. */
    public static final int ANCHO_POR_DEFECTO = 300;

    /** Calidad JPEG usada al recomprimir. */
    private static final float CALIDAD_JPEG = 0.82f;

    /** Tamaño máximo del archivo remoto que se acepta descargar. */
    private static final int MAXIMO_BYTES_DESCARGA = 8 * 1024 * 1024;

    /** Número máximo de imágenes en la caché en memoria. */
    private static final int MAXIMO_ENTRADAS_CACHE = 400;

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Caché LRU: la portada de un juego se pide muchas veces (index, búsqueda,
     * lista) y siempre con el mismo ancho, así que recomprimirla en cada
     * petición sería tirar CPU.
     */
    private final Map<String, byte[]> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAXIMO_ENTRADAS_CACHE;
        }
    };

    /**
     * Indica si la URL apunta al CDN permitido.
     *
     * @param url URL de la portada.
     * @return true si el host está en la lista blanca.
     */
    public static boolean esUrlPermitida(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String esquema = uri.getScheme();
            return ("http".equals(esquema) || "https".equals(esquema))
                    && HOST_PERMITIDO.equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Ajusta el ancho pedido a un rango razonable.
     *
     * @param ancho Ancho pedido por el cliente (puede ser nulo o absurdo).
     * @return Ancho efectivo entre el mínimo y el máximo.
     */
    public static int normalizarAncho(int ancho) {
        if (ancho <= 0) {
            return ANCHO_POR_DEFECTO;
        }
        return Math.max(ANCHO_MINIMO, Math.min(ancho, ANCHO_MAXIMO));
    }

    /**
     * Devuelve la portada redimensionada en JPEG.
     *
     * <p>Si la imagen ya está en caché no se toca la red. Si la descarga o la
     * conversión fallan se propaga {@link IOException} para que el controlador
     * responda 404 y el frontend caiga a la URL original.</p>
     *
     * @param url   URL de la portada (debe pasar {@link #esUrlPermitida(String)}).
     * @param ancho Ancho deseado en píxeles.
     * @return Bytes de la imagen JPEG.
     * @throws IOException Si no se pudo descargar o convertir.
     */
    public byte[] obtener(String url, int ancho) throws IOException {
        int anchoEfectivo = normalizarAncho(ancho);
        String clave = anchoEfectivo + "|" + url;

        synchronized (cache) {
            byte[] guardada = cache.get(clave);
            if (guardada != null) {
                return guardada;
            }
        }

        byte[] original = descargar(url);
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(original));
        if (imagen == null) {
            throw new IOException("El recurso remoto no es una imagen legible");
        }

        byte[] recomprimida = redimensionar(imagen, anchoEfectivo);

        synchronized (cache) {
            cache.put(clave, recomprimida);
        }
        return recomprimida;
    }

    /**
     * Descarga la imagen remota con límite de tamaño y de tiempo.
     *
     * @param url URL a descargar.
     * @return Bytes de la imagen original.
     * @throws IOException Si la respuesta no es correcta o excede el límite.
     */
    private byte[] descargar(String url) throws IOException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url.trim()))
                .timeout(Duration.ofSeconds(6))
                .header("User-Agent", "GameBoXD/1.0 (image resizer)")
                .GET()
                .build();

        HttpResponse<byte[]> respuesta;
        try {
            respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Descarga interrumpida", ex);
        }

        if (respuesta.statusCode() != 200) {
            throw new IOException("El CDN respondió " + respuesta.statusCode());
        }
        byte[] cuerpo = respuesta.body();
        if (cuerpo == null || cuerpo.length == 0) {
            throw new IOException("Respuesta vacía del CDN");
        }
        if (cuerpo.length > MAXIMO_BYTES_DESCARGA) {
            throw new IOException("La imagen remota excede el tamaño permitido");
        }
        return cuerpo;
    }

    /**
     * Escala la imagen al ancho pedido conservando la proporción y la guarda
     * como JPEG.
     *
     * <p>Se compone sobre blanco porque JPEG no admite transparencia: sin esto
     * las zonas transparentes saldrían negras.</p>
     *
     * @param original Imagen descargada.
     * @param ancho    Ancho destino.
     * @return Bytes JPEG.
     * @throws IOException Si el codificador JPEG no está disponible.
     */
    private static byte[] redimensionar(BufferedImage original, int ancho) throws IOException {
        int anchoFinal = Math.min(ancho, original.getWidth());
        int altoFinal = Math.max(1,
                (int) Math.round(original.getHeight() * (anchoFinal / (double) original.getWidth())));

        BufferedImage destino = new BufferedImage(anchoFinal, altoFinal, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, anchoFinal, altoFinal);
            g.drawImage(original, 0, 0, anchoFinal, altoFinal, null);
        } finally {
            g.dispose();
        }

        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
        if (!escritores.hasNext()) {
            throw new IOException("No hay codificador JPEG disponible");
        }
        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPEG);
            escritor.write(null, new IIOImage(destino, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }
}
