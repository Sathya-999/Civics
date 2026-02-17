package com.civics.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtil {
    private static final String KEY = "1234567890123456"; // 16 characters for AES-128
    private static final String ALGORITHM = "AES";

    public static String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes("UTF-8"));
            // Use URL-safe Base64 to avoid +, /, = which break cookies
            String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedData);
            return URLEncoder.encode(base64, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            String decoded = URLDecoder.decode(encryptedData, "UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            // Try URL-safe decoder first, then standard decoder as fallback
            byte[] decodedData;
            try {
                decodedData = Base64.getUrlDecoder().decode(decoded);
            } catch (IllegalArgumentException e) {
                decodedData = Base64.getDecoder().decode(decoded);
            }
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
