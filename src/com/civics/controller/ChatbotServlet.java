package com.civics.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ChatbotServlet")
public class ChatbotServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String API_KEY = "AIzaSyBfuld77nJNHS9QsnindjDpqX-IkC6g_4c";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 1. Read the incoming JSON body
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        // 2. Extract "message" value with proper JSON unescaping
        String message = extractJsonValue(sb.toString(), "message");
        if (message == null || message.trim().isEmpty()) {
            message = "Hello";
        }

        System.out.println("Chatbot request - message: " + message);

        // 3. Build prompt with raw text, then escape ONCE for JSON embedding
        String prompt = "You are CAI (Civic AI), an advanced agentic assistant for the Civic Connect portal. " +
                "Your goal is to help citizens solve issues effectively. " +
                "Identify if this is a COMPLAINT (pothole, garbage, water, street light, sanitation, corruption, road, electricity etc.) or an INFO request. " +
                "If COMPLAINT, respond ONLY with this JSON format: " +
                "{\"type\": \"COMPLAINT\", \"category\": \"<Category>\", \"desc\": \"<Brief description>\", \"reply\": \"<Your helpful message>\"} " +
                "If INFO, respond ONLY with this JSON format: " +
                "{\"type\": \"INFO\", \"reply\": \"<Your helpful message>\"} " +
                "USER INPUT: " + message + " " +
                "Return ONLY valid raw JSON. No markdown. No code blocks. No explanation outside JSON.";

        // 4. Single escape pass for the entire prompt
        String escapedPrompt = escapeForJson(prompt);
        String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

        try {
            URL url = new URL(GEMINI_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    statusCode < 400 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String rawResponse = responseBuilder.toString();
            System.out.println("Gemini status: " + statusCode);

            if (statusCode >= 400) {
                System.err.println("Gemini API error: " + rawResponse);
                response.setStatus(500);
                response.getWriter().write("{\"type\":\"INFO\",\"reply\":\"AI Service returned an error. Falling back to local assistant.\"}");
                return;
            }

            // 5. Extract "text" from Gemini response with proper unescaping
            String aiText = extractJsonValue(rawResponse, "text");

            if (aiText == null || aiText.trim().isEmpty()) {
                System.err.println("Empty AI text from response: " + rawResponse);
                response.setStatus(500);
                response.getWriter().write("{\"type\":\"INFO\",\"reply\":\"AI Service unavailable. Falling back to local assistant.\"}");
                return;
            }

            // 6. Extract the JSON object from AI text (strip any surrounding text/markdown)
            String jsonResult = aiText.trim();
            int firstBrace = jsonResult.indexOf('{');
            int lastBrace = jsonResult.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace > firstBrace) {
                jsonResult = jsonResult.substring(firstBrace, lastBrace + 1);
            } else {
                // AI didn't return JSON structure, wrap its text as INFO
                jsonResult = "{\"type\":\"INFO\",\"reply\":\"" + escapeForJson(aiText) + "\"}";
            }

            response.getWriter().write(jsonResult);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            String errMsg = e.getMessage() != null ? escapeForJson(e.getMessage()) : "Unknown error";
            response.getWriter().write("{\"type\":\"INFO\",\"reply\":\"Server Error: " + errMsg + "\"}");
        }
    }

    /**
     * Extract a JSON string value by key name with proper unescaping.
     * Handles \" -> ", \\ -> \, \n -> newline, \r -> CR, \t -> tab.
     */
    private static String extractJsonValue(String json, String key) {
        if (json == null || key == null) return null;

        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx == -1) return null;

        // Find the colon after the key
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx == -1) return null;

        // Find the opening quote of the value (skip whitespace)
        int startQuote = -1;
        for (int i = colonIdx + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') { startQuote = i; break; }
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return null;
        }
        if (startQuote == -1) return null;

        // Parse the string value with proper unescaping
        StringBuilder result = new StringBuilder();
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  result.append('"');  break;
                    case '\\': result.append('\\'); break;
                    case 'n':  result.append('\n'); break;
                    case 'r':  result.append('\r'); break;
                    case 't':  result.append('\t'); break;
                    case '/':  result.append('/');  break;
                    default:   result.append('\\').append(next); break;
                }
                i++;
            } else if (c == '"') {
                break; // End of string value
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Escape a string for safe embedding inside a JSON string value.
     */
    private static String escapeForJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"':  out.append("\\\""); break;
                case '\n': out.append("\\n");  break;
                case '\r': out.append("\\r");  break;
                case '\t': out.append("\\t");  break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }
}
