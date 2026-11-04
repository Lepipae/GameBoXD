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

    /** Alto mínimo aceptado cuando se pide recorte, en píxeles. */
    private static final int ALTO_MINIMO = 80;

    /** Alto máximo aceptado cuando se pide recorte, en píxeles. */
    private static final int ALTO_MAXIMO = 800;

    /** Alto que significa "sin recorte": se conserva la proporción original. */
    public static final int ALTO_SIN_RECORTE = 0;

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
     * Ajusta el alto pedido a un rango razonable.
     *
     * <p>Un valor {@code <= 0} significa "sin recorte": la imagen conserva la
     * proporción del original (comportamiento histórico del endpoint).</p>
     *
     * @param alto Alto pedido por el cliente.
     * @return Alto efectivo, o {@link #ALTO_SIN_RECORTE} si no se pidió recorte.
     */
    public static int normalizarAlto(int alto) {
        if (alto <= 0) {
            return ALTO_SIN_RECORTE;
        }
        return Math.max(ALTO_MINIMO, Math.min(alto, ALTO_MAXIMO));
    }

    /**
     * Devuelve la portada redimensionada en JPEG conservando la proporción.
     *
     * @param url   URL de la portada.
     * @param ancho Ancho deseado en píxeles.
     * @return Bytes de la imagen JPEG.
     * @throws IOException Si no se pudo descargar o convertir.
     */
    public byte[] obtener(String url, int ancho) throws IOException {
        return obtener(url, ancho, ALTO_SIN_RECORTE);
    }

    /**
     * Devuelve la portada redimensionada en JPEG.
     *
     * <p>Si la imagen ya está en caché no se toca la red. Si la descarga o la
     * conversión fallan se propaga {@link IOException} para que el controlador
     * responda 404 y el frontend caiga a la URL original.</p>
     *
     * <p>Con {@code alto > 0} se recorta al centro hasta esa altura exacta. Es
     * lo que necesita el hero de la portada: pide 600x338 (16:9) una portada de
     * 600x900 y hoy se descarga entera para que `object-fit: cover` tire un
     * tercio del píxel y del ancho de banda.</p>
     *
     * @param url   URL de la portada (debe pasar {@link #esUrlPermitida(String)}).
     * @param ancho Ancho deseado en píxeles.
     * @param alto  Alto deseado en píxeles; {@code <= 0} conserva la proporción.
     * @return Bytes de la imagen JPEG.
     * @throws IOException Si no se pudo descargar o convertir.
     */
    public byte[] obtener(String url, int ancho, int alto) throws IOException {
        int anchoEfectivo = normalizarAncho(ancho);
        int altoEfectivo = normalizarAlto(alto);
        String clave = anchoEfectivo + "x" + altoEfectivo + "|" + url;

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

        byte[] recomprimida = altoEfectivo > 0
                ? recortar(imagen, anchoEfectivo, altoEfectivo)
                : redimensionar(imagen, anchoEfectivo);

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

        return comprimirJpeg(escalarProgresivo(original, anchoFinal, altoFinal));
    }

    /**
     * Reduce la imagen en varias pasadas de como mucho la mitad de tamaño.
     *
     * <p>Las portadas miden 600 px de ancho y una tarjeta pide 180-300. Escalar
     * 600 -> 180 en un solo `drawImage` bilineal solo mira 2x2 píxeles de origen
     * por píxel de destino, así que se salta la mayor parte de la imagen y
     * aparecen dientes de sierra. Promediando por mitades primero se conserva la
     * información de todos los píxeles y el resultado final es más nítido, sin
     * coste en bytes.</p>
     *
     * @param original Imagen de partida.
     * @param ancho    Ancho final.
     * @param alto     Alto final.
     * @return Imagen escalada.
     */
    private static BufferedImage escalarProgresivo(BufferedImage original, int ancho, int alto) {
        BufferedImage actual = original;
        while (actual.getWidth() / 2 >= ancho && actual.getHeight() / 2 >= alto) {
            actual = dibujar(actual, Math.max(ancho, actual.getWidth() / 2),
                    Math.max(alto, actual.getHeight() / 2));
        }
        return dibujar(actual, ancho, alto);
    }

    /**
     * Copia la imagen al tamaño indicado sobre fondo blanco.
     *
     * @param origen Imagen de partida.
     * @param ancho  Ancho de destino.
     * @param alto   Alto de destino.
     * @return Nueva imagen RGB.
     */
    private static BufferedImage dibujar(BufferedImage origen, int ancho, int alto) {
        if (origen.getWidth() == ancho && origen.getHeight() == alto) {
            return origen;
        }

        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ancho, alto);
            g.drawImage(origen, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    /**
     * Escala la imagen para cubrir el marco pedido y recorta el sobrante por el
     * centro (el equivalente a `object-fit: cover`).
     *
     * <p>Se recorta en el servidor y no en CSS porque el navegador descargaría
     * píxeles que nunca pinta: el hero pide 600x338 sobre portadas de 600x900,
     * así que dos tercios del archivo eran peso muerto.</p>
     *
     * @param original Imagen descargada.
     * @param ancho    Ancho exacto del resultado.
     * @param alto     Alto exacto del resultado.
     * @return Bytes JPEG de exactamente {@code ancho}x{@code alto}.
     * @throws IOException Si el codificador JPEG no está disponible.
     */
    private static byte[] recortar(BufferedImage original, int ancho, int alto) throws IOException {
        double escala = Math.max(ancho / (double) original.getWidth(),
                alto / (double) original.getHeight());
        int escaladoAncho = Math.max(ancho, (int) Math.round(original.getWidth() * escala));
        int escaladoAlto = Math.max(alto, (int) Math.round(original.getHeight() * escala));
        int desplazamientoX = (ancho - escaladoAncho) / 2;
        int desplazamientoY = (alto - escaladoAlto) / 2;

        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            // El clip del propio BufferedImage descarta lo que sobresale del marco.
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ancho, alto);
            g.drawImage(original, desplazamientoX, desplazamientoY, escaladoAncho, escaladoAlto, null);
        } finally {
            g.dispose();
        }

        return comprimirJpeg(destino);
    }

    /**
     * Codifica la imagen destino como JPEG con la calidad configurada.
     *
     * @param destino Imagen ya escalada y compuesta sobre blanco.
     * @return Bytes JPEG.
     * @throws IOException Si no hay codificador JPEG o falla la escritura.
     */
    private static byte[] comprimirJpeg(BufferedImage destino) throws IOException {
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
