package com.civics.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailUtil {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_EMAIL = "dreameranji527@gmail.com";
    private static final String SENDER_PASSWORD = "exxrrfoujtqouxww";
    private static final String SENDER_NAME = "Civic Connect";

    /**
     * Send an email via Gmail SMTP in a background thread (non-blocking).
     */
    public static void sendEmail(String recipient, String subject, String body) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.ssl.trust", SMTP_HOST);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                System.out.println("[EmailUtil] Email sent to: " + recipient + " | Subject: " + subject);
            } catch (Exception e) {
                System.err.println("[EmailUtil] Failed to send email to " + recipient + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, "EmailSender-" + System.currentTimeMillis()).start();
    }

    /**
     * Notify user when a complaint is raised.
     */
    public static void notifyComplaintRaised(String email, String userName, int complaintId, String title, String category, String priority) {
        String subject = "\uD83D\uDCE2 Complaint Registered - #" + complaintId + " | Civic Connect";
        String body = "Dear " + (userName != null ? userName : "Citizen") + ",\n\n"
                + "Your complaint has been successfully registered on Civic Connect.\n\n"
                + "\uD83D\uDCCB Complaint Details:\n"
                + "   Complaint ID  : #" + complaintId + "\n"
                + "   Title         : " + (title != null ? title : "N/A") + "\n"
                + "   Category      : " + (category != null ? category : "General") + "\n"
                + "   Priority      : " + (priority != null ? priority : "Medium") + "\n"
                + "   Status        : Open\n\n"
                + "We will keep you updated via email as your complaint progresses.\n"
                + "You can also track it at: Dashboard > Track Complaint\n\n"
                + "Thank you for making your city better!\n"
                + "- Team Civic Connect";
        sendEmail(email, subject, body);
    }

    /**
     * Notify user when complaint status changes - with status-specific messaging.
     */
    public static void notifyStatusChange(String email, String userName, int complaintId, String title, String oldStatus, String newStatus) {
        String statusEmoji;
        String statusMessage;
        String subject;

        switch (newStatus.toLowerCase()) {
            case "open":
                statusEmoji = "\uD83D\uDCE5";
                statusMessage = "Your complaint has been received and is now open for review by the concerned department.";
                break;
            case "pending":
                statusEmoji = "\u23F3";
                statusMessage = "Your complaint is currently pending review. Our team is looking into it and will take action shortly.";
                break;
            case "in progress":
                statusEmoji = "\uD83D\uDD27";
                statusMessage = "Great news! Work has begun on your complaint. The concerned department is actively working to resolve your issue.";
                break;
            case "resolved":
                statusEmoji = "\u2705";
                statusMessage = "Your complaint has been resolved! The issue you reported has been addressed by the authorities.\n\n"
                        + "If you feel the issue is not fully resolved, you can reopen it from your Dashboard.";
                break;
            case "closed":
                statusEmoji = "\uD83D\uDD12";
                statusMessage = "Your complaint has been officially closed. Thank you for helping improve your community!\n\n"
                        + "Your civic score has been updated accordingly.";
                break;
            case "rejected":
                statusEmoji = "\u274C";
                statusMessage = "Unfortunately, your complaint could not be processed. This may be due to insufficient details or a duplicate report.\n\n"
                        + "You can raise a new complaint with more details if needed.";
                break;
            default:
                statusEmoji = "\uD83D\uDD14";
                statusMessage = "Your complaint status has been updated to: " + newStatus;
                break;
        }

        subject = statusEmoji + " Complaint #" + complaintId + " - " + capitalize(newStatus) + " | Civic Connect";

        String body = "Dear " + (userName != null && !userName.isEmpty() ? userName : "Citizen") + ",\n\n"
                + statusMessage + "\n\n"
                + "\uD83D\uDCCB Complaint Details:\n"
                + "   Complaint ID  : #" + complaintId + "\n"
                + "   Title         : " + (title != null ? title : "N/A") + "\n"
                + "   Previous Status: " + (oldStatus != null ? oldStatus : "N/A") + "\n"
                + "   Current Status : " + capitalize(newStatus) + "\n\n"
                + "Track your complaint anytime at: Dashboard > Track Complaint\n\n"
                + "Thank you for using Civic Connect!\n"
                + "- Team Civic Connect";

        sendEmail(email, subject, body);
    }

    /**
     * Capitalize the first letter of a string.
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Notify user when complaint is escalated.
     */
    public static void notifyEscalation(String email, int complaintId, int newLevel) {
        String levelName;
        switch (newLevel) {
            case 1: levelName = "Municipal / GHMC Level"; break;
            case 2: levelName = "State / District Level"; break;
            default: levelName = "Level " + newLevel; break;
        }
        String subject = "\u26A0\uFE0F Complaint Escalated - #" + complaintId + " | Civic Connect";
        String body = "Dear Citizen,\n\n"
                + "Your complaint (ID: #" + complaintId + ") has been escalated to a higher authority.\n\n"
                + "   Escalation Level: " + levelName + "\n\n"
                + "A higher authority will now review your complaint for faster resolution.\n"
                + "You will receive further updates via email.\n\n"
                + "Thank you for your patience.\n"
                + "- Team Civic Connect";
        sendEmail(email, subject, body);
    }

    /**
     * Notify the municipality/department when a new complaint is raised.
     * Sends a detailed email with all complaint info so the department can act on it.
     */
    public static void notifyMunicipality(String deptEmail, String deptName, int complaintId,
            String citizenName, String citizenEmail, String title, String description,
            String category, String priority, String location, String latitude, String longitude) {
        
        // If no department email, send to the app admin as fallback
        String recipient = (deptEmail != null && !deptEmail.isEmpty()) ? deptEmail : SENDER_EMAIL;
        
        String subject = "\uD83C\uDFE2 New Complaint #" + complaintId + " — " + priority + " Priority | " + (deptName != null ? deptName : "Municipality");
        
        String body = "═══════════════════════════════════════════\n"
                + "    CIVIC CONNECT — NEW COMPLAINT ALERT\n"
                + "═══════════════════════════════════════════\n\n"
                + "A citizen has raised a new complaint that requires your attention.\n\n"
                + "\uD83D\uDCCB COMPLAINT DETAILS:\n"
                + "─────────────────────────────────────\n"
                + "   Complaint ID  : #" + complaintId + "\n"
                + "   Title         : " + (title != null ? title : "N/A") + "\n"
                + "   Category      : " + (category != null ? category : "General") + "\n"
                + "   Department    : " + (deptName != null ? deptName : "Not Assigned") + "\n"
                + "   Priority      : " + (priority != null ? priority : "Medium") + "\n"
                + "   Status        : Open (New)\n\n"
                + "\uD83D\uDCDD DESCRIPTION:\n"
                + "─────────────────────────────────────\n"
                + (description != null ? description : "No description provided.") + "\n\n"
                + "\uD83D\uDCCD LOCATION:\n"
                + "─────────────────────────────────────\n"
                + "   Address/Landmark : " + (location != null && !location.isEmpty() ? location : "Not specified") + "\n"
                + "   GPS Coordinates  : " + (latitude != null ? latitude : "N/A") + ", " + (longitude != null ? longitude : "N/A") + "\n";
        
        if (latitude != null && longitude != null && !latitude.isEmpty() && !longitude.isEmpty()) {
            body += "   Google Maps      : https://maps.google.com/?q=" + latitude + "," + longitude + "\n";
        }
        
        body += "\n\uD83D\uDC64 CITIZEN INFO:\n"
                + "─────────────────────────────────────\n"
                + "   Name  : " + (citizenName != null ? citizenName : "Anonymous") + "\n"
                + "   Email : " + (citizenEmail != null ? citizenEmail : "N/A") + "\n\n"
                + "⚡ ACTION REQUIRED:\n"
                + "Please review this complaint and take appropriate action.\n"
                + "Update the status via the Civic Connect admin panel.\n\n"
                + "─────────────────────────────────────\n"
                + "This is an automated notification from Civic Connect.\n"
                + "Citizen-Municipality Complaint Management System\n";
        
        sendEmail(recipient, subject, body);
    }

    /**
     * Notify municipality when a complaint status changes.
     */
    public static void notifyMunicipalityStatusChange(String deptEmail, String deptName,
            int complaintId, String title, String oldStatus, String newStatus, String updatedBy) {
        String recipient = (deptEmail != null && !deptEmail.isEmpty()) ? deptEmail : SENDER_EMAIL;
        String subject = "\uD83D\uDD14 Complaint #" + complaintId + " Status: " + capitalize(newStatus) + " | " + (deptName != null ? deptName : "Municipality");
        String body = "Complaint Status Update\n\n"
                + "Complaint ID  : #" + complaintId + "\n"
                + "Title         : " + (title != null ? title : "N/A") + "\n"
                + "Previous Status: " + (oldStatus != null ? oldStatus : "N/A") + "\n"
                + "New Status    : " + capitalize(newStatus) + "\n"
                + "Updated By    : " + (updatedBy != null ? updatedBy : "System") + "\n\n"
                + "- Civic Connect System";
        sendEmail(recipient, subject, body);
    }
}
