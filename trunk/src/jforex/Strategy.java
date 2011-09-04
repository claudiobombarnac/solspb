package jforex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.Properties;

public class Strategy implements solspb.jforex.IStrategy {
	//
	private 		Vector<Stg_Base> 	strategys;        
    private			boolean			 	mInitialized = false;
    private			long				mTimeInfoS	 = 0;
    private			long				mTimeLev	 = 0;
    private	static	boolean				test		 = false;
	//
    private static	IEngine 			engine 		= null;	
	private static 	IConsole 			console		= null;
	private static 	IContext 			context		= null;
	//
	public  static 	Properties 	    	propiedades	= new Properties();;
	public  static	Logger  			logger		= null;
    //
    public void onStart(IContext context) throws JFException {
    	try {
            this.context    = context;
            engine 			= context.getEngine();	        	        
            console 		= context.getConsole();	 
            strategys		= new Vector<Stg_Base>();
            logger			= new Strategy.Logger(console, "", true, true, false);
             
            Stg_Base 		stg;
            Set<Instrument> susinst = context.getSubscribedInstruments();
            //
            console.getOut().println("Satarting strategy ... ");
            //
            loadProperties();
            //
            console.getOut().println("Setting up instruments/strategys/signalers");
            //            
			addInstrument(Instrument.EURUSD);			
            addInstrument(Instrument.USDJPY);
            addInstrument(Instrument.EURCHF);			
            addInstrument(Instrument.USDCHF);        
            addInstrument(Instrument.EURJPY);
            addInstrument(Instrument.EURGBP);			
            addInstrument(Instrument.GBPUSD);        
            addInstrument(Instrument.GBPCHF);
            addInstrument(Instrument.CHFJPY);
            addInstrument(Instrument.AUDCHF);
            addInstrument(Instrument.AUDNZD);
            addInstrument(Instrument.USDCAD);
            addInstrument(Instrument.AUDCAD);
            addInstrument(Instrument.AUDJPY);
            addInstrument(Instrument.CADCHF);
            addInstrument(Instrument.CADJPY);
            addInstrument(Instrument.EURAUD);
            addInstrument(Instrument.EURCAD);
            addInstrument(Instrument.GBPAUD);
            addInstrument(Instrument.GBPCAD);
            addInstrument(Instrument.GBPJPY);
            addInstrument(Instrument.GBPNZD);
            addInstrument(Instrument.NZDCAD);
            addInstrument(Instrument.NZDCHF);
            addInstrument(Instrument.NZDJPY);
            addInstrument(Instrument.NZDUSD);
            addInstrument(Instrument.USDMXN);        
            addInstrument(Instrument.EURDKK);
            addInstrument(Instrument.EURHKD);
            addInstrument(Instrument.EURNOK);
            addInstrument(Instrument.EURSEK);
            addInstrument(Instrument.USDDKK);
            addInstrument(Instrument.USDNOK);
            addInstrument(Instrument.USDSEK);
            addInstrument(Instrument.USDSGD);
            addInstrument(Instrument.USDTRY);
            //
            console.getOut().println("Initializing instruments/strategys/signalers");        
            if (!Strategy.test) susinst.removeAll(context.getSubscribedInstruments());
            for (int i=0; i < strategys.size(); i++) {
                stg = (Stg_Base)strategys.get(i);
                stg.initialize();
                if (!Strategy.test) susinst.add(stg.instrument);
                console.getOut().println("Strategy <"+stg.instrument.toString()+"> <"+stg.getName()+"> <"+stg.signaler.getSignalerName()+"> initialized");            
            }
            if (!Strategy.test) context.setSubscribedInstruments(susinst);	
            //
            console.getOut().println("STARTED");
            mInitialized = true;
            //    		
    	} catch (Exception e) {
    		console.getOut().println("ERROR starting strategy");
    		logger.error(e.getMessage());
    	}
    }

    public void onStop() throws JFException {
    	//
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        //
    	try {
    		if (!mInitialized) return;
    		//
	    	for (int i=0; i < strategys.size(); i++) {
				Stg_Base stg = (Stg_Base)strategys.get(i);
				if (stg.instrument == instrument) stg.evaluateTick(tick);
			}
	    	//	    		
    	} catch (Exception e) {	 
    		logger.error(e.getMessage());
    	}
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
    	try {
    		if (!mInitialized) return;
    		//
    		if (period == Period.THIRTY_MINS) {
    			int segs;
        		if (!Strategy.test) {
            		try {
            			segs = Math.round((askBar.getTime() - mTimeInfoS) / 1000);                
                        // control every minute
                        if (segs > 3600) {
            				Set<Instrument> susinst = context.getSubscribedInstruments();
            	            Iterator i    = susinst.iterator();
            				while (i.hasNext()) {
            					Instrument inst = (Instrument) i.next();
            					console.getOut().println("Subscribed --> " + inst.name());
            				}
            				mTimeInfoS = askBar.getTime();
            			}
            		} catch (Exception e){}    			
        		}
        		//
        		Calendar cal = Calendar.getInstance();
        		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            	cal.setTimeInMillis(askBar.getTime());
            	int dow = cal.get(Calendar.DAY_OF_WEEK); 
            	int hor = cal.get(Calendar.HOUR_OF_DAY); 
            	if ((dow >=6) && (hor >= 17)) {
            		segs = Math.round((askBar.getTime() - mTimeLev) / 1000);
            		if (segs > 1800) {
            			double uol = context.getAccount().getUseOfLeverage();
            			if (uol > 30) {
            				closeOrders();
            				logger.info(FormatDate(askBar.getTime()) +  " LEVERAGE CONTROL: " + uol + " -- Orders closed");
            			}
            			mTimeLev = askBar.getTime();
            		}
            	}
    		}  		
    		//
    		for (int i=0; i < strategys.size(); i++) {
				Stg_Base stg = (Stg_Base)strategys.get(i);
				if (stg.instrument == instrument) stg.evaluateBar(period, askBar, bidBar);
			}
	    	//	    		
    	} catch (Exception e) {	 
    		logger.error(e.getMessage());
    	}	    	
    }

    public void onMessage(IMessage message) throws JFException {
    	//
    }

    public void onAccount(IAccount account) throws JFException {	    	
    	//
    }
    
    private void loadProperties() throws Exception {
        try {
        	console.getOut().println("Loading properties");
         	//
        	propiedades.clear();        	
            propiedades.put("instrument.EURCAD","macd");                             
            propiedades.put("instrument.EURCHF","macd");
            propiedades.put("instrument.EURGBP","macd");
        	propiedades.put("instrument.EURJPY","macd");
        	propiedades.put("instrument.EURUSD","macd");        	
        	propiedades.put("instrument.GBPCHF","macd");
        	propiedades.put("instrument.GBPUSD","macd");        	
        	propiedades.put("instrument.NZDCHF","macd");            
        	propiedades.put("instrument.USDCHF","macd");        	
        	// Default strategy configuration
        	propiedades.put("strategy.gebCantidad","0.01");
        	propiedades.put("strategy.gebMaxOrders","1");
        	propiedades.put("strategy.gebRatGoal","60");
        	propiedades.put("strategy.gebRatStop","150");
        	propiedades.put("strategy.gebRatTrailing","0");
        	propiedades.put("strategy.gebOrderWithLimit","true");
        	propiedades.put("strategy.gebOrderToRun","false");
        	propiedades.put("strategy.gebReEnter","true");
        	// Default signaler and macd configuration
        	propiedades.put("signaler.gesPivotPart","3");
        	propiedades.put("signaler.gesEnterAtPR","true");
        	propiedades.put("signaler.gesSupRes","false");
        	propiedades.put("signaler.gesGoWithMainTrend","false");
        	propiedades.put("signaler.gesCountPerBreak","0");
        	propiedades.put("signaler.gesPlayInRange","false");
        	propiedades.put("signaler.macd.gemFast","12");
        	propiedades.put("signaler.macd.gemSlow","26");
        	propiedades.put("signaler.macd.gemSignal","9");
        	propiedades.put("signaler.macd.gemNumPer","5");
        	propiedades.put("signaler.macd.gemDivReg","false");
        	propiedades.put("signaler.macd.gemExitSignal","true");
        	propiedades.put("signaler.macd.gemExitWithSAR","false");
        	propiedades.put("signaler.macd.gemContinue","true");
        	//
        	propiedades.put("USDCHF.signaler.gesRatGoal","100");
        	propiedades.put("USDCHF.signaler.gesRatStop","100");
        	//
        	propiedades.put("EURCHF.signaler.gesRatGoal","60");
        	propiedades.put("EURCHF.signaler.gesRatStop","150");
        	//        	
        	propiedades.put("EURUSD.signaler.gesRatGoal","50");
        	propiedades.put("EURUSD.signaler.gesRatStop","200");
        	propiedades.put("EURUSD.signaler.gesPlayInRange","true");
        	//
        	propiedades.put("EURJPY.signaler.gesPlayInRange","true");
        	//
        	propiedades.put("USDJPY.signaler.gesRatGoal","60");
        	propiedades.put("USDJPY.signaler.gesRatStop","150");
        	//
        	propiedades.put("EURGBP.signaler.gesRatGoal","60");
        	propiedades.put("EURGBP.signaler.gesRatStop","100");
        	//        	
        	propiedades.put("GBPCHF.signaler.gesPlayInRange","true");
        	//
        	propiedades.put("NZDCHF.signaler.gesRatGoal","60");  
        	propiedades.put("NZDCHF.signaler.gesRatStop","150"); 
        	propiedades.put("NZDCHF.signaler.gesPlayInRange","true");
        	//
        	propiedades.put("EURCAD.signaler.gesPlayInRange","true");        	
        	//        	
    	} catch (Exception e) {
    		console.getOut().println("NO properties file found");
    		throw e;
    	}
    }
    
    private void addInstrument(Instrument instrument) {
    	try {
    		// Check if instrument is enabled
    		String 	sig = propiedades.getProperty("instrument."+instrument.name(), "").toLowerCase();
    		// Creating the signaler
    		Stg_Signaler stgsig = null;
    		if ("macd".equals(sig)) {
    			stgsig = new Stg_Signaler_MACD(context);     			
    		} else if ("stoch".equals(sig)) {
    			//stgsig = new Stg_Signaler_STOCH(context);
    		}
    		// If instrument has signaler, we add it
    		if (stgsig != null) {
    			strategys.add(new Stg_Base (context, instrument, stgsig));
    		}    		
    		//
    	} catch (Exception e) {
    		console.getOut().println("ERROR creating signaler for <"+instrument.name()+">");
    	}
    }
    
    
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   // enums

    enum Signals {
        NONE,
        ENTER_LONG,
        ENTER_SHORT,
        EXIT_LONG,
        EXIT_SHORT,
        ENTER_LONG_IFPOSITIVE,
        ENTER_SHORT_IFPOSITIVE,
        EXIT_LONG_IFPOSITIVE,
        EXIT_SHORT_IFPOSITIVE,
        POTENTIAL_EXIT_LONG,
        POTENTIAL_EXIT_SHORT,
        CHANGE_TO_LONG,
        CHANGE_TO_SHORT
    }
    
    enum States {
        NONE,
        ABOVE_SIGNAL,
        BELOW_SIGNAL,
        CROSSED_UP,
        CROSSED_DOWN
    }
    
    enum Levels {
	    PIVOT_ABOVE,
	    PIVOT_BELOW,
	    S1_ABOVE,
	    S1_BELOW,
	    S2_ABOVE,
	    S2_BELOW,
	    S3_ABOVE,
	    S3_BELOW,
	    R1_ABOVE,
	    R1_BELOW,
	    R2_ABOVE,
	    R2_BELOW,
	    R3_ABOVE,
	    R3_BELOW,
	    PVS1_MID,
	    PVR1_MID,
	    S1S2_MID,
	    R1R2_MID,
	    NONE
   }
   
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   // statics methods

    public static void openOrder(Instrument instrument, ITick tick, OrderCommand command, double Cantidad, double stop, double limit) {
        try {
            String label = getLabel(instrument);                
            double price = 0;
            if (command == IEngine.OrderCommand.SELL) {
                price = tick.getBid();
            } else { 
                price = tick.getAsk(); 
            }
            engine.submitOrder(label, instrument, command, Cantidad, 0, 0, stop, limit);            
            logger.info(FormatDate(tick.getTime()) + ": openOrder " + label + " " + FormatString(command.toString(), 4) + " --> lot: " + FormatFloat(Cantidad) + " price: " + FormatFloat(price) + ", limit: " + FormatFloat(limit) + ", stop: " + FormatFloat(stop));
            //
        } catch (JFException e) {
            logger.error(e.getMessage());
        }            
    }

    public static void openOrder(Instrument instrument, ITick tick, OrderCommand command, double Cantidad, boolean OrderWithLimit, int PipsGoal, int PipsStop) {
        try {
            double price = 0;
            double stop  = 0; 
            double limit = 0;
            if (command == IEngine.OrderCommand.SELL) {
                price = tick.getBid();
                stop  = price + instrument.getPipValue() * PipsStop;    
                limit = price - instrument.getPipValue() * PipsGoal;
            }
            else { 
                price = tick.getAsk(); 
                stop  = price - instrument.getPipValue() * PipsStop;
                limit = price + instrument.getPipValue() * PipsGoal;
            }
            if (!OrderWithLimit) limit = 0;
            //
            openOrder(instrument, tick, command, Cantidad, stop, limit);
            //
        } catch (Exception e) {
            logger.error(e.getMessage());
        }            
    }
    
    public static void closeOrder(String label) {
        try {
            for (IOrder order : engine.getOrders()) {
                if ((order.getState() == IOrder.State.FILLED) && (order.getLabel().equals(label))) {
                    order.close();                                  
                    ITick tick = context.getHistory().getLastTick(order.getInstrument());
                    double open  = order.getOpenPrice();
                    double close = 0;
                    String res   = "";                    
                    double diff  = Strategy.profitInPips(tick, order);
                    if (diff > 0) res = "  +++++ " + FormatFloat(diff) + " +++++ "; else res = "  ----- " + FormatFloat(diff) + " -----";                        
                    logger.info(FormatDate(tick.getTime()) + ": closeOrder  " + order.getLabel() + " " + FormatString(order.getOrderCommand().toString(),4) + " --> open : " + FormatFloat(open) + ", close: " + FormatFloat(close) + res);
                    return;
                }
            }
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }            
    }
    
    public static void closeOrders(Instrument instrument, OrderCommand command) {
        try {
            for (IOrder order : engine.getOrders(instrument)) {
                if ((order.getState() == IOrder.State.FILLED) && (order.getOrderCommand() == command)) {
                    closeOrder(order.getLabel());
                }
            }
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }            
    }
    
    public static void closeOrders() {
        try {
            for (IOrder order : engine.getOrders()) {
                if ((order.getState() == IOrder.State.FILLED)) {
                    closeOrder(order.getLabel());
                }
            }
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }            
    }
    
    public static boolean closePositiveOrders(ITick tick) {
        try {
            for (IOrder order : engine.getOrders()) {
                if ((order.getState() == IOrder.State.FILLED)) {
                   if (positiveOrder(order.getInstrument(), order.getOrderCommand())) {
                	   logger.info(FormatDate(tick.getTime()) + ": closeOrder  " + order.getLabel() + " -- POSITIVE");
                	   closeOrder(order.getLabel());
                	   return true;
                   }                                    
                }
            }
            //
            return false;
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        	return false;
        }            
    }
    
    public static void changeOrder(String label, double stop, double limit) {
        try {
            for (IOrder order : engine.getOrders()) {
                if ((order.getState() == IOrder.State.FILLED) && (order.getLabel().equals(label))) {
                	ITick tick = context.getHistory().getLastTick(order.getInstrument());
                	if ((stop > 0)  && (Math.abs(numberOfPips(order.getOpenPrice(),stop))  > 0))  order.setStopLossPrice(stop);
                    if ((limit > 0) && (Math.abs(numberOfPips(order.getOpenPrice(),limit)) > 0))  order.setTakeProfitPrice(limit);
                    logger.info(FormatDate(tick.getTime()) + ": changeOrder " + order.getLabel() + " " + FormatString(order.getOrderCommand().toString(),4) + " -- price: " + FormatFloat(tick.getBid()) + " open: " + FormatFloat(order.getOpenPrice()) + ", stop: " + FormatFloat(stop) + ", limit: " + FormatFloat(limit) + " (limit: "+FormatFloat(Math.abs(numberOfPips(order.getOpenPrice(),limit)))+")  (stop: "+FormatFloat(Math.abs(numberOfPips(order.getOpenPrice(),stop)))+")");                                        
                    return;
                }
            }
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }            
    }

    public static boolean changeOrders(Instrument instrument, ITick tick, int PipsGoal, int PipsTrailing) {
        try {
        	boolean res = false;
            for (IOrder order : engine.getOrders(instrument)) {
                if (order.getState() == IOrder.State.FILLED) {
                    double price  = 0;
                    double diff   = 0;
                    double open   = order.getOpenPrice();
                    double limit  = order.getTakeProfitPrice();                                
                    // We only change stop/limit once (when difference is close to goal)
                    double np = Math.abs(numberOfPips(open, limit));
                    if (np <= PipsGoal) {
                        diff = 0;
                        // We want to win pips with the STOP in the change, so we wait until we are very close to the LIMIT
                        if ((tick.getAsk() > open) && (order.getOrderCommand() == IEngine.OrderCommand.BUY) || (order.getOrderCommand() == IEngine.OrderCommand.BUYLIMIT)) {
                            price = tick.getAsk();
                            diff  = Math.abs(numberOfPips(price, open)); //(price - open) * 10000; // diferencia en pips
                        } 
                        if ((tick.getBid() < price) && (order.getOrderCommand() == IEngine.OrderCommand.SELL) || (order.getOrderCommand() == IEngine.OrderCommand.SELLLIMIT)) { 
                            price = tick.getBid(); 
                            diff  = Math.abs(numberOfPips(open, price)); //(open - price) * 10000; // diferencia en pips
                        }                                    
                        // We just chage stop/limit if is at 90% of the first goal
                        if (diff > PipsGoal * 0.9) {
                            double dstop  =  getNewStop(order, tick, instrument, PipsTrailing); //Math.abs(numberOfPips(order.getStopLossPrice(), stop));
                            double dlimit =  getNewLimit(order, tick, instrument, PipsTrailing); //Math.abs(numberOfPips(order.getTakeProfitPrice(), limit));
                            changeOrder(order.getLabel(), dstop, dlimit);
                            res = true;
                        }                                                               
                    }                                                                                                                            
                }
            }
            //
            return res;
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        	return false;
        }            
    }

    public static double getNewLimit(IOrder order, ITick tick, Instrument instrument, double pipsTrailing) {
    	try {
    		double limit  = -1;
    		double pipval = instrument.getPipValue();
    		if ((tick.getAsk() > order.getOpenPrice()) && (order.getOrderCommand() == IEngine.OrderCommand.BUY) || (order.getOrderCommand() == IEngine.OrderCommand.BUYLIMIT)) {
                limit = order.getTakeProfitPrice() + pipval * pipsTrailing;   // We set limit mPipsTrailing highger than last
            } 
            if ((tick.getBid() < order.getOpenPrice()) && (order.getOrderCommand() == IEngine.OrderCommand.SELL) || (order.getOrderCommand() == IEngine.OrderCommand.SELLLIMIT)) { 
                limit = order.getTakeProfitPrice() - pipval * pipsTrailing;   // We set limit mPipsTrailing lower than last
            }  
            return limit;
            //
    	} catch (Exception e) {
        	logger.error(e.getMessage());
        	return -1;
        }
    }

    public static double getNewStop(IOrder order, ITick tick, Instrument instrument, double pipsTrailing) {
    	try {
    		double stop	  = -1;
    		double pipval = instrument.getPipValue();
    		if ((tick.getAsk() > order.getOpenPrice()) && (order.getOrderCommand() == IEngine.OrderCommand.BUY) || (order.getOrderCommand() == IEngine.OrderCommand.BUYLIMIT)) {
    			stop = order.getStopLossPrice() + pipval * pipsTrailing;
            } 
            if ((tick.getBid() < order.getOpenPrice()) && (order.getOrderCommand() == IEngine.OrderCommand.SELL) || (order.getOrderCommand() == IEngine.OrderCommand.SELLLIMIT)) { 
            	stop = order.getStopLossPrice() - pipval * pipsTrailing;
            } 
            return stop;
            //
    	} catch (Exception e) {
        	logger.error(e.getMessage());
        	return -1;
        }
    }
    
    public static boolean positiveOrder(Instrument instrument, OrderCommand command) {
        boolean res = false;
        try {
            for (IOrder order : engine.getOrders(instrument)) {
                if ((order.getState() == IOrder.State.FILLED) && (order.getOrderCommand() == command)) {                        
                    ITick tick = context.getHistory().getLastTick(instrument);                	
                	double open   = order.getOpenPrice();
                    double limit  = order.getTakeProfitPrice();	
                    double price  = open;
                    double diff   = 0;
                    double expt	  = Math.abs(numberOfPips(limit, open));
                    if ((open < tick.getAsk()) && (order.getOrderCommand() == IEngine.OrderCommand.BUY) || (order.getOrderCommand() == IEngine.OrderCommand.BUYLIMIT)) {
                    	price = tick.getAsk();
                    	diff  = Math.abs(numberOfPips(price, open));
                    } 
                    if ((open > tick.getBid()) && (order.getOrderCommand() == IEngine.OrderCommand.SELL) || (order.getOrderCommand() == IEngine.OrderCommand.SELLLIMIT)) { 
                        price = tick.getBid(); 
                        diff  = Math.abs(numberOfPips(price, open));
                    }
                    if (diff >= expt) { // if (diff >= expt / 2) { // as we are doing trailing, we don´t exit if is not positive
                    	res = true; // If it has reached 50% we consider positive
                    	break;
                    }
                }
            }                                                    
            //
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }            
        return res;
    }
    
    public static String getLabel(Instrument instrument) {
        String label = instrument.name();
        label = label.substring(0, 2) + label.substring(3, 5);
        label = label + new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date());      
        label = label.toUpperCase();
        return label;
    }
    
    public static int numberOfSecs(Period period) {
        int res = 0;
        if (period == Period.FIVE_MINS)         res = 300;
        else if (period == Period.TEN_MINS)     res = 600;            
        else if (period == Period.FIFTEEN_MINS) res = 900;
        else if (period == Period.THIRTY_MINS)  res = 1800;
        else if (period == Period.ONE_HOUR)     res = 3600;
        else if (period == Period.FOUR_HOURS)   res = 14400;            
        else if (period == Period.DAILY)        res = 86400;        
        return res;
    }
    
    public static double numberOfPips(double p1, double p2) {
        // TODO: review
    	if (Math.abs(p1) >= 10) p1 = p1 * 100;
        else p1 = p1 * 10000;            
        //
        if (Math.abs(p2) >= 10) p2 = p2 * 100;
        else p2 = p2 * 10000;
        //            
        return p1 - p2;
    }
    
    public static double profitInPips(ITick tick, IOrder order) {
    	double diff = 0;
    	if (order.getOrderCommand() == OrderCommand.BUY) {
            diff  = numberOfPips(tick.getAsk(), order.getOpenPrice());                            
        } else {
            diff  = numberOfPips(order.getOpenPrice(),  tick.getBid());                            
        }
    	return diff;
    }
    
    public static long DifTime(long close, long open) {        
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(open);
        int wo = Calendar.WEEK_OF_YEAR;
        cal.setTimeInMillis(close);
        int wc = Calendar.WEEK_OF_YEAR;
        long dif = close - open;
        if (wc > wo) dif = dif - (wc-wo)*49*3600*1000;  //friday 21:00, sunday 22:00 (49 hours)     
        return dif; 
    }
    
    public static String DifTimeToStr(long time) {
        int taux = (int)time / 1000; //        
        int h = taux / 3600;
        taux = taux - 3600 * h;
        int m = taux / 60;
        taux = taux - 60 * m;
        int s = taux;        
        String res = "";
        if (h < 10) res = "0"+h+"h "; else res = h+"h ";
        if (m < 10) res = res + "0"+m+"m "; else res = res + m+"m ";
        if (s < 10) res = res + "0"+s+"s "; else res = res + s+"s ";
        return  res;
    }
    
    public static String FormatDate(long date) {        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");            
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date); 
    }
    
    public static String FormatFloat(double num) {
        DecimalFormat df = new DecimalFormat("##0.00000");
        String aux         = df.format(num);
        return aux;
    }
    
    public static String FormatString(String str, int len) {
        for (int i=str.length(); i <len; i++) str = str + " ";
        return str;
    }
    
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   // logger

    class Logger {
    	
    	private IConsole console = null;
    	private String	 logFile	= "";
    	private boolean  info	= true;
    	private boolean  error	= true;
    	private boolean  debug	= true;
    	
    	    	
    	public Logger(IConsole cons, String lf, boolean inf, boolean err, boolean dbg) {
    		console = cons;
    		logFile = lf;
    		info	= inf;
    		error	= err;
    		debug	= dbg;    
    		if (!"".equals(lf)) new File(logFile).delete();
    	}
    	
    	public void info(String Datos) {
    		if (info) {
    			Datos = "INFO  : " + Datos;
    			Log(Datos);
    		}
    	}
    	
    	public void error(String Datos) {
    		if (error) {
    			Datos = "ERROR :  " + Datos;
    			Log(Datos);    			
    		}    		
    	}
    	
    	public void debug(String Datos) {
    		if (debug) {
    			Datos = "DEBUG :  " + Datos;
    			Log(Datos);    			
    		}    		
    	}
    	
        private void Log(String Datos) {
      	   try {
      		   if (console != null) console.getOut().println(Datos);
      	       LogToFile(Datos);
      	   } catch (Exception e) {
      	   }
         }    
         
         private void LogToFile(String Datos) {
     		try {	
     			if ("".equals(Datos)) return;
     			if ("".equals(logFile)) return;
     			//
     			File 				file = new File(logFile);								
     			FileOutputStream 	fos  = new FileOutputStream(file, true);
     			OutputStreamWriter 	osw  = new OutputStreamWriter(fos, "Cp1252" /* "ISO-8859-1" */);
     			BufferedWriter 		bw 	 = new BufferedWriter(osw);
     			try {
     				bw.write(Datos+"\n");
     			} finally {
     				try {
     					bw.flush();
     					bw.close();
     				} catch (Exception e) {}
     				try {
     					osw.flush();
     					osw.close();
     				} catch (Exception e) {}
     				try {
     					fos.flush();
     					fos.close();
     				} catch (Exception e) {}
     			}
     		} catch (Exception e) {
     		}
     	}

    	
    }
        
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   // Stg_Base
  
   class Stg_Base  {
    //
    private		  long				 mFirstTickTime = 0;
    private		  long				 mTimeInfoB     = 0;
    //
    protected     IConsole           console        = null;
    protected     IContext           context        = null;
    protected     IEngine            engine         = null;
    protected     IIndicators        indicators     = null;
    protected     Instrument         instrument;
    protected     Stg_Signaler       signaler;
    // Strategy STATIC parameters
    protected     double             gebCantidad;         // Millions parts (0.01 = 10K = 10000)
    protected     int                gebMaxOrders;        // Maxim number of orders open simunltaneously
    protected     int                gebRatGoal;          // Ratio in % for objective, over pips day average
    protected     int                gebRatStop;          // Ratio in % for stop, over goal
    protected     int                gebRatTrailing;      // Ratio in % for stop/limit, over goal
    protected     boolean            gebOrderWithLimit;   // Create order with or without limit
    protected     boolean            gebOrderToRun;       // If it haven´t reached limit, we don´t close it in 30/70 signals (only if it has limit)
    protected     boolean            gebReEnter;          // If we have an enter signal, we are in and is positive, we close it and open another one
    // Strategy DYNAMIC parameters (instrument)
    protected     int                gdbPipsAvgDay;       // Average pis per day
    protected     int                gdbPipsGoal;         // Number of pips for goal
    protected     int                gdbPipsStop;         // Number of pips for stop
    protected     int                gdbPipsTrailing;     // Number of pips for trailing stop/limit
    //
    protected     boolean            mOpenedOrders        = false;
    protected     long               mTimeTrailing        = 0;
    //
    protected     String             mStgName            = "base";
    protected     boolean            mInitialized        = false;
    //
    
    public Stg_Base(IContext cont, Instrument inst, Stg_Signaler sig) {
        context     = cont;
        engine      = cont.getEngine();
        console     = cont.getConsole();
        instrument  = inst;
        signaler    = sig;
        indicators  = context.getIndicators();
    }
    
    public void initialize() {
        try {
        	logger.info("Initializing strategy <"+mStgName+"> for <"+instrument.name()+">");
        	//
        	// ESTATIC properties
            gebCantidad         = Strategy.propiedades.getProperty("strategy.gebCantidad", 0.01);
            gebMaxOrders        = Strategy.propiedades.getProperty("strategy.gebMaxOrders", 1);                
            gebRatGoal          = Strategy.propiedades.getProperty("strategy.gebRatGoal", 60);
            gebRatStop          = Strategy.propiedades.getProperty("strategy.gebRatStop", 100);
            gebRatTrailing      = Strategy.propiedades.getProperty("strategy.gebRatTrailing", 0);
            gebOrderWithLimit   = Strategy.propiedades.getProperty("strategy.gebOrderWithLimit", true);
            gebOrderToRun       = Strategy.propiedades.getProperty("strategy.gebOrderToRun", false);
            gebReEnter          = Strategy.propiedades.getProperty("strategy.gebReEnter", true);
            // DYNAMIC properties
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -180);
            Ind_price indp = new Ind_price(context, instrument, Period.DAILY);
            gdbPipsAvgDay  = indp.getPipsAverage(cal.getTimeInMillis(), 180);
            if (gdbPipsAvgDay > 0) {
                gdbPipsGoal        = Math.round(gdbPipsAvgDay * gebRatGoal / 100); 
                gdbPipsStop        = Math.round(gdbPipsGoal   * gebRatStop / 100);
                if (gebRatTrailing > 0) gdbPipsTrailing = Math.round(gdbPipsGoal * gebRatTrailing / 100);
                mInitialized    = true;                
            }
            //
            signaler.setPipsAvgDay(gdbPipsAvgDay);
            signaler.setPipsGoal(gdbPipsGoal);
            signaler.setPipsStop(gdbPipsStop);
            signaler.setPipsTrailing(gdbPipsTrailing);
            signaler.setInstrument(instrument);
            signaler.initialize();
            //
            logger.info("Strategy <"+mStgName+"> for <"+instrument.name()+"> initialized");
            //
        } catch (Exception e) {
            mInitialized    = false;    
            logger.error("ERROR initializing strategy <"+instrument.name()+"> <"+mStgName+"> <"+signaler.getSignalerName()+">");
        }                
    }
    
    public void evaluateTick(ITick tick) {
        try {
            if (!mInitialized) return;
            //
	    	if (mFirstTickTime == 0) mFirstTickTime = tick.getTime();
            boolean ordersAllowed = (numOrders() < gebMaxOrders);
            IOrder	order		  = getOrder(instrument);
            mOpenedOrders         = (order != null);
            //
            signaler.evaluateTick(tick, order, ordersAllowed);
            //
            processSignal(tick);
            //
            // Check if we change order stop/limit
            if ((mOpenedOrders) && (gebOrderWithLimit && (getPipsTrailing() > 0))) {
                int segs = Math.round((tick.getTime() - mTimeTrailing) / 1000);                
                // We check it every minute
                if (segs > 60) {
                    if (changeOrders(instrument, tick, getPipsGoal(), getPipsTrailing())) {
                    	signaler.setOrderChanged(true);
                    }
                    mTimeTrailing = tick.getTime();
                }
            }                                    
            //                    
        } catch (Exception e) {     
        	logger.error(e.getMessage());
        }
    }
    
    public void evaluateBar(Period period, IBar askBar, IBar bidBar) {
        try {
            if (!mInitialized) return;
            //
            try {
    			int segs = Math.round((askBar.getTime() - mTimeInfoB) / 1000);                
                // We check it every minute
                if (segs > 3600) {
    				signaler.info();
    				mTimeInfoB = askBar.getTime();
    			}
    		} catch (Exception e){}
    		//
            signaler.evaluateBar(period, askBar, bidBar);
            //                    
        } catch (Exception e) {   
        	logger.error(e.getMessage());
        }
    }
    
    public void info() {
    	try {
    		//    		
    	} catch (Exception e) {
    		logger.error(e.getMessage());
    	}
    }
    
    
    public void processSignal(ITick tick) {
        try {
            switch (signaler.getSignal()) {
                case ENTER_LONG :
                        if (mOpenedOrders && gebReEnter) {
                            if (positiveOrder(instrument, IEngine.OrderCommand.BUY)) {
                                closeOrders(instrument, IEngine.OrderCommand.BUY);
                                mOpenedOrders = false;
                            }
                        }
                        if (!mOpenedOrders) openOrder(instrument, tick, IEngine.OrderCommand.BUY, getCantidad(), gebOrderWithLimit, getPipsGoal(), getPipsStop());                                                                    
                        signaler.resetSignal();
                        break;
                        //
                case ENTER_SHORT:
                        if (mOpenedOrders && gebReEnter) {
                            if (positiveOrder(instrument, IEngine.OrderCommand.SELL)) {
                                closeOrders(instrument, IEngine.OrderCommand.SELL);
                                mOpenedOrders = false;
                            }
                        }
                        if (!mOpenedOrders) openOrder(instrument, tick, IEngine.OrderCommand.SELL, getCantidad(), gebOrderWithLimit, getPipsGoal(), getPipsStop());                                    
                        signaler.resetSignal();
                        break;
                        //
                case EXIT_LONG:
                        if (mOpenedOrders) closeOrders(instrument, IEngine.OrderCommand.BUY);
                        break;
                        //
                case EXIT_SHORT:
                        if (mOpenedOrders) closeOrders(instrument, IEngine.OrderCommand.SELL);
                        break;
                        //
                case ENTER_LONG_IFPOSITIVE:                       
                		if (closePositiveOrders(tick)) {
                			openOrder(instrument, tick, IEngine.OrderCommand.BUY, getCantidad(), gebOrderWithLimit, getPipsGoal(), getPipsStop());                                                                    
                            signaler.resetSignal();                			
                		}
                		break;
                case ENTER_SHORT_IFPOSITIVE:
                		if (closePositiveOrders(tick)) {
                			openOrder(instrument, tick, IEngine.OrderCommand.SELL, getCantidad(), gebOrderWithLimit, getPipsGoal(), getPipsStop());                                    
                            signaler.resetSignal();            			
                		}
                		break;
                		//
                case EXIT_LONG_IFPOSITIVE:
                        if (mOpenedOrders && (positiveOrder(instrument, IEngine.OrderCommand.BUY))) closeOrders(instrument, IEngine.OrderCommand.BUY);
                        break;
                        //
                case EXIT_SHORT_IFPOSITIVE:
                        if (mOpenedOrders && (positiveOrder(instrument, IEngine.OrderCommand.SELL))) closeOrders(instrument, IEngine.OrderCommand.SELL);
                        break;
                        //
                case POTENTIAL_EXIT_LONG:
                        if (!gebOrderWithLimit || (gebOrderWithLimit && !gebOrderToRun)) {
                            if (mOpenedOrders) closeOrders(instrument, IEngine.OrderCommand.BUY);                     
                        }
                        break;
                        //
                case POTENTIAL_EXIT_SHORT:
                        if (!gebOrderWithLimit || (gebOrderWithLimit && !gebOrderToRun)) {
                            if (mOpenedOrders) closeOrders(instrument, IEngine.OrderCommand.SELL);                     
                        }
                        break;
                        //
                case CHANGE_TO_LONG:
                        if (mOpenedOrders) {
                            if (getOrderCommand(instrument).equals("SELL")) {
                                closeOrders(instrument, IEngine.OrderCommand.SELL);
                                openOrder(instrument, tick, IEngine.OrderCommand.BUY, getCantidad(), false, getPipsGoal(), getPipsStop());
                            }
                        } 
                        else openOrder(instrument, tick, IEngine.OrderCommand.BUY, getCantidad(), false, getPipsGoal(), getPipsStop()); 
                        break;
                        //
                case CHANGE_TO_SHORT:
                        if (mOpenedOrders) {
                            if (getOrderCommand(instrument).equals("BUY")) {
                                closeOrders(instrument, IEngine.OrderCommand.BUY);
                                openOrder(instrument, tick, IEngine.OrderCommand.SELL, getCantidad(), false, getPipsGoal(), getPipsStop());
                            }
                        }
                        else openOrder(instrument, tick, IEngine.OrderCommand.SELL, getCantidad(), false, getPipsGoal(), getPipsStop());
                        break;
                        //
            }
            //
        } catch (Exception e) {         
        	logger.error(e.getMessage());
        }                        
    }
    
    public String getName() {
    	return mStgName;
    }
    
    public double getCantidad() throws Exception{
    	try {
        	/*
    		double cant = 0.01;    	
        	if (signaler.getCantidad() > -1) cant = signaler.getCantidad();
            else cant = gebCantidad;
        	return cant;
        	*/
        	// lets play hard for the contest !!!
        	long time  = context.getHistory().getLastTick(instrument).getTime();
        	Calendar cal = Calendar.getInstance();
        	cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        	cal.setTimeInMillis(time);
        	int dow = cal.get(Calendar.DAY_OF_WEEK); 
        	double uol  = 40;       // use of leverage we want to use
        	if (dow >= 5) uol = 20; // we decrease uol from thrusday
        	//
            double bal = context.getAccount().getEquity();
        	double lev = context.getAccount().getLeverage();
	    	double tftl = bal * lev; // Total free trading line (equity * leverage)
	    	double lot  = uol * tftl / 100 / 1000000;
	    	logger.info("CANTIDAD " + FormatDate(time) + " day:" + dow + " lot:" + lot);
	    	return Math.min(7.5, lot);
        	//
    	} catch (Exception e) {
    		logger.error(e.getMessage());
    		throw e;
    	}
    }

    public int getPipsGoal() {
        if (signaler.getPipsGoal() > -1) return signaler.getPipsGoal();
        else return gdbPipsGoal;
    }
    
    public int getPipsStop() {
        if (signaler.getPipsStop() > -1) return signaler.getPipsStop();
        else return gdbPipsStop;
    }

    public int getPipsTrailing() {
        if (signaler.getPipsTrailing() > -1) return signaler.getPipsTrailing();
        else return gdbPipsTrailing;        
    }
    
    public int getRatGoal() {
        if (signaler.getRatGoal() > -1) return signaler.getRatGoal();
        else return gebRatGoal;
    }

    public int getRatStop() {
        if (signaler.getRatStop() > -1) return signaler.getRatStop();
        else return gebRatStop;        
    }

    public int getRatTrailing() {
        if (signaler.getRatTrailing() > -1) return signaler.getRatTrailing();
        else return gebRatTrailing;        
    }
    
    protected boolean openedOrders(Instrument instrument) throws JFException {
        return (engine.getOrders(instrument).size() > 0);
    }
    
    protected int numOrders() {
        int res = 0;
        try {
            for (IOrder order : engine.getOrders()) {
                if (order.getState() == IOrder.State.FILLED) {
                    res = res + 1;
                }
            }
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }
        return res;
    }
    
    protected int numOrders(Instrument instrument) {
        int res = 0;
        try {
            for (IOrder order : engine.getOrders(instrument)) {
                if (order.getState() == IOrder.State.FILLED) {
                    res = res + 1;
                }
            }
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }
        return res;
    }
    
    protected IOrder getOrder(Instrument instrument) {
        try {
            for (IOrder order : engine.getOrders(instrument)) {
                if (order.getState() == IOrder.State.FILLED) {
                    return order;
                }
            }
        } catch (Exception e) {
        	logger.error(e.getMessage());
        }
        return null;
    }
    
    protected String getOrderCommand(Instrument instrument) {
    	String res = "";
    	IOrder order = getOrder(instrument);
    	if (order != null) res = order.getOrderCommand().name();
    	return res;
    }
        
                
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Stg_Signaler

  class Stg_Signaler {
    protected     IContext           context;
    protected     IIndicators        indicators             = null;
    protected     Instrument         instrument				= null;
    protected     String             SignalerName			= "";
    protected     Signals            Signal					= Signals.NONE;
    protected     States             State					= States.NONE;
    protected	  Ind_adx			 adx					= null;
    //
    // STATIC signaler parameters
    protected     int                gesPivotPart           = 3;
    protected     boolean            gesEnterAtPR			= false;
    protected	  double			 gesCantidad			= -1;
    protected     int                gesRatGoal				= -1;
    protected     int                gesRatStop				= -1;
    protected     int                gesRatTrailing			= -1;
    protected     boolean            gesSupRes				= true;
    protected     boolean            gesGoWithMainTrend		= false;
    protected     int                gesCountPerBreak		= 0;
    protected 	  double    		 gesTrend        		= 25;
    protected	  boolean			 gesPlayInRange			= true;
    // DYNAMIC signaler parameters
    protected     int                gdsPipsAvgDay			= -1;      
    protected     boolean            gdsOrderChanged        = false;
    protected     int                gdsPipsGoal            = -1; 
    protected     int                gdsPipsStop            = -1; 
    protected     int                gdsPipsTrailing        = -1;    
    //
    protected double	mAllTickVal		 	= 0;
    protected long		mAllTickTime	 	= 0;	
    protected Signals	mAllSignal		 	= Signals.NONE;	
    //
    protected long      mPivotTime			= 0;
    protected boolean   mPivotOpenAbove		= true;
    protected boolean   mPriceOpenAbove		= true;
    protected double	mOpenPrice			= 0;
    //
    public Stg_Signaler (IContext cont) {
        context             = cont;
        indicators          = cont.getIndicators();
        resetSignal();
    }
    
    public Signals getSignal() {
        return Signal;
    }
    
    public void setInstrument(Instrument inst) {
        instrument = inst;        
    }
    
    public void initialize() {
    	try {
        	//Log("Initializing signaler <"+SignalerName+"> for <"+instrument.name()+">");
        	//        
        	gesPivotPart        = Strategy.propiedades.getProperty("signaler.gesPivotPart", 3);
            gesEnterAtPR        = Strategy.propiedades.getProperty("signaler.gesEnterAtPR", false);
            gesSupRes           = Strategy.propiedades.getProperty("signaler.gesSupRes",true);
            gesGoWithMainTrend  = Strategy.propiedades.getProperty("signaler.gesGoWithMainTrend",false);
            gesCountPerBreak    = Strategy.propiedades.getProperty("signaler.gesCountPerBreak",0);
            gesTrend			= Strategy.propiedades.getProperty("signaler.gesTrend",25);
            gesPlayInRange      = Strategy.propiedades.getProperty("signaler.gesPlayInRange",true);
            //
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesCantidad"))        gesCantidad      = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesCantidad", -1.0);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesPivotPart"))       gesPivotPart     = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesPivotPart", 3);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesEnterAtPR"))       gesEnterAtPR     = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesEnterAtPR", false);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesSupRes"))          gesSupRes        = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesSupRes",true);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesGoWithMainTrend")) gesGoWithMainTrend = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesGoWithMainTrend",false);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesCountPerBreak"))   gesCountPerBreak = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesCountPerBreak",0);        
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesRatTrailing"))     gesRatTrailing   = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesRatTrailing",0);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesRatGoal"))         gesRatGoal       = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesRatGoal", 100);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesRatStop"))         gesRatStop       = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesRatStop", 100);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesTrend"))           gesTrend         = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesTrend",25);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.gesPlayInRange"))     gesPlayInRange   = Strategy.propiedades.getProperty(instrument.name()+".signaler.gesPlayInRange",true);
            //    
            if (gdsPipsAvgDay > 0) {
                if (gesRatGoal > 0) gdsPipsGoal = Math.round(gdsPipsAvgDay * gesRatGoal / 100);
                if (gesRatStop > 0) gdsPipsStop = Math.round(gdsPipsGoal   * gesRatStop / 100);
                if (gesRatTrailing > 0) gdsPipsTrailing = Math.round(gdsPipsGoal * gesRatTrailing / 100);
            }
            //
            Calendar cal = Calendar.getInstance();
            long time = cal.getTimeInMillis();
            long barTime = context.getHistory().getPreviousBarStart(Period.DAILY_SUNDAY_IN_MONDAY, time);
            //calculatePivot(barTime);
            //
            adx = new Ind_adx(context, indicators, instrument, Period.DAILY, gesTrend, gesPlayInRange);
            adx.calculate(barTime);
            //
            //Log("Signaler <"+SignalerName+"> for <"+instrument.name()+"> initialized");    		
    	} catch (Exception e) {	
    		logger.error(e.getMessage());
    	}
    }
    
    public String getSignalerName() {
        return SignalerName;
    }
    
    public double getCantidad() {
    	return gesCantidad;
    }
    
    public int getPipsGoal() {
        return gdsPipsGoal;
    }
    
    public int getPipsStop() {
        return gdsPipsStop; 
    }

    public int getPipsTrailing() {
        return gdsPipsTrailing; 
    }

    public int getRatGoal() {
        return gesRatGoal; 
    }

    public int getRatStop() {
        return gesRatStop; 
    }

    public int getRatTrailing() {
        return gesRatTrailing; 
    }
    
    public void setPipsGoal(int pips){
        gdsPipsGoal = pips;
    }

    public void setPipsStop(int pips){
        gdsPipsStop = pips;
    }
    
    public void setPipsTrailing(int pips){
        gdsPipsTrailing = pips;
    }

    public void setPipsAvgDay(int pips){
        gdsPipsAvgDay = pips;
    }
    
    public void setOrderChanged(boolean changed) {
        gdsOrderChanged = changed;
    }
    
    public void resetSignal() {
        Signal                = Signals.NONE;
        gdsOrderChanged        = false;
    }
    
    
    protected boolean signalWithTrend(Signals sig, long time) {
     	// Even if we don´t want to go with trend, if trend is high and is falling, signas won´t be very reliable, we avoid it
    	if (!gesGoWithMainTrend || (gesTrend == 0)) {
    		return true; //!adx.trendSlowing();
    	}
    	return adx.signalWithTrend(sig, time);
    }
    
    protected boolean notAllowedPending() {
    	return (mAllSignal != Signals.NONE);
    }
    
    protected void resetNotAllowed() {
        mAllTickVal		 = 0;
        mAllTickTime	 = 0;	
        mAllSignal		 = Signals.NONE;	
    }

    public void evaluateTick(ITick tick, IOrder order, boolean ordersAllowed) {
        //
    }
    
    public void evaluateBar(Period period, IBar askBar, IBar bidBar) {
    	try {
    		if (period == Period.DAILY) {
            	adx.calculate(bidBar.getTime());
            }
            //
    		if (period == Period.THIRTY_MINS) {
               long barTime = context.getHistory().getPreviousBarStart(Period.DAILY_SUNDAY_IN_MONDAY, bidBar.getTime());
               if (barTime != mPivotTime) {
             	  //calculatePivot(barTime);
             	  resetNotAllowed(); // Al cambiar de dia reseteamos las ordenes pendientes que no se hubieran entrado
               }
               //Levels lev = situatePrice(bidBar.getTime(), bidBar.getClose());
               //sumPriceLevel(lev);
               adx.calculate(bidBar.getTime());
            }
            //
            //situatePrice(bidBar.getTime(), bidBar.getClose());
            //
        } catch (Exception e) { 
     	   logger.error(e.getMessage());
        }  
    }
    
    public void info() {
    	try {
    		long time = new Date().getTime();
    		String trend = "NO_TREND";
    		if (adx.trending(time)) {
    			if (adx.trendIsLong(time)) trend = "LONG_TREND";
    			else if (adx.trendIsShort(time)) trend = "SHORT_TREND";
    		}
    		logger.info(instrument.name() + " " +  trend);
    		//    		
    	} catch (Exception e) {
    		logger.error(e.getMessage());
    	}
    }
          
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Stg_Signaler_MACD
  
  class Stg_Signaler_MACD extends Stg_Signaler {
        private Ind_macd  macd_l = null;
        private Ind_macd  macd_m = null;
        private Ind_macd  macd_s = null;
        //
        private long		mLastTickCalc       = 0;
        private boolean		mclE1			    = false;
        private boolean		mclE2			    = false;
        private boolean		mclE3			    = false;
        private boolean		mcsE1			    = false;
        private boolean		mcsE2			    = false;
        private boolean		mcsE3			    = false;
        //
        // STATIC MACD parameters
        private int        	gemFast            	= 12;
        private int        	gemSlow            	= 26;
        private int        	gemSignal        	= 9;
        private int        	gemNumPer        	= 5;
        private boolean    	gemExitSignal    	= false;
        private boolean    	gemExitWithSAR    	= false;
        private boolean 	gemContinue        	= true;
        
        public Stg_Signaler_MACD (IContext cont) {
            super(cont);
            //
            SignalerName 	= "MACD";
            //
            gemFast			= Strategy.propiedades.getProperty("signaler.macd.gemFast", 12);
            gemSlow			= Strategy.propiedades.getProperty("signaler.macd.gemSlow",26);
            gemSignal		= Strategy.propiedades.getProperty("signaler.macd.gemSignal",9);
            gemNumPer		= Strategy.propiedades.getProperty("signaler.macd.gemNumPer",5);
            gemExitSignal	= Strategy.propiedades.getProperty("signaler.macd.gemExitSignal",false);
            gemExitWithSAR	= Strategy.propiedades.getProperty("signaler.macd.gemExitWithSAR",false);        
            gemContinue		= Strategy.propiedades.getProperty("signaler.macd.gemContinue",true);
        }
                
        public void initialize() {
        	logger.info("Initializing signaler <"+SignalerName+"> for <"+instrument.name()+">");
        	//
        	super.initialize();
            //
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemFast"))        gemFast        = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemFast", 12);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemSlow"))        gemSlow        = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemSlow",26);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemSignal"))      gemSignal      = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemSignal",9);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemNumPer"))      gemNumPer      = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemNumPer",5);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemExitSignal"))  gemExitSignal  = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemExitSignal",false);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemExitWithSAR")) gemExitWithSAR = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemExitWithSAR",false);
            if (Strategy.propiedades.containsKey(instrument.name()+".signaler.macd.gemContinue"))    gemContinue    = Strategy.propiedades.getProperty(instrument.name()+".signaler.macd.gemContinue",true);
            //
            macd_s = new Ind_macd(context, indicators, instrument, Period.ONE_HOUR, gemFast, gemSlow, gemSignal, gemNumPer, gesPlayInRange);
            //
            logger.info("Signaler <"+SignalerName+"> for <"+instrument.name()+"> initialized");
        }


        public void evaluateTick(ITick tick, IOrder order, boolean ordersAllowed) {
            try {
            	boolean eval = true;
            	// 30 segs for testing
            	int psec = 60;
                int segs = Math.round((tick.getTime() - mLastTickCalc) / 1000);
                eval = (segs > psec);
                //
                if (eval) {
                    macd_s.calculate(tick.getTime());
                    if (macd_l != null) macd_l.calculate(tick.getTime());
                    if (macd_m != null) macd_m.calculate(tick.getTime());
                    mLastTickCalc = tick.getTime();
                    //
                    Signal         = Signals.NONE;
                    //       
                    if (order != null) {                        
                    	String  orderCommand = order.getOrderCommand().name();
                    	if (orderCommand.indexOf("BUY") > -1) {
                    		if (gemExitWithSAR && gdsOrderChanged) {
                            	double mSAR[]    = indicators.sar(instrument, Period.ONE_HOUR, OfferSide.BID, 0.05, 0.2, Filter.ALL_FLATS, 1, tick.getTime(), 0);
                            	if (mSAR[0] > tick.getAsk()) {
                                    Signal = Signals.EXIT_LONG;
                                    return;
                                }             	
                            }
                    		if (gemExitSignal) {
                    			controlLong(order, tick);
                    		}
                    	}
                    	//
                    	if (orderCommand.indexOf("SELL") > -1) {
                    		if (gemExitWithSAR && gdsOrderChanged) {
                            	double mSAR[]    = indicators.sar(instrument, Period.ONE_HOUR, OfferSide.BID, 0.05, 0.2, Filter.ALL_FLATS, 1, tick.getTime(), 0);
                            	if (mSAR[0] < tick.getAsk()) {    
                            		Signal = Signals.EXIT_SHORT;
                                    return;    
                            	} 
                    		} 
                    		if (gemExitSignal) {
                    			controlShort(order, tick);
                    		}
                    	}                                  	
                    }
                    //
                    // Check for LONG entry
                    if (signalBelow(1)) {
                        if (order == null) {
                            if ((State == States.ABOVE_SIGNAL) || notAllowedPending() || gemContinue) {
                            	if (signalWithTrend(Signals.ENTER_LONG, tick.getTime()) && enterLong(tick.getTime())) {
                                    // Upward cross
                                    if (ordersAllowed) {
                                    	boolean enter = true;
                                    	// If we have signals pending in the same direccion and price is worst, we don´t entry 
                                    	if (notAllowedPending()) {
                                    		if (mAllSignal == Signals.ENTER_LONG) {
                                    			enter = false; 
                                    			logger.debug(Strategy.FormatDate(tick.getTime()) + " ENTER_LONG ("+instrument.toString()+")  - NOT ALLOWED TO REENTER");
                                    		}
                                    	}
                                    	if (enter) {
                                        	Signal = Signals.ENTER_LONG;
                                        	resetNotAllowed();
                                        	resetExtensions();
                                        	logger.info(Strategy.FormatDate(tick.getTime()) + " ENTER_LONG ("+instrument.toString()+")  - Val: " +Strategy.FormatFloat(tick.getBid()) + "  " +  macd_s.getValues());                                                                            	                                    		
                                    	}
                                    } else {
                                    	mAllSignal	 = Signals.ENTER_LONG;
                                    	mAllTickVal  = tick.getBid();
                                    	mAllTickTime = tick.getTime();       
                                    	Signal = Signals.ENTER_LONG_IFPOSITIVE;
                                    	logger.debug(Strategy.FormatDate(tick.getTime()) + " ENTER_LONG ("+instrument.toString()+")  - NOT ALLOWED");
                                    }
                                    State  = States.BELOW_SIGNAL;
                                }
                            }
                        } 
                        if (State == States.NONE) State  = States.BELOW_SIGNAL;
                    }
                    //
                    // Check for SHORT entry
                    if (signalAbove(1)) {
                        if (order == null) {
                            if ((State == States.BELOW_SIGNAL) || notAllowedPending() || gemContinue) {
                            	if (signalWithTrend(Signals.ENTER_SHORT, tick.getTime()) && enterShort(tick.getTime())) {
                                    // Downward cross
                                	if (ordersAllowed) {
                                		boolean enter = true;
                                    	// If we have signals pending in the same direccion and price is worst, we don´t entry 
                                    	if (notAllowedPending()) {
                                    		if (mAllSignal == Signals.ENTER_SHORT) {
                                    			enter = false;
                                    			logger.debug(Strategy.FormatDate(tick.getTime()) + " ENTER_SHORT ("+instrument.toString()+") - NOT ALLOWED TO REENTER");
                                    		}
                                    	}
                                    	if (enter) {
                                    		Signal = Signals.ENTER_SHORT;
                                            resetNotAllowed();
                                            resetExtensions();
                                            logger.info(Strategy.FormatDate(tick.getTime()) + " ENTER_SHORT ("+instrument.toString()+") - Val: " +Strategy.FormatFloat(tick.getBid()) + "  " +  macd_s.getValues());                                                                        	                                    		
                                    	}
                                	} else {
                                		mAllSignal	 = Signals.ENTER_SHORT;
                                		mAllTickVal  = tick.getBid();
                                    	mAllTickTime = tick.getTime();       
                                    	Signal = Signals.ENTER_SHORT_IFPOSITIVE;
                                    	logger.debug(FormatDate(tick.getTime()) + " ENTER_SHORT ("+instrument.toString()+") - NOT ALLOWED");
                                	}
                                	State  = States.ABOVE_SIGNAL;
                                }
                            }
                        }
                        if (State == States.NONE) State  = States.ABOVE_SIGNAL;
                    }
                    //                    
                }
                //
            } catch (Exception e) {     
            	logger.error(e.getMessage());
            }
        }
        
        
        public void info() {
        	try {
        		super.info();
        		//
        		long time = new Date().getTime();
        		String ano = "";
        		if (macd_s.anomaliaLong(time)) ano = ano + "ANOMALIA_LONG";
        		if (macd_s.anomaliaShort(time)) ano = ano + "ANOMALIA_SHORT";
        		if (!ano.equals("")) logger.info(instrument.toString() + " " + ano);
        		//    		
        	} catch (Exception e) {
        		logger.error(e.getMessage());
        	}
        }

        
        protected void controlLong(IOrder order, ITick tick) {
        	//
        	double profitpips = profitInPips(tick, order);
        	double ext   = 0.9;
        	if (profitpips < ext*getPipsGoal()) return;
        	//
        	double stop    = 0;
        	double limit   = 0;
        	double pipste1 = getPipsGoal() * 0.6;
        	double pipste2 = pipste1 * 0.5;
        	double pipste3 = pipste2 * 0.5;
        	// First extension
        	if (!mclE1 && (profitpips < getPipsGoal())) {
        		// we check trailing (macd is ascending and the difference is still big)
        		if (macd_s.macdAscending(3) && (macd_s.diffOpen())) {
        			stop  = order.getOpenPrice(); //getNewStop(order, tick, instrument, pipste1); 
                    limit = getNewLimit(order, tick, instrument, pipste1);
                    logger.debug(FormatDate(tick.getTime()) + " TRAILING_LONG ("+instrument.toString()+") - E1");
                    changeOrder(order.getLabel(), stop, limit);
                    mclE1 = true;
        		}
        		// if macd falls, we exit
        		if ((macd_s.fastMacdDescending(2) && (!macd_s.diffOpen()))) {
        			logger.debug(FormatDate(tick.getTime()) + " EXIT_LONG ("+instrument.toString()+") - E1");
        			closeOrder(order.getLabel());
        		}
            }
        	// Second extension
        	if (!mclE2 && (profitpips > pipste1 + pipste2*ext)) {
        		// we check trailing (macd is ascending and the difference is still big)
        		if (macd_s.macdAscending(3) && (macd_s.diffOpen())) {
        			stop  = pipste1; //getNewStop(order, tick, instrument, pipste2); 
                    limit = getNewLimit(order, tick, instrument, pipste2);
                    logger.debug(FormatDate(tick.getTime()) + " TRAILING_LONG ("+instrument.toString()+") - E2");
                    changeOrder(order.getLabel(), stop, limit);
                    mclE2 = true;
        		}
        		// if macd falls, we exit
        		if ((macd_s.fastMacdDescending(2) && (!macd_s.diffOpen()))) {
        			logger.debug(FormatDate(tick.getTime()) + " EXIT_LONG ("+instrument.toString()+") - E2");
        			closeOrder(order.getLabel());
        		}        		
        	}
        	// Third extension
        	if (!mclE3 && (profitpips > pipste1 + pipste2 + pipste3*ext)) {
        		// limit is ok, maybe we change stop
        		// we check trailing (macd is ascending and the difference is still big)
        		if (macd_s.macdAscending(3) && (macd_s.diffOpen())) {
        			stop  = pipste2; //getNewStop(order, tick, instrument, pipste3); 
                    limit = getNewLimit(order, tick, instrument, pipste3);
                    logger.debug(FormatDate(tick.getTime()) + " TRAILING_LONG ("+instrument.toString()+") - E3");
                    changeOrder(order.getLabel(), stop, limit);
                    mclE3 = true;
        		}
        		// if macd falls, we exit
        		if ((!macd_s.diffOpen())) {
        			logger.debug(FormatDate(tick.getTime()) + " EXIT_LONG ("+instrument.toString()+") - E3");
        			closeOrder(order.getLabel());
        		}        		
        	}        	        	
        }
        
        protected void controlShort(IOrder order, ITick tick) {
        	//
        	double profitpips = profitInPips(tick, order);
        	double ext   = 0.8;
        	if (profitpips < ext*getPipsGoal()) return;
        	//
        	double stop    = 0;
        	double limit   = 0;
        	double pipste1 = getPipsGoal() * 0.6;
        	double pipste2 = pipste1 * 0.8;
        	double pipste3 = pipste2 * 0.6;
        	// First extension
        	if (!mcsE1 && (profitpips < getPipsGoal())) {
        		// we check trailing (macd is descending and the difference is still big)
        		if (macd_s.macdDescending(3) && (macd_s.diffOpen())) {
        			stop  = getNewStop(order, tick, instrument, pipste1); 
                    limit = getNewLimit(order, tick, instrument, pipste1);
                    logger.debug(FormatDate(tick.getTime()) + " TRAILING_SHORT ("+instrument.toString()+") - E1");
                    changeOrder(order.getLabel(), stop, limit);
                    mcsE1 = true;
        		}
        		// if macd raise, we exit
        		if ((macd_s.fastMacdAscending(2) && (!macd_s.diffOpen()))) {
        			logger.debug(FormatDate(tick.getTime()) + " EXIT_SHORT ("+instrument.toString()+") - E1");
        			closeOrder(order.getLabel());
        		}
            }
        	// Second extension
        	if (!mcsE2 && (profitpips > pipste1 + pipste2*ext)) {
        		// we check trailing (macd is ascending and the difference is still big)
        		if (macd_s.macdDescending(3) && (macd_s.diffOpen())) {
        			stop  = getNewStop(order, tick, instrument, pipste2); 
                    limit = getNewLimit(order, tick, instrument, pipste2);
                    logger.debug(FormatDate(tick.getTime()) + " TRAILING_SHORT ("+instrument.toString()+") - E2");
                    changeOrder(order.getLabel(), stop, limit);
                    mcsE2 = true;
        		}
        		// if macd raise, we exit
        		if ((macd_s.fastMacdAscending(2) && (!macd_s.diffOpen()))) {
        			logger.debug(FormatDate(tick.getTime()) + " EXIT_SHORT ("+instrument.toString()+") - E2");
        			closeOrder(order.getLabel());
        		}        		
        	}
        	// Third extension
        	if (!mcsE3 && (profitpips > pipste1 + pipste2 + pipste3*ext)) {
        		// limit is ok, maybe we change stop
        		// comprobamos si hacemos trailing y subimos el limite (macd sigue ascendiendo y la diferencia todavia es grande)
        		if (macd_s.macdDescending(3) && (macd_s.diffOpen())) {
        			stop  = getNewStop(order, tick, instrument, pipste3); 
                    limit = getNewLimit(order, tick, instrument, pipste3);
                    logger.debug(FormatDate(tick.getTime()) + " TRAILING_SHORT ("+instrument.toString()+") - E3");
                    changeOrder(order.getLabel(), stop, limit);
                    mcsE3 = true;
        		}
        		// if macd raise, we exit
        		if ((!macd_s.diffOpen())) {
        			logger.debug(FormatDate(tick.getTime()) + " EXIT_SHORT ("+instrument.toString()+") - E3");
        			closeOrder(order.getLabel());
        		}        		
        	}        	        	
        }
        
        protected void resetExtensions() {
        	mclE1 = false;
            mclE2 = false;
            mclE3 = false;
            mcsE1 = false;
            mcsE2 = false;
            mcsE3 = false;
        }
        
        protected boolean signalBelow(int nPer) {
            boolean res = macd_s.signalBelow(nPer);
            if (macd_l != null) res = res && macd_l.signalBelow(nPer);
            if (macd_m != null) res = res && macd_m.signalBelow(nPer);            
            return res;
        }
        
        protected boolean signalAbove(int nPer) {
            boolean res = macd_s.signalAbove(nPer); 
            if (macd_l != null) res = res && macd_l.signalAbove(nPer);
            if (macd_m != null) res = res && macd_m.signalAbove(nPer);            
            return res;
        }
        
        protected boolean signalAscending(int nPer) {
            boolean res = macd_s.signalAscending(nPer); 
            if (macd_l != null) res = res && macd_l.signalAscending(nPer);
            if (macd_m != null) res = res && macd_m.signalAscending(nPer);            
            return res;
        }
        
        protected boolean signalDescending(int nPer) {
            boolean res = macd_s.signalDescending(nPer); 
            if (macd_l != null) res = res && macd_l.signalDescending(nPer);
            if (macd_m != null) res = res && macd_m.signalDescending(nPer);            
            return res;
        }

        protected boolean macdAscending(int nPer) {
            boolean res = macd_s.macdAscending(nPer); 
            if (macd_l != null) res = res && macd_l.macdAscending(nPer);
            if (macd_m != null) res = res && macd_m.macdAscending(nPer);            
            return res;
        }
        
        protected boolean macdDescending(int nPer) {
            boolean res = macd_s.macdDescending(nPer); 
            if (macd_l != null) res = res && macd_l.macdDescending(nPer);
            if (macd_m != null) res = res && macd_m.macdDescending(nPer);            
            return res;
        }

        protected boolean diffAscending(int nPer) {
            boolean res = macd_s.diffAscending(nPer); 
            if (macd_l != null) res = res && macd_l.diffAscending(nPer);
            if (macd_m != null) res = res && macd_m.diffAscending(nPer);            
            return res;
        }
        
        protected boolean diffDescending(int nPer) {
            boolean res = macd_s.diffDescending(nPer); 
            if (macd_l != null) res = res && macd_l.diffDescending(nPer);
            if (macd_m != null) res = res && macd_m.diffDescending(nPer);            
            return res;
        }
        
        protected boolean macdLong() {
            boolean res = macd_s.macdLong(); 
            if (macd_l != null) res = res && macd_l.macdLong();
            if (macd_m != null) res = res && macd_m.macdLong();            
            return res;
        }
        
        protected boolean macdShort() {
            boolean res = macd_s.macdShort(); 
            if (macd_l != null) res = res && macd_l.macdShort();
            if (macd_m != null) res = res && macd_m.macdShort();            
            return res;
        }        
        
        protected boolean flat(int nPer) {
            boolean res = macd_s.flat(nPer); 
            if (macd_l != null) res = res || macd_l.flat(nPer);
            if (macd_m != null) res = res || macd_m.flat(nPer);
            return res;
        }
                
        protected boolean enterLong(long time) {
            boolean res = macd_s.enterLong(time); 
            if (macd_l != null) res = res || macd_l.enterLong(time);
            if (macd_m != null) res = res || macd_m.enterLong(time);
            return res;
        }  
        
        protected boolean enterShort(long time) {
            boolean res = macd_s.enterShort(time); 
            if (macd_l != null) res = res || macd_l.enterShort(time);
            if (macd_m != null) res = res || macd_m.enterShort(time);
            return res;
        } 
        
        protected boolean exitLong(long time) {
        	boolean res = macd_s.exitLong(time); 
            if (macd_l != null) res = res || macd_l.exitLong(time);
            if (macd_m != null) res = res || macd_m.exitLong(time);
            return res;
        }  
        
        protected boolean exitShort(long time) {
        	boolean res = macd_s.exitShort(time); 
            if (macd_l != null) res = res || macd_l.exitShort(time);
            if (macd_m != null) res = res || macd_m.exitShort(time);
            return res;
        } 
        
   }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Ind_macd

  class Ind_macd {
    
    private IIndicators     indicators;
    private IContext        context;
    private Instrument      instrument;
    private Ind_adx			adx_daily;
    private Ind_adx			adx_period;
    private Ind_price		price;
    // Parameters
    private double          MACD[][];
    private int           	mFast;
    private int           	mSlow;
    private int           	mSignal;
    private int           	mNumPer;  
    private Period          mPeriod;
    private boolean			mRange;
    // Indicator statistics
    private double          mGapDiff;
    private double          mMinPerMovMacd;
    private double          mMinPerMovSignal;
    private double          mAvgDiff;
    private double			mAvgMacdUp;
    private double			mAvgMacdDw;
    //
    private long         	mIniTime	= 0;
    private long			mHiMacd		= 0;
    private long			mLoMacd		= 0;
    //
    private int				mFacGap		= 3;  // Conversion factor from AvgDiff - GapDiff
    private	int				mFastMov	= 3;  // Number of times needed to condiser a movemvent fast
    private int				mPosNeg 	= 2;  // How much are we going to shift 0 over macd average
    private	int				mPerAno 	= 30; // Number of periods to end an anomali period
    
    public Ind_macd(IContext con, IIndicators ind, Instrument inst, Period period, int fast, int slow, int signal, int numPer, boolean range) {
        context		= con;
        indicators	= ind;
        instrument	= inst;
        mPeriod		= period;
        mFast		= fast;
        mSlow		= slow;
        mSignal		= signal;
        mNumPer		= numPer;
        mRange		= range;
        mIniTime	= 0;
        adx_daily	= new Ind_adx(con, ind, inst, Period.DAILY, 25, true);
        adx_period	= new Ind_adx(con, ind, inst, period, 25, false);
        price		= new Ind_price(con, inst, period);
    }
    
    public double calculate(long time) {
        try {
            MACD    = indicators.macd(instrument, mPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, mFast, mSlow, mSignal, Filter.ALL_FLATS, mNumPer, time, 0); // Fast 12, Slow 26, Signal 9 green
            initialize(time);
            controlAnomalia(time);
            return MACD[0][mNumPer-1];
            //
        } catch (Exception e) {
            return 0;
        }
    }
    
    private void initialize(long time) {
        try {
        	//if (mIniTime != 0) return;
        	long barTime = context.getHistory().getPreviousBarStart(Period.DAILY, time);            
        	if (barTime != mIniTime) {
            	int     numper    = 180;                              
                double  hmacd[][] = indicators.macd(instrument, mPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, mFast, mSlow, mSignal, Filter.ALL_FLATS, numper, barTime, 0); 
                double  df  = 0;
                double	pmm = 0;
                double	pms = 0;
                double  up  = 0;
                double  dw  = 0;
                int		cup = 0;
                int		cdw = 0;
                for (int i=1; i<numper; i++) {
                     df = df + Math.abs(hmacd[2][i]);
                     pmm = pmm + Math.abs(hmacd[0][i] - hmacd[0][i-1]);
                     pms = pms + Math.abs(hmacd[1][i] - hmacd[1][i-1]);
                     if (hmacd[0][i] > 0 ) {
                    	 up  = up + hmacd[0][i];
                    	 cup = cup + 1;
                     } else {
                    	 dw  = dw + Math.abs(hmacd[0][i]);
                    	 cdw = cdw + 1;
                     }
                }
                mAvgDiff         = df / numper;
                mAvgMacdUp	     = up / cup;
                mAvgMacdDw       = dw / cdw;
                mMinPerMovMacd   = pmm / numper;
                mMinPerMovSignal = pms / numper;
                mGapDiff         = mAvgDiff / mFacGap;
                mIniTime     	 = barTime;
                //Strategy.Log(Strategy.FormatDate(mIniTime) + "  -- " + instrument.toString() + " -- INITIALIZE : " + getValues());
            }
            //
        } catch (Exception e) {
        }
    }
        
    public String getValues() {
        return "<MACD: " +  Strategy.FormatFloat(MACD[0][mNumPer-1]) + " Signal: " + Strategy.FormatFloat(MACD[1][mNumPer-1]) + " Diff: " + Strategy.FormatFloat(Math.abs(MACD[2][mNumPer-1])) + "> -- <AvgDif: " + Strategy.FormatFloat(mAvgDiff) + " GapDif: " + Strategy.FormatFloat(mGapDiff)  + " AvgMacdUp: " + Strategy.FormatFloat(mAvgMacdUp) + " AvgMacdDw: " + Strategy.FormatFloat(mAvgMacdDw) + " MinPerMovMacd: " + Strategy.FormatFloat(mMinPerMovMacd) + " MinPerMovSignal: " + Strategy.FormatFloat(mMinPerMovSignal) + ">";
    }
    
    protected boolean signalAscending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res = ascending(nPer, 1);
        res = res && (MACD[1][mNumPer-1] > MACD[1][0] + (mNumPer-1)*mMinPerMovSignal);
        return res;
    }
    
    protected boolean signalDescending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res =  descending(nPer, 1);
        res = res && (MACD[1][mNumPer-1] + (mNumPer-1)*mMinPerMovSignal < MACD[1][0]); 
        return res;
    }
    
    protected boolean macdAscending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res = ascending(nPer, 0);
        res = res && (MACD[0][mNumPer-1] > MACD[0][0] + (mNumPer-1)*mMinPerMovMacd);
        return res;
    }
    
    protected boolean macdDescending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res = descending(nPer, 0);
        res = res && (MACD[0][mNumPer-1] + (mNumPer-1)*mMinPerMovMacd < MACD[0][0]); 
        return res;
    }

    protected boolean fastSignalAscending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res = ascending(nPer, 1);
        res = res && (MACD[1][mNumPer-1] > MACD[1][0] + (mNumPer-1)*mMinPerMovSignal*mFastMov);
        return res;
    }
    
    protected boolean fastSignalignalDescending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res =  descending(nPer, 1);
        res = res && (MACD[1][mNumPer-1] + (mNumPer-1)*mMinPerMovSignal*mFastMov < MACD[1][0]); 
        return res;
    }
    
    protected boolean fastMacdAscending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res = ascending(nPer, 0);
        res = res && (MACD[0][mNumPer-1] > MACD[0][0] + (mNumPer-1)*mMinPerMovMacd*mFastMov);
        return res;
    }
    
    protected boolean fastMacdDescending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	boolean res = descending(nPer, 0);
        res = res && (MACD[0][mNumPer-1] + (mNumPer-1)*mMinPerMovMacd*mFastMov < MACD[0][0]); 
        return res;
    }
    
    protected boolean diffAscending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	return ascending(nPer, 2);
    }
    
    protected boolean diffDescending(int nPer) {
    	if (nPer <= 1) nPer = 2;
    	return descending(nPer, 2);
    }
    
    protected boolean flat(int nPer) {
        boolean res = true;
        if (mNumPer >= nPer) {
            res = (Math.abs(MACD[0][mNumPer-1] - MACD[0][mNumPer-nPer]) < nPer*mMinPerMovMacd) && (Math.abs(MACD[1][mNumPer-1] - MACD[1][mNumPer-nPer]) < nPer*mMinPerMovSignal);
            res = res && !signalAscending(nPer) && !signalDescending(nPer);
        }                
        return res;
    }
    
    protected int numberOfCrosses(int nPer) {
        int res = 0;
        if (mNumPer >= nPer) {
            for (int i = 1; i <= nPer-1; i++) {
                if ((MACD[1][mNumPer-i] < MACD[0][mNumPer-i]) && (MACD[1][mNumPer-i-1] > MACD[0][mNumPer-i-1])) res = res + 1;
                else if ((MACD[1][mNumPer-i] > MACD[0][mNumPer-i]) && (MACD[1][mNumPer-i-1] < MACD[0][mNumPer-i-1])) res = res + 1;
            }                    
        }
        return res;
    }
    
    protected boolean signalBelow(int nPer) {
        boolean res = true;
        if (mNumPer >= nPer) {
            for (int i = 1; i <= nPer; i++) {
                res = res && (MACD[1][mNumPer-i] < MACD[0][mNumPer-i]) && (Math.abs(MACD[2][mNumPer-i]) > mGapDiff/nPer); // mSignal < mMACD;
            }                    
        }
        return res;
    }
    
    protected boolean signalAbove(int nPer) {
        boolean res = true;                 
        if (mNumPer >= nPer) {
            for (int i = 1; i <= nPer; i++) {
                res = res && (MACD[1][mNumPer-i] > MACD[0][mNumPer-i]) && (Math.abs(MACD[2][mNumPer-i]) > mGapDiff/nPer); // mSignal < mMACD;
            }                    
        }
        return res;                  
    }
                                
    protected boolean macdPositive() {
        return (MACD[0][mNumPer-1] > 0);    
    }
    
    protected boolean macdNegative() {
        return (MACD[0][mNumPer-1] < 0);   
    }   
    
    protected boolean macdLong() {
        return (MACD[0][mNumPer-1] > -mAvgMacdDw/mPosNeg);
    }
    
    protected boolean macdShort() {
        return (MACD[0][mNumPer-1] < mAvgMacdUp/mPosNeg);  
    }                
    
    private boolean diffOpen() {
        return (Math.abs(MACD[2][mNumPer-1]) > mAvgDiff) ;
    }
    
    
    protected boolean enterLong(long time) {
    	boolean res = true;
    	//
    	if (!signalBelow(1)) return false;
    	if (anomaliaLong(time)) return false;
    	if (flat(mNumPer)) return false;
    	if (adx_daily.trendIsLong(time)) {
    		res	= res && macdLong();
    		res = res && (diffAscending(1) || fastMacdAscending(1));
    	} 
    	else {
    		res = mRange;
    		res = res && adx_period.trendIsLong(time); //adx_period.trending(time);
    		res	= res && macdPositive();
    		res = res && (diffAscending(1) || fastMacdAscending(1));
    	} 
    	//
    	return res;
    }
    
    protected boolean enterShort(long time) {
    	boolean res = true;
    	//
    	if (!signalAbove(1)) return false;
    	if (anomaliaShort(time)) return false;
    	if (flat(mNumPer)) return false;
    	if (adx_daily.trendIsShort(time)) {
    		res = res && macdShort();
    		res = res && (diffAscending(1) || fastMacdDescending(1));    	
    	} 
    	else {
    		res = mRange;
    		res = res && adx_period.trendIsShort(time); //adx_period.trending(time);
    		res = res && macdNegative();
    		res = res && (diffAscending(1) || fastMacdDescending(1));    
    	} 
    	//
    	return res;
    }
        
    protected boolean exitLong(long time) {
    	boolean res = false;
    	if (signalAbove(1)) {
    		//if (adx.trendIsShort(time)) res = Signals.EXIT_LONG;
    		//if (adx_period.trending(time)) res = Signals.EXIT_LONG;
    		res = true;
    	} else {
    		//diffAscending(nPer)
    	}
    	return res;
    }
    
    protected boolean exitShort(long time) {
    	boolean res = false;
    	if (signalBelow(1)) {
    		//if (adx.trendIsLong(time)) res = Signals.EXIT_SHORT;
    		//if (adx_period.trending(time)) res = Signals.EXIT_SHORT;
    		res = true;
    	}
    	return res;
    }
    
    private void controlAnomalia(long time) {
    	double macd   = MACD[0][mNumPer-1];
    	// macd esta muy arriba, ojo con las se�ales porque macd descendera, aunque no lo hagan los precios
    	if ((mHiMacd == 0) && ((macd > 3* mAvgMacdUp) || ((macd > 0) && fastMacdAscending(3)))) {
    		mHiMacd = time;   
    		logger.debug(Strategy.FormatDate(time) + " -- ANOMALIA HIGH  - Iniciada");
    	}
    	// macd esta muy abajo, ojo con las se�ales porque macd ascendera, aunque no lo hagan los precios
    	if ((mLoMacd == 0) && ((macd < -3*mAvgMacdDw) || ((macd < 0) && fastMacdDescending(3)))) {
    		mLoMacd = time; 
    		logger.debug(Strategy.FormatDate(time) + " -- ANOMALIA LOW   - Iniciada");
    	}
    	// volatilidad
    	if ((mHiMacd == 0) && (mLoMacd == 0) && (price.volatility(time))) {
    		mLoMacd = time;
			mHiMacd = time;
			logger.debug(Strategy.FormatDate(time) + " -- ANOMALIA PRICE - Iniciada");
    	}
    }
    
    private boolean anomaliaLong(long time) {
    	boolean res = false;
    	// anomalia long, we´ll end when time is over or lines cross
    	if (mLoMacd > 0) {
    		int segs = Math.round((time - mLoMacd) / 1000);
    		int sano = mPerAno * Strategy.numberOfSecs(mPeriod); 
    		boolean ex = segs > sano;
    		ex = ex || (segs > sano/2) && (MACD[0][mNumPer-1] > 0) && (mHiMacd == 0); //&& (MACD[1][mNumPer-1] > MACD[0][mNumPer-1]);
    		if (ex) {
    			logger.debug(Strategy.FormatDate(time) + " -- ANOMALIA LOW   - Finalizada -- " +Strategy.FormatDate(mLoMacd)+ " Macd: " + Strategy.FormatFloat(MACD[0][mNumPer-1]) + " Signal: " + Strategy.FormatFloat(MACD[1][mNumPer-1]));
    			mLoMacd = 0;
    		} else {
    			res = true;
    		}
    	}
    	//
    	return res;
    }
    
    private boolean anomaliaShort(long time) {
    	boolean res = false;
    	// anomalia short, we´ll end when time is over or lines cross
    	if (mHiMacd > 0) {
    		int segs = Math.round((time - mHiMacd) / 1000);
    		int sano = mPerAno * Strategy.numberOfSecs(mPeriod); 
    		boolean ex = segs > sano;
    		ex = ex || (segs > sano/2) && (MACD[0][mNumPer-1] < 0) && (mLoMacd == 0); //(MACD[1][mNumPer-1] < MACD[0][mNumPer-1]);
    		if (ex) {
    			logger.debug(Strategy.FormatDate(time) + " -- ANOMALIA HIGH  - Finalizada -- " +Strategy.FormatDate(mHiMacd)+ " Macd: " + Strategy.FormatFloat(MACD[0][mNumPer-1]) + " Signal: " + Strategy.FormatFloat(MACD[1][mNumPer-1]));
    			mHiMacd = 0;
    		} else {
    			res = true;
    		}
    	}
    	//
    	return res;
    }
        
    private boolean ascending(int nPer, int ind) {
        boolean res = true;
        if (nPer <= 1) nPer = 2; // two periods need for compare
        if (mNumPer >= nPer) {
            for (int i = 1; i < nPer; i++) {
                res = res && (MACD[ind][mNumPer-i] > MACD[ind][mNumPer-i-1]);
            }                    
        }                
        return res;
    }
    
    private boolean descending(int nPer, int ind) {
        boolean res = true;
        if (nPer <= 1) nPer = 2; // two periods need for compare
        if (mNumPer >= nPer) {
            for (int i = 1; i < nPer; i++) {
                res = res && (MACD[ind][mNumPer-i] < MACD[ind][mNumPer-i-1]);
            }                    
        }                                
        return res;
    }

    
  }
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Ind_macd

  class Ind_adx {
	    
	    private IIndicators     indicators;
	    private IContext        context;
	    private Instrument      instrument;
	    // 
	    private Period          mPeriod;
	    private double			mValTrend;
	    private boolean			mUnderValTrend;    
	    //
	    private   long		mAdxTime		= 0;
	    protected double	mAdx			= 0;
	    protected double 	mDiPlus			= 0;
	    protected double	mDiMinus		= 0;
	    
	    
	    public Ind_adx(IContext con, IIndicators ind, Instrument inst, Period period, double gesTrend, boolean undervTrend) {
	        context			= con;
	        indicators		= ind;
	        instrument		= inst;
	        mPeriod			= period;
	        mValTrend		= gesTrend;
	        mUnderValTrend 	= undervTrend;
	    }
	    
	    
	    public void calculate(long time) {
	        try {        	
	    		long barTime = context.getHistory().getPreviousBarStart(mPeriod, time);
	            if (barTime != mAdxTime) {
	            	double Adx[]     = indicators.adx(instrument, mPeriod, OfferSide.BID, 14, Filter.ALL_FLATS, 1, time, 0);                    
	                double DiPlus[]  = indicators.plusDi(instrument, mPeriod, OfferSide.BID, 14, Filter.ALL_FLATS, 1, time, 0);
	                double DiMinus[] = indicators.minusDi(instrument, mPeriod, OfferSide.BID, 14, Filter.ALL_FLATS, 1, time, 0);
	                //
	                mAdx			 = Adx[0];
	                mDiPlus			 = DiPlus[0];
	                mDiMinus		 = DiMinus[0];
	            	mAdxTime	 	 = barTime;
	            	//Log("TREND  -- ("+mPeriod+") " + instrument.toString() + " -- " +Strategy.FormatDate(time)+ " -- " + " (Adx: " + Strategy.FormatFloat(mAdx) + " DI+: " + Strategy.FormatFloat(mDiPlus)+ " DI-: " + Strategy.FormatFloat(mDiMinus) +")");
	            }        	
	            //
	            // If adx is very high, we only enter if is still raising
	            if (mAdx > 45) {
	            	//Adx = indicators.adx(instrument, Period.DAILY, OfferSide.BID, 14, Filter.ALL_FLATS, 3, time, 0);
	            	//mTrendSlowing = (Adx[2] > Adx[1]) && (Adx[1] > Adx[0]);
	            }
	            //
	        } catch (Exception e) {
	        	//logger.error(e.getMessage());
	        }
	    }
	    
	    public boolean signalWithTrend(Signals sig, long time) {
	    	boolean res   = false;
	    	String  trend = "";
	     	// 
	    	if (mValTrend == 0) return true; //!mTrendSlowing;
	    	//
	    	if ((sig == Signals.ENTER_LONG) || (sig == Signals.ENTER_LONG_IFPOSITIVE) || (sig == Signals.CHANGE_TO_LONG)) {
	    		res = trendIsLong(time); 
	    		if (res) trend = "UP_TREND";
	    	} else if ((sig == Signals.ENTER_SHORT) || (sig == Signals.ENTER_SHORT_IFPOSITIVE) || (sig == Signals.CHANGE_TO_SHORT)) {
	    		res = trendIsShort(time);
	    		if (res) trend = "DOWN_TREND";
	    	}
	    	//
	    	//if (res) Log(Strategy.FormatDate(time) + "  -- " + instrument.toString() + " -- " + trend + " (Adx: " + Strategy.FormatFloat(mAdx) + " DI+: " + Strategy.FormatFloat(mDiPlus)+ " DI-: " + Strategy.FormatFloat(mDiMinus) +")");
	    	//
	    	return res;
	    }
	    
	    public boolean trending(long time) {
	    	calculate(time);        	
	    	return mAdx > mValTrend;
	    }

	    public boolean trendIsLong(long time) {
	    	calculate(time);
	    	boolean res = (mAdx > mValTrend) && (mDiPlus > mDiMinus); 
			if (!res) res = mUnderValTrend && (mDiPlus > mDiMinus + 15);
	    	return res;
	    }
	    
	    public boolean trendIsShort(long time) {
	    	calculate(time);
	    	boolean res = (mAdx > mValTrend) && (mDiPlus < mDiMinus);
			if (!res) res = mUnderValTrend && (mDiMinus > mDiPlus + 15);
	    	return res;
	    }
	        
	    public boolean descending(long time, int nPer) {
	    	boolean res = true;
	    	try {
	        	if (nPer <= 1) nPer = 2;
	        	double Adx[] = indicators.adx(instrument, mPeriod, OfferSide.BID, 14, Filter.ALL_FLATS, nPer, time, 0);
	            for (int i = 1; i < nPer; i++) {
	        	   res = res && (Adx[nPer-i] < Adx[nPer-i-1]);
	            }                   
	            res = res && (Adx[nPer-1] + (nPer-1)*1 < Adx[0]); 
	            if (res) logger.debug(Strategy.FormatDate(time) + "  -- " + instrument.toString() + " -- ("+ Strategy.FormatFloat(Adx[nPer-1]) + " < " + Strategy.FormatFloat(Adx[0]) +")");
	    	} catch (Exception e) {	
	    	}
	        return res;
	    }       
	   
	 }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Ind_price

  class Ind_price {
	    private IContext      context;
	    private Instrument    instrument;
	    private Period        mPeriod;
	    private	double		  mPipsAvg = -1;
	    
	    public Ind_price(IContext con, Instrument inst, Period period) {
	        context			= con;
	        instrument		= inst;
	        mPeriod			= period;
	    }
	    
	    public int getPipsAverage(long time, int numPer) {
	        try {
	            IHistory history = context.getHistory();
	            //
	            long firstBarTime = history.getPreviousBarStart(mPeriod, time);                    
	            List<IBar> bars   = history.getBars(instrument, mPeriod, OfferSide.BID, history.getTimeForNBarsBack(mPeriod, firstBarTime, numPer), firstBarTime);
	            //
	            int  diff  = 0;
	            int  count = 0;
	            for (IBar bar: bars) {                    
	                double high  = bar.getHigh();
	                double low   = bar.getLow();                        
	                int    range = (int)Math.round(numberOfPips(high,  low));
	                if (range > 0) {
	                    diff  = diff + range;
	                    count = count + 1;
	                }                    
	            }
	            return diff / count;        
	            //
	        } catch (Exception e) {
	        	logger.error(e.getMessage());
	        	return -1;
	        }
	    }
	    
	    public boolean volatility(long time) {
	    	try {
	    		boolean res = false;
	    		if (mPipsAvg < 0) mPipsAvg = getPipsAverage(time, 180);
	    		//
	    		IBar bar  = context.getHistory().getBar(instrument, mPeriod, OfferSide.BID, 0);
	    		double lb = Math.round(numberOfPips(bar.getHigh(),  bar.getLow()));
                res = (2*mPipsAvg < lb);
	    		return res;
	    		//
	    	} catch (Exception e) {
	        	logger.error(e.getMessage());
	        	return false;
	        }
	    }
	  
  }


  
 
  
}

