import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    public static void registerUser(String name, String email) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            
     
            con.setAutoCommit(false);

            // 1. Insert user
            String insertUser = "INSERT INTO users (name, email) VALUES (?, ?)";
            PreparedStatement psUser = con.prepareStatement(insertUser, new String[]{"id"});
            psUser.setString(1, name);
            psUser.setString(2, email);
            psUser.executeUpdate();

            // 2. Get generated user ID
            ResultSet rs = psUser.getGeneratedKeys();
            int userId = 0;
            if (rs.next()) {
                userId = rs.getInt(1);
            }

            // 3. Insert job
            String insertJob = "INSERT INTO jobs (user_id, type) VALUES (?, ?)";
            PreparedStatement psJob = con.prepareStatement(insertJob);
            psJob.setInt(1, userId);
            psJob.setString(2, "WELCOME_EMAIL");
            psJob.executeUpdate();

           
            con.commit();
            System.out.println("User registered and job created successfully");

        } catch (SQLException e) {
          
            try {
                if (con != null) {
                    con.rollback();
                    System.out.println(" Transaction rolled back — nothing was saved");
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();

        } finally {
            try { 
                if (con != null) {
                
                    con.setAutoCommit(true);
                    con.close(); 
                }
            } catch (SQLException e) { 
                e.printStackTrace(); 
            }
        }
    }

    public static void main(String[] args) {
        registerUser("Yamuna", "yamuna@example.com");
    }
}