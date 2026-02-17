package com.civics.controller;

import com.civics.dao.UserDAO;
import com.civics.model.User;
import com.civics.util.SecurityUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles Google Sign-In. Receives email + name from the frontend popup,
 * finds or creates the user, creates session, and redirects to dashboard.
 */
@WebServlet("/GoogleLoginServlet")
public class GoogleLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    // Keep these for future real OAuth if needed
    public static final String CLIENT_ID     = "";
    public static final String CLIENT_SECRET = "";
    public static final String REDIRECT_URI  = "http://localhost:9550/civics/GoogleCallback";

    private UserDAO userDAO;

    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String name = request.getParameter("name");

        if (email == null || email.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=google_failed");
            return;
        }

        email = email.trim();
        name = (name != null && !name.trim().isEmpty()) ? name.trim()
             : email.split("@")[0].substring(0,1).toUpperCase() + email.split("@")[0].substring(1);

        try {
            System.out.println("Google Sign-In: " + email + " / " + name);

            User user = userDAO.getUserByEmail(email);

            if (user == null) {
                user = new User();
                user.setFullName(name);
                user.setEmail(email);
                user.setPassword(UserDAO.hashPassword(UUID.randomUUID().toString()));
                user.setPhone("");
                user.setCity("");

                if (!userDAO.registerUser(user)) {
                    throw new Exception("DB registration failed");
                }
                user = userDAO.getUserByEmail(email);
                System.out.println("Google: New user created — " + email);
            } else {
                // Update name if provided from Google and current name is generic
                String currentName = user.getFullName();
                if (name != null && !name.isEmpty()) {
                    boolean isGeneric = currentName == null || currentName.isEmpty()
                        || currentName.equalsIgnoreCase("Civic User")
                        || currentName.equalsIgnoreCase("Member")
                        || currentName.equalsIgnoreCase("User")
                        || currentName.equalsIgnoreCase("Citizen")
                        || currentName.equalsIgnoreCase("Restored User");
                    if (isGeneric || !currentName.equals(name)) {
                        userDAO.updateUserName(user.getUserId(), name);
                        user.setFullName(name);
                    }
                }
                System.out.println("Google: Existing user — " + email + " name: " + user.getFullName());
            }

            HttpSession old = request.getSession(false);
            if (old != null) old.invalidate();

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);

            try {
                String tok = user.getUserId() + ":" + user.getEmail() + ":" + System.currentTimeMillis();
                String enc = SecurityUtil.encrypt(tok);
                if (enc != null) {
                    Cookie c = new Cookie("AUTH_TOKEN", enc);
                    c.setHttpOnly(true);
                    c.setPath("/");
                    c.setMaxAge(86400);
                    response.addCookie(c);
                }
            } catch (Exception e) {
                System.err.println("Cookie error: " + e.getMessage());
            }

            response.sendRedirect(request.getContextPath() + "/pages/dashboard.html");

        } catch (Exception e) {
            System.err.println("Google Sign-In error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=google_failed");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Redirect GET to login page (the Google button uses POST via the popup form)
        response.sendRedirect(request.getContextPath() + "/pages/login.html");
    }
}
