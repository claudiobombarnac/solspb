package jforex;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.IIndicators.MaType;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;

public class Arnab2 implements IStrategy {
    private IEngine engine = null;
    private IIndicators indicators = null;
    private IContext context;
    private int tagCounter = 0;
    private double[] ma1 = new double[Instrument.values().length];
    //private IConsole console;
    private double lot = 1.0;
    private boolean t = false;

    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        indicators = context.getIndicators();
        //this.console = context.getConsole();
        this.context = context;
    }

    public void onStop() throws JFException {
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
		if(instrument.name().equalsIgnoreCase(Instrument.EURUSD.name())) {
			t = true;
		}
		if (instrument.name().equalsIgnoreCase(Instrument.USDJPY.name())) {
			t = false;
		}
		if (instrument.name().equalsIgnoreCase(Instrument.NZDUSD.name())) {
			lot = 1.0;
		}
		if (instrument.name().equalsIgnoreCase(Instrument.USDSGD.name())) {
			lot = 5.0;
		}
		if (instrument.name().equalsIgnoreCase(Instrument.USDCAD.name())) {
			lot = 10.0;
		}
    	if (instrument.name().equalsIgnoreCase(Instrument.EURUSD.name())
    			|| instrument.name().equalsIgnoreCase(Instrument.USDJPY.name())
    			|| instrument.name().equalsIgnoreCase(Instrument.NZDUSD.name())
    			|| instrument.name().equalsIgnoreCase(Instrument.USDSGD.name())
    			|| instrument.name().equalsIgnoreCase(Instrument.USDCAD.name())
    			|| !t) return;
        if (ma1[instrument.ordinal()] == -1) {
            ma1[instrument.ordinal()] = indicators.ema(instrument, Period.FOUR_HOURS, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 7, 1);
        }
        double ma0 = indicators.ema(instrument, Period.FOUR_HOURS, OfferSide.BID, IIndicators.AppliedPrice.MEDIAN_PRICE, 3, 0);
        if (ma0 == 0 || ma1[instrument.ordinal()] == 0) {
            ma1[instrument.ordinal()] = ma0;
            return;
        }

        double diff = (ma1[instrument.ordinal()] - ma0) / (instrument.getPipValue());

        if (positionsTotal(instrument) == 0) {
    		OfferSide bid = OfferSide.BID;
    		AppliedPrice close = AppliedPrice.CLOSE;
    		MaType sma = MaType.SMA;

    		calculate(instrument, bid, close, sma);
    		
    		double tt = pc + nc;
    		double nr = nc / tt;
    		double pos = pc / tt;
    		IChart chart = context.getChart(instrument);
    		if (chart != null) {
    			chart.comment("nr : " + nr + ",pos : " + pos + ",diff : " + diff);
    		}
            if (diff > 1  && nr > 0.7) {
                engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.SELL, lot, 0, 0, tick.getAsk()
                        + instrument.getPipValue() * 100, tick.getAsk() - instrument.getPipValue() * 30);
            }
            if (diff < -1 && pos > 0.7) {
                engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.BUY, lot, 0, 0, tick.getBid()
                        - instrument.getPipValue() * 100, tick.getBid() + instrument.getPipValue() * 30);
            }
        }
        ma1[instrument.ordinal()] = ma0;
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
    }

    //count open positions
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
        label = label.substring(0, 2) + label.substring(3, 5);
        label = label + (tagCounter++);
        label = label.toLowerCase();
        return label;
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onAccount(IAccount account) throws JFException {
    }

	private int pc = 0;
	
	private int nc = 0;

	
	private void calculate(Instrument instrument, OfferSide bid, AppliedPrice close, MaType sma) throws JFException {
		pc = 0;
		nc = 0;
		int bar = 0;
		Period period = Period.FOUR_HOURS;
		double ad = indicators.ad(instrument, period, bid, bar);
		double ad1 = indicators.ad(instrument, period, bid, bar + 1);
		double awesomeOsc = indicators.awesome(instrument, period, bid,
				close, 5, sma, 34, sma, bar)[0];
		double acceleratorOsc = indicators.adOsc(instrument, period,
				bid, 3, 10, bar);
		double adx = indicators.adx(instrument, period, bid, 14, bar);
		double adx1 = indicators.adx(instrument, period, bid, 14,
				bar + 1);
		double apo = indicators.apo(instrument, period, bid, close, 12,
				26, sma, bar);
		double apo1 = indicators.apo(instrument, period, bid, close,
				12, 26, sma, bar + 1);
		double aroonOsc = indicators.aroonOsc(instrument, period, bid,
				14, bar);
		double aroonOsc1 = indicators.aroonOsc(instrument, period, bid,
				14, bar + 1);
		double atan = indicators.atan(instrument, period, bid, close,
				bar);
		double atan1 = indicators.atan(instrument, period, bid, close,
				bar + 1);
		double atr = indicators.atr(instrument, period, bid, 14, bar);
		double atr1 = indicators.atr(instrument, period, bid, 14,
				bar + 1);

		double bop = indicators.bop(instrument, period, bid, bar);
		double bop1 = indicators.bop(instrument, period, bid, bar + 1);

		double cci = indicators.cci(instrument, period, bid, 14, bar);
		double cmo = indicators.cmo(instrument, period, bid, close, 14,
				bar);
		double cmo1 = indicators.cmo(instrument, period, bid, close,
				14, bar + 1);
		double cos = indicators
				.cos(instrument, period, bid, close, bar);
		double cos1 = indicators.cos(instrument, period, bid, close,
				bar + 1);

		double dema = indicators.dema(instrument, period, bid, close,
				30, bar);
		double dema1 = indicators.dema(instrument, period, bid, close,
				30, bar + 1);
		double dx = indicators.dx(instrument, period, bid, 14, bar);
		double dx1 = indicators
				.dx(instrument, period, bid, 14, bar + 1);

		double ema = indicators.ema(instrument, period, bid, close, 30,
				bar);
		double ema1 = indicators.ema(instrument, period, bid, close,
				30, bar + 1);
		double exp = indicators
				.exp(instrument, period, bid, close, bar);
		double exp1 = indicators.exp(instrument, period, bid, close,
				bar + 1);

		double ht_dcperiod = indicators.ht_dcperiod(instrument, period,
				bid, close, bar);
		double ht_dcperiod1 = indicators.ht_dcperiod(instrument,
				period, bid, close, bar + 1);
		double ht_dcphase = indicators.ht_dcphase(instrument, period,
				bid, close, bar);
		double ht_dcphase1 = indicators.ht_dcphase(instrument, period,
				bid, close, bar + 1);
		double ht_phasor = indicators.ht_phasor(instrument, period,
				bid, close, bar)[0];
		double ht_phasor1 = indicators.ht_phasor(instrument, period,
				bid, close, bar + 1)[0];
		double ht_sine = indicators.ht_sine(instrument, period, bid,
				close, bar)[0];
		double ht_sine1 = indicators.ht_sine(instrument, period, bid,
				close, bar + 1)[0];
		double ht_trendline = indicators.ht_trendline(instrument,
				period, bid, close, bar);
		double ht_trendline1 = indicators.ht_trendline(instrument,
				period, bid, close, bar + 1);

		double kama = indicators.kama(instrument, period, bid, close,
				30, bar);
		double kama1 = indicators.kama(instrument, period, bid, close,
				30, bar + 1);

		double linearReg = indicators.linearReg(instrument, period,
				bid, close, 14, bar);
		double linearReg1 = indicators.linearReg(instrument, period,
				bid, close, 14, bar + 1);
		double linearRegAngle = indicators.linearRegAngle(instrument,
				period, bid, close, 14, bar);
		double linearRegAngle1 = indicators.linearRegAngle(instrument,
				period, bid, close, 14, bar + 1);
		double linearRegIntercept = indicators.linearRegIntercept(
				instrument, period, bid, close, 14, bar);
		double linearRegIntercept1 = indicators.linearRegIntercept(
				instrument, period, bid, close, 14, bar + 1);
		double linearRegSlope = indicators.linearRegSlope(instrument,
				period, bid, close, 14, bar);
		double linearRegSlope1 = indicators.linearRegSlope(instrument,
				period, bid, close, 14, bar + 1);
		double ln = indicators.ln(instrument, period, bid, close, bar);
		double ln1 = indicators.ln(instrument, period, bid, close,
				bar + 1);
		double log = indicators.log10(instrument, period, bid, close,
				bar);
		double log1 = indicators.log10(instrument, period, bid, close,
				bar + 1);

		double macd[] = indicators.macd(instrument, period, bid, close,
				12, 26, 9, bar);
		double macdMain = macd[0];
		double macdSignal = macd[1];
		double mfi = indicators.mfi(instrument, period, bid, 14, bar);
		double mfi1 = indicators.mfi(instrument, period, bid, 14,
				bar + 1);

		double natr = indicators.natr(instrument, period, bid, 14, bar);
		double natr1 = indicators.natr(instrument, period, bid, 14,
				bar + 1);

		double obv = indicators.obv(instrument, period, bid, close,
				bid, bar);
		double obv1 = indicators.obv(instrument, period, bid, close,
				bid, bar + 1);

		double plusDi = indicators.plusDi(instrument, period, bid, 14,
				bar);
		double plusDi1 = indicators.plusDi(instrument, period, bid, 14,
				bar + 1);
		double ppo = indicators.ppo(instrument, period, bid, close, 12,
				26, sma, bar);
		double ppo1 = indicators.ppo(instrument, period, bid, close,
				12, 26, sma, bar + 1);

		double rvi[] = indicators.rvi(instrument, period, bid, 10, bar);
		double rviMain = rvi[0];
		double rviSignal = rvi[1];
		double rsi = indicators.rsi(instrument, period, bid, close, 14,
				bar);
		double rsi1 = indicators.rsi(instrument, period, bid, close,
				14, bar + 1);
		double roc = indicators.roc(instrument, period, bid, close, 10,
				bar);
		double roc1 = indicators.roc(instrument, period, bid, close,
				10, bar + 1);

		double stoch[] = indicators.stoch(instrument, period, bid, 5,
				3, sma, 3, sma, bar);
		double stoch1[] = indicators.stoch(instrument, period, bid, 5,
				3, sma, 3, sma, bar + 1);
		double stochMain = stoch[0];
		double stochMain1 = stoch1[0];
		double stochSignal = stoch[1];
		double stochSignal1 = stoch1[1];
		double sar = indicators.sar(instrument, period, bid, 0.02, 0.2,
				bar);
		double sar1 = indicators.sar(instrument, period, bid, 0.02,
				0.2, bar + 1);
		double sin = indicators
				.sin(instrument, period, bid, close, bar);
		double sin1 = indicators.sin(instrument, period, bid, close,
				bar + 1);
		double smaInd = indicators.sma(instrument, period, bid, close,
				30, bar);
		double smaInd1 = indicators.sma(instrument, period, bid, close,
				30, bar + 1);
		double sqrt = indicators.sqrt(instrument, period, bid, close,
				bar);
		double sqrt1 = indicators.sqrt(instrument, period, bid, close,
				bar + 1);
		double stdDev = indicators.stdDev(instrument, period, bid,
				close, 5, 1, bar);
		double stdDev1 = indicators.stdDev(instrument, period, bid,
				close, 5, 1, bar + 1);
		double sum = indicators.sum(instrument, period, bid, close, 30,
				bar);
		double sum1 = indicators.sum(instrument, period, bid, close,
				30, bar + 1);

		double t3 = indicators.t3(instrument, period, bid, close, 5,
				0.7, bar);
		double t31 = indicators.t3(instrument, period, bid, close, 5,
				0.7, bar + 1);
		double tan = indicators
				.tan(instrument, period, bid, close, bar);
		double tan1 = indicators.tan(instrument, period, bid, close,
				bar + 1);
		double tanh = indicators.tanh(instrument, period, bid, close,
				bar);
		double tanh1 = indicators.tanh(instrument, period, bid, close,
				bar + 1);
		double tdi = indicators.td_i(instrument, period, bid, 14, bar)[0];
		double tdi1 = indicators.td_i(instrument, period, bid, 14,
				bar + 1)[0];
		double tema = indicators.tema(instrument, period, bid, close,
				30, bar);
		double tema1 = indicators.tema(instrument, period, bid, close,
				30, bar + 1);
		double trangge = indicators
				.trange(instrument, period, bid, bar);
		double trangge1 = indicators.trange(instrument, period, bid,
				bar + 1);
		double trima = indicators.trima(instrument, period, bid, close,
				30, bar);
		double trima1 = indicators.trima(instrument, period, bid,
				close, 30, bar + 1);
		double trix = indicators.trix(instrument, period, bid, close,
				30, bar);
		double trix1 = indicators.trix(instrument, period, bid, close,
				30, bar + 1);
		double tsf = indicators.tsf(instrument, period, bid, close, 14,
				bar);
		double tsf1 = indicators.tsf(instrument, period, bid, close,
				14, bar + 1);
		// double tvs = indicators.tvs(instrument, period, bid, close,
		// 18, bar);
		double typPrice = indicators.typPrice(instrument, period, bid,
				bar);
		double typPrice1 = indicators.typPrice(instrument, period, bid,
				bar + 1);

		double ultOsc = indicators.ultOsc(instrument, period, bid, 7,
				14, 28, bar);
		double ultOsc1 = indicators.ultOsc(instrument, period, bid, 7,
				14, 28, bar + 1);

		// moving up/down
		double var = indicators.var(instrument, period, bid, close, 5,
				1, bar);
		double var1 = indicators.var(instrument, period, bid, close, 5,
				1, bar + 1);

		double waddahAttar = indicators.waddahAttar(instrument, period,
				bid, bar);
		double wclPrice = indicators.wclPrice(instrument, period, bid,
				bar);
		double wclPrice1 = indicators.wclPrice(instrument, period, bid,
				bar + 1);
		double willr = indicators.willr(instrument, period, bid, 14,
				bar);
		double willr1 = indicators.willr(instrument, period, bid, 14,
				bar + 1);

		if ((ad - ad1) > 0)
			pc++;
		else if ((ad - ad1) < 0)
			nc++;

		if (awesomeOsc > 0)
			pc++;
		else if (awesomeOsc < 0)
			nc++;

		if (acceleratorOsc > 0)
			pc++;
		else if (acceleratorOsc < 0)
			nc++;

		if ((adx - adx1) > 0)
			pc++;
		else if ((adx - adx1) < 0)
			nc++;

		if ((apo - apo1) > 0)
			pc++;
		else if ((apo - apo1) < 0)
			nc++;

		if ((aroonOsc - aroonOsc1) > 0)
			pc++;
		else if ((aroonOsc - aroonOsc1) < 0)
			nc++;

		if ((atan - atan1) > 0)
			pc++;
		else if ((atan - atan1) < 0)
			nc++;

		if ((atr - atr1) > 0)
			pc++;
		else if ((atr - atr1) < 0)
			nc++;

		if ((bop - bop1) > 0)
			pc++;
		else if ((bop - bop1) < 0)
			nc++;

		if (cci > 0)
			pc++;
		else if (cci < 0)
			nc++;

		if ((cmo - cmo1) > 0)
			pc++;
		else if ((cmo - cmo1) < 0)
			nc++;

		if ((cos - cos1) > 0)
			pc++;
		else if ((cos - cos1) < 0)
			nc++;

		if ((dema - dema1) > 0)
			pc++;
		else if ((dema - dema1) < 0)
			nc++;

		if ((dx - dx1) > 0)
			pc++;
		else if ((dx - dx1) < 0)
			nc++;

		if ((ema - ema1) > 0)
			pc++;
		else if ((ema - ema1) < 0)
			nc++;

		if ((exp - exp1) > 0)
			pc++;
		else if ((exp - exp1) < 0)
			nc++;

		if ((ht_dcperiod - ht_dcperiod1) > 0)
			pc++;
		else if ((ht_dcperiod - ht_dcperiod1) < 0)
			nc++;

		if ((ht_dcphase - ht_dcphase1) > 0)
			pc++;
		else if ((ht_dcphase - ht_dcphase1) < 0)
			nc++;

		if ((ht_phasor - ht_phasor1) > 0)
			pc++;
		else if ((ht_phasor - ht_phasor1) < 0)
			nc++;

		if ((ht_sine - ht_sine1) > 0)
			pc++;
		else if ((ht_sine - ht_sine1) < 0)
			nc++;

		if ((ht_trendline - ht_trendline1) > 0)
			pc++;
		else if ((ht_trendline - ht_trendline1) < 0)
			nc++;

		if ((kama - kama1) > 0)
			pc++;
		else if ((kama - kama1) < 0)
			nc++;

		if ((linearReg - linearReg1) > 0)
			pc++;
		else if ((linearReg - linearReg1) < 0)
			nc++;

		if ((linearRegAngle - linearRegAngle1) > 0)
			pc++;
		else if ((linearRegAngle - linearRegAngle1) < 0)
			nc++;

		if ((linearRegIntercept - linearRegIntercept1) > 0)
			pc++;
		else if ((linearRegIntercept - linearRegIntercept1) < 0)
			nc++;

		if ((linearRegSlope - linearRegSlope1) > 0)
			pc++;
		else if ((linearRegSlope - linearRegSlope1) < 0)
			nc++;

		if ((ln - ln1) > 0)
			pc++;
		else if ((ln - ln1) < 0)
			nc++;

		if ((log - log1) > 0)
			pc++;
		else if ((log - log1) < 0)
			nc++;

		if (macdMain > 0)
			pc++;
		else if (macdMain < 0)
			nc++;

		if (macdSignal > 0)
			pc++;
		else if (macdSignal < 0)
			nc++;

		if ((mfi - mfi1) > 0)
			pc++;
		else if ((mfi - mfi1) < 0)
			nc++;

		if ((natr - natr1) > 0)
			pc++;
		else if ((natr - natr1) < 0)
			nc++;

		if ((obv - obv1) > 0)
			pc++;
		else if ((obv - obv1) < 0)
			nc++;

		if ((plusDi - plusDi1) > 0)
			pc++;
		else if ((plusDi - plusDi1) < 0)
			nc++;

		if ((ppo - ppo1) > 0)
			pc++;
		else if ((ppo - ppo1) < 0)
			nc++;

		if (rviMain > 0)
			pc++;
		else if (rviMain < 0)
			nc++;

		if (rviSignal > 0)
			pc++;
		else if (rviSignal < 0)
			nc++;

		if ((rsi - rsi1) > 0)
			pc++;
		else if ((rsi - rsi1) < 0)
			nc++;

		if ((roc - roc1) > 0)
			pc++;
		else if ((roc - roc1) < 0)
			nc++;

		if ((stochMain - stochMain1) > 0)
			pc++;
		else if ((stochMain - stochMain1) < 0)
			nc++;

		if ((stochSignal - stochSignal1) > 0)
			pc++;
		else if ((stochSignal - stochSignal1) < 0)
			nc++;

		if ((sar - sar1) > 0)
			pc++;
		else if ((sar - sar1) < 0)
			nc++;

		if ((sin - sin1) > 0)
			pc++;
		else if ((sin - sin1) < 0)
			nc++;

		if ((smaInd - smaInd1) > 0)
			pc++;
		else if ((smaInd - smaInd1) < 0)
			nc++;

		if ((sqrt - sqrt1) > 0)
			pc++;
		else if ((sqrt - sqrt1) < 0)
			nc++;

		if ((stdDev - stdDev1) > 0)
			pc++;
		else if ((stdDev - stdDev1) < 0)
			nc++;

		if ((sum - sum1) > 0)
			pc++;
		else if ((sum - sum1) < 0)
			nc++;

		if ((sum - sum1) > 0)
			pc++;
		else if ((sum - sum1) < 0)
			nc++;

		if ((sum - sum1) > 0)
			pc++;
		else if ((sum - sum1) < 0)
			nc++;

		if ((sum - sum1) > 0)
			pc++;
		else if ((sum - sum1) < 0)
			nc++;

		if ((sum - sum1) > 0)
			pc++;
		else if ((sum - sum1) < 0)
			nc++;

		if ((sum - sum1) > 0)
			pc++;
		else if ((sum - sum1) < 0)
			nc++;

		if ((t3 - t31) > 0)
			pc++;
		else if ((t3 - t31) < 0)
			nc++;

		if ((tan - tan1) > 0)
			pc++;
		else if ((tan - tan1) < 0)
			nc++;

		if ((tanh - tanh1) > 0)
			pc++;
		else if ((tanh - tanh1) < 0)
			nc++;

		if ((tdi - tdi1) > 0)
			pc++;
		else if ((tdi - tdi1) < 0)
			nc++;

		if ((tema - tema1) > 0)
			pc++;
		else if ((tema - tema1) < 0)
			nc++;

		if ((trangge - trangge1) > 0)
			pc++;
		else if ((trangge - trangge1) < 0)
			nc++;

		if ((trima - trima1) > 0)
			pc++;
		else if ((trima - trima1) < 0)
			nc++;

		if ((trix - trix1) > 0)
			pc++;
		else if ((trix - trix1) < 0)
			nc++;

		if ((tsf - tsf1) > 0)
			pc++;
		else if ((tsf - tsf1) < 0)
			nc++;

		// if(tvs > 0)
		// positiveCount++;
		// else if(tvs < 0)
		// negativeCount++;

		if ((typPrice - typPrice1) > 0)
			pc++;
		else if ((typPrice - typPrice1) < 0)
			nc++;

		if ((ultOsc - ultOsc1) > 0)
			pc++;
		else if ((ultOsc - ultOsc1) < 0)
			nc++;

		if ((var - var1) > 0)
			pc++;
		else if ((var - var1) < 0)
			nc++;

		if (waddahAttar > 0)
			pc++;
		else if (waddahAttar < 0)
			nc++;

		if ((wclPrice - wclPrice1) > 0)
			pc++;
		else if ((wclPrice - wclPrice1) < 0)
			nc++;

		if ((willr - willr1) > 0)
			pc++;
		else if ((willr - willr1) < 0)
			nc++;
	}
}