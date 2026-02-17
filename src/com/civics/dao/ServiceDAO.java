package com.civics.dao;

import com.civics.model.Service;
import com.civics.model.ServiceApplication;
import com.civics.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {
    public List<Service> getAllServices() {
        List<Service> list = new ArrayList<>();
        String query = "SELECT * FROM services";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Service s = new Service();
                s.setServiceId(rs.getInt("service_id"));
                s.setServiceName(rs.getString("service_name"));
                s.setCategory(rs.getString("category"));
                s.setFee(rs.getBigDecimal("fee"));
                s.setDescription(rs.getString("description"));
                list.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean applyForService(ServiceApplication app) {
        String query = "INSERT INTO service_applications (user_id, service_id) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, app.getUserId());
            pstmt.setInt(2, app.getServiceId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ServiceApplication> getApplicationsByUser(int userId) {
        List<ServiceApplication> list = new ArrayList<>();
        String query = "SELECT * FROM service_applications WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ServiceApplication sa = new ServiceApplication();
                sa.setApplicationId(rs.getInt("application_id"));
                sa.setUserId(rs.getInt("user_id"));
                sa.setServiceId(rs.getInt("service_id"));
                sa.setStatus(rs.getString("status"));
                sa.setApplicationDate(rs.getTimestamp("application_date"));
                sa.setVerificationDate(rs.getTimestamp("verification_date"));
                sa.setResponsiblePerson(rs.getString("responsible_person"));
                list.add(sa);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
