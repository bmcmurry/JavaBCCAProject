import java.util.HashMap;

public class BuildHashMap {
    public static HashMap<String, String> buildHashMap(String keyword) {
        GetStockSymbolApi myObject = new GetStockSymbolApi();
        HashMap<String, String> stockSymbols = myObject.getStockSymbols(keyword);

        if (stockSymbols.containsKey(keyword)) {
            String stockSymbol = stockSymbols.get(keyword);
            System.out.println("Stock symbol for " + keyword + ": " + stockSymbol);
        } else {
            System.out.println("Symbol not found for " + keyword);
        }

        return stockSymbols;
    }



}
