package com.civics.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtil {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SECRET = loadSecret();

    private static String loadSecret() {
        String configured = System.getenv("CIVICS_AUTH_SECRET");
        if (configured != null && configured.trim().length() >= 32) {
            return configured.trim();
        }
        System.err.println("[Security] CIVICS_AUTH_SECRET is not set or too short; using development fallback secret.");
        return "change-this-development-secret-before-production";
    }

    public static String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(data.getBytes(StandardCharsets.UTF_8));
            String signature = sign(payload);
            return payload + "." + signature;
        } catch (Exception e) {
            System.err.println("Token generation failed: " + e.getMessage());
            return null;
        }
    }

    public static String decrypt(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            String payload = parts[0];
            String signature = parts[1];
            String expected = sign(payload);
            if (!constantTimeEquals(expected, signature)) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
