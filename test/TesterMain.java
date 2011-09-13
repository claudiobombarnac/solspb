

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solspb.jforex.ADStockMarket;
import solspb.jforex.CurvesProtocolHandler;
import solspb.jforex.TaskManager;
import solspb.jforex.TesterFeedDataProvider;
import solspb.jforex.TaskManager.Environment;
import artist.api.BaseThread;
import artist.api.BrokerInt;
import artist.api.Constants;
import artist.api.ContextLoader;
import artist.api.IContext;
import artist.api.IDataQueue;
import artist.api.beans.Queue;
import artist.api.beans.Quote;
import artist.api.beans.Tick;

import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.impl.connect.IndicatorsSettingsStorage;
import com.dukascopy.api.impl.connect.PrintStreamNotificationUtils;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.FeedDataProvider;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;
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
        final TaskManager manager = new TaskManager(Environment.REMOTE, true, "sol", console, testerFeedDataProvider, null, null,null,null,null,null, null, null);
        LOGGER.info("Subscribing instruments...");
        
        //start the strategy
        LOGGER.info("Starting strategy");

        manager.startStrategy(new jforex.MA6_Play(), null, "Arnab2", true);
        
                FeedDataProvider.getDefaultInstance().connected();
        
        final IContext ctx = ContextLoader.getInstance();
        BrokerInt broker = ctx.getBroker();
//        CandleData d = (CandleData)FeedDataProvider.getDefaultInstance().getLastCandle(Instrument.LKOH, Period.ONE_MIN, OfferSide.BID);
//        for (int i = 0; i < 1000; i++)
//        	manager.newCandle(Instrument.LKOH, Period.ONE_MIN, d, d);
//        for (int i = 0; i < 1000000; i++) {
//        	Tick tick = broker.getTick("GAZP");
//        	testerFeedDataProvider.tickReceived(Instrument.GAZP, tick.time, tick.ask, tick.bid, tick.askVol, tick.bidVol);
//        	long time = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, System.currentTimeMillis());
//        	testerFeedDataProvider.barsReceived(Instrument.GAZP, Period.ONE_SEC, new IntraPeriodCandleData(false, time, 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random()), new IntraPeriodCandleData(false, time, 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random(), 100*Math.random()));
//            manager.onMarketState(new ADStockMarket("GAZP", BigDecimal.valueOf(100*Math.random()), BigDecimal.valueOf(100*Math.random())));
//            Thread.sleep(100);
//        }

        final TesterFeedDataProvider tfp = testerFeedDataProvider;

        BaseThread queueThread = new BaseThread() {
        	private double price;
        	public void tick() throws InterruptedException {
                IDataQueue<Queue> dataQueue = (IDataQueue<Queue>)ctx.getQueue(Queue.class);
                	if (dataQueue.size() == 0)
        			synchronized(dataQueue) {
							dataQueue.wait();
        			}
        		while (dataQueue.size() != 0) {
        			Queue q = dataQueue.pop();
        			if (q.getPrice() > price && q.getBuyQty() > 0) {
	        	        LOGGER.debug("Buy " + q.getBuyQty() + " price " + q.getPrice());
	                	tfp.tickReceived(Instrument.fromString(q.getPaper()), q.getLastUpdateMillies().getTime(), q.getPrice(), price, q.getSellQty(), q.getBuyQty());
	                	manager.onMarketState(new ADStockMarket(Constants.PAPER, BigDecimal.ONE, BigDecimal.ONE));
	                	price = q.getPrice();
        			}
        			else if (q.getPrice() < price && q.getSellQty() > 0) {
	        	        LOGGER.debug("Sell " + q.getSellQty() + " price " + q.getPrice());
	                	tfp.tickReceived(Instrument.fromString(q.getPaper()), q.getLastUpdateMillies().getTime(), price, q.getPrice(), q.getSellQty(), q.getBuyQty());
	                	manager.onMarketState(new ADStockMarket(Constants.PAPER, BigDecimal.ONE, BigDecimal.ONE));
	                	price = q.getPrice();        			
	                	}
        			}
        	}
        	public void tack() {
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        };

        BaseThread quoteThread = new BaseThread() {
        	public void tick() throws InterruptedException {
                IDataQueue<Quote> dataQueue = (IDataQueue<Quote>)ctx.getQueue(Quote.class);
                	if (dataQueue.size() == 0) {
        			synchronized(dataQueue) {
							dataQueue.wait();
        			}
                }
        		while (dataQueue.size() != 0) {
        			Quote q = dataQueue.pop();
        	        LOGGER.debug("Size" + dataQueue.size() + " date " + q.getDate());
                	tfp.barsReceived(Instrument.fromString(q.getTicker()), Period.ONE_SEC, new IntraPeriodCandleData(false, q.getDate().getTime(), q.getOpen(), q.getClose(), q.getLow(), q.getHi(), q.getVol()), new IntraPeriodCandleData(false, q.getDate().getTime(), q.getOpen(), q.getClose(), q.getLow(), q.getHi(), q.getVol()));
                	manager.onMarketState(new ADStockMarket(Constants.PAPER, BigDecimal.ONE, BigDecimal.ONE));
                	}
        	}
                	public void tack() {
                		try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
                	}
        };

        queueThread.start();
        quoteThread.start();
        Thread.sleep(10000);
        manager.stopStrategy();
        queueThread.stopIt();
        quoteThread.stopIt();
        ctx.stopIt();
        FeedDataProvider.getDefaultInstance().disconnected();
    }
}
