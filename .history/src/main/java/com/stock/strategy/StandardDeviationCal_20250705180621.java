package com.stock.strategy;

public class StandardDeviationCal {
double annualizedFactor = 0.0625;
    public double compute_std_dev_1(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor;
     }
    public double compute_std_dev_2(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*2;
     }
    public double compute_std_dev_3(double close_price,double vol_val)
     {
         return close_price*vol_val*annualizedFactor*3;

     }
    
}
