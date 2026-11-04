package org.palomafp.apijuegos.api.services;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El redimensionador tiene que devolver exactamente el marco pedido cuando se
 * pide recorte: el hero pinta 580x326 pero descargaba 600x900 y tiraba dos
 * tercios del archivo con `object-fit: cover`.
 *
 * <p>La prueba se hace contra un servidor HTTP local que sirve una portada
 * sintética de 600x900, así que no depende de SteamGridDB ni de la red.</p>
 */
class ImagenServiceTest {

    private HttpServer servidor;
    private String urlPortada;
    private String urlRayas;
    private final AtomicInteger peticiones = new AtomicInteger();
    private ImagenService imagenService;

    /** PNG de 600x900 con una franja de color distinta en cada tercio vertical. */
    private static byte[] portadaSintetica() throws IOException {
        BufferedImage imagen = new BufferedImage(600, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 600, 300);
            g.setColor(Color.GREEN);
            g.fillRect(0, 300, 600, 300);
            g.setColor(Color.BLUE);
            g.fillRect(0, 600, 600, 300);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    @BeforeEach
    void levantarServidor() throws IOException {
        byte[] png = portadaSintetica();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/portada.png", intercambio -> {
            peticiones.incrementAndGet();
            intercambio.sendResponseHeaders(200, png.length);
            try (OutputStream cuerpo = intercambio.getResponseBody()) {
                cuerpo.write(png);
            }
        });
        // Patrón de alta frecuencia (rayas de 1 px) para comprobar el escalado.
        byte[] rayas = rayasSinteticas();
        servidor.createContext("/rayas.png", intercambio -> {
            intercambio.sendResponseHeaders(200, rayas.length);
            try (OutputStream cuerpo = intercambio.getResponseBody()) {
                cuerpo.write(rayas);
            }
        });
        servidor.start();
        urlPortada = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/portada.png";
        urlRayas = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/rayas.png";
        imagenService = new ImagenService();
    }

    /** PNG de 600x900 con rayas verticales alternas de 1 px. */
    private static byte[] rayasSinteticas() throws IOException {
        BufferedImage imagen = new BufferedImage(600, 900, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 600; x++) {
            int tono = (x % 2 == 0) ? 0 : 255;
            for (int y = 0; y < 900; y++) {
                imagen.setRGB(x, y, (tono << 16) | (tono << 8) | tono);
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    @AfterEach
    void pararServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private static BufferedImage decodificar(byte[] jpeg) throws IOException {
        BufferedImage imagen = ImageIO.read(new java.io.ByteArrayInputStream(jpeg));
        assertNotNull(imagen, "el resultado debe ser una imagen legible");
        return imagen;
    }

    /**
     * Desviación típica de la luminosidad media de cada columna.
     *
     * <p>Sirve para detectar banding: una imagen uniforme da ~0 y una con rayas
     * alternas da un valor alto.</p>
     *
     * @param imagen Imagen a medir.
     * @return Desviación típica de las medias por columna.
     */
    private static double desviacionColumnas(BufferedImage imagen) {
        double[] medias = new double[imagen.getWidth()];
        for (int x = 0; x < imagen.getWidth(); x++) {
            double suma = 0;
            for (int y = 0; y < imagen.getHeight(); y++) {
                int rgb = imagen.getRGB(x, y);
                suma += ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3.0;
            }
            medias[x] = suma / imagen.getHeight();
        }
        double media = 0;
        for (double valor : medias) {
            media += valor;
        }
        media /= medias.length;

        double varianza = 0;
        for (double valor : medias) {
            varianza += (valor - media) * (valor - media);
        }
        return Math.sqrt(varianza / medias.length);
    }

    @Test
    void normalizarAltoTrataCeroComoSinRecorteYAcotaElResto() {
        assertEquals(ImagenService.ALTO_SIN_RECORTE, ImagenService.normalizarAlto(0));
        assertEquals(ImagenService.ALTO_SIN_RECORTE, ImagenService.normalizarAlto(-40));
        assertEquals(80, ImagenService.normalizarAlto(10));
        assertEquals(338, ImagenService.normalizarAlto(338));
        assertEquals(800, ImagenService.normalizarAlto(5000));
    }

    @Test
    void conAltoDevuelveExactamenteElMarcoPedido() throws IOException {
        byte[] jpeg = imagenService.obtener(urlPortada, 600, 338);

        BufferedImage resultado = decodificar(jpeg);
        assertEquals(600, resultado.getWidth());
        assertEquals(338, resultado.getHeight());
    }

    @Test
    void elRecorteEsCentralYNoEstiraLaImagen() throws IOException {
        // Escala = max(600/600, 300/900)=1 sobre una fuente de 600x900:
        // el marco de 600x300 debe caer en el tercio central (verde).
        BufferedImage resultado = decodificar(imagenService.obtener(urlPortada, 600, 300));

        int centro = resultado.getRGB(300, 150);
        Color color = new Color(centro);
        assertTrue(color.getGreen() > 150 && color.getRed() < 120 && color.getBlue() < 120,
                "el centro debe venir del tercio verde, no del rojo ni del azul: " + color);
    }

    @Test
    void sinAltoConservaLaProporcionOriginal() throws IOException {
        byte[] jpeg = imagenService.obtener(urlPortada, 300, 0);

        BufferedImage resultado = decodificar(jpeg);
        assertEquals(300, resultado.getWidth());
        // 600x900 escalado a 300 de ancho -> 450 de alto.
        assertEquals(450, resultado.getHeight());
    }

    @Test
    void reducirEnVariosPasosEvitaElBandingDeUnaSolaPasada() throws IOException {
        // 600 -> 180 en un único `drawImage` bilineal solo promedia 2x2 píxeles
        // de origen, así que las rayas de 1 px reaparecen como bandas (moiré).
        // Escalando por mitades se promedian todos los píxeles y el resultado
        // es casi uniforme: mismo peso, más nitidez.
        BufferedImage origen = ImageIO.read(new java.io.ByteArrayInputStream(rayasSinteticas()));

        BufferedImage unaPasada = new BufferedImage(180, 270, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = unaPasada.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(origen, 0, 0, 180, 270, null);
        } finally {
            g.dispose();
        }

        BufferedImage progresivo = decodificar(imagenService.obtener(urlRayas, 180));
        assertEquals(180, progresivo.getWidth());
        assertEquals(270, progresivo.getHeight());

        double bandingUnaPasada = desviacionColumnas(unaPasada);
        double bandingProgresivo = desviacionColumnas(progresivo);
        assertTrue(bandingProgresivo < bandingUnaPasada / 2,
                "el escalado por mitades debe dejar menos banding que una sola pasada: "
                        + bandingUnaPasada + " -> " + bandingProgresivo);
    }

    @Test
    void laSegundaPeticionSaleDeLaCacheSinTocarLaRed() throws IOException {
        imagenService.obtener(urlPortada, 600, 338);
        imagenService.obtener(urlPortada, 600, 338);

        assertEquals(1, peticiones.get(), "la portada ya convertida no se vuelve a descargar");
    }

    @Test
    void recorteYOriginalTienenClavesDeCacheDistintas() throws IOException {
        byte[] recortada = imagenService.obtener(urlPortada, 600, 338);
        byte[] entera = imagenService.obtener(urlPortada, 600);

        assertNotEquals(recortada.length, entera.length);
        assertEquals(338, decodificar(recortada).getHeight());
        assertEquals(900, decodificar(entera).getHeight());
        assertEquals(2, peticiones.get(), "cada variante se descarga una vez");
    }

    @Test
    void soloSeAceptaElCdnDePortadas() {
        assertTrue(ImagenService.esUrlPermitida("https://cdn2.steamgriddb.com/grid/abc.png"));
        assertFalse(ImagenService.esUrlPermitida("http://127.0.0.1:8080/portada.png"));
        assertFalse(ImagenService.esUrlPermitida("https://ejemplo.com/portada.png"));
        assertFalse(ImagenService.esUrlPermitida(""));
        assertFalse(ImagenService.esUrlPermitida(null));
    }
}
