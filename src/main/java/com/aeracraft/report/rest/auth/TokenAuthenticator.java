package com.aeracraft.report.rest.auth;

import com.aeracraft.report.AeracraftReport;
import spark.Request;

public class TokenAuthenticator {

    private final AeracraftReport plugin;

    public TokenAuthenticator(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public boolean authenticate(Request request) {
        String configToken = plugin.getConfigManager().getRestApiToken();

        if (configToken == null || configToken.isEmpty()) {
            return false;
        }

        String authHeader = request.headers("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            return false;
        }

        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return configToken.equals(token);
        }

        return false;
    }
}
