package com.aeracraft.report.rest;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.rest.auth.TokenAuthenticator;
import com.aeracraft.report.rest.handler.PunishmentHandler;
import com.aeracraft.report.rest.handler.ReportHandler;
import com.aeracraft.report.rest.handler.StatsHandler;
import spark.Request;
import spark.Service;
import static spark.Spark.halt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class RestServer {

    private final AeracraftReport plugin;
    private Service server;
    private boolean running;
    private final Map<String, RateLimit> rateLimits = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_WINDOW_MS = 60000;
    private static final int MAX_REQUEST_SIZE = 1024 * 100;

    public RestServer(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (running) {
            return;
        }

        int port = plugin.getConfigManager().getRestApiPort();
        String token = plugin.getConfigManager().getRestApiToken();

        if (token == null || token.isEmpty()) {
            plugin.getLogger().severe("REST API token 未配置，无法启动 REST API");
            return;
        }

        server = Service.ignite();
        server.port(port);

        server.before((request, response) -> {
            response.type("application/json");
            
            String allowedOrigin = plugin.getConfig().getString("rest-api.allowed-origin", "");
            if (!allowedOrigin.isEmpty()) {
                response.header("Access-Control-Allow-Origin", allowedOrigin);
            }
            response.header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Authorization, Content-Type");
            response.header("Content-Security-Policy", "default-src 'none'");
            response.header("X-Content-Type-Options", "nosniff");
            response.header("X-Frame-Options", "DENY");
            response.header("X-XSS-Protection", "1; mode=block");
        });

        server.before((request, response) -> {
            String contentLength = request.headers("Content-Length");
            if (contentLength != null) {
                try {
                    int length = Integer.parseInt(contentLength);
                    if (length > MAX_REQUEST_SIZE) {
                        response.status(413);
                        response.body("{\"error\": \"Request entity too large\"}");
                        halt();
                    }
                } catch (NumberFormatException e) {
                    response.status(400);
                    response.body("{\"error\": \"Invalid Content-Length\"}");
                    halt();
                }
            }
        });

        server.before((request, response) -> {
            if (!checkRateLimit(request)) {
                response.status(429);
                response.header("Retry-After", "60");
                response.body("{\"error\": \"Too many requests\"}");
                halt();
            }
        });

        server.before((request, response) -> {
            if (!new TokenAuthenticator(plugin).authenticate(request)) {
                response.status(401);
                response.header("WWW-Authenticate", "Bearer");
                response.body("{\"error\": \"Unauthorized\"}");
                halt();
            }
        });

        server.options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        String basePath = "/api/v1";

        new ReportHandler(plugin).registerRoutes(server, basePath);
        new PunishmentHandler(plugin).registerRoutes(server, basePath);
        new StatsHandler(plugin).registerRoutes(server, basePath);

        server.exception(Exception.class, (exception, request, response) -> {
            plugin.getLogger().log(Level.WARNING, "REST API 异常: " + request.pathInfo(), exception);
            response.status(500);
            response.body("{\"error\": \"Internal server error\"}");
        });

        server.init();
        running = true;

        plugin.getLogger().info("REST API 已启动，监听端口: " + port);
    }

    private boolean checkRateLimit(Request request) {
        String clientIp = getClientIp(request);
        RateLimit rateLimit = rateLimits.computeIfAbsent(clientIp, k -> new RateLimit());
        
        long now = System.currentTimeMillis();
        
        synchronized (rateLimit) {
            if (now - rateLimit.lastRequestTime > RATE_LIMIT_WINDOW_MS) {
                rateLimit.reset();
            }
            
            rateLimit.lastRequestTime = now;
            rateLimit.count.incrementAndGet();
            
            int limit = plugin.getConfigManager().getRestApiRateLimit();
            return rateLimit.count.get() <= limit;
        }
    }

    private String getClientIp(Request request) {
        String xForwardedFor = request.headers("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.headers("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.ip();
    }

    public void stop() {
        if (server != null && running) {
            server.stop();
            running = false;
            rateLimits.clear();
            plugin.getLogger().info("REST API 已停止");
        }
    }

    public boolean isRunning() {
        return running;
    }

    private static class RateLimit {
        long lastRequestTime = 0;
        AtomicInteger count = new AtomicInteger(0);

        void reset() {
            lastRequestTime = 0;
            count.set(0);
        }
    }
}
