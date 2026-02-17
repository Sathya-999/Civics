package com.civics.dao;

import com.civics.model.Complaint;
import com.civics.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {
    public boolean raiseComplaint(Complaint complaint) {
        // Automatically fetch department_id based on category_id
        String deptQuery = "SELECT department_id FROM categories WHERE category_id = ?";
        String insertQuery = "INSERT INTO complaints (user_id, category_id, department_id, title, description, location, latitude, longitude, image_url, priority, escalation_level, is_verified, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection()) {
            Integer deptId = null;
            try (PreparedStatement dpstmt = conn.prepareStatement(deptQuery)) {
                dpstmt.setInt(1, complaint.getCategoryId());
                ResultSet drs = dpstmt.executeQuery();
                if (drs.next()) {
                    deptId = drs.getInt("department_id");
                }
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, complaint.getUserId());
                pstmt.setInt(2, complaint.getCategoryId());
                if (deptId != null) {
                    pstmt.setInt(3, deptId);
                } else {
                    pstmt.setNull(3, java.sql.Types.INTEGER);
                }
                pstmt.setString(4, complaint.getTitle());
                pstmt.setString(5, complaint.getDescription());
                pstmt.setString(6, complaint.getLocation());
                pstmt.setBigDecimal(7, complaint.getLatitude());
                pstmt.setBigDecimal(8, complaint.getLongitude());
                pstmt.setString(9, complaint.getImageUrl());
                pstmt.setString(10, complaint.getPriority());
                pstmt.setInt(11, 0); // escalation_level
                pstmt.setBoolean(12, false); // is_verified
                pstmt.setString(13, complaint.getStatus());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            complaint.setComplaintId(generatedKeys.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error in raiseComplaint: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Complaint> getComplaintsByUser(int userId) {
        List<Complaint> list = new ArrayList<>();
        String query = "SELECT * FROM complaints WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Complaint c = new Complaint();
                c.setComplaintId(rs.getInt("complaint_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setCategoryId(rs.getInt("category_id"));
                c.setDepartmentId(rs.getInt("department_id"));
                c.setTitle(rs.getString("title"));
                c.setDescription(rs.getString("description"));
                c.setLocation(rs.getString("location"));
                c.setLatitude(rs.getBigDecimal("latitude"));
                c.setLongitude(rs.getBigDecimal("longitude"));
                c.setImageUrl(rs.getString("image_url"));
                c.setStatus(rs.getString("status"));
                c.setPriority(rs.getString("priority"));
                c.setEscalationLevel(rs.getInt("escalation_level"));
                c.setVerified(rs.getBoolean("is_verified"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(c);
            }
        } catch (SQLException e) {
            System.err.println("DB Error in getComplaintsByUser: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public boolean escalateComplaint(int complaintId) {
        String query = "UPDATE complaints SET escalation_level = escalation_level + 1, updated_at = CURRENT_TIMESTAMP WHERE complaint_id = ? AND escalation_level < 2";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, complaintId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getComplaintCount(int userId, String status) {
        String query = "SELECT COUNT(*) FROM complaints WHERE user_id = ?";
        if (status != null) {
            query += " AND status = ?";
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            if (status != null) {
                pstmt.setString(2, status);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get a single complaint by its ID.
     */
    public Complaint getComplaintById(int complaintId) {
        String query = "SELECT * FROM complaints WHERE complaint_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, complaintId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Complaint c = new Complaint();
                c.setComplaintId(rs.getInt("complaint_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setCategoryId(rs.getInt("category_id"));
                c.setDepartmentId(rs.getInt("department_id"));
                c.setTitle(rs.getString("title"));
                c.setDescription(rs.getString("description"));
                c.setLocation(rs.getString("location"));
                c.setLatitude(rs.getBigDecimal("latitude"));
                c.setLongitude(rs.getBigDecimal("longitude"));
                c.setImageUrl(rs.getString("image_url"));
                c.setStatus(rs.getString("status"));
                c.setPriority(rs.getString("priority"));
                c.setEscalationLevel(rs.getInt("escalation_level"));
                c.setVerified(rs.getBoolean("is_verified"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                return c;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update the status of a complaint and return the old status.
     * Returns the previous status if successful, null if failed.
     */
    public String updateComplaintStatus(int complaintId, String newStatus) {
        String selectQuery = "SELECT status FROM complaints WHERE complaint_id = ?";
        String updateQuery = "UPDATE complaints SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE complaint_id = ?";
        try (Connection conn = DBUtil.getConnection()) {
            // Get old status first
            String oldStatus = null;
            try (PreparedStatement selStmt = conn.prepareStatement(selectQuery)) {
                selStmt.setInt(1, complaintId);
                ResultSet rs = selStmt.executeQuery();
                if (rs.next()) {
                    oldStatus = rs.getString("status");
                }
            }
            // Update to new status
            try (PreparedStatement updStmt = conn.prepareStatement(updateQuery)) {
                updStmt.setString(1, newStatus);
                updStmt.setInt(2, complaintId);
                if (updStmt.executeUpdate() > 0) {
                    return oldStatus; // success: return old status
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the email of the user who owns a complaint.
     */
    public String[] getUserInfoByComplaintId(int complaintId) {
        String query = "SELECT u.email, u.full_name FROM users u JOIN complaints c ON u.user_id = c.user_id WHERE c.complaint_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, complaintId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new String[]{rs.getString("email"), rs.getString("full_name")};
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String[]> getCityStats() {
        List<String[]> stats = new ArrayList<>();
        String query = "SELECT city, COUNT(*) as total, SUM(CASE WHEN status='Closed' THEN 1 ELSE 0 END) as closed FROM users u JOIN complaints c ON u.user_id = c.user_id GROUP BY city";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stats.add(new String[]{rs.getString("city"), rs.getString("total"), rs.getString("closed")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    /**
     * Get top 5 users ranked by civic score.
     * Civic score = 20 per complaint + 30 per verified + 50 per closed.
     * Returns list of [fullName, civicScore, totalComplaints].
     */
    public List<String[]> getTopUsers() {
        List<String[]> top = new ArrayList<>();
        String query = "SELECT u.full_name, " +
            "COUNT(*) * 20 + SUM(CASE WHEN c.is_verified = TRUE THEN 30 ELSE 0 END) + SUM(CASE WHEN c.status = 'Closed' THEN 50 ELSE 0 END) AS civic_score, " +
            "COUNT(*) AS total " +
            "FROM users u JOIN complaints c ON u.user_id = c.user_id " +
            "GROUP BY u.user_id, u.full_name " +
            "ORDER BY civic_score DESC LIMIT 5";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                top.add(new String[]{rs.getString("full_name"), rs.getString("civic_score"), rs.getString("total")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    /**
     * Get the department contact email for a given category.
     */
    public String getDepartmentEmailByCategoryId(int categoryId) {
        String query = "SELECT d.contact_email FROM departments d JOIN categories c ON d.department_id = c.department_id WHERE c.category_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("contact_email");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the department name for a given category.
     */
    public String getDepartmentNameByCategoryId(int categoryId) {
        String query = "SELECT d.department_name FROM departments d JOIN categories c ON d.department_id = c.department_id WHERE c.category_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("department_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get category name by category ID.
     */
    public String getCategoryNameById(int categoryId) {
        String query = "SELECT category_name FROM categories WHERE category_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("category_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
