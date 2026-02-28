package com.civics.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthCookieUtil {
    private static final int AUTH_TTL_SECONDS = 60 * 60 * 24;

    private AuthCookieUtil() {}

    public static void setAuthCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        boolean secure = request.isSecure();
        StringBuilder sb = new StringBuilder();
        sb.append("AUTH_TOKEN=").append(token)
          .append("; Max-Age=").append(AUTH_TTL_SECONDS)
          .append("; Path=/; HttpOnly; SameSite=Strict");
        if (secure) {
            sb.append("; Secure");
        }
        response.addHeader("Set-Cookie", sb.toString());
    }
}
