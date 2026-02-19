import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "system";  // your Oracle username
    private static final String PASSWORD = "system"; // your Oracle password

    public static Connection getConnection() {
        try {
            // Updated driver for ojdbc11.jar
 
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }  catch (SQLException e) {
            throw new RuntimeException("Connection failed: " + e.getMessage());
        }
    }
}
