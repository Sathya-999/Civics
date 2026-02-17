package com.civics.controller;

import com.civics.dao.ComplaintDAO;
import com.civics.model.Complaint;
import com.civics.model.User;
import com.civics.util.EmailUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

@WebServlet("/ComplaintServlet")
public class ComplaintServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ComplaintDAO complaintDAO;

    public void init() {
        complaintDAO = new ComplaintDAO();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * GET handler: List complaints for the logged-in user.
     * Usage: ComplaintServlet?action=list
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");

        if ("list".equals(action)) {
            List<Complaint> complaints = complaintDAO.getComplaintsByUser(user.getUserId());
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < complaints.size(); i++) {
                Complaint c = complaints.get(i);
                json.append("{");
                json.append("\"complaintId\":").append(c.getComplaintId()).append(",");
                json.append("\"title\":\"").append(escapeJson(c.getTitle())).append("\",");
                json.append("\"description\":\"").append(escapeJson(c.getDescription())).append("\",");
                json.append("\"status\":\"").append(escapeJson(c.getStatus())).append("\",");
                json.append("\"priority\":\"").append(escapeJson(c.getPriority())).append("\",");
                json.append("\"location\":\"").append(escapeJson(c.getLocation())).append("\",");
                json.append("\"escalationLevel\":").append(c.getEscalationLevel()).append(",");
                json.append("\"isVerified\":").append(c.isVerified()).append(",");
                json.append("\"createdAt\":").append(c.getCreatedAt() != null ? c.getCreatedAt().getTime() : 0);
                json.append("}");
                if (i < complaints.size() - 1) json.append(",");
            }
            json.append("]");
            out.print(json.toString());
            out.flush();
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid action");
        }
    }

    /**
     * POST handler: Raise a new complaint OR update complaint status.
     * For status update: action=updateStatus, complaintId=xxx, newStatus=xxx
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/pages/login.html");
            return;
        }
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/pages/login.html");
            return;
        }

        String action = request.getParameter("action");

        // ── Handle Status Update ──
        if ("updateStatus".equals(action)) {
            handleStatusUpdate(request, response, user);
            return;
        }

        // ── Handle Raise Complaint (default) ──
        String title = request.getParameter("title");
        String categoryIdStr = request.getParameter("categoryId");
        String description = request.getParameter("description");
        String location = request.getParameter("location");
        String latitudeStr = request.getParameter("latitude");
        String longitudeStr = request.getParameter("longitude");
        String priority = request.getParameter("priority");

        Complaint complaint = new Complaint();
        complaint.setUserId(user.getUserId());
        try {
            if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                complaint.setCategoryId(Integer.parseInt(categoryIdStr));
            } else {
                complaint.setCategoryId(1);
            }
        } catch (NumberFormatException e) {
            complaint.setCategoryId(1);
        }

        complaint.setTitle(title != null ? title : "Untitled Issue");
        complaint.setDescription(description != null ? description : "No description provided.");
        complaint.setLocation(location);
        
        try {
            if (latitudeStr != null && !latitudeStr.isEmpty()) {
                complaint.setLatitude(new BigDecimal(latitudeStr));
            }
            if (longitudeStr != null && !longitudeStr.isEmpty()) {
                complaint.setLongitude(new BigDecimal(longitudeStr));
            }
        } catch (Exception e) {
            // Ignore bad GPS data
        }
        
        complaint.setPriority(priority != null ? priority : "Medium");
        complaint.setStatus("Open");

        // Save live camera photo to disk and store path
        String imageData = request.getParameter("imageData");
        if (imageData != null && imageData.startsWith("data:image")) {
            try {
                // Strip the data URL prefix (e.g., "data:image/jpeg;base64,")
                String base64Data = imageData.substring(imageData.indexOf(",") + 1);
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                // Create uploads directory in the webapp
                String uploadsDir = getServletContext().getRealPath("/uploads");
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();

                // Generate unique filename
                String fileName = "evidence_" + user.getUserId() + "_" + System.currentTimeMillis() + ".jpg";
                File imageFile = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    fos.write(imageBytes);
                }

                // Store relative path for web access
                complaint.setImageUrl("uploads/" + fileName);
                System.out.println("[ComplaintServlet] Evidence photo saved: " + imageFile.getAbsolutePath());
            } catch (Exception imgEx) {
                System.err.println("[ComplaintServlet] Failed to save evidence photo: " + imgEx.getMessage());
                // Store the raw base64 as fallback (truncated for DB safety)
                if (imageData.length() > 60000) {
                    complaint.setImageUrl(imageData.substring(0, 60000));
                } else {
                    complaint.setImageUrl(imageData);
                }
            }
        }

        boolean success = complaintDAO.raiseComplaint(complaint);

        if (success) {
            // Send email notification to CITIZEN for complaint raised
            try {
                String userEmail = user.getEmail();
                String categoryName = complaintDAO.getCategoryNameById(complaint.getCategoryId());
                if (userEmail != null && !userEmail.isEmpty()) {
                    EmailUtil.notifyComplaintRaised(
                        userEmail,
                        user.getFullName(),
                        complaint.getComplaintId(),
                        complaint.getTitle(),
                        categoryName != null ? categoryName : "Category-" + complaint.getCategoryId(),
                        complaint.getPriority()
                    );
                }
            } catch (Exception e) {
                System.err.println("User email notification failed: " + e.getMessage());
            }

            // Send email notification to MUNICIPALITY DEPARTMENT
            try {
                String deptEmail = complaintDAO.getDepartmentEmailByCategoryId(complaint.getCategoryId());
                String deptName = complaintDAO.getDepartmentNameByCategoryId(complaint.getCategoryId());
                String categoryName = complaintDAO.getCategoryNameById(complaint.getCategoryId());
                EmailUtil.notifyMunicipality(
                    deptEmail,
                    deptName,
                    complaint.getComplaintId(),
                    user.getFullName(),
                    user.getEmail(),
                    complaint.getTitle(),
                    complaint.getDescription(),
                    categoryName != null ? categoryName : "Category-" + complaint.getCategoryId(),
                    complaint.getPriority(),
                    complaint.getLocation(),
                    complaint.getLatitude() != null ? complaint.getLatitude().toPlainString() : null,
                    complaint.getLongitude() != null ? complaint.getLongitude().toPlainString() : null
                );
                System.out.println("[ComplaintServlet] Municipality notified for complaint #" + complaint.getComplaintId());
            } catch (Exception e) {
                System.err.println("Municipality email notification failed: " + e.getMessage());
            }

            javax.servlet.http.Cookie flashMsg = new javax.servlet.http.Cookie("flash_msg", "Complaint%20raised%20successfully!%20Tracking%20number%20generated.");
            flashMsg.setPath("/");
            flashMsg.setMaxAge(10);
            response.addCookie(flashMsg);
            response.sendRedirect(request.getContextPath() + "/pages/dashboard.html");
        } else {
            javax.servlet.http.Cookie flashError = new javax.servlet.http.Cookie("flash_error", "Failed%20to%20submit%20complaint.%20Please%20verify%20all%20fields.");
            flashError.setPath("/");
            flashError.setMaxAge(10);
            response.addCookie(flashError);
            response.sendRedirect(request.getContextPath() + "/pages/raise-complaint.html");
        }
    }

    /**
     * Handle complaint status update with email notification.
     * Every status change (Open, Pending, In Progress, Resolved, Closed, Rejected)
     * sends an email to the complaint owner.
     */
    private void handleStatusUpdate(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        String complaintIdStr = request.getParameter("complaintId");
        String newStatus = request.getParameter("newStatus");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (complaintIdStr == null || newStatus == null || newStatus.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Missing complaintId or newStatus\"}");
            return;
        }

        int complaintId;
        try {
            complaintId = Integer.parseInt(complaintIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Invalid complaintId\"}");
            return;
        }

        // Get complaint details before update (for email)
        Complaint complaint = complaintDAO.getComplaintById(complaintId);
        if (complaint == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"success\":false,\"message\":\"Complaint not found\"}");
            return;
        }

        String oldStatus = complaintDAO.updateComplaintStatus(complaintId, newStatus);
        if (oldStatus != null) {
            // Send email notification to CITIZEN for status change
            try {
                String[] userInfo = complaintDAO.getUserInfoByComplaintId(complaintId);
                String ownerEmail = null;
                String ownerName = null;

                if (userInfo != null) {
                    ownerEmail = userInfo[0];
                    ownerName = userInfo[1];
                } else if (complaint.getUserId() == user.getUserId()) {
                    ownerEmail = user.getEmail();
                    ownerName = user.getFullName();
                }

                if (ownerEmail != null && !ownerEmail.isEmpty()) {
                    EmailUtil.notifyStatusChange(
                        ownerEmail,
                        ownerName,
                        complaintId,
                        complaint.getTitle(),
                        oldStatus,
                        newStatus
                    );
                    System.out.println("[ComplaintServlet] Status email sent for complaint #" + complaintId + ": " + oldStatus + " → " + newStatus);
                }
            } catch (Exception e) {
                System.err.println("Status update email to citizen failed: " + e.getMessage());
            }

            // Send email notification to MUNICIPALITY DEPARTMENT for status change
            try {
                String deptEmail = complaintDAO.getDepartmentEmailByCategoryId(complaint.getCategoryId());
                String deptName = complaintDAO.getDepartmentNameByCategoryId(complaint.getCategoryId());
                EmailUtil.notifyMunicipalityStatusChange(
                    deptEmail,
                    deptName,
                    complaintId,
                    complaint.getTitle(),
                    oldStatus,
                    newStatus,
                    user.getFullName()
                );
            } catch (Exception e) {
                System.err.println("Status update email to municipality failed: " + e.getMessage());
            }

            out.write("{\"success\":true,\"message\":\"Status updated to " + escapeJson(newStatus) + "\",\"oldStatus\":\"" + escapeJson(oldStatus) + "\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Failed to update status\"}");
        }
    }
}
