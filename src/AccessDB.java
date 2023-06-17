import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Scanner;

public class AccessDB {
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "password";

    public static void main(String[] args) {
        try {
            // Establish connection to the database
            Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
            Scanner scanner = new Scanner(System.in);

            // Ask for user input to add a new user
            System.out.print("Enter user name: ");
            String user_name = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            // Generate current date as date_created
            LocalDate dateCreated = LocalDate.now();

            // Prepare the SQL statement to insert a new user
            String SQL_INSERT_USER = "INSERT INTO users(user_name, password, date_created) VALUES (?, ?, ?)";
            PreparedStatement insertUserStatement = connection.prepareStatement(SQL_INSERT_USER, Statement.RETURN_GENERATED_KEYS);
            insertUserStatement.setString(1, user_name);
            insertUserStatement.setString(2, password);
            insertUserStatement.setObject(3, dateCreated);

            // Execute the query to insert a new user
            int rowsAffected = insertUserStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("New user added successfully.");

                // Retrieve the generated user_id
                ResultSet generatedKeys = insertUserStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    System.out.println("Generated user_id: " + userId);
                }
            } else {
                System.out.println("Failed to add new user.");
            }

            // Close the resources
            insertUserStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
