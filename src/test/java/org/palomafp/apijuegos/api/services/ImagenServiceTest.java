package org.palomafp.apijuegos.api.services;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
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
        servidor.start();
        urlPortada = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/portada.png";
        imagenService = new ImagenService();
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
