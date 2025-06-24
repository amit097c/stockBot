package com.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{

    // Setting the API key and secret as constants
    private static final String API_KEY = "PKMXFOXOMDPMNUMB3TXR";
    private static final String API_SECRET = "64LdMS9gQnAf4bm8el3NIhNkuuj3slpG3lfvde6p";
    private static final String BASE_URL = "https://data.alpaca.markets/v2/stocks/bars";

    public static void main( String[] args )
    {
      String symbol = "AAPL";
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      ZonedDateTime lastWeek = now.minusDays(7);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    // last 7 trading days of market data.
      Map<String, String> timeWindow = getMarketTimeWindowUtc();
      String start = timeWindow.get("start");
      String end = timeWindow.get("end");

     
     
      //String start = formatter.format(lastWeek);
      //String end = formatter.format(now);

     String queryParams = String.format(
    "symbols=%s&timeframe=15Min&start=%s&end=%s&limit=1000&adjustment=raw&feed=sip&sort=asc",
    symbol, start, end
);     //String endpoint = "/stocks/" + symbol + "/bars?timeframe=1Day&limit=5";
       try {

            System.out.println("base url + queryParams: " + BASE_URL + "?" + queryParams);
            URL url1=new URL(BASE_URL + "?" + queryParams);
            
            // Construct the full URL
//"https://data.alpaca.markets/v2/stocks/bars";
            URL url = new URL("https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=15Min&start=2025-05-18T00:00:00Z&end=2025-05-25T00:00:00Z&limit=1000&adjustment=raw&feed=sip&sort=asc");
            String actual_url=url.toString();
            System.out.println("Actual URL: " +  actual_url);
            System.out.println("are they equal? "+ actual_url.equals(BASE_URL + "?" + queryParams));
            
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
           
            conn.setRequestMethod("GET");

            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("accept", "application/json");

            
            conn1.setRequestMethod("GET");

            conn1.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn1.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn1.setRequestProperty("accept", "application/json");

            
            
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            int responseCode1 = conn.getResponseCode();
            System.out.println("Response Code of dynamic url: " + responseCode1);

            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            BufferedReader in1 = new BufferedReader(
                new InputStreamReader(conn1.getInputStream())
            );

            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println("Response of static url JSON:");
            System.out.println(response.toString());

            response = new StringBuilder();

            while ((inputLine = in1.readLine()) != null) {
                response.append(inputLine);
            }
            in1.close();
            System.out.println("Response of dynamic url JSON:");
            System.out.println(response.toString());



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> getMarketTimeWindowUtc() {
        // Define the Eastern Time Zone and formatter
        ZoneId eastern = ZoneId.of("America/New_York");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        //initialize the list to hold market days
        List<ZonedDateTime> marketDays = new ArrayList<>();
        ZonedDateTime current = ZonedDateTime.now(eastern).withHour(16).withMinute(0).withSecond(0).withNano(0);

        // Step 1: Skip to previous weekday if today is Sat/Sun
        while (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }

        // Step 2: Collect 7 previous weekdays
        while (marketDays.size() < 7) {
            if (current.getDayOfWeek().getValue() >= 1 && current.getDayOfWeek().getValue() <= 5) {
                marketDays.add(current);
            }
            current = current.minusDays(1);
        }

        // Step 3: Get earliest and latest valid market timestamps in UTC
        ZonedDateTime startET = marketDays.get(6).withHour(9).withMinute(30); // Oldest day start
        ZonedDateTime endET = marketDays.get(0).withHour(16).withMinute(0);   // Newest day end

        ZonedDateTime startUTC = startET.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endUTC = endET.withZoneSameInstant(ZoneOffset.UTC);

        Map<String, String> result = new HashMap<>();
        result.put("start", formatter.format(startUTC));
        result.put("end", formatter.format(endUTC));

        return result;
    }
}
