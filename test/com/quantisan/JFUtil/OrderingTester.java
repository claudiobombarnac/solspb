package com.quantisan.JFUtil;

import java.util.List;
import java.util.concurrent.Future;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Library;
import com.dukascopy.api.Period;

@Library("JFQuantisan.jar")
public class OrderingTester implements IStrategy {
	Ordering orderer;
	Logging logger;
	
	@Configurable("Instrument") public Instrument selectedInst = Instrument.EURUSD;
	
	@Override
	public void onStart(IContext context) throws JFException {		
		orderer = new Ordering(context, 3);
		logger = new Logging(context.getConsole());
		
		IOrder order = null;
		logger.print("Placing bid");

		Future<IOrder> future = 
			orderer.placeMarketOrder("ABC", "xyz", selectedInst, 
									IEngine.OrderCommand.SELL,
									0.1, 112.00, 50, 0d);
//		try {
//			order = future.get();
//		}
//		catch (Exception ex) {
//			logger.printErr("Bid order not ready yet.", ex);				
//			return;
//		}
//		order.waitForUpdate(1000);
		logger.print("done onStart");
	}

	@Override
	public void onTick(Instrument instrument, ITick tick) throws JFException {
		

	}

	@Override
	public void onBar(Instrument instrument, Period period, IBar askBar,
			IBar bidBar) throws JFException {
		if (period != Period.ONE_HOUR || instrument != selectedInst)	return;
		
		List<IOrder> orders = orderer.getOrders(selectedInst);
		for (IOrder order : orders)
			logger.printOrderInfo(order);
	}

	@Override
	public void onMessage(IMessage message) throws JFException {


	}

	@Override
	public void onAccount(IAccount account) throws JFException {

	}

	@Override
	public void onStop() throws JFException {
		//for (IOrder order : engine.getOrders())
			//engine.closeOrders(order);

	}

}
