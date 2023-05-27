import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Main {

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

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a company name: ");
        String companyName = scanner.nextLine();

        String stockSymbol = getStockSymbol(companyName);
        if (stockSymbol != null) {
            String stockData = getStockData(stockSymbol);
            if (stockData != null) {
                processStockData(stockData);
            } else {
                System.out.println("Failed to fetch stock data for " + stockSymbol);
            }
        } else {
            System.out.println("Stock symbol not found for " + companyName);
        }
    }

    private static String getStockSymbol(String companyName) {
        String stockSymbol = null;

        try {
            String apiUrl = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" +
                    companyName + "&apikey=" + API_KEY;

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
                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(response.toString()).getAsJsonObject();

                JsonArray resultArray = json.getAsJsonArray("bestMatches");
                if (resultArray.size() > 0) {
                    JsonObject companyObject = resultArray.get(0).getAsJsonObject();
                    stockSymbol = companyObject.get("1. symbol").getAsString();
                }
            } else {
                System.out.println("Error: " + responseCode);
            }
        } catch (IOException e) {
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
            boolean isUpLastWeek = closingPrices.size() >= 2 ? isStockUp(getSubList(closingPrices, 2)) : false;
            boolean isUpLast3Months = closingPrices.size() >= 14 ? isStockUp(getSubList(closingPrices, 14)) : false;
            boolean isUpLast6Months = closingPrices.size() >= 27 ? isStockUp(getSubList(closingPrices, 27)) : false;
            boolean isUpLastYear = closingPrices.size() >= 54 ? isStockUp(getSubList(closingPrices, 54)) : false;

            LocalDate lastWeekEndDate = currentWeekStart.minusDays(1); // Get the end date of the previous week

            LocalDate last3MonthsStartDate = currentDate.minusMonths(3).plusDays(1); // Get the start date of the last 3 months
            LocalDate last3MonthsEndDate = currentDate; // Get the end date of the last 3 months

            LocalDate last6MonthsStartDate = currentDate.minusMonths(6).plusDays(1); // Get the start date of the last 6 months
            LocalDate last6MonthsEndDate = currentDate; // Get the end date of the last 6 months

            LocalDate lastYearStartDate = currentDate.minusYears(1).plusDays(1); // Get the start date of the last year
            LocalDate lastYearEndDate = currentDate; // Get the end date of the last year

            System.out.println("Stock Analysis:");
            System.out.println("==========================================");
            System.out.println("Is the stock up this week? " + isUpThisWeek);
            System.out.println("Is the stock up last week (" + lastWeekEndDate + ")? " + isUpLastWeek);
            System.out.println("Is the stock up last 3 months (" + last3MonthsStartDate + " to " + last3MonthsEndDate + ")? " + isUpLast3Months);
            System.out.println("Is the stock up last 6 months (" + last6MonthsStartDate + " to " + last6MonthsEndDate + ")? " + isUpLast6Months);
            System.out.println("Is the stock up last year (" + lastYearStartDate + " to " + lastYearEndDate + ")? " + isUpLastYear);
        } else {
            System.out.println("No stock data available.");
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
