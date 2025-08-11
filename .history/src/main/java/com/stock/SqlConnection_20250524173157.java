import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/stockdb"; // Replace with your DB name
        String user = "root"; // Your MySQL username
        String password = "root"; // Your MySQL password

        try
          {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connected to MySQL database!");
            conn.close();mvn -v
          } 
        catch (SQLException e)
          {
            System.out.println("❌ Connection failed.");
            e.printStackTrace();
          }
    }
}
