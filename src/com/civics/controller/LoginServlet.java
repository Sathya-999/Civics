package com.civics.controller;

import com.civics.dao.UserDAO;
import com.civics.model.User;
import com.civics.util.SecurityUtil;
import com.civics.util.EmailUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    public void init() {
        userDAO = new UserDAO();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Force a session refresh
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        
        System.out.println("Login attempt: " + email);

        // Hash the password to compare against stored hash in DB
        String hashedPassword = UserDAO.hashPassword(password);
        User user = userDAO.validateUser(email, hashedPassword);

        if (user == null) {
            System.err.println("Login failed: User is null");
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=invalid_credentials");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        System.out.println("Session created for user: " + user.getEmail());

        // Security: Create an encrypted auth token cookie
        try {
            String tokenData = user.getUserId() + ":" + user.getEmail() + ":" + System.currentTimeMillis();
            String encryptedToken = SecurityUtil.encrypt(tokenData);
            if (encryptedToken != null) {
                Cookie authCookie = new Cookie("AUTH_TOKEN", encryptedToken);
                authCookie.setHttpOnly(true);
                authCookie.setPath("/");
                authCookie.setMaxAge(60 * 60 * 24);
                response.addCookie(authCookie);
            }
        } catch (Exception e) {
            System.err.println("Failed to set auth cookie: " + e.getMessage());
        }
        
        // Use absolute path with context path to prevent redirect loops
        String redirectPath = request.getContextPath() + "/pages/dashboard.html";
        System.out.println("Redirecting to: " + redirectPath);
        response.sendRedirect(redirectPath);
    }
}
