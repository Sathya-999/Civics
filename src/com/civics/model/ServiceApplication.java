package com.civics.model;

import java.sql.Timestamp;

public class ServiceApplication {
    private int applicationId;
    private int userId;
    private int serviceId;
    private String status; // Pending, In Verification, Approved, Rejected
    private Timestamp applicationDate;
    private Timestamp verificationDate;
    private String responsiblePerson;

    public ServiceApplication() {}

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getApplicationDate() { return applicationDate; }
    public void setApplicationDate(Timestamp applicationDate) { this.applicationDate = applicationDate; }

    public Timestamp getVerificationDate() { return verificationDate; }
    public void setVerificationDate(Timestamp verificationDate) { this.verificationDate = verificationDate; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
}
