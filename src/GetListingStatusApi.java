import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GetListingStatusApi {
    public List<String[]> getListingStatus() {
        String apiKey = "ZHF986AD1CJJSSAP";
        List<String[]> listingStatus = new ArrayList<>();

        try {
            String urlString = "https://www.alphavantage.co/query?function=LISTING_STATUS&apikey=" + apiKey;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            CSVReader csvReader = new CSVReader(new InputStreamReader(connection.getInputStream()));
            try {
                listingStatus = csvReader.readAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (CsvException e) {
                throw new RuntimeException(e);
            }

            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listingStatus;
    }
}
