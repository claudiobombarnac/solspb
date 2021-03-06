package jforex;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.IEngine.OrderCommand;

public class MA6_Play implements solspb.jforex.IStrategy {
    private IEngine engine = null;
    private IIndicators indicators = null;
    private int tagCounter = 0;
    private double[] ma1 = new double[Instrument.values().length];
    private IConsole console;

    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        indicators = context.getIndicators();
        this.console = context.getConsole();
        console.getOut().println("Started");
    }

    public void onStop() throws JFException {
        for (IOrder order : engine.getOrders()) {
            order.close();
        }
        console.getOut().println("Stopped");
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        System.out.println("onTick");
    	if (ma1[instrument.ordinal()] == -1) {
            ma1[instrument.ordinal()] = indicators.ema(instrument, Period.ONE_HOUR, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 7, 1);
        }
        double ma0 = indicators.ema(instrument, Period.ONE_HOUR, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 3, 0);
        if (ma0 == 0 || ma1[instrument.ordinal()] == 0) {
            ma1[instrument.ordinal()] = ma0;
            return;
        }

        double diff = (ma1[instrument.ordinal()] - ma0) / (instrument.getPipValue());
        System.out.print(diff + " ");
        if (positionsTotal(instrument, diff > 1 ? OrderCommand.SELL : OrderCommand.BUY) == 0) {
            if (diff > 1) {
                engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.SELL, 1, tick.getAsk(), 2, tick.getAsk()
                        + instrument.getPipValue() * 100, tick.getAsk() - instrument.getPipValue() * 30);
            }
            if (diff < -1) {
                engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.BUY, 1, tick.getBid(), 2, tick.getBid()
                        - instrument.getPipValue() * 100, tick.getBid() + instrument.getPipValue() * 30);
            }
        }
        ma1[instrument.ordinal()] = ma0;
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        System.out.println("onBar");
    }

    //count open positions
    protected int positionsTotal(Instrument instrument, OrderCommand command) throws JFException {
        int counter = 0;
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED && order.getOrderCommand().equals(command)) {
                counter++;
            }
            else if (order.getState() == IOrder.State.FILLED && !order.getOrderCommand().equals(command)) {
                counter--;
            }
        }
        return counter;
    }

    protected String getLabel(Instrument instrument) {
        String label = instrument.name();
        label = label + (tagCounter++);
        label = label.toLowerCase();
        return label;
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }
}