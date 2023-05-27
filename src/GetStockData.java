import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetStockData {
    private static final String API_KEY = "ZHF986AD1CJJSSAP";

    public String fetchStockData(String symbol) throws IOException {
        String apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=" +
                symbol + "&apikey=" + API_KEY;

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
            // Parse JSON response using Gson
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.toString(), JsonObject.class);

            // Extract relevant data points
            JsonObject timeSeries = json.getAsJsonObject("Weekly Time Series");

            if (timeSeries != null) {
                for (String key : timeSeries.keySet()) {
                    JsonObject dataPoint = timeSeries.getAsJsonObject(key);

                    // Extract high and low values
                    String high = dataPoint.get("2. high").getAsString();
                    String low = dataPoint.get("3. low").getAsString();

                    // Perform analysis or comparisons with the data
                    // ...

                    // Print the high and low values
                    System.out.println("Date: " + key);
                    System.out.println("High: " + high);
                    System.out.println("Low: " + low);
                }
            } else {
                System.out.println("No time series data found.");
            }
        } else {
            System.out.println("Error: " + responseCode);
            return null;
        }

        return apiUrl;
    }
}
