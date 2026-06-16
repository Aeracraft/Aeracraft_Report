package com.aeracraft.report.rest.auth;

import com.aeracraft.report.AeracraftReport;
import spark.Request;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TokenAuthenticator {

    private final AeracraftReport plugin;
    private static final String BEARER_PREFIX = "Bearer ";

    public TokenAuthenticator(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public boolean authenticate(Request request) {
        String configToken = plugin.getConfigManager().getRestApiToken();

        if (configToken == null || configToken.isEmpty()) {
            plugin.getLogger().warning("REST API token 未配置");
            return false;
        }

        String authHeader = request.headers("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            return false;
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        
        return constantTimeEquals(configToken, token);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }
}
