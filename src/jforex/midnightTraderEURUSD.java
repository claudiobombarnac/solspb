package jforex;

import java.util.*;
import com.dukascopy.api.*;

public class midnightTraderEURUSD implements IStrategy { 

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IAccount account;

    public Period periodMA2 = Period.ONE_MIN;
       
    public double size;
    public double slip = 5;
    private double prevMA = 0;
    private double prevMA2 = 0;
    public boolean activeTrade = true;
    private int tagCounter = 0;
    public double tempProfit = 0;
    public long signalBarTime = 0;
    private double accountEquity;
    public double workingUnit = 0.01;
    public double Risk = 0.1; 
    public double buyHigh1;
    public double sellLow1;
    public double buyHigh2;
    public double sellLow2;
    public double buyHighTemp;
    public double sellLowTemp;
    public boolean buyOrderOpen = false;
    public boolean sellOrderOpen = false;
    private double dailyStopLimit1 = 0;
    private double dailyProfitLimit1 = 0;
    private double dailyStopLimit2 = 0;
    private double dailyProfitLimit2 = 0;
    private double dailyStopLimitTemp;
    private double dailyProfitLimitTemp;
    private double temp = 0;
    public double dailyHigh;
    public double dailyLow;
    public double dailyATR;

    
    public Instrument instrument1 = Instrument.EURUSD; 
    public Instrument instrument2 = Instrument.GBPUSD;  
    

    public double stopATRRatio = 1.0;
    

    public double profitATRRatio = 0.5;

    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.indicators = context.getIndicators();
        this.console = context.getConsole();
        this.history = context.getHistory();        
        this.context = context;
        this.userInterface = context.getUserInterface();   
    }

    public void onAccount(IAccount account) throws JFException {

       accountEquity = account.getEquity();
       
       this.account = account;
    // One Trade at a time, check to see if either SL or TP has been tripped
        if (account.getUseOfLeverage() == 0) { 
            activeTrade = false;
        }                 
        else {
            activeTrade = true;
            buyOrderOpen = false;
            sellOrderOpen = false;
        }   

    }

    public void onStop() throws JFException {
        console.getOut().println("It's over!!!!");
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (period != (periodMA2) ||  (!instrument.equals(instrument1) && !instrument.equals(instrument2))) return; 
            
        double Un = Risk * accountEquity / 1000;
        double workingUnit = 0.25 * Math.round(Un); 

        signalBarTime = bidBar.getTime(); 
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(signalBarTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int minute = calendar.get(Calendar.MINUTE);
        int month = calendar.get(Calendar.MONTH);
        double askPrice = askBar.getClose();
        double bidPrice = bidBar.getClose();              

        if (hour == 23 & minute == 54) {
            closeOpenOrders();
//            closeAllPositions();
            buyOrderOpen = false;
            sellOrderOpen = false;
//            }                   
            
//        if (hour == 23 & minute == 55) {
                dailyHigh = indicators.max(instrument, Period.ONE_HOUR, OfferSide.ASK, IIndicators.AppliedPrice.HIGH, 24, 0);
                dailyLow = indicators.min(instrument, Period.ONE_HOUR, OfferSide.ASK, IIndicators.AppliedPrice.LOW, 24, 0);
                dailyATR = indicators.atr(instrument, Period.DAILY, OfferSide.ASK, 5,0);
                
                if (instrument == instrument1){
                      dailyStopLimit1 = Math.round(dailyATR * stopATRRatio*10000);
                      dailyProfitLimit1 = Math.round(dailyATR * profitATRRatio*10000);
                      if (dailyHigh < askPrice + 10*instrument.getPipValue()) {
                           buyHigh1 = askPrice + 10*instrument.getPipValue();
                      }
                      else {
                          buyHigh1 = dailyHigh;
                      }
                      if (dailyLow > bidPrice - 10*instrument.getPipValue()) {
                          sellLow1 = bidPrice - 10*instrument.getPipValue();
                      }
                      else {
                          sellLow1 = dailyLow;
                      }
                      console.getOut().println (" New Day " );
                      console.getOut().println (month + " " + day + " " + hour + " " + minute + " " + instrument + "  Daily Low = " + dailyLow + "  Daily High = " + dailyHigh
                           + "    ask Price = " + askPrice + "    Buy High  = "  + buyHigh1 + "    Sell Low = " + sellLow1);          
               }  
               if (instrument == instrument2){
                      dailyStopLimit2 = Math.round(dailyATR * stopATRRatio*10000);
                      dailyProfitLimit2 = Math.round(dailyATR * profitATRRatio*10000);
                      if (dailyHigh < askPrice + 10*instrument.getPipValue()) {
                           buyHigh2 = askPrice + 10*instrument.getPipValue();
                      }
                      else {
                          buyHigh2 = dailyHigh;
                      }
                      if (dailyLow > bidPrice - 10*instrument.getPipValue()) {
                          sellLow2 = bidPrice - 10*instrument.getPipValue();
                      }
                      else {
                          sellLow2 = dailyLow;
                      }

                      console.getOut().println (month + " " + day + " " + hour + " " + minute + " " + instrument + "  Daily Low = " + dailyLow + "  Daily High = " + dailyHigh
                           + "    ask Price = " + askPrice + "    Buy High  = "  + buyHigh2 + "    Sell Low = " + sellLow2);   
                } 
        }
        
        if (instrument == instrument1){
            buyHighTemp = buyHigh1;
            sellLowTemp = sellLow1;
            dailyStopLimitTemp = dailyStopLimit1;
            dailyProfitLimitTemp = dailyProfitLimit1;
            }
        if ( instrument == instrument2) {
            buyHighTemp = buyHigh2;
            sellLowTemp = sellLow2;
            dailyStopLimitTemp = dailyStopLimit2;
            dailyProfitLimitTemp = dailyProfitLimit2;
           } 
//        }     
        
        if ((askPrice > buyHighTemp - 10*instrument.getPipValue() 
            & askPrice < buyHighTemp) 
            & buyOrderOpen == false
            & activeTrade == false ) {
              engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.BUYSTOP, workingUnit, buyHighTemp, slip, 
              buyHighTemp - (dailyStopLimitTemp*instrument.getPipValue()), buyHighTemp + (instrument.getPipValue() * dailyProfitLimitTemp));
 //             buyHighTemp - (dailyStopLimitTemp), buyHighTemp + (dailyProfitLimitTemp));              
              buyOrderOpen = true;
              console.getOut().println ( "Submit Order for " + instrument + " @ Ask Price = " + askPrice + "  Daily Buy Price = "
                    + buyHighTemp + "  Put in Order Long for " + workingUnit + "Units   dailyStopLimitTemp=" + dailyStopLimitTemp + " dailyProfitLimitTemp=" + dailyProfitLimitTemp);
            }
        
        if (buyOrderOpen == true
            & askPrice < buyHighTemp - 10*instrument.getPipValue()) {
                buyOrderOpen = false;
                console.getOut().println ( "Cancel Order Long");
                closeOpenOrders();
                
            }
         if (bidPrice < sellLowTemp + 10*instrument.getPipValue()
             & bidPrice> sellLowTemp
             & sellOrderOpen == false
             & activeTrade == false) {
                engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.SELLSTOP, workingUnit, sellLowTemp, slip, 
                sellLowTemp + (dailyStopLimitTemp*instrument.getPipValue()), sellLowTemp - (instrument.getPipValue() * dailyProfitLimitTemp));
 //               sellLowTemp + (dailyStopLimitTemp), sellLowTemp - (dailyProfitLimitTemp));
                sellOrderOpen = true;
              console.getOut().println ( "Submit Order for " + instrument + " @ Ask Price = " + askPrice + "  Daily Buy Price = " 
                        + sellLowTemp + "  Put in Order Short for " + workingUnit + "Units   dailyStopLimitTemp=" + dailyStopLimitTemp + " dailyProfitLimitTemp=" + dailyProfitLimitTemp);
              }
        if (sellOrderOpen == true
            & bidPrice > sellLowTemp + 10*instrument.getPipValue()) {
                sellOrderOpen = false;
                console.getOut().println (" Cancel Order Short");
                closeOpenOrders();
            }  
    
    }
   
    public void onMessage(IMessage message) throws JFException {
    }

    protected String getLabel(Instrument instrument) {
        String label = instrument.name();
        label = label.substring(0, 2) + label.substring(3, 5) + "mt";
        label = label + (tagCounter++);
        label = label.toLowerCase();
        return label;
    }

    public void closeAllPositions() throws JFException {
         for (IOrder order : engine.getOrders())
             {IOrder.State myStateOrder = order.getState();
                    if (myStateOrder == IOrder.State.OPENED ||
                        myStateOrder == IOrder.State.CREATED  ||
                        myStateOrder == IOrder.State.FILLED)
                    {order.close();
                    }
        }
    }
    
    public void closeOpenOrders() throws JFException {
         for (IOrder order : engine.getOrders())
             {IOrder.State myStateOrder = order.getState();
                    if (myStateOrder == IOrder.State.OPENED)
                    {order.setRequestedAmount(0);
                    }
        }
    }
}   