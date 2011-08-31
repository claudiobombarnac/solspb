package solspb;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

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

        final TesterClient client = new TesterClient();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                File reportFile = new File("C:\\report.html");
                try {
                    client.createReport(processId, reportFile);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });

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
        instruments.add(Instrument.EURUSD);
        LOGGER.info("Subscribing instruments...");
        //start the strategy
        LOGGER.info("Starting strategy");
        manager.startStrategy(new MA6_Play(), null, "MA6_Play", true);
        for (int i = 0; i < 1000; i++)
        	manager.newCandle(Instrument.EURUSD, Period.ONE_MIN, new CandleData(), new CandleData());
        manager.stopStrategy();
        //now it's running
    }
}
