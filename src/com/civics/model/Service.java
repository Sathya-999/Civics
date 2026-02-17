package com.civics.model;

import java.math.BigDecimal;

public class Service {
    private int serviceId;
    private String serviceName;
    private String category; // Income, Caste, Residence, Other
    private BigDecimal fee;
    private String description;

    public Service() {}

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
