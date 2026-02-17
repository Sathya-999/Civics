package com.civics.util;

import java.sql.Connection;
import java.sql.Statement;

public class DbSetup {
    public static void setup() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Add city pillar to users if not exists
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN city VARCHAR(100)");
            } catch (Exception e) {
                // Column might already exist
            }
            
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN reward_points INT DEFAULT 0");
            } catch (Exception e) {}

            // Add escalation level and fraud status to complaints
            try {
                stmt.executeUpdate("ALTER TABLE complaints ADD COLUMN escalation_level INT DEFAULT 0"); // 0: Local, 1: GHMC/Mnc, 2: State
                stmt.executeUpdate("ALTER TABLE complaints ADD COLUMN is_verified BOOLEAN DEFAULT FALSE");
                stmt.executeUpdate("ALTER TABLE complaints ADD COLUMN assigned_officer VARCHAR(100)");
            } catch (Exception e) {}

            // Ensure image_url column can hold file paths
            try {
                stmt.executeUpdate("ALTER TABLE complaints MODIFY COLUMN image_url TEXT");
            } catch (Exception e) {}

            // Setup Categories and Departments
            try {
                stmt.executeUpdate("INSERT IGNORE INTO departments (department_id, department_name) VALUES " +
                    "(1, 'Public Works'), (2, 'Water Board'), (3, 'Electrical Dept'), (4, 'Health & Sanitation'), (5, 'Lokayukta / Anti-Corruption')");
            } catch (Exception e) {}

            try {
                stmt.executeUpdate("INSERT IGNORE INTO categories (category_id, category_name, department_id) VALUES " +
                    "(1, 'Roads & Transport', 1), (2, 'Water Supply', 2), (3, 'Street Lights', 3), (4, 'Sanitation & Garbage', 4), (5, 'Electricity', 3), (6, 'Corruption / Bribe', 5)");
            } catch (Exception e) {}

            System.out.println("Database columns and master data updated successfully.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        setup();
    }
}
