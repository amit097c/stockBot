package com.stock.api;


import com.stock.model.StockBar;
import com.stock.util.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AlpacaApiClient 
{
    private static final String API_KEY = "PKMXFOXOMDPMNUMB3TXR";
    private static final String API_SECRET = "64LdMS9gQnAf4bm8el3NIhNkuuj3slpG3lfvde6p";
    private static final String BASE_URL = "https://data.alpaca.markets/v2/stocks/bars";
    public List<StockBar> fetchStockBars(String symbol) {
        List<StockBar> result = new ArrayList<>();
        try {
            String queryParams = String.format(
                "symbols=%s&timeframe=15Min&start=%s&end=%s&limit=1000&adjustment=raw&feed=sip&sort=asc",
                symbol, TimeUtils.getTimeWindow().get("start"), TimeUtils.getTimeWindow().get("end")
            );

            URL url = new URL(BASE_URL + "?" + queryParams);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("APCA-API-KEY-ID", API_KEY);
            conn.setRequestProperty("APCA-API-SECRET-KEY", API_SECRET);
            conn.setRequestProperty("accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray barsArray = jsonResponse.getJSONObject("bars").getJSONArray(symbol);

            for (int i = 0; i < barsArray.length(); i++) {
                JSONObject bar = barsArray.getJSONObject(i);
                StockBar stockBar = new StockBar(
                        symbol,
                        bar.getDouble("o"),
                        bar.getDouble("h"),
                        bar.getDouble("l"),
                        bar.getDouble("c"),
                        bar.getLong("v"),
                        bar.getString("t"),
                        bar.getInt("n"),
                        bar.getDouble("vw"),
                        TimeUtils.getCurrentDateTime()
                );
                result.add(stockBar);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}

    

