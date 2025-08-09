import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class CryptoPriceApp {
    public static void main(String[] args) {
        String cryptoId = "bitcoin"; // Can be "ethereum", "dogecoin", etc.
        String currency = "usd";     // You can use "eur", "vnd", etc.

        try {
            String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" 
                            + cryptoId + "&vs_currencies=" + currency;

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            double price = json.getJSONObject(cryptoId).getDouble(currency);

            System.out.println("Current " + cryptoId + " price in " + currency.toUpperCase() + ": " + price);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
