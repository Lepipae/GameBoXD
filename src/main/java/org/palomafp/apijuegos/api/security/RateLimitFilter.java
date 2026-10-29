package org.palomafp.apijuegos.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de limitación de tasa (rate limiting) aplicado únicamente al endpoint
 * de login. Protege frente a ataques de fuerza bruta limitando a 5 intentos por
 * cada 60 segundos por dirección IP de cliente.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L;

    /** Mapa en memoria: IP del cliente -> lista de timestamps de peticiones. */
    private final ConcurrentHashMap<String, List<Long>> requests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Solo aplica a POST /api/auth/login
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/auth/login".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        List<Long> timestamps = requests.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (timestamps) {
            // Limpieza de timestamps anteriores a la ventana de 60s
            timestamps.removeIf(t -> (now - t) > WINDOW_MS);

            if (timestamps.size() >= MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Demasiados intentos. Inténtalo más tarde.\"}");
                return; // no continuar la cadena
            }

            timestamps.add(now);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Obtiene la IP del cliente. Prioriza la cabecera X-Forwarded-For si está
     * presente (proxy/load balancer), y si no usa getRemoteAddr().
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return (remote != null) ? remote : "unknown";
    }
}