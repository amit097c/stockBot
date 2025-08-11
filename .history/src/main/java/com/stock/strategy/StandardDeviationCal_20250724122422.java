package com.stock.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StandardDeviationCal {
double annualizedFactor = 0.0247;//0.0625;
public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException("Decimal places must be non-negative.");

    BigDecimal bd = BigDecimal.valueOf(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
}
   public double compute(double close_price,double vol_val,double level)
     {
        return round(close_price*vol_val*annualizedFactor*level,2);
     }
    public double compute_std_dev_1(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor,2);
     }
    public double compute_std_dev_1_25(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*1.25,2);
     }
    public double compute_std_dev_1_50(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*1.5,2);
     } 
    public double compute_std_dev_1_75(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*1.75,2);
     }  
    public double compute_std_dev_1_77(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*1.77,2);
     }  
    public double compute_std_dev_2(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*2,2);
     }
     public double compute_std_dev_2_25(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*2.25,2);
     } 

       public double compute_std_dev_2_50(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*2.5,2);
     } 
       public double compute_std_dev_2_75(double close_price,double vol_val)
     {
        return round(close_price*vol_val*annualizedFactor*2.75,2);
     }
    public double compute_std_dev_3(double close_price,double vol_val)
     {
         return round(close_price*vol_val*annualizedFactor*3,2);

     }
    
}
