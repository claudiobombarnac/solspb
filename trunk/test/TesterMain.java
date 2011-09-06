

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jforex.Arnab2;
import jforex.AutoTrader_Demo;
import jforex.BKJAN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solspb.jforex.ADStockMarket;
import solspb.jforex.FeedDataProvider;
import solspb.jforex.TaskManager;
import solspb.jforex.TaskManager.Environment;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.impl.connect.IndicatorsSettingsStorage;
import com.dukascopy.api.impl.connect.PrintStreamNotificationUtils;
import com.dukascopy.charts.data.datacache.CandleData;
import com.dukascopy.charts.data.orders.OrdersProvider;
import com.dukascopy.charts.math.indicators.IndicatorsProvider;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;

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
        FeedDataProvider.createFeedDataProvider("sol");
        FeedDataProvider.getDefaultInstance().addInstrumentNamesSubscribed(new HashSet<String>() {{add("LKOH");add("GAZP");}});
        TaskManager manager = new TaskManager(Environment.REMOTE, true, "sol", console, null,null,null,null,null,null, null, null);
        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.LKOH);
        LOGGER.info("Subscribing instruments...");
        //start the strategy
        LOGGER.info("Starting strategy");
        manager.startStrategy(new jforex.MA6_Play(), null, "Arnab2", true);
        CandleData d = (CandleData)FeedDataProvider.getDefaultInstance().getLastCandle(Instrument.LKOH, Period.ONE_MIN, OfferSide.BID);
        for (int i = 0; i < 1000; i++)
        	manager.newCandle(Instrument.LKOH, Period.ONE_MIN, d, d);
        for (int i = 0; i < 2000; i++) {
            manager.onMarketState(new ADStockMarket("GAZP", BigDecimal.valueOf(100*Math.random()), BigDecimal.valueOf(100*Math.random())));
//            manager.onMarketState(new ADStockMarket("LKOH", BigDecimal.valueOf(10*Math.random()), BigDecimal.valueOf(10*Math.random())));
        }
        manager.stopStrategy();
    }
}
