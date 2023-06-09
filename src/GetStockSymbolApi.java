import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class GetStockSymbolApi {
    public HashMap<String, String> getStockSymbols(String companyName) {
        String apiKey = "ZHF986AD1CJJSSAP";
        HashMap<String, String> stockSymbols = new HashMap<>();

        try {
            // Create the URL for the API call
            String urlString = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + companyName + "&apikey=" + apiKey;
            URL url = new URL(urlString);

            // Create a connection object
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set the request method
            conn.setRequestMethod("GET");

            // Read the API response
            Main.apiCall(conn);

            // Check if the API call was successful
            JsonObject jsonResponse = new JsonObject();
            if (jsonResponse.has("bestMatches")) {
                // Extract company names and symbols from the response
                JsonArray resultArray = jsonResponse.getAsJsonArray("bestMatches");
                for (int i = 0; i < resultArray.size(); i++) {
                    JsonObject companyObject = resultArray.get(i).getAsJsonObject();
                    String symbol = companyObject.get("1. symbol").getAsString();
                    String name = companyObject.get("2. name").getAsString();

                    stockSymbols.put(name, symbol);
                }
            } else {
                System.out.println("No stock symbols found for " + companyName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stockSymbols;
    }
}
