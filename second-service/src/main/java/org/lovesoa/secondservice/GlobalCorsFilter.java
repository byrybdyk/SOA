package org.lovesoa.secondservice;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.PreMatching;      // <-- правильный импорт
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;
import java.net.URISyntaxException;

@Provider
@PreMatching                               // <-- ключ: ловим OPTIONS до матчинга ресурсов
@Priority(Priorities.AUTHENTICATION - 1)
public class GlobalCorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final boolean ALLOW_CREDENTIALS = false;

    private static final String DEFAULT_ALLOWED_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD";
    private static final String BASE_ALLOWED_HEADERS   = "Content-Type, Authorization, X-Requested-With, Accept";
    private static final String EXPOSE_HEADERS         = "Location, Content-Disposition";

    private static boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) return false;
        try {
            URI u = new URI(origin);
            String scheme = u.getScheme();
            String host   = u.getHost();
            return "http".equalsIgnoreCase(scheme)
                    && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // PRE-FLIGHT (OPTIONS)
    @Override
    public void filter(ContainerRequestContext req) {
        if (!"OPTIONS".equalsIgnoreCase(req.getMethod())) return;

        String origin    = req.getHeaderString("Origin");
        String reqMethod = req.getHeaderString("Access-Control-Request-Method");
        String reqHdrs   = req.getHeaderString("Access-Control-Request-Headers");

        if (!isAllowedOrigin(origin)) {
            req.abortWith(Response.status(Response.Status.NO_CONTENT).build());
            return;
        }

        Response.ResponseBuilder rb = Response.noContent()
                .header("Access-Control-Allow-Origin", origin)
                .header("Vary", "Origin")
                .header("Access-Control-Max-Age", "86400")
                .header("Access-Control-Allow-Methods",
                        (reqMethod != null && !reqMethod.isBlank()) ? reqMethod : DEFAULT_ALLOWED_METHODS)
                .header("Access-Control-Allow-Headers",
                        (reqHdrs != null && !reqHdrs.isBlank()) ? (reqHdrs + ", " + BASE_ALLOWED_HEADERS) : BASE_ALLOWED_HEADERS);

        if (ALLOW_CREDENTIALS) rb.header("Access-Control-Allow-Credentials", "true");

        // поддержка приватной сети (Chrome dev)
        if ("true".equalsIgnoreCase(req.getHeaderString("Access-Control-Request-Private-Network"))) {
            rb.header("Access-Control-Allow-Private-Network", "true");
        }

        req.abortWith(rb.build());
    }

    // Обычные ответы (GET/POST/…)
    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        String origin = req.getHeaderString("Origin");
        if (!isAllowedOrigin(origin)) return;

        res.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
        res.getHeaders().putSingle("Vary", "Origin");
        if (ALLOW_CREDENTIALS) {
            res.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        }
        res.getHeaders().putSingle("Access-Control-Expose-Headers", EXPOSE_HEADERS);
    }
}
