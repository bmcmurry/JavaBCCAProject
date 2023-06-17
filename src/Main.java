import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void apiCall(HttpURLConnection conn) {
    }

    public static class StockData {
        private LocalDate date;
        private double average;
        private double close;

        public StockData(LocalDate date, double average, double close) {
            this.date = date;
            this.average = average;
            this.close = close;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getAverage() {
            return average;
        }

        public double getClose() {
            return close;
        }
    }
    private static final String API_KEY = "ZHF986AD1CJJSSAP";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "password";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean loggedIn = login();
        if (loggedIn) {
            boolean quit = false;
            while (!quit) {
                System.out.println("What would you like to do?");
                System.out.println("[search] a stock's price");
                System.out.println("[watchlist] view your watchlist");
                System.out.println("[edit] your watchlist");
                System.out.println("[quit]");

                String input = scanner.nextLine().trim().toLowerCase();
                switch (input) {
                    case "search":
                        searchStockPrice();
                        break;
                    case "watchlist":
                        viewWatchlist();
                        break;
                    case "edit":
                        editWatchlist();
                        break;
                    case "quit":
                        quit = true;
                        break;
                    default:
                        System.out.println("Invalid command.");
                }
            }
        } else {
            System.out.println("Login failed. Exiting the program.");
        }
    }

    private static boolean login() {
        System.out.println("Welcome!");

        // Ask the user if they want to create a new user or login
        System.out.println("Do you want to [create] a new user or [login] to an existing user?");
        String choice = scanner.nextLine().trim().toLowerCase();

        if (choice.equals("create")) {
            createUser();
            return true;
        } else if (choice.equals("login")) {
            System.out.print("Username: ");
            String user_name = scanner.nextLine();

            System.out.print("Password: ");
            String password = scanner.nextLine();
            return authenticateUser(user_name, password);
        } else {
            System.out.println("Invalid choice.");
            return false;
        }
    }
    private static void createUser() {
        System.out.println("Enter the following details to create a new user:");

        System.out.print("Username: ");
        String user_name = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        // Add the user to the database
        LocalDate currentDate = LocalDate.now();
        boolean userAdded = addUserToDatabase(user_name, password, currentDate);

        if (userAdded) {
            System.out.println("User created successfully.");
        } else {
            System.out.println("Failed to create user.");
        }
    }
    private static boolean addUserToDatabase(String user_name, String password, LocalDate date) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (user_name, date_created, password) VALUES (?, ?, ?)");
            statement.setString(1, user_name);
            statement.setObject(2, date);
            statement.setString(3, password);

            int rowsAffected = statement.executeUpdate();

            statement.close();
            connection.close();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }




    private static boolean authenticateUser(String user_name, String password) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) AS count FROM users WHERE user_name = ? AND password = ?");
            statement.setString(1, user_name);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                return count > 0;
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    private static void searchStockPrice() {
        System.out.print("Enter a company name: ");
        String companyName = scanner.nextLine();

        String stockSymbol = getStockSymbolFromDatabase(companyName);
        if (stockSymbol != null) {
            String stockData = getStockData(stockSymbol);
            processStockData(stockData);
        } else {
            System.out.println("Stock symbol not found for the company: " + companyName);
        }
    }


    private static void viewWatchlist() {
        System.out.println("User's Watchlist");
        System.out.println("================");

        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement("SELECT stock_symbol FROM watchlist WHERE user_id = ?");
            statement.setInt(1, 1); // Assuming the user ID is 1 for now

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String stockSymbol = resultSet.getString("stock_symbol");
                System.out.println(stockSymbol);
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void editWatchlist() {
        System.out.print("Enter stock symbol to add/remove from watchlist: ");
        String stockSymbol = scanner.nextLine();

        // Check if the stock symbol is already in the user's watchlist
        boolean isInWatchlist = checkIfStockInWatchlist(stockSymbol);

        if (isInWatchlist) {
            removeFromWatchlist(stockSymbol);
            System.out.println("Stock removed from watchlist: " + stockSymbol);
        } else {
            addToWatchlist(stockSymbol);
            System.out.println("Stock added to watchlist: " + stockSymbol);
        }
    }

    public static void removeFromWatchlist(String stockSymbol) {
        try {
            // Establish database connection
            Connection connection = DriverManager.getConnection("your_database_url", "username", "password");

            // Create the SQL query
            String query = "DELETE FROM watchlist WHERE symbol = ?";

            // Create a prepared statement
            PreparedStatement statement = connection.prepareStatement(query);

            // Set the parameter values
            statement.setString(1, stockSymbol);

            // Execute the query
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Stock removed from watchlist successfully.");
            } else {
                System.out.println("Stock symbol not found in the watchlist.");
            }

            // Close resources
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace(); // Handle the exception appropriately (e.g., log an error message)
        }
    }


    private static boolean checkIfStockInWatchlist(String stockSymbol) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) AS count FROM watchlist WHERE user_id = ? AND stock_symbol = ?");
            statement.setInt(1, 1); // Assuming the user ID is 1 for now
            statement.setString(2, stockSymbol);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                return count > 0;
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void addToWatchlist(String stockSymbol) {
        try {
            // Get the user_id for the current user (you need to implement this)
            int userId = getCurrentUserId();

            // Get the last_close and most_recent_close_date using the API call
            double lastClose = getLastClosePrice(stockSymbol);
            LocalDate mostRecentCloseDate = getLastCloseDate(stockSymbol);

            // Set the date_added_to_watchlist as the current date
            LocalDate dateAddedToWatchlist = LocalDate.now();

            // Get the initial_price using an API call (similar to getLastClosePrice method)
            double initialPrice = getInitialPrice(stockSymbol);

            // Calculate the gain_loss percentage
            double gainLoss = ((lastClose - initialPrice) / initialPrice) * 100.0;

            // Insert the data into the watchlist table
            String query = "INSERT INTO watchlist (user_id, stock_symbol, last_close, most_recent_close_date, " +
                    "date_added_to_watchlist, initial_price, gain_loss) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, userId);
                statement.setString(2, stockSymbol);
                statement.setDouble(3, lastClose);
                statement.setDate(4, java.sql.Date.valueOf(mostRecentCloseDate));
                statement.setDate(5, java.sql.Date.valueOf(dateAddedToWatchlist));
                statement.setDouble(6, initialPrice);
                statement.setDouble(7, gainLoss);

                statement.executeUpdate();
                System.out.println("Stock added to watchlist successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private static String getStockSymbolFromDatabase(String companyName) {
        String stockSymbol = null;

        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement statement = connection.prepareStatement("SELECT symbol FROM listing_status WHERE name = ?");
            statement.setString(1, companyName);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                stockSymbol = resultSet.getString("symbol");
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stockSymbol;
    }

    private static String getStockData(String stockSymbol) {
        String stockData = null;

        try {
            String apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=" +
                    stockSymbol + "&apikey=" + API_KEY;

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                stockData = response.toString();
            } else {
                System.out.println("Error: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stockData;
    }


    private static void processStockData(String stockData) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(stockData).getAsJsonObject();
        JsonObject timeSeries = jsonResponse.getAsJsonObject("Weekly Time Series");

        if (timeSeries != null) {
            List<StockData> closingPrices = new ArrayList<>();
            LocalDate currentDate = LocalDate.now();
            DecimalFormat decimalFormat = new DecimalFormat("#.##");

            System.out.println("Stock Trend Analysis");
            System.out.println("====================");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate currentWeekStart = null;
            LocalDate currentWeekEnd = null;
            int weekCount = 0;

            double averageWeek = 0.0; // Initialize averageWeek with a default value
            double closeWeek = 0.0; // Initialize closeWeek with a default value

            for (String key : timeSeries.keySet()) {
                LocalDate date = LocalDate.parse(key, dateFormatter);
                if (currentWeekStart == null) {
                    currentWeekStart = date;
                }
                currentWeekEnd = date;

                if (weekCount == 0 || date.isAfter(currentDate.minusDays(7))) {
                    JsonObject data = timeSeries.getAsJsonObject(key);
                    String high = data.get("2. high").getAsString();
                    String low = data.get("3. low").getAsString();
                    String close = data.get("4. close").getAsString();
                    double highValue = Double.parseDouble(high);
                    double lowValue = Double.parseDouble(low);
                    double closeValue = Double.parseDouble(close);
                    double average = (highValue + lowValue) / 2;

                    StockData stockDataEntry = new StockData(date, average, closeValue);
                    closingPrices.add(stockDataEntry);

                    averageWeek = average; // Update averageWeek with the average value for the current week
                    closeWeek = closeValue; // Update closeWeek with the closing price for the current week

                    System.out.println("Date: " + date);
                    System.out.println("Average: " + decimalFormat.format(averageWeek));
                    System.out.println("Close: " + closeWeek);
                    System.out.println();
                }

                weekCount++;
            }

            boolean isUpThisWeek = isStockUp(closingPrices);
            boolean isUpLastWeek = closingPrices.size() >= 2 && isStockUp(getSubList(closingPrices, 2));
            boolean isUpLast3Months = closingPrices.size() >= 14 && isStockUp(getSubList(closingPrices, 14));
            boolean isUpLast6Months = closingPrices.size() >= 27 && isStockUp(getSubList(closingPrices, 27));
            boolean isUpLastYear = closingPrices.size() >= 54 && isStockUp(getSubList(closingPrices, 54));

            LocalDate lastWeekEndDate = currentWeekStart.minusDays(1); // Get the end date of the previous week

            LocalDate last3MonthsStartDate = currentDate.minusMonths(3).plusDays(1); // Get the start date of the last 3 months
            LocalDate last3MonthsEndDate = currentDate; // Get the end date of the last 3 months

            LocalDate last6MonthsStartDate = currentDate.minusMonths(6).plusDays(1); // Get the start date of the last 6 months
            LocalDate last6MonthsEndDate = currentDate; // Get the end date of the last 6 months

            LocalDate lastYearStartDate = currentDate.minusYears(1).plusDays(1); // Get the start date of the last year
            LocalDate lastYearEndDate = currentDate; // Get the end date of the last year

            System.out.println("Stock Performance Analysis");
            System.out.println("==========================");
            System.out.println("This Week: " + (isUpThisWeek ? "Up" : "Down"));
            System.out.println("Last Week (" + currentWeekStart + " to " + lastWeekEndDate + "): " + (isUpLastWeek ? "Up" : "Down"));
            System.out.println("Last 3 Months (" + last3MonthsStartDate + " to " + last3MonthsEndDate + "): " + (isUpLast3Months ? "Up" : "Down"));
            System.out.println("Last 6 Months (" + last6MonthsStartDate + " to " + last6MonthsEndDate + "): " + (isUpLast6Months ? "Up" : "Down"));
            System.out.println("Last Year (" + lastYearStartDate + " to " + lastYearEndDate + "): " + (isUpLastYear ? "Up" : "Down"));
        } else {
            System.out.println("No stock data available for the specified symbol.");
        }
    }



    private static int getIndexForDate(List<StockData> stockDataList, LocalDate date) {
        for (int i = stockDataList.size() - 1; i >= 0; i--) {
            if (stockDataList.get(i).getDate().isEqual(date)) {
                return i;
            }
        }
        return -1; // Return -1 if date is not found
    }

    private static boolean isStockUp(List<StockData> stockDataList) {
        int size = stockDataList.size();
        if (size <= 1) {
            return false;
        }

        double sum = 0.0;
        for (int i = 1; i < size; i++) {
            double priceDiff = stockDataList.get(i).getClose() - stockDataList.get(i - 1).getClose();
            sum += priceDiff;
        }

        double averageChange = sum / (size - 1);
        return averageChange > 0;
    }




    private static List<StockData> getSubList(List<StockData> list, int size) {
        int listSize = list.size();
        if (size >= listSize) {
            return list;
        } else {
            return list.subList(listSize - size, listSize);
        }
    }


    private static double calculatePercentageChange(double initialValue, double finalValue) {
        return (finalValue - initialValue) / initialValue * 100;
    }
}
