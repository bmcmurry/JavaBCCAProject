import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockLookup {

    private Map<String, String> listingStatus;

    public StockLookup() {
        listingStatus = new HashMap<>();
        populateListingStatus();
    }

    private void populateListingStatus() {
        // Establish database connection
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String username = "postgres";
        String password = "password";

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            // Execute SQL query to retrieve stock data from the listing_status table
            String query = "SELECT name, symbol FROM listing_status";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            // Populate the listingStatus map with the retrieved data
            while (resultSet.next()) {
                String stockName = resultSet.getString("name");
                String stockSymbol = resultSet.getString("symbol");
                listingStatus.put(stockName, stockSymbol);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String findStockSymbol(String stockName) {
        if (stockName == "") {
            System.out.println("you must enter a company to search");
            return null;
        }
        // First, check if the stock symbol is already in the listingStatus map
        if (listingStatus.containsKey(stockName)) {
            return listingStatus.get(stockName);
        }

        // If not found, use Alpha Vantage API to search for the stock symbol
        try {
            String apiKey = "ZHF986AD1CJJSSAP";
            String queryUrl = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + URLEncoder.encode(stockName, "UTF-8") + "&apikey=" + apiKey;

            // Make a HTTP request to the Alpha Vantage API
            URL url = new URL(queryUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read the response from the API
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Extract the viable stock names from the JSON response
            List<String> stockNames = extractStockNames(response.toString());

            // Check if the response contains any viable stock names
            if (!stockNames.isEmpty()) {
                // Get the first stock name as the chosen stock
                String chosenStockName = stockNames.get(0);

                // Check if the stock symbol already exists in the listingStatus map
                if (listingStatus.containsValue(chosenStockName)) {
                    System.out.println("Stock symbol already exists: " + chosenStockName);
                    return null;
                }

                // Add the retrieved stock symbol and name to the listingStatus map
                listingStatus.put(chosenStockName, stockName);
                return stockName;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Stock not found.");
        return null;
    }

    private List<String> extractStockNames(String jsonResponse) {
        List<String> stockNames = new ArrayList<>();

        // Parse the JSON response
        JSONObject json = new JSONObject(jsonResponse);
        printJsonData(json);


        // Check if the response contains any stock symbols
        if (json.has("bestMatches")) {
            JSONArray matches = json.getJSONArray("bestMatches");
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String stockName = match.getString("2. name");
                stockNames.add(stockName);
            }
        }

        return stockNames;
    }

    private void printJsonData(JSONObject json) {
        boolean isNewName = true; // Flag to track new name

        for (String key : json.keySet()) {
            if (key.equals("bestMatches")) {
                JSONArray matches = json.getJSONArray(key);
                for (int i = 0; i < matches.length(); i++) {
                    JSONObject match = matches.getJSONObject(i);
                    String stockName = match.getString("2. name");
                    String stockSymbol = match.getString("1. symbol");
                    String stockType = match.getString("3. type");
                    String currency = match.getString("8. currency");


                    System.out.println("Name: " + stockName);
                    System.out.println("Symbol: " + stockSymbol);
                    System.out.println("Type: " + stockType);
                    System.out.println("Currency: " + currency);
                    System.out.println();
                }
            }
        }
    }

}