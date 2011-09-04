package jforex;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.IUserInterface;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

public class AutoTrader_Demo implements solspb.jforex.IStrategy {
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

	public Instrument defInst1 = Instrument.GBPUSD;
	public Period defPeriod = Period.FOUR_HOURS;        
	public double slippage = 4;
    	public double lot;
	public double minlot = 0.001;
	public double maxlot = 5;
	public double riskpct = 0.33;
	public double TP = 38;   
	public double SL = 105;
	public int trailingStop = 10;
	public int _UseHourTrade=0;
	public int _FromHourTrade=18;
	public int _ToHourTrade=24; 
	public int lockPip = 3;
	public boolean moveBE = true;

	public SimpleDateFormat df = new SimpleDateFormat("MMdd_HHmmss");
	
	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole(); 
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		this.userInterface = context.getUserInterface();
		console.getOut().println("---Auto Trader ON---");
     	}

	public void onAccount(IAccount account) throws JFException {
        	this.account = account;
    	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
		tradingAllowed = true;
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
		    
		double PIP = instrument.getPipValue();        
		int pos=0;
		int dos=0;

		for (IOrder order : engine.getOrders(instrument)) {    
			if (order.getState() == IOrder.State.CREATED) tradingAllowed = false;
			if (order.getState() == IOrder.State.FILLED ) {
				tradingAllowed = false;
				if (order.isLong())  pos=1;                                 
				else                 pos=-1; 
													       
				if (SL>0 && order.getStopLossPrice()==0) order.setStopLossPrice(order.getOpenPrice() - pos*PIP*SL, OfferSide.BID, trailingStop);
				if (TP>0 && order.getTakeProfitPrice()==0) order.setTakeProfitPrice(order.getOpenPrice() + pos*PIP*TP);

            			boolean isLong;
                                double open, stop, diff, newStop;

                                isLong = order.isLong();
                                open = order.getOpenPrice();
                                stop = order.getStopLossPrice();
                                diff = open - stop;             // stop loss distance
 				if (isLong) {                   // long side order             
                                	if (moveBE && diff > 0 && tick.getBid() > (open + diff)) {
						newStop = open + instrument.getPipValue() * lockPip;
						order.setStopLossPrice(newStop);                                               
            				}    
        			} else {                                  // short side order                    
                   			if (moveBE && diff < 0 && tick.getAsk() < (open + diff)) {      // diff is negative                        
                   				newStop = open - (instrument.getPipValue() * lockPip);
                                                order.setStopLossPrice(newStop);
                                        }                              
                                }
			}
         	}
         
		if (pos==0) {
			double rsi = indicators.rsi(instrument, defPeriod, OfferSide.BID, IIndicators.AppliedPrice.OPEN, 5, 0);  

                   	if (rsi <= 0) return;
  
			if (rsi<30 && tradingAllowed == true) {
				lot = getLot(instrument);
				engine.submitOrder(getLabel(instrument,calendar)+"_L", instrument, IEngine.OrderCommand.BUY, lot, 0, slippage);
				print(instrument.name() + ": Long entry, lot = " + lot);
			}
			if (rsi>70 && tradingAllowed == true) {
				lot = getLot(instrument);
				engine.submitOrder(getLabel(instrument,calendar)+"_S", instrument, IEngine.OrderCommand.SELL, lot, 0, slippage);
				print(instrument.name() + ": Short entry, lot = " + lot);
			}
		}      
	}
    
	//Calculate and return lot size based on acceptable risk and min/max lot sizes
	private double getLot(Instrument instrument) throws JFException {
		double amount = riskpct * account.getEquity();
		double lotsize = amount / (SL * instrument.getPipValue());
		lotsize /= 1e6;
		if(lotsize < minlot) lotsize = minlot;
		if(lotsize > maxlot) lotsize = maxlot;
		lotsize = (int)(lotsize * 1000) / (1000d);
		print("Setting risk to $" + (int)amount);
		return lotsize;
	}

	//Create label
	private String getLabel(Instrument instrument, Calendar calendar) {
		String label = instrument.name();
		label = label.substring(0, 3) + label.substring(3, 6);
		label = label + "_" + df.format(calendar.getTime());
		return label;
	}	
	
	public void print(String string) { this.console.getOut().println(string); }
	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {}
	public void onMessage(IMessage message) throws JFException {}
	public void onStop() throws JFException { print("---Auto Trader OFF---"); }
}
