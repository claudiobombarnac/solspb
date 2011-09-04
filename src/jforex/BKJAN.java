package jforex;

import java.util.*;

import com.dukascopy.api.*;

public class BKJAN implements solspb.IStrategy {
    private IEngine engine = null;
    private IIndicators indicators = null;
    private int tagCounter = 0;
    private IConsole console;
    private double lastdelh = 0;
    int timeout=-1;
    IOrder theorder;
    boolean unlocked=true;
    int direction=0;
    Period theperiod = Period.FIFTEEN_MINS;
    Instrument theinstrument = Instrument.EURUSD;
    int thespread = 10;
    double baseorder=25; //use of leverage. S
    double fastnumber = 0.3;
    double slownumber = 0.06;
    int timeoutvalue= 5;
    int tp = 150;
    public int sl = 40;
    
    IAccount account;
    double orderlockin = 20;
    double slfastnumber=0.02;
    double slslownumber=0.15;
    
    boolean startallowed=false; //Added to prevent strategy from starting an order immediately
    boolean wasdown=false;
    boolean wasup=false;
     
    private int activepositions() throws JFException{
        int i=0;
        for (IOrder order : engine.getOrders()) {
            if (order.getState() == IOrder.State.FILLED && order.getLabel().equals("MS")){
                i++;
            }
        }
        return(i);
        
    }
    public void onStart(IContext context) throws JFException {
        
        engine = context.getEngine();
        indicators = context.getIndicators();
        this.console = context.getConsole();
        console.getOut().println("Started");
        for (IOrder order : engine.getOrders()) {
            if (order.getLabel().equals("MS")){
                theorder = order;
            }
        }
    }

    public void onStop() throws JFException {     
        console.getOut().println("Stopped");
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (instrument == theinstrument){ 
            if (activepositions()>0){
            double[] h12forstop = indicators.mama(instrument,
                      Period.ONE_HOUR,
                      OfferSide.BID,
                      IIndicators.AppliedPrice.CLOSE,
                      slfastnumber,
                      slslownumber,
                      0);
            double thestop = h12forstop[1];
                            
            if(theorder.getState() == IOrder.State.FILLED){
                    if (theorder.getProfitLossInPips() >orderlockin){
                        if (theorder.isLong()){
                            if (theorder.getStopLossPrice()!=thestop&&thestop>theorder.getOpenPrice()){
                                theorder.setStopLossPrice(thestop);
                            }
                        }
                        else {
                            if (theorder.getStopLossPrice()!=thestop&&thestop<theorder.getOpenPrice()){
                                theorder.setStopLossPrice(thestop);
                            }
                        }
                    }               
            }
        }
        
            double ordersize = baseorder*account.getEquity()/1000000/(tick.getAsk()+(tp+20)*instrument.getPipValue());
            if (unlocked){
                unlocked=false;
            
                double[] h12 = indicators.mama(instrument,
                      theperiod,
                      OfferSide.BID,
                      IIndicators.AppliedPrice.CLOSE,
                      fastnumber,
                      slownumber,
                      0);
                double tl = indicators.ht_trendline(instrument, theperiod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 0); 
                double delh = h12[0]-h12[1];
                double spread = Math.max(Math.max(h12[0],h12[1]),tl)-Math.min(Math.min(h12[0],h12[1]),tl);
                
                if (!startallowed){
	                if (delh<0){
	                    wasdown=true;
	                }
	                if (delh>0){
	                    wasup =true;
	                }
                    if(wasup&&wasdown){
                        startallowed=true;
                    }
                }
                if (startallowed&&delh>1*instrument.getPipValue()) {
                    if (timeout <0 && activepositions() == 0&& spread>thespread*instrument.getPipValue()&&direction!=1) {  
                        theorder = engine.submitOrder("MS", 
                        instrument, 
                        IEngine.OrderCommand.BUY, 
                        ordersize, 
                        0, 
                        20, 
                        tick.getBid() - instrument.getPipValue() * sl, 
                        tick.getBid() + instrument.getPipValue() * tp);
                        timeout = timeoutvalue;
                        direction=1;
                    } 
                }
                else if (startallowed&&delh<-1*instrument.getPipValue())  {
                    if (timeout <0 && activepositions() == 0&& spread>thespread*instrument.getPipValue()&&direction!=-1) {                               
                        theorder = engine.submitOrder("MS", 
                        instrument, 
                        IEngine.OrderCommand.SELL, 
                        ordersize, 
                        0,
                        20, 
                        tick.getAsk() + instrument.getPipValue() * sl, 
                        tick.getAsk() - instrument.getPipValue() * tp);
                        
                        timeout=timeoutvalue;
                        direction=-1;
                     }
                }
                lastdelh=delh;
            }
        }
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
	   if (instrument == theinstrument){ 
	       if (period == theperiod){
	           unlocked=true;
               timeout--;	       
	       }
	   }
    }

    public void onMessage(IMessage message) throws JFException {}
    public void onAccount(IAccount account) throws JFException {this.account=account;}
}