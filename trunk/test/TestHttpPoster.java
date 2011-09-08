import java.util.GregorianCalendar;

import com.dukascopy.charts.data.datacache.Data;

import junit.framework.TestCase;
import solspb.client.FinamDataLoader;
import solspb.client.FinamDataLoader.Format;
import solspb.client.FinamDataLoader.Period;

public class TestHttpPoster extends TestCase {
    public void testSendGetRequest() {
        System.setProperty("http.proxyHost", "10.10.0.20");
        System.setProperty("http.proxyPort", "80");
//        String result = HTTPRequestPoster.sendGetRequest("http://195.128.78.52/GAZP_100907_110907.txt", "d=d&market=1&em=16842&df=7&mf=8&yf=2010&dt=7&mt=8&yt=2011&p=7&f=GAZP_100907_110907&e=.txt&cn=GAZP&dtf=1&tmf=1&MSOR=0&dep=1&sep2=1&datf=2&at=1");
//        System.out.println(result);
        Data[] result = FinamDataLoader.loadData(new GregorianCalendar(2011, 1, 1), new GregorianCalendar(), FinamDataLoader.Market.MICEX_STOCKS, "SBER", Period.DAILY);
        System.out.println(result);

    }

}
