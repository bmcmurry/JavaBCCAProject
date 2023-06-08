import java.sql.*;


public class AccessDB {
        public static Connection getConnection() throws SQLException {
            String url = "jdbc:postgresql://localhost:5432/postgres";
            String username = "postgres";
            String password = "password";

            return DriverManager.getConnection(url, username, password);
        }

    public static void main(String[] args) {
        try {
            Connection connection = AccessDB.getConnection();

            // Execute a simple SQL query
            String sql = "SELECT 1";
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            // Process the result
            if (resultSet.next()) {
                int result = resultSet.getInt(1);
                System.out.println("Result: " + result);
            }

            // Close resources
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle connection errors
        }


    }
}
