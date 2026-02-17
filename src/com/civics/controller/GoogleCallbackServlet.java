package com.civics.controller;

import com.civics.dao.UserDAO;
import com.civics.model.User;
import com.civics.util.SecurityUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Step 2 of Google OAuth 2.0: Handle the callback from Google.
 * Google redirects here with ?code=XXX after the user picks their account.
 * We exchange the code for an access token, fetch user info, and log them in.
 */
@WebServlet("/GoogleCallback")
public class GoogleCallbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UserDAO userDAO;

    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code = request.getParameter("code");
        String error = request.getParameter("error");

        if (error != null) {
            System.err.println("Google OAuth error: " + error);
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=google_failed");
            return;
        }

        if (code == null || code.isEmpty()) {
            System.err.println("Google OAuth: No authorization code received");
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=google_failed");
            return;
        }

        try {
            // ===== STEP 1: Exchange authorization code for access token =====
            String tokenResponse = exchangeCodeForToken(code);
            String accessToken = extractJsonValue(tokenResponse, "access_token");

            if (accessToken == null || accessToken.isEmpty()) {
                throw new Exception("Failed to get access token from Google. Response: " + tokenResponse);
            }
            System.out.println("Google OAuth: Got access token");

            // ===== STEP 2: Fetch user info from Google =====
            String userInfoJson = fetchGoogleUserInfo(accessToken);
            String email = extractJsonValue(userInfoJson, "email");
            String name = extractJsonValue(userInfoJson, "name");
            String picture = extractJsonValue(userInfoJson, "picture");

            if (email == null || email.isEmpty()) {
                throw new Exception("No email in Google user info. Response: " + userInfoJson);
            }

            System.out.println("Google OAuth: email=" + email + ", name=" + name);

            // ===== STEP 3: Find or create user in database =====
            User user = userDAO.getUserByEmail(email);

            if (user == null) {
                // New user — register automatically
                user = new User();
                user.setFullName(name != null ? name : "Google User");
                user.setEmail(email);
                user.setPassword(UserDAO.hashPassword(UUID.randomUUID().toString()));
                user.setPhone("");
                user.setCity("");

                boolean registered = userDAO.registerUser(user);
                if (!registered) {
                    throw new Exception("Failed to register Google user in DB");
                }
                user = userDAO.getUserByEmail(email);
                System.out.println("New user registered via Google: " + email);
            } else {
                // Update name if it was previously empty
                if ((user.getFullName() == null || user.getFullName().isEmpty()
                        || "Google User".equals(user.getFullName())) && name != null) {
                    System.out.println("Existing user signed in via Google: " + email);
                }
            }

            // ===== STEP 4: Create session and auth cookie =====
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);

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
                System.err.println("Auth cookie error: " + e.getMessage());
            }

            // ===== STEP 5: Redirect to dashboard =====
            response.sendRedirect(request.getContextPath() + "/pages/dashboard.html");

        } catch (Exception e) {
            System.err.println("Google OAuth callback error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/pages/login.html?error=google_failed");
        }
    }

    /**
     * Exchange the authorization code for an access token via POST to Google's token endpoint.
     */
    private String exchangeCodeForToken(String code) throws IOException {
        String params = "code=" + URLEncoder.encode(code, "UTF-8")
            + "&client_id=" + URLEncoder.encode(GoogleLoginServlet.CLIENT_ID, "UTF-8")
            + "&client_secret=" + URLEncoder.encode(GoogleLoginServlet.CLIENT_SECRET, "UTF-8")
            + "&redirect_uri=" + URLEncoder.encode(GoogleLoginServlet.REDIRECT_URI, "UTF-8")
            + "&grant_type=authorization_code";

        URL url = new URL("https://oauth2.googleapis.com/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes("UTF-8"));
            os.flush();
        }

        return readResponse(conn);
    }

    /**
     * Fetch the user's profile info (email, name, picture) using the access token.
     */
    private String fetchGoogleUserInfo(String accessToken) throws IOException {
        URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        return readResponse(conn);
    }

    /**
     * Read the full response body from an HTTP connection.
     */
    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }
        if (is == null) return "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Simple JSON string value extractor (no external library needed).
     */
    private String extractJsonValue(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + pattern.length());
        if (colonIdx < 0) return null;

        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            // Parse string value handling escaped quotes
            StringBuilder result = new StringBuilder();
            for (int i = start + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"') { result.append('"'); i++; }
                    else if (next == '\\') { result.append('\\'); i++; }
                    else { result.append(c); }
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }
}
