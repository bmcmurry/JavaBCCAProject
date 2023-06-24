import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class Main {
    private static StockLookup stockLookup = new StockLookup();

    private static String loggedInUser = null;

    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "password";
    private static final String API_KEY = "ZHF986AD1CJJSSAP";

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Welcome to the Stock Watchlist Program!");
            boolean programRunning = true;
            while (programRunning) {
                try {

                    System.out.println("Please select the number of the option you wish to choose:");
                    System.out.println("1. Login");
                    System.out.println("2. Create an account");
                    System.out.println("3. Quit the program");

                    System.out.print("Option: ");
                    String option = "";
                    if (scanner.hasNextLine()) {
                        option = scanner.nextLine();
                    } else {
                        break;
                    }

                    switch (option) {
                        case "1":
                            System.out.print("Enter user name: ");
                            String userName = scanner.nextLine();
                            System.out.print("Enter password: ");
                            String password = scanner.nextLine();

                            boolean loginSuccessful = login(userName, password);
                            if (loginSuccessful) {
                                editWatchlist();
                            } else {
                                System.out.println("Login failed. Invalid credentials.");
                            }
                            break;
                        case "2":
                            System.out.print("Enter user name: ");
                            String newUserName = scanner.nextLine();
                            System.out.print("Enter password: ");
                            String newPassword = scanner.nextLine();

                            boolean accountCreated = createUser(newUserName, newPassword);
                            if (accountCreated) {
                                System.out.println("Account created successfully. Please log in.");
                            } else {
                                System.out.println("Failed to create account. Please try again.");
                            }
                            break;
                        case "3":
                            System.out.println("Exiting program...");
                            programRunning = false;
                            break;
                        default:
                            System.out.println("Invalid option. Please try again.");
                            break;
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("No input found. Please try again.");
                    if (scanner.hasNextLine()) {
                        scanner.nextLine(); // Consume the newline character
                    } else {
                        break;
                    }
                }
            }

            scanner.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    private static boolean createUser(String userName, String password) {
        if (userName == "" || password == "") {
            System.out.println("You must enter a username and password to create a user");
            return false;
        }

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD)) {
            PreparedStatement userStatement = connection.prepareStatement("SELECT * FROM users WHERE user_name = ?");
            userStatement.setString(1, userName);
            ResultSet result = userStatement.executeQuery();
            if (result != null) {
                System.out.println("that username already exists");
                return false;
            }
            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (user_name, date_created, password) VALUES (?, ?, ?)");
            statement.setString(1, userName);
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            statement.setString(3, password);
            statement.executeUpdate();
            return true; // Account creation successful
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Failed to create account
        }
    }



    private static boolean login(String userName, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_name = ? AND password = ?")) {
            statement.setString(1, userName);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    loggedInUser = userName; // Set the logged-in user
                    return true;
                }
            }
        }

        return false;
    }


    private static int getCurrentUserId(String userName) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT user_id FROM users WHERE user_name = ?")) {
            statement.setString(1, userName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("user_id");
                } else {
                    throw new IllegalStateException("Current user not found in the database.");
                }
            }
        }
    }


    private static void editWatchlist() {
        Scanner scanner = new Scanner(System.in);
        boolean editingWatchlist = true;

        while (editingWatchlist) {
            try {
            System.out.println("1. Add to watchlist");
            System.out.println("2. Remove from watchlist");
            System.out.println("3. View watchlist");
            System.out.println("4. Fetch stock data");
            System.out.println("5. Quit");
            System.out.print("Enter option: ");
                String option = "";
                if (scanner.hasNextLine()) {
                    option = scanner.nextLine();
                } else {
                    break;
                }
            switch (option) {
                case "1":
                    System.out.println("Enter stock name to add:");
                    String stockName = scanner.nextLine();

                    // Use the findStockSymbol method to retrieve the stock symbol
                    String stockSymbol = stockLookup.findStockSymbol(stockName);
                    addToWatchlist(stockSymbol);
                    break;
                case "2":
                    System.out.println("Enter stock name to remove:");
                    stockName = scanner.nextLine();

                    // Use the findStockSymbol method to retrieve the stock symbol
                    stockSymbol = stockLookup.findStockSymbol(stockName);
                    try {
                        removeFromWatchlist(stockSymbol);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "3":
                    try {
                        viewWatchlist();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "4":
                    System.out.print("Enter stock symbol to fetch price history: ");
                    String symbolToFetch = scanner.next();
                    scanner.nextLine();
                    try {
                        fetchStockData(symbolToFetch);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "5":
                    System.out.println("Returning to previous options...");
                    editingWatchlist = false;
                    break;
                default:
                    System.out.println("Invalid option.");
                    break;
            }
            } catch (NoSuchElementException e) {
                System.out.println("No input found. Please try again.");
                if (scanner.hasNextLine()) {
                    scanner.nextLine(); // Consume the newline character
                } else {
                    break;
                }
            }
        }

        scanner.close();
    }

    private static void addToWatchlist(String stockSymbol) {
        try {
            String userName = loggedInUser;
            int userId = getCurrentUserId(userName);

            // Verify the stock symbol in the listing_status table
            String stockName;
            double lastClose;
            LocalDate mostRecentCloseDate;
            double initialPrice;

            try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
                 PreparedStatement statement = connection.prepareStatement("SELECT name FROM listing_status WHERE symbol = ?")) {
                statement.setString(1, stockSymbol);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        stockName = resultSet.getString("name");
                    } else {
                        System.out.println("Invalid stock symbol.");
                        return; // Exit the method without adding the stock to the watchlist
                    }
                }
            }

            lastClose = getLastClosePrice(stockSymbol);
            mostRecentCloseDate = getLastCloseDate(stockSymbol);
            LocalDate dateAddedToWatchlist = LocalDate.now();
            initialPrice = getInitialPrice(stockSymbol);
            double gainLoss = ((lastClose - initialPrice) / initialPrice) * 100.0;

            String query = "INSERT INTO watchlist (user_id, stock_symbol, stock_name, last_close, most_recent_close_date, " +
                    "date_added_to_watchlist, initial_price, gain_loss) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, userId);
                statement.setString(2, stockSymbol);
                statement.setString(3, stockName);
                statement.setDouble(4, lastClose);
                statement.setDate(5, java.sql.Date.valueOf(mostRecentCloseDate));
                statement.setDate(6, java.sql.Date.valueOf(dateAddedToWatchlist));
                statement.setDouble(7, initialPrice);
                statement.setDouble(8, gainLoss);

                statement.executeUpdate();
                System.out.println("Stock added to watchlist successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return; // Exit the method without adding the stock to the watchlist
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    private static void removeFromWatchlist(String symbol) throws SQLException {
        String userName = loggedInUser;
        int userId = getCurrentUserId(userName);
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM watchlist WHERE user_id = ? AND stock_symbol = ?")) {
            statement.setInt(1, userId);
            statement.setString(2, symbol);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Symbol removed from watchlist.");
            } else {
                System.out.println("Failed to remove symbol from watchlist.");
            }
        }
    }


    private static void viewWatchlist() throws SQLException {
        String userName = loggedInUser;
        int userId = getCurrentUserId(userName);
        System.out.println("Hello " + userName + ", here are all of your stock positions.");
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT watchlist.stock_symbol, watchlist.date_added_to_watchlist, watchlist.gain_loss FROM users INNER JOIN watchlist ON users.user_id = watchlist.user_id WHERE users.user_id = ?")) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> watchlistData = new ArrayList<>();

                while (resultSet.next()) {
                    String symbol = resultSet.getString("stock_symbol");
                    String dateAdded = resultSet.getString("date_added_to_watchlist");
                    String gainLoss = resultSet.getString("gain_loss");

                    String data = String.format("Stock: %s, Date Added: %s, Gain/Loss: %s", symbol, dateAdded, gainLoss);
                    watchlistData.add(data);
                }

                if (watchlistData.isEmpty()) {
                    System.out.println("Watchlist is empty.");
                } else {
                    System.out.println("Watchlist data:");
                    for (String data : watchlistData) {
                        System.out.println(data);
                    }
                }
            }
        }
    }




    private static void fetchStockData(String symbol) throws IOException {
        String apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=" + symbol + "&apikey=" + API_KEY;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            System.out.println("Error: " + responseCode);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
        JsonObject timeSeries = json.getAsJsonObject("Weekly Time Series");

        if (timeSeries != null) {
            List<StockData> stockDataList = extractStockDataList(timeSeries);
            processStockData(stockDataList);
        } else {
            System.out.println("No time series data found.");
        }
    }

    private static List<StockData> extractStockDataList(JsonObject timeSeries) {
        List<StockData> stockDataList = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : timeSeries.entrySet()) {
            String dateString = entry.getKey();
            JsonObject data = entry.getValue().getAsJsonObject();
            double open = data.get("1. open").getAsDouble();
            double high = data.get("2. high").getAsDouble();
            double low = data.get("3. low").getAsDouble();
            double close = data.get("4. close").getAsDouble();
            long volume = data.get("5. volume").getAsLong();

            LocalDate date = LocalDate.parse(dateString);
            StockData stockData = new StockData(date, open, high, low, close, volume);
            stockDataList.add(stockData);
        }

        return stockDataList;
    }

    private static void processStockData(List<StockData> stockDataList) {
        // Process the stock data as needed
        for (StockData stockData : stockDataList) {
            System.out.println(stockData);
        }
    }

    private static double getLastClosePrice(String symbol) throws IOException {
        String apiUrl = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + API_KEY;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("Error: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
        JsonObject globalQuote = json.getAsJsonObject("Global Quote");
        double lastClose = globalQuote.get("05. price").getAsDouble();

        return lastClose;
    }

    private static LocalDate getLastCloseDate(String symbol) throws IOException {
        String apiUrl = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + API_KEY;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("Error: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
        JsonObject globalQuote = json.getAsJsonObject("Global Quote");
        String lastCloseDateStr = globalQuote.get("07. latest trading day").getAsString();
        LocalDate lastCloseDate = LocalDate.parse(lastCloseDateStr);

        return lastCloseDate;
    }

    public static double getInitialPrice(String symbol) throws IOException {
        // Get the last close date
        LocalDate lastCloseDate = getLastCloseDate(symbol);

        // Construct the URL to retrieve stock data for the last close date
        String apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=" + symbol +
                "&apikey=" + API_KEY;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to retrieve stock data. Response Code: " + responseCode);
        }

        // Read the response data
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        // Parse the JSON response
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
        JsonObject timeSeries = json.getAsJsonObject("Time Series (Daily)");
        JsonObject dailyData = timeSeries.getAsJsonObject(lastCloseDate.toString());
        double initialPrice = Double.parseDouble(dailyData.get("4. close").getAsString());

        return initialPrice;
    }


    public static void apiCall(HttpURLConnection conn) {
    }
}

class StockData {
    private LocalDate date;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    public StockData(LocalDate date, double open, double high, double low, double close, long volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public long getVolume() {
        return volume;
    }

    @Override
    public String toString() {
        return "Date: " + date +
                ", Open: " + open +
                ", High: " + high +
                ", Low: " + low +
                ", Close: " + close +
                ", Volume: " + volume;
    }
}
