package solspb;

import java.io.File;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jforex.Arnab2;
import jforex.MA6_Play;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solspb.TaskManager.Environment;

import com.dukascopy.api.IConsole;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.charts.data.datacache.CandleData;

public class TesterMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(TesterMain.class);

    //url of the DEMO jnlp
    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    //user name
    private static String userName = "DEMO2AySCo";
    //password
    private static String password = "AySCo";

    public static void main(String[] args) throws Exception {
        //get the instance of the IClient interface
        System.setProperty("http.proxyHost", "10.10.0.20");
        System.setProperty("http.proxyPort", "80");

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        //connection is needed for data downloading
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

        TaskManager manager = new TaskManager(Environment.REMOTE, true, "sol", console, null,null,null,null,null,null, null, null);
        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.LKOH);
        LOGGER.info("Subscribing instruments...");
        //start the strategy
        LOGGER.info("Starting strategy");
        manager.startStrategy(new Arnab2(), null, "Arnab2", true);
        for (int i = 0; i < 1000; i++)
        	manager.newCandle(Instrument.LKOH, Period.ONE_MIN, new CandleData(), new CandleData());
        for (int i = 0; i < 1000; i++)
        	manager.onMarketState(new ADStockMarket("LKOH", BigDecimal.valueOf(100), BigDecimal.valueOf(100)));

        manager.stopStrategy();
        //now it's running
    }
}
