package com.civics.filter;

import com.civics.dao.UserDAO;
import com.civics.model.User;
import com.civics.util.SecurityUtil;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/pages/*")
public class AuthFilter implements Filter {
    private static final long TOKEN_MAX_AGE_MS = 24L * 60 * 60 * 1000;
    private UserDAO userDAO;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        applySecurityHeaders(res);

        // Allow login page and public pages to bypass filter
        if (path.endsWith("login.html") || path.endsWith("verify.html")
            || path.endsWith("faq.html") || path.endsWith("awareness.html")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        // Check for AUTH_TOKEN cookie as a second line of defense (for persistent login)
        if (!loggedIn) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("AUTH_TOKEN".equals(cookie.getName())) {
                        String decrypted = SecurityUtil.decrypt(cookie.getValue());
                        if (decrypted != null && decrypted.contains(":")) {
                            String[] parts = decrypted.split(":");
                            if (parts.length >= 3) {
                                String userEmail = parts[1];
                                long issuedAt = parseLong(parts[2]);
                                boolean tokenFresh = issuedAt > 0 && (System.currentTimeMillis() - issuedAt) <= TOKEN_MAX_AGE_MS;
                                if (tokenFresh) {
                                    User dbUser = userDAO.getUserByEmail(userEmail);
                                    if (dbUser != null) {
                                        loggedIn = true;
                                        if (session == null) {
                                            session = req.getSession(true);
                                        }
                                        session.setAttribute("user", dbUser);
                                        session.setMaxInactiveInterval(30 * 60);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        if (!loggedIn && !path.endsWith("index.html")) {
            res.sendRedirect(req.getContextPath() + "/pages/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    private void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://fonts.googleapis.com; font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com data:; img-src 'self' data: https:; connect-src 'self' https://accounts.google.com https://oauth2.googleapis.com https://www.googleapis.com;");
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void destroy() {}
}
