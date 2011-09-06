

package jforex;

import java.util.concurrent.TimeUnit;

import solspb.jforex.IStrategy;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IIndicators.MaType;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

public class CashboneNov implements IStrategy {

	// these are strategy interfaces which are used to execute trades, request indicators, print messages
	private IEngine engine;
    private IIndicators indicators;
    private IConsole console;
    private IHistory history;

    
    public double amount = 3.3;
    public double slippage = 150;
    public Instrument myInstrument = Instrument.EURUSD;
    public Period myPeriod = Period.TEN_MINS;
    public int hiStoch = 68;
    public int loStoch = 41;
    public int fastKperiod = 11;
 
 /////////////// Belongs to stopLossATR() /////////////////////////////////
    public int periodSpan = 180;
    public double stepFactor = 3.7;
    public double favorFactor = 5.1;
    long nuTime = 0;
    double step = 0.0;
    double pipsInFavor = 0.0;
    
///////////////// For countStuff() //////////////////////////
    double diffLong = 0;
    double diffShort = 0;
    double totalLong = 0;
    double totalShort = 0;
    int tradeCount = 0;
    int winCount = 0;
    int lossCount = 0; 
///////////////////////////////////////////////////////////  
 
    double ccyScale = Math.pow(10, myInstrument.getPipScale());
    private boolean stopSet = false;
    private boolean tradingAllowed;
    private boolean loggingEnabled = true;
    private IOrder order = null;  
           
          
    @Override
    public void onStart(IContext context) throws JFException {
// we initialize interfaces in the onStart method
        engine = context.getEngine();
        indicators = context.getIndicators();
        console = context.getConsole();
        history = context.getHistory();       
        
        print("Started");
    }

	
	@Override
	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
		if (myPeriod == period && myInstrument == instrument && tradingAllowed) {
		
            countStuff();
            
            double stoch = indicators.stoch(myInstrument, myPeriod, OfferSide.BID, fastKperiod, 3, MaType.SMA, 3, MaType.SMA, 1) [0];
            double stochPrev = indicators.stoch(myInstrument, myPeriod, OfferSide.BID, fastKperiod, 3, MaType.SMA, 3, MaType.SMA, 2) [0];
            
			// condition when we BUY
			if (stoch > hiStoch && stochPrev < hiStoch && order == null) {
                submitOrder("Buy", OrderCommand.BUY);
				
			// condition when we SELL
			} else if (stoch < loStoch && stochPrev > loStoch && order == null) {
                submitOrder("Sell", OrderCommand.SELL);
			}
            

       }
    
	}
    
    
    private void submitOrder(String label, OrderCommand orderCmd) throws JFException {
        order = engine.submitOrder(label, myInstrument, orderCmd, amount);
        
        while (order.getState() == IOrder.State.CREATED || order.getState() == IOrder.State.OPENED){
            order.waitForUpdate(2, TimeUnit.SECONDS);
        }
        
        if (order.getState() == IOrder.State.CANCELED){                                                            
           order = null;
        }                           
    }
    
   
    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {    
        if (order != null && instrument.equals(myInstrument)) {
      
           if (order.getState() == IOrder.State.FILLED) { 
                           
              stopLossATR(tick); 
                      
           }
          
        }
    }
    
    
	public void print(String string) {

		if (loggingEnabled) console.getOut().println(string);
		
	}
    
    
    // Count pips made/lost for long/short respectively (printed in onStop). Also resets stuff.
    public void countStuff() {
            if (order != null && order.getState() == IOrder.State.CLOSED){
                
                    double open = order.getOpenPrice();
                    double close = order.getClosePrice();
                
                    if (order.isLong()) {
                        diffLong = close - open;
                        totalLong = totalLong + diffLong;
                        if (diffLong > 0) {
                            winCount++;
                        } else {
                            lossCount++;
                        }
                    }
                    else if (!order.isLong()) {
                        diffShort = open - close;
                        totalShort = totalShort + diffShort;
                        if (diffShort > 0) {
                            winCount++;
                        } else {
                            lossCount++;
                        }
                    }
                
                tradeCount++;
                order = null;
                stopSet = false;
            }
     }  
      
 
   public void stopLossATR(ITick tick) throws JFException {
       
      long time = history.getStartTimeOfCurrentBar(myInstrument, myPeriod);
           
          if (nuTime != time) {
              double atr = indicators.atr(myInstrument, myPeriod, OfferSide.BID, periodSpan, Filter.WEEKENDS, 1, time, 0)[0];
              step = atr * stepFactor;
              pipsInFavor = atr * favorFactor;
              nuTime = time;
          } 
            
              double fillPrice = order.getOpenPrice();
              double currentStopLossPrice = order.getStopLossPrice();
              double bid = tick.getBid();
              double ask = tick.getAsk();
              double moveStopLong = bid - step;
              double moveStopShort = ask + step;
              
              
              // We set initial stop-loss / move stop-loss as rate moves in our favor
              if (order.isLong()) {
                   if (!stopSet) {
                   order.setStopLossPrice(fillPrice - step);
                   stopSet = true;
                   }
                   else if (currentStopLossPrice + pipsInFavor < moveStopLong) {
                        order.setStopLossPrice(moveStopLong);
                      //  print("Stop for LONG moved to " + moveStopLong);
                   }
              }   
              else if (!order.isLong()) {
                  if (!stopSet) {
                  order.setStopLossPrice(fillPrice + step);
                  stopSet = true;
                  }
                  else if (currentStopLossPrice - pipsInFavor > moveStopShort) {
                        order.setStopLossPrice(moveStopShort);
                     //   print("Stop for SHORT moved to " + moveStopShort);
                  }
              }
              
    } // closes stopLossATR
        
                            
	@Override
	public void onAccount(IAccount account) throws JFException {
		
		if (account.getUseOfLeverage()==0) tradingAllowed=true;

	}

	@Override
	public void onMessage(IMessage message) throws JFException {

	}

	@Override
	public void onStop() throws JFException {

        double printLong = (double)Math.round(totalLong * 100000) / 100000;
        printLong = (double)Math.round(printLong * 1000000) / 100d;
        double printShort = (double)Math.round(totalShort * 100000) / 100000;
        printShort = (double)Math.round(printShort * 1000000) / 100d;
        double printTotal = printLong + printShort;
        
        print("Pips long +/-: " + printLong);
        print("Pips short +/-: " + printShort);
        print("Total pips +/-: " + (printLong+printShort));
        print(printTotal + " pips x " + (amount * 100) + " dollar/pip equals " + printTotal * (amount * 100) + " dollar");
        print("Number of positions closed: " + tradeCount + " ( -> trades = " + tradeCount*2 + ")");
        print("Winners: " + winCount);
        print("Losers: " + lossCount);
        
        print("Stopped");
        
	}
    

}



