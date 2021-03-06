package com.quantisan.JFUtil;

import java.util.HashSet;
import java.util.Set;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Library;
import com.dukascopy.api.Period;

@Library("JFQuantisan.jar")
public class AccountingTester implements IStrategy {
	private Accounting accounter;
	private Logging logger;
	private IContext context;
	
	@Configurable("Instrument") public Instrument selectedInst = Instrument.EURJPY;
	@Configurable("StopSize") public int stopSize = 100;
	
	@Override
	public void onStart(IContext context) throws JFException {
		logger = new Logging(context.getConsole());
		accounter = new Accounting(context);
		this.context = context;
		Set<Instrument> instSet = new HashSet<Instrument>();
		instSet.add(selectedInst);
		accounter.subscribeTransitionalInstruments(instSet);
		logger.print("done onStart");
		
	}

	@Override
	public void onTick(Instrument instrument, ITick tick) throws JFException {
		
	}

	@Override
	public void onBar(Instrument instrument, Period period, IBar askBar,
			IBar bidBar) throws JFException 
	{
		if (period == Period.TEN_SECS) {					
			double stopSizePip = this.stopSize * selectedInst.getPipValue();
			//logger.print("" + accounter.getAccountRiskPerUnit(selectedInst, stopSizePip));
			this.context.stop();
		}

	}

	@Override
	public void onMessage(IMessage message) throws JFException {


	}

	@Override
	public void onAccount(IAccount account) throws JFException {
		accounter.update(account);

	}

	@Override
	public void onStop() throws JFException {


	}

}
