package com.civics.controller;

import com.civics.dao.ComplaintDAO;
import com.civics.model.User;
import com.civics.model.Complaint;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/DashboardServlet")
public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ComplaintDAO complaintDAO;

    public void init() {
        complaintDAO = new ComplaintDAO();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Use the real name from DB/session
        String displayName = user.getFullName();
        if (displayName == null || displayName.trim().isEmpty()) {
            // Only fall back to email prefix if name is totally empty
            if (user.getEmail() != null && user.getEmail().contains("@")) {
                String namePart = user.getEmail().split("@")[0];
                displayName = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
            } else {
                displayName = "User";
            }
        }

        System.out.println("DEBUG: Dashboard for user: " + displayName + " (" + user.getEmail() + ")");

        int total = complaintDAO.getComplaintCount(user.getUserId(), null);
        int closed = complaintDAO.getComplaintCount(user.getUserId(), "Closed");
        int open = complaintDAO.getComplaintCount(user.getUserId(), "Open");
        int inProgress = complaintDAO.getComplaintCount(user.getUserId(), "In Progress");
        int resolved = complaintDAO.getComplaintCount(user.getUserId(), "Resolved");
        int rejected = complaintDAO.getComplaintCount(user.getUserId(), "Rejected");
        
        // Calculate Priority Score (Circle 2)
        // High = 100, Medium = 60, Low = 30
        List<Complaint> userComplaints = complaintDAO.getComplaintsByUser(user.getUserId());
        double prioritySum = 0;
        for (Complaint c : userComplaints) {
            if ("High".equalsIgnoreCase(c.getPriority())) prioritySum += 100;
            else if ("Medium".equalsIgnoreCase(c.getPriority())) prioritySum += 60;
            else prioritySum += 30;
        }
        int priorityScore = (total == 0) ? 0 : (int)(prioritySum / total);

        // Calculate Civic Score (Circle 3 and Gauge)
        // Based ONLY on the complaints section
        // Points: 20 per raised, 30 bonus per verified, 50 bonus per closed
        int civicScore = 0;
        for (Complaint c : userComplaints) {
            civicScore += 20; // Points for raising
            if (c.isVerified()) civicScore += 30; // Points for verification
            if ("Closed".equalsIgnoreCase(c.getStatus())) civicScore += 50; // Points for resolution
        }
        int progress = Math.min(civicScore, 100); // Scale to 100 for gauge

        boolean shortlisted = (civicScore >= 100); 
        
        List<String[]> cityStats = complaintDAO.getCityStats();
        List<String[]> topUsers = complaintDAO.getTopUsers();

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"userName\": \"").append(escapeJson(displayName)).append("\",");
        json.append("\"email\": \"").append(escapeJson(user.getEmail())).append("\",");
        json.append("\"phone\": \"").append(escapeJson(user.getPhone() != null ? user.getPhone() : "")).append("\",");
        json.append("\"address\": \"").append(escapeJson(user.getAddress() != null ? user.getAddress() : "")).append("\",");
        json.append("\"city\": \"").append(escapeJson(user.getCity() != null ? user.getCity() : "")).append("\",");
        json.append("\"totalComplaints\": ").append(total).append(",");
        json.append("\"closedComplaints\": ").append(closed).append(",");
        json.append("\"openComplaints\": ").append(open).append(",");
        json.append("\"inProgressComplaints\": ").append(inProgress).append(",");
        json.append("\"resolvedComplaints\": ").append(resolved).append(",");
        json.append("\"rejectedComplaints\": ").append(rejected).append(",");
        json.append("\"priorityScore\": ").append(priorityScore).append(",");
        json.append("\"civicScore\": ").append(civicScore).append(",");
        json.append("\"progress\": ").append(progress).append(",");
        json.append("\"shortlisted\": ").append(shortlisted).append(",");

        // Recent complaints (latest 5)
        List<Complaint> recentComplaints = userComplaints.subList(0, Math.min(5, userComplaints.size()));
        json.append("\"recentComplaints\": [");
        for (int i = 0; i < recentComplaints.size(); i++) {
            Complaint rc = recentComplaints.get(i);
            json.append("{");
            json.append("\"id\": ").append(rc.getComplaintId()).append(",");
            json.append("\"title\": \"").append(escapeJson(rc.getTitle())).append("\",");
            json.append("\"status\": \"").append(escapeJson(rc.getStatus())).append("\",");
            json.append("\"priority\": \"").append(escapeJson(rc.getPriority())).append("\",");
            json.append("\"location\": \"").append(escapeJson(rc.getLocation() != null ? rc.getLocation() : "")).append("\",");
            json.append("\"escalation\": ").append(rc.getEscalationLevel()).append(",");
            json.append("\"verified\": ").append(rc.isVerified()).append(",");
            json.append("\"createdAt\": \"").append(rc.getCreatedAt() != null ? rc.getCreatedAt().toString() : "").append("\"");
            json.append("}");
            if (i < recentComplaints.size() - 1) json.append(",");
        }
        json.append("],");
        
        json.append("\"cityStats\": [");
        for (int i = 0; i < cityStats.size(); i++) {
            String[] s = cityStats.get(i);
            json.append("{");
            json.append("\"cityName\": \"").append(escapeJson(s[0])).append("\",");
            json.append("\"total\": ").append(s[1] != null ? s[1] : "0").append(",");
            json.append("\"closed\": ").append(s[2] != null ? s[2] : "0");
            json.append("}");
            if (i < cityStats.size() - 1) json.append(",");
        }
        json.append("],");

        json.append("\"leaderboard\": [");
        for (int i = 0; i < topUsers.size(); i++) {
            String[] u = topUsers.get(i);
            json.append("{");
            json.append("\"name\": \"").append(escapeJson(u[0])).append("\",");
            json.append("\"score\": ").append(u[1] != null ? u[1] : "0").append(",");
            json.append("\"complaints\": ").append(u[2] != null ? u[2] : "0");
            json.append("}");
            if (i < topUsers.size() - 1) json.append(",");
        }
        json.append("]");
        json.append("}");
        
        out.print(json.toString());
        out.flush();
    }
}
