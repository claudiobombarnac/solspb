

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jforex.Arnab2;
import jforex.AutoTrader_Demo;
import jforex.BKJAN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solspb.jforex.ADCurrencyMarket;
import solspb.jforex.ADStockMarket;
import solspb.jforex.CurvesProtocolHandler;
import solspb.jforex.TaskManager;
import solspb.jforex.TaskManager.Environment;
import solspb.jforex.TesterFeedDataProvider;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.impl.connect.IndicatorsSettingsStorage;
import com.dukascopy.api.impl.connect.PrintStreamNotificationUtils;
import com.dukascopy.charts.data.datacache.CandleData;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.FeedDataProvider;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;
import com.dukascopy.charts.data.orders.OrdersProvider;
import com.dukascopy.charts.math.indicators.IndicatorsProvider;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterOrdersProvider;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;
import com.dukascopy.transport.common.msg.request.CurrencyMarket;

public class TesterMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(TesterMain.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("Connecting...");
        IConsole console = new IConsole() {

            public PrintStream getErr()
            {
                return System.err;
            }

            public PrintStream getOut()
            {
                return System.out;
            }
        };
        PrintStream out = System.out;
        PrintStream err = System.err;
        NotificationUtilsProvider.setNotificationUtils(new PrintStreamNotificationUtils(out, err));
        OrdersProvider.createInstance(null);
        IndicatorsProvider.createInstance(new IndicatorsSettingsStorage("sol"));
 
        TesterFeedDataProvider testerFeedDataProvider = null;
        try
        {
            testerFeedDataProvider = new TesterFeedDataProvider("sol", new CurvesProtocolHandler(), OrdersProvider.getInstance());
        }
        catch(DataCacheException e)
        {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException((new StringBuilder()).append("Cannot create TesterFeedDataProvider:").append(e.getMessage()).toString());
        }
        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.LKOH);
        instruments.add(Instrument.GAZP);

        testerFeedDataProvider.setInstrumentsSubscribed(instruments);
        FeedDataProvider.getDefaultInstance().setInstrumentsSubscribed(instruments);
        TaskManager manager = new TaskManager(Environment.REMOTE, true, "sol", console, testerFeedDataProvider, null,null,null,null,null,null, null, null);
        LOGGER.info("Subscribing instruments...");
        
        //start the strategy
        LOGGER.info("Starting strategy");

        manager.startStrategy(new jforex.MA6_Play(), null, "Arnab2", true);
        new Thread() {
        	public void run() {
                FeedDataProvider.getDefaultInstance().connected();
        		
        	}
        }.start();
        
//        CandleData d = (CandleData)FeedDataProvider.getDefaultInstance().getLastCandle(Instrument.LKOH, Period.ONE_MIN, OfferSide.BID);
//        for (int i = 0; i < 1000; i++)
//        	manager.newCandle(Instrument.LKOH, Period.ONE_MIN, d, d);
        for (int i = 0; i < 1000; i++) {
        	testerFeedDataProvider.tickReceived(Instrument.GAZP, System.currentTimeMillis(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random());
        	long time = DataCacheUtils.getNextCandleStartFast(Period.ONE_MIN, System.currentTimeMillis() + 100000);
//        	testerFeedDataProvider.barsReceived(Instrument.GAZP, Period.ONE_MIN, new IntraPeriodCandleData(false, time, 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random()), new IntraPeriodCandleData(false, time, 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random()));
        }
        for (int i = 0; i < 1000; i++) {
//        	testerFeedDataProvider.barsReceived(Instrument.GAZP, Period.ONE_MIN, new IntraPeriodCandleData(false, System.currentTimeMillis(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random()), new IntraPeriodCandleData(false, System.currentTimeMillis(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random()));
            manager.onMarketState(new ADStockMarket("GAZP", BigDecimal.valueOf(100*Math.random()), BigDecimal.valueOf(100*Math.random())));
//            manager.onMarketState(new ADStockMarket("LKOH", BigDecimal.valueOf(10*Math.random()), BigDecimal.valueOf(10*Math.random())));
            Thread.sleep(1000);
        }

        Thread.sleep(120000);
        manager.stopStrategy();
    }
}
