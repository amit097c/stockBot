package com.stock.strategy;

import java.util.List;

import com.stock.model.StockBar;

public class MovingAverageStrategy {
      public enum Action {
        BUY, SELL, HOLD
    }
     /*
      *Executes a 50-period moving average crossover strategy.
     * @param bars List of historical StockBar data
     * @return Action (BUY, SELL, or HOLD)
     */
    public static Action getSignal(List<StockBar> bars) {
        int n = bars.size();
        if (n < 51) return Action.HOLD;

        double movingAvg = 0.0;
        for (int i = n - 51; i < n - 1; i++) {
            movingAvg += bars.get(i).getClose();
        }
        movingAvg /= 50;

        double prevClose = bars.get(n - 2).getClose();
        double currentClose = bars.get(n - 1).getClose();

        if (prevClose < movingAvg && currentClose > movingAvg) 
         {
            System.out.println("Moving Average Crossover BUY Signal at "+currentClose );
            return Action.BUY;
         }
        if (prevClose > movingAvg && currentClose < movingAvg) 
         {
            System.out.println("Moving Average Crossover Sell Signal at "+currentClose );
            return Action.SELL;
         }
        return Action.HOLD;
    }
}
