package com.aeracraft.report.rest;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.rest.auth.TokenAuthenticator;
import com.aeracraft.report.rest.handler.PunishmentHandler;
import com.aeracraft.report.rest.handler.ReportHandler;
import com.aeracraft.report.rest.handler.StatsHandler;
import spark.Service;
import static spark.Spark.halt;

import java.util.logging.Level;

public class RestServer {

    private final AeracraftReport plugin;
    private Service server;
    private boolean running;

    public RestServer(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (running) {
            return;
        }

        int port = plugin.getConfigManager().getRestApiPort();
        String token = plugin.getConfigManager().getRestApiToken();

        server = Service.ignite();
        server.port(port);

        server.before((request, response) -> {
            response.type("application/json");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Authorization, Content-Type");

            if (!new TokenAuthenticator(plugin).authenticate(request)) {
                response.status(401);
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
            plugin.getLogger().log(Level.WARNING, "REST API 异常", exception);
            response.status(500);
            response.body("{\"error\": \"Internal server error\"}");
        });

        server.init();
        running = true;

        plugin.getLogger().info("REST API 已启动，监听端口: " + port);
    }

    public void stop() {
        if (server != null && running) {
            server.stop();
            running = false;
            plugin.getLogger().info("REST API 已停止");
        }
    }

    public boolean isRunning() {
        return running;
    }
}
