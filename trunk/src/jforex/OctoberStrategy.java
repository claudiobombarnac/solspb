package jforex;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

public class OctoberStrategy implements solspb.jforex.IStrategy {
    
  
    private IContext context = null;
    private IEngine engine = null;
    private IChart chart = null;
    private IHistory history = null;
    private IIndicators indicators = null;
    private IConsole console = null;
    private double volume = 4;
    private int profitLimit;
    private int lossLimit;
    private double bidPrice;
    private double askPrice;
    private double accountEquity;

  
    public void onStart(IContext context) throws JFException {
        this.context = context;  
        engine = context.getEngine();
        indicators = context.getIndicators();
        history = context.getHistory();
        console = context.getConsole();
        indicators = context.getIndicators();
    }

    public void onStop() throws JFException {
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {

    }

    // count opened positions
    protected int positionsTotal(Instrument instrument) throws JFException {
        int counter = 0;
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED) {
                counter++;
            }
        }
        return counter;
    }

    protected String getLabel(Instrument instrument) {
        String label = instrument.name();
        
        long time = new java.util.Date().getTime();

        label = label.substring(0, 2) + label.substring(3, 5);
        label = label + time;
        label = label.toLowerCase();
        return label;
    }

    public void onBar(Instrument instrument, Period period, IBar askbar, IBar bidbar) throws JFException {
        
       if(period == Period.ONE_HOUR) { 
      
        profitLimit = 42;
        lossLimit = 36;
       
                      
      if (askbar.getVolume() == 0) return;
      

             double openPrice = bidbar.getOpen();
             
             askPrice = askbar.getClose();
             bidPrice = bidbar.getClose();
            
          
           double sma50 = this.indicators.sma(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 50, 0);
           double sma100 = this.indicators.sma(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 100, 0); 
           double sma200 = this.indicators.sma(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 200, 0);
           
          double [] bands = this.indicators.bbands(instrument,
                period,
                OfferSide.BID,
                IIndicators.AppliedPrice.CLOSE,
                30,
                0.8,
                0.8,
                IIndicators.MaType.DEMA,
                0);
                
          // double adx = this.indicators.adx(instrument, period, OfferSide.BID, 14, 0);
               
       if (positionsTotal(instrument) == 0) {
           
            
                if (bidPrice > bands[0]  && openPrice < bands[0] && bidPrice > sma50 
                && bidPrice > sma100 && bidPrice > sma200) {
                     
                 buy(instrument, engine, profitLimit, lossLimit, volume);
                  
                 }
                
                else  if (bidPrice < bands[2] && openPrice > bands[2]  && bidPrice < sma50
                && bidPrice < sma100 && bidPrice < sma200) { 
                    
                   sell(instrument, engine, profitLimit, lossLimit, volume);
                    
                  }   
                
    }
    }
}
  public void onMessage(IMessage message) throws JFException {
        
        if (message != null && message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
            
            IOrder lastOne = message.getOrder();
            
            double profitsLoss = lastOne.getProfitLossInPips();
            
            console.getOut().println("Order : "+lastOne.getLabel()+ " "+ lastOne.getOrderCommand()+ " Pips: "+profitsLoss);
            
           
        }
}
     

  @Override
   public void onAccount(IAccount account) throws JFException {
       
   }
    public void sell(Instrument instrument, IEngine engine, int takeProfitPipLevel, int endLossPipLevel, double volumeParam)  throws JFException {
        
        engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.SELL, volumeParam, 0, 3, bidPrice
                        + instrument.getPipValue() *endLossPipLevel, bidPrice - instrument.getPipValue() * takeProfitPipLevel);
     } 
     
      public void buy(Instrument instrument, IEngine engine, int takeProfitPipLevel, int endLossPipLevel, double volumeParam)  throws JFException {
        
         engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.BUY, volumeParam, 0, 3, askPrice
                        - instrument.getPipValue() * endLossPipLevel, askPrice + instrument.getPipValue() * takeProfitPipLevel);
     } 
}