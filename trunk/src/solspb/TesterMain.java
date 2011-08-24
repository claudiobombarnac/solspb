package solspb;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import jforex.MA6_Play;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.system.ISystemListener;

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
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
        //setting initial deposit
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryCurrency(), 50000);
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = client.downloadData(null);
        //wait for downloading to complete
        future.get();
        //start the strategy
        LOGGER.info("Starting strategy");
        client.startStrategy(new MA6_Play(), new LoadingProgressListener() {
            @Override
            public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                LOGGER.info(information);
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        });
        //now it's running
    }
}
