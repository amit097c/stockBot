package com.stock.strategy;

public class StandardDeviationCal {
double annualizedFactor = 0.0247;//0.0625;
   public double compute(double close_price,double vol_val,int level)
    {
      return close_price*vol_val*annualizedFactor*level;
    }


    public double compute_std_dev_1(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor;
     }
    public double compute_std_dev_1_25(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*1.25;
     }
    public double compute_std_dev_1_50(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*1.5;
     } 
    public double compute_std_dev_1_75(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*1.75;
     }  
    public double compute_std_dev_1_77(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*1.77;
     }  
    public double compute_std_dev_2(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*2;
     }
     public double compute_std_dev_2_25(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*2.25;
     } 

       public double compute_std_dev_2_50(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*2.5;
     } 
       public double compute_std_dev_2_75(double close_price,double vol_val)
     {
        return close_price*vol_val*annualizedFactor*2.75;
     }
    public double compute_std_dev_3(double close_price,double vol_val)
     {
         return close_price*vol_val*annualizedFactor*3;

     }
    
}
