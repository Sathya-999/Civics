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
                            if (parts.length >= 2) {
                                String userEmail = parts[1];
                                // Load user from DB by email to restore session properly
                                User dbUser = userDAO.getUserByEmail(userEmail);
                                if (dbUser != null) {
                                    loggedIn = true;
                                    if (session == null) {
                                        session = req.getSession(true);
                                    }
                                    session.setAttribute("user", dbUser);
                                    System.out.println("Session restored from cookie for: " + userEmail);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Redirect to login if not logged in and not accessing allowed pages
        if (!loggedIn && !path.endsWith("index.html")) {
            res.sendRedirect(req.getContextPath() + "/pages/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
