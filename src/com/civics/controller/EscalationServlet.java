package com.civics.controller;

import com.civics.dao.ComplaintDAO;
import com.civics.model.User;
import com.civics.util.EmailUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/EscalationServlet")
public class EscalationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ComplaintDAO complaintDAO = new ComplaintDAO();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("unauthorized");
            return;
        }

        String complaintIdStr = request.getParameter("complaintId");
        if (complaintIdStr == null || complaintIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("missing complaintId");
            return;
        }

        int complaintId;
        try {
            complaintId = Integer.parseInt(complaintIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("invalid complaintId");
            return;
        }

        boolean escalated = complaintDAO.escalateComplaint(complaintId);
        if (escalated) {
            // Send escalation email notification
            try {
                User user = (User) session.getAttribute("user");
                if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    // Get the actual escalation level from DB after escalation
                    com.civics.model.Complaint c = complaintDAO.getComplaintById(complaintId);
                    int newLevel = (c != null) ? c.getEscalationLevel() : 1;
                    EmailUtil.notifyEscalation(user.getEmail(), complaintId, newLevel);
                }
            } catch (Exception e) {
                System.err.println("Escalation email failed: " + e.getMessage());
            }
            response.getWriter().write("success");
        } else {
            response.getWriter().write("failed");
        }
    }
}
