package com.civics.controller;

import com.civics.dao.UserDAO;
import com.civics.model.User;
import com.civics.util.EmailUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    public void init() {
        userDAO = new UserDAO();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String phone = request.getParameter("phone");
        String city = request.getParameter("city");

        // Check if email is already registered
        if (userDAO.emailExists(email)) {
            System.out.println("Email already registered: " + email);
            javax.servlet.http.Cookie flashError = new javax.servlet.http.Cookie("flash_error", "Email%20already%20registered.%20Please%20sign%20in%20instead.");
            flashError.setPath("/");
            flashError.setMaxAge(10);
            response.addCookie(flashError);
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=email_exists");
            return;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        // Hash the password before storing
        user.setPassword(UserDAO.hashPassword(password));
        user.setPhone(phone);
        user.setCity(city);
        
        System.out.println("Processing registration for: " + email);
        boolean success = userDAO.registerUser(user);

        if (success) {
            System.out.println("Registration successful for: " + email);
            
            // Send welcome email
            try {
                if (email != null && !email.isEmpty()) {
                    EmailUtil.sendEmail(email, 
                        "\uD83C\uDF1F Welcome to Civic Connect!", 
                        "Dear " + (fullName != null ? fullName : "Citizen") + ",\n\n"
                        + "Welcome to Civic Connect! Your account has been created successfully.\n\n"
                        + "You can now:\n"
                        + "   \u2022 Raise complaints about civic issues\n"
                        + "   \u2022 Track complaint status in real-time\n"
                        + "   \u2022 Get email updates on every status change\n"
                        + "   \u2022 Earn civic score and rewards\n\n"
                        + "Login at: Dashboard > Sign In\n\n"
                        + "Together, let's build a better community!\n"
                        + "- Team Civic Connect");
                }
            } catch (Exception e) {
                System.err.println("Welcome email failed: " + e.getMessage());
            }

            javax.servlet.http.Cookie flashMsg = new javax.servlet.http.Cookie("flash_msg", "Registration%20successful!%20You%20can%20now%20sign%20in.");
            flashMsg.setPath("/");
            flashMsg.setMaxAge(10);
            response.addCookie(flashMsg);
            response.sendRedirect(request.getContextPath() + "/pages/login.html?success=registered");
        } else {
            System.err.println("Registration failed for: " + email);
            javax.servlet.http.Cookie flashError = new javax.servlet.http.Cookie("flash_error", "Registration%20failed.%20Please%20try%20again.");
            flashError.setPath("/");
            flashError.setMaxAge(10);
            response.addCookie(flashError);
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=registration_failed");
        }
    }
}
