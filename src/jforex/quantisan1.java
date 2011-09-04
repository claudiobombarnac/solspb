/*
	quantisan1 - JForex contest strategy
	version 2.16
	August 2010
	Copyright 2010 Paul at Quantisan.com
	
	Changelog:
	v2.16	- August commit
	v2.15	- fixed riskPct not initiated dynamically
			- added momentum check as entry condition
			- appended reverse trade upon closing
			- removed getRiskAdj()
	v2.14	- commit for July 2010 competition
			- optimized parameters
			- fixed sentiment on start
			- added regression position sizing
	v2.13	- name changed from ma_retrace2 to quantisan1
			- new rules in July
			- removed all @Configurable
			- ensure max. 1 position in acct
			- added position sizing
			- breakeven stop
			- two-tier time frame strategy
	v2.12	- commit for June 2010 competition
	v2.11	- fixed getLabel open position number overlap bug
	v2.10	- removed @Config modifying open position to adhere to contest rules
			- drawTrade() NP exception bug
			- added exit automation
			- added exit target and stop limit prices
	v2.0 	- renamed from ma_scalp1 (April 2010 contest)

*/
package jforex;

import com.dukascopy.api.*;
import java.util.*;
import java.awt.Color;

public class quantisan1 implements solspb.IStrategy {
	private final double version = 2.16;
	
	private IEngine engine;
	private IIndicators indicators;
	private IHistory history;
	private int tagCounter;
	private IConsole console;
	private IContext context;

	private double[] maShr1 = new double[Instrument.values().length];
	private double[] maInt1 = new double[Instrument.values().length];
	private double[] atr = new double[Instrument.values().length];
	private double[] ult = new double[Instrument.values().length];
	
	private Sentiment[] sentiment = new Sentiment[Instrument.values().length];		
	private boolean tradingAllowed;
	private boolean isAfterNorm;			// is it after a normal trade
	
	// parameters
	private static final int LOCKPIP = 3;
	private static final int SLIPPAGE = 5;
	
	private static final Period PERIODSHR = Period.THIRTY_MINS;
	private static final Period PERIODINT = Period.FOUR_HOURS;
	
	private static final Filter indFilter = Filter.ALL_FLATS;
	private static final IIndicators.MaType maType = IIndicators.MaType.SMA;
	
	// position management
	private static final double startEquity = 100000d;
	private double acctEquity = startEquity;
	
	private static final double beRatio = 0.66;	
	private double riskPct = 20.0;
	private static final double maxRiskPct = 30.0;
	private static final double minRiskPct = 1;
	private static final double riskAdjStep = 1.2;
	private static final int revPips = 30;
	
	// indicators
	private static final double atrFactor = 7.0;
	private int lenShr = 13;	
	private int lenInt = 55;
	private double momoOB = 65;


    // ** onBar used for entries **
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException 
	{
		double maShr, maInt;
		double stopLoss, lot, chanTop, chanBot;
		
		// higher time frame get sentiment
		if (period == PERIODINT)
		{				
			setSentiment(instrument, bidBar, askBar);			
			return;			
		}
		
		if (period != PERIODSHR)	return;		// skip all other periods
				
		this.atr[instrument.ordinal()] = indicators.atr(instrument, PERIODSHR, 
			OfferSide.BID, lenShr, indFilter, 1, bidBar.getTime(), 0)[0];
		
		// moving averages
		maShr = indicators.ma(instrument, period,
			OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 
			lenShr, maType, indFilter, 1, bidBar.getTime(), 0)[0];
			
		maInt = indicators.ma(instrument, period,
			OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 
			lenInt, maType, indFilter, 1, bidBar.getTime(), 0)[0];
		
		chanTop = indicators.ma(instrument, period,
			OfferSide.ASK, IIndicators.AppliedPrice.HIGH, 
			lenInt, maType, indFilter, 1, bidBar.getTime(), 0)[0];
		
		chanBot = indicators.ma(instrument, period,
			OfferSide.BID, IIndicators.AppliedPrice.LOW, 
			lenInt, maType, indFilter, 1, bidBar.getTime(), 0)[0];		
		
		// if not enough bars
		if (this.maShr1[instrument.ordinal()] <= 0 || maShr <= 0 ||
			this.maInt1[instrument.ordinal()] <= 0 || maInt <= 0 || 
			this.atr[instrument.ordinal()] <= 0) 
		{
         updateMA(instrument, maShr, maInt);
			return;
		}		
		
		IBar prevBidBar = history.getBar(instrument, period, OfferSide.BID, 2);
		IBar prevAskBar = history.getBar(instrument, period, OfferSide.ASK, 2);		
		if (prevBidBar == null || prevAskBar == null)	return;		//not formed yet
		
// *****   ENTRY SETUP   *******************************************************
		if (tradingAllowed)
		{									
			if (this.sentiment[instrument.ordinal()] == Sentiment.BULL && 
				bidBar.getLow() > chanBot && prevBidBar.getLow() < chanBot && 
				bidBar.getLow() > prevBidBar.getLow() && bidBar.getHigh() > prevBidBar.getHigh() &&
				bidBar.getClose() > prevBidBar.getClose())
			{
				// go long
				stopLoss = bidBar.getLow() - (this.atr[instrument.ordinal()] * atrFactor);
				stopLoss = roundPip(stopLoss);
				
				String label = getLabel(instrument);
				lot = getLot(instrument, bidBar.getClose());
				
				engine.submitOrder(label, instrument, IEngine.OrderCommand.BUY, 
					lot, 0, SLIPPAGE, stopLoss, 0);		
				this.isAfterNorm = true;	
				print(label + ": Long entry, lot = " + lot);			
			}
			else if (this.sentiment[instrument.ordinal()] == Sentiment.BEAR &&
				askBar.getHigh() < chanTop && prevAskBar.getHigh() > chanTop &&  				
				askBar.getLow() < prevAskBar.getLow() && askBar.getHigh() < prevAskBar.getHigh() &&
				askBar.getClose() < prevAskBar.getClose())
			{
				// go short
				stopLoss = askBar.getHigh() + (this.atr[instrument.ordinal()] * atrFactor);
				stopLoss = roundPip(stopLoss);
								
				String label = getLabel(instrument);
				lot = getLot(instrument, askBar.getClose());
				
				engine.submitOrder(label, instrument, IEngine.OrderCommand.SELL, 
					lot, 0, SLIPPAGE, stopLoss, 0);			
				this.isAfterNorm = true;
				print(label + ": Short entry, lot = " + lot);
			}
		}


		
	// ************ onBar clean up ************
		
		
		updateMA(instrument, maShr, maInt);
    }
	   
// ****************************************************************************


    // ** onTICK: for open position management **
    public void onTick(Instrument instrument, ITick tick) throws JFException {
	

		if (maInt1[instrument.ordinal()] <= 0) 			// ma not ready
			return;
		
		boolean isLong;	
		double open, stop, diff, newStop;
		
		for (IOrder order : engine.getOrders(instrument)) {
			if (order.getState() == IOrder.State.FILLED) {
			
				// Rule 6.3, profit per trade cannot be > 50%
				if (order.getProfitLossInUSD()> this.acctEquity*0.49) {
               order.close();
               print(order.getLabel() + ": CLOSED for Rule 6.3");
            }
            
				if (order.getProfitLossInAccountCurrency() <= 0d || // skip if losing (use stop)
					order.getTakeProfitPrice() != 0d) continue;		 // or TP price set
				
				isLong = order.isLong();
				open = order.getOpenPrice();
				stop = order.getStopLossPrice(); 
				diff = (open - stop);
				
// ********* BREAKEVEN *********************************************************						
				if (isLong && diff > 0 && tick.getBid() > (open + diff))
				{
					order.close(roundLot(order.getAmount() * beRatio));	// close a portion
					
					
					newStop = open + instrument.getPipValue() * LOCKPIP;
					order.setStopLossPrice(newStop);						
               print(order.getLabel() + ": Moved STOP to breakeven");
				}
				else if (!isLong && diff < 0 && tick.getAsk() < (open + diff))
				{
					order.close(roundLot(order.getAmount() * beRatio));
					
					newStop = open - (instrument.getPipValue() * LOCKPIP);
					order.setStopLossPrice(newStop);
					print(order.getLabel() + ": Moved STOP to breakeven");
				}
				
// ********* TAKE PROFIT *******************************************************				
				// get out if price crossed maInt1
				if (isLong ? tick.getAsk() < maInt1[instrument.ordinal()] : 
					tick.getBid() > maInt1[instrument.ordinal()]) 
				{						
					order.close();							
					print(order.getLabel() + ": Take PROFIT");
				}
			}
		}
// *****************************************************************************
    }

	
// *****  UTILS start  *********************************************************


	private double getLot(Instrument instrument, double price) throws JFException
	{
		double riskAmt;
		double lotSize = 0;
		
		riskAmt = this.acctEquity * (this.riskPct / 100d); // CCYUSD only
		lotSize = riskAmt / (this.atr[instrument.ordinal()] * this.atrFactor);
		
		lotSize /= 1000000d;					// in millions
		return roundLot(lotSize);
	}

	private double roundLot(double lot)
	{	
		lot = (int)(lot * 1000) / (1000d);		// 1000 units min.
		return lot;
	}

	// keep prev MA instead of calc to improve performance
	private void updateMA(Instrument instrument, double maShr, double maInt) throws JFException
	{
		this.maShr1[instrument.ordinal()] = maShr;
		this.maInt1[instrument.ordinal()] = maInt;
	} 

	private double roundPip(double value) {
	// rounding to nearest half, 0, 0.5, or 1
		int pipsMultiplier = value <= 20 ? 10000 : 100;
		int rounded = (int) (value * pipsMultiplier * 10 + 0.5);
		rounded *= 2;
		rounded = (int) (((double) rounded) / 10d + 0.5d);
		value = ((double) rounded) / 2d;
		value /= pipsMultiplier;
		return value;
    }

	private String getLabel(Instrument instrument) throws JFException {
		int max = 0;
		int parse;
	
		// check for open position label numbers
		for (IOrder order : engine.getOrders(instrument)) {
			if (order.getState() == IOrder.State.FILLED) {
				parse = 0;
				try {
					parse = Integer.parseInt(order.getLabel().substring(5));
				}
				catch (NullPointerException e) {
					print(e.getMessage() + ": getLabel()");
					parse = 0;
				}
			
				if (parse > max) max = parse;
			}
		}
		if (max > tagCounter)		// continue count of existing number
			tagCounter = max;
		
		Date datetime = new Date();
      int day = datetime.getDate();
		
		String label = instrument.name();
		label = label.substring(0, 3) + label.substring(3, 6);
		label = label + (day * 100) + (tagCounter++);
		label = label.toLowerCase();
		return label;
   }
	
	public void print(String string) {
		this.console.getOut().println(string);
	}
	
	private void drawTrade(IOrder myOrder) {
		Instrument instrument;
		String label;
		boolean isLong;
		double openPrice, closePrice;
		IChart myChart;
		IChart.Type objType;
		IChartObject myObject;
		Color color;
	
		instrument = myOrder.getInstrument();
		isLong = myOrder.isLong();
		label = myOrder.getLabel();
		
		myChart = this.context.getChart(instrument);
		if (myChart == null)	return;		// no chart
		
		if (myOrder.getState() == IOrder.State.FILLED) 
		{
			objType = IChart.Type.PRICEMARKER;
			try {
				myObject = myChart.draw(label, objType, 
					myOrder.getFillTime(), myOrder.getOpenPrice());				
			} catch (NullPointerException e) {
				print(e.getMessage() + ": chart not available");
				return;
			}
			
			color = isLong ? Color.CYAN : Color.PINK;
		
			myObject.setColor(color);
		}
		else if (myOrder.getState() == IOrder.State.CLOSED) 
		{
			
			try {
				myChart.remove(label);			// remove entry marker
			} catch (NullPointerException e) {
				print(e.getMessage() + ": chart not available");
				return;
			}			
		
			objType = IChart.Type.SHORT_LINE;
			openPrice = myOrder.getOpenPrice();
			closePrice = myOrder.getClosePrice();
			try {
				myObject = myChart.draw(label, objType, 
					myOrder.getFillTime(), openPrice,
					myOrder.getCloseTime(), closePrice);			
			} catch (NullPointerException e) {
				print(e.getMessage() + ": chart not available");
				return;
			}
			
			if (isLong) {
				color = openPrice < closePrice ? Color.BLUE : Color.RED;
			}
			else {
				color = openPrice > closePrice ? Color.BLUE : Color.RED;
			}
			
			myObject.setColor(color);
			//myObject.setAttrInt(IChartObject.ATTR_INT.WIDTH, 1);
		}
	}
	
	private void setRiskPct()
	{
			double x;
					
			x = this.acctEquity;
			// regression
			this.riskPct = -2.88462e-10 * x * x  - 0.000156731 * x + 38.5577;	
			
			// ensure riskPct is within defined range
			if (this.riskPct > this.maxRiskPct)	this.riskPct = this.maxRiskPct;
			else if (this.riskPct < this.minRiskPct)	this.riskPct = this.minRiskPct;
			
			print("Set percent risk to: " + this.riskPct);
	}
	
	private void setSentiment(Instrument instrument, IBar bidBar, IBar askBar) throws JFException
	{
		double chanTop, chanBot, ult;
		chanTop = this.indicators.ma(instrument, PERIODINT,
			OfferSide.ASK, IIndicators.AppliedPrice.HIGH, 
			lenInt, maType, indFilter, 1, askBar.getTime(), 0)[0];
	
		chanBot = this.indicators.ma(instrument, PERIODINT,
			OfferSide.BID, IIndicators.AppliedPrice.LOW, 
			lenInt, maType, indFilter, 1, bidBar.getTime(), 0)[0];
				
		ult = this.indicators.ultOsc(instrument, PERIODINT,
			OfferSide.BID, 5, 13, 34, indFilter, 1, bidBar.getTime(), 0)[0];
			
		if (bidBar.getLow() > chanTop && ult < this.momoOB) {
			if (this.sentiment[instrument.ordinal()] != Sentiment.BULL)
				print(instrument.toString() + " Bullish");
			this.sentiment[instrument.ordinal()] = Sentiment.BULL;

		}
		else if (askBar.getHigh() < chanBot && ult > (100 - this.momoOB)) {
			if (this.sentiment[instrument.ordinal()] != Sentiment.BEAR)
				print(instrument.toString() + " Bearish");
			this.sentiment[instrument.ordinal()] = Sentiment.BEAR;

		}
		else {
			if (this.sentiment[instrument.ordinal()] != Sentiment.NEUTRAL)
				print(instrument.toString() + " Neutral");
			this.sentiment[instrument.ordinal()] = Sentiment.NEUTRAL;
		}
	}
	
	private void goReverse(IOrder order) throws JFException
	{
		double target, stop, entry;
		double pos;
		long gtTime;
		String label;
		Instrument instrument = order.getInstrument();		
		double pipValue = instrument.getPipValue();
		
		if (order.isLong()) 	pos = 1d;
		else 						pos = -1d;
		
		entry = order.getClosePrice();
		target = entry + (pos * pipValue * revPips);
		target = roundPip(target);
		stop = entry - (pos * pipValue * revPips);
		stop = roundPip(stop);
		
		label = order.getLabel() + "R";
		gtTime = order.getCloseTime() + PERIODSHR.getInterval();
		
		IEngine.OrderCommand side = order.isLong() ? 
			IEngine.OrderCommand.PLACE_OFFER : IEngine.OrderCommand.PLACE_BID;
		
		if (order.isLong()) {
			engine.submitOrder(label, instrument, side, roundLot(order.getAmount()/2), 
				entry, SLIPPAGE, stop, target, gtTime, "Reversal");
			print("Reversing at: " + entry);
		}
		
		this.isAfterNorm = false;
	}

// *****  MISC  ****************************************************************

	public enum Sentiment {
		BEAR, BULL, NEUTRAL
	}



// *****  API  ****************************************************************
	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.indicators = context.getIndicators();
		this.history = context.getHistory();
		this.console = context.getConsole();	// allows printing to console
		this.context = context;
		
		Set instSet;
		instSet = context.getSubscribedInstruments();
		Iterator instIter = instSet.iterator();
		IBar prevBidBar, prevAskBar;
		Instrument instrument;
		
		this.console.getOut().print("--- Started "+quantisan1.class+" v." + version + " ---");
		this.console.getOut().print(Instrument.toStringSet(instSet));
		print(" ---");
		
		setRiskPct();
		
		// initialize sentiment
		while (instIter.hasNext())	
		{
			instrument = (Instrument)instIter.next();
			prevBidBar = this.history.getBar(instrument, PERIODINT, OfferSide.BID, 1);
			prevAskBar = this.history.getBar(instrument, PERIODINT, OfferSide.ASK, 1);
			setSentiment(instrument, prevBidBar, prevAskBar);			
		}
		
		for (IOrder order : this.engine.getOrders()) 
		{
			 drawTrade(order);
		}

	}
	
	// Represents message sent from server to client application 
	public void onMessage(IMessage message) throws JFException {
		IMessage.Type msgType = message.getType();
		IOrder order = message.getOrder();
		
		// Draw trades on the chart
		if (msgType == IMessage.Type.ORDER_FILL_OK ||
			msgType == IMessage.Type.ORDER_CLOSE_OK) 
		{
			drawTrade(order);
		}
		
		// adjust riskPct based on win/lose
		if (msgType == IMessage.Type.ORDER_CLOSE_OK) {
			if (isAfterNorm) {			// after a normal trade
				setRiskPct();			// getRiskAdj(order)
				goReverse(order);				
			}		
		}		
    }
	
   public void onAccount(IAccount account) throws JFException {
		this.acctEquity = account.getEquity();
		
		// Rule 6.2, max. one position only
		this.tradingAllowed = (account.getUseOfLeverage() == 0);

	}
	
	public void onStop() throws JFException {		
		print("---Stopped---");
   }
}
// ****************************************************************************
