import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    // Registers a user and creates a pending job
    public static void registerUser(String name, String email) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();

            // 1️⃣ Insert user into users table
            String insertUser = "INSERT INTO users (name, email) VALUES (?, ?)";
            PreparedStatement psUser = con.prepareStatement(insertUser, new String[]{"id"});
            psUser.setString(1, name);
            psUser.setString(2, email);
            psUser.executeUpdate();

            // Get generated user ID
            ResultSet rs = psUser.getGeneratedKeys();
            int userId = 0;
            if (rs.next()) {
                userId = rs.getInt(1);
            }

            // 2️⃣ Insert job into jobs table as PENDING
            String insertJob = "INSERT INTO jobs (user_id, type) VALUES (?, ?)";
            PreparedStatement psJob = con.prepareStatement(insertJob);
            psJob.setInt(1, userId);
            psJob.setString(2, "WELCOME_EMAIL");
            psJob.executeUpdate();

            System.out.println("✅ User registered and job created successfully");

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // For testing
    public static void main(String[] args) {
        registerUser("Yamuna", "yamuna@example.com");
    }
}
