package jforex;

import java.util.*;
import com.dukascopy.api.*;

public class power implements solspb.IStrategy {
    

private IEngine engine;
private IConsole console;
private IHistory history;
private IContext context;
private IIndicators indicators;
private IUserInterface userInterface;
private IAccount account;

public int counter = 0;
public int _day;
public int _hours;
public int _minutes;  
public long _signalBarTime = 0;
private boolean tradingAllowed;
private boolean allowed;
private boolean good;

public Instrument defInst1 = Instrument.GBPUSD;
    
public Period defPeriod = Period.FOUR_HOURS;        

public double acceleration = 0.01;
    
public double maximum = 0.01;

public int shift = 0;

public double slippage = 4;
    
public double lot;
    
public double TP = 38;
    
public double SL = 105;

public int trailingStop = 10;

public int _UseHourTrade=0;

public int _FromHourTrade=18;

public int _ToHourTrade=24; 

public int lockPip = 3;
       
public boolean moveBE = true;

 
    

  public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.indicators = context.getIndicators();
        this.console = context.getConsole();    
        console.getOut().println("---Started---");
        this.history = context.getHistory();        
    this.context = context;
    this.userInterface = context.getUserInterface();                   
        
    }

public void onAccount(IAccount account) throws JFException {
      
        this.account = account;
        if (account.getUseOfLeverage() == 0) { tradingAllowed = true;
} else tradingAllowed = false;
    }


public void onStop() throws JFException {
        
    }

    
    public void onTick(Instrument instrument, ITick tick) throws JFException {
       
            
        if (instrument != defInst1)
            return;
            
            _signalBarTime=tick.getTime();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(_signalBarTime);
                        
        _day=calendar.get(Calendar.DAY_OF_WEEK)-1;
        _hours=calendar.get(Calendar.HOUR_OF_DAY);
        _minutes=calendar.get(Calendar.MINUTE);        
        
        if(_day==5 && _hours>20) return;
        if(_day>5 || (_day==0 && _hours<21)) return;
        
        
        if (_UseHourTrade==1) {
              if (!(_hours >= _FromHourTrade && _hours <= _ToHourTrade)) {
                 return;
              }
        }      
            
  double rsi = indicators.rsi(instrument, defPeriod, OfferSide.BID, IIndicators.AppliedPrice.OPEN, 5, 0);  
  
  



        
double PIP = instrument.getPipValue();        
int pos=0;
int dos=0;

for (IOrder order : engine.getOrders(instrument)) {
            

            
if (order.getState() == IOrder.State.FILLED ) {
if (order.isLong())  pos=1;                                 
else                 pos=-1; 
                
            
                                                                                               
if (SL>0 && order.getStopLossPrice()==0) order.setStopLossPrice(order.getOpenPrice() - pos*PIP*SL, OfferSide.BID, trailingStop);
if (TP>0 && order.getTakeProfitPrice()==0) order.setTakeProfitPrice(order.getOpenPrice() + pos*PIP*TP);

            


        
               
               boolean isLong;
                                double open, stop, diff, newStop;
                                String label = order.getLabel();
                                
                               
                                isLong = order.isLong();
                                open = order.getOpenPrice();
                                stop = order.getStopLossPrice();
                                diff = open - stop;             // stop loss distance
 if (isLong) {                   // long side order             
                                        if (moveBE && diff > 0 && tick.getBid() > (open + diff)) {
                                                                                              newStop = open + instrument.getPipValue() * lockPip;
                                                order.setStopLossPrice(newStop);                                               
                
            }    
        } 
        
        else {                                  // short side order                    
                   if (moveBE && diff < 0 && tick.getAsk() < (open + diff)) {      // diff is negative                        
                   newStop = open - (instrument.getPipValue() * lockPip);
                                                order.setStopLossPrice(newStop);
                                                               }                              
                                }

        
        
        
        }
         }
         
                       

if (pos==0) {
           double eq = this.account.getEquity();                      

          
lot = 4.2;



if (rsi <= 0)                
            return;
        

Date datetime = new Date(tick.getTime());

  int day  = datetime.getDate();  
  

if (rsi<30 && tradingAllowed == true)  engine.submitOrder("LONG"+(day*100+counter++), instrument, IEngine.OrderCommand.BUY, lot, 0, slippage);              
if (rsi>70 && tradingAllowed == true)  engine.submitOrder("SHORT"+(day*100+counter++), instrument, IEngine.OrderCommand.SELL, lot, 0, slippage); 

         }      
    }

    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        
        
        
       
        }



    



    
    
    
    public void onMessage(IMessage message) throws JFException {

    
}
    
    
}