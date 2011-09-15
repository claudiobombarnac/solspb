package solspb.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.charts.data.datacache.CandleData;
import com.dukascopy.charts.data.datacache.Data;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;

public class FinamDataLoader {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd,HHmmss");
    private static final String DELIM = ",;";
    
    private static Logger logger = LoggerFactory.getLogger(FinamDataLoader.class);

    public enum Market {NONE, MICEX_STOCKS, MICEX_BONDS, RTS, N4, FOREX, WORLD_IDX, US_FUTURES, ADR, SPFB_ARCH, RTS_GAZ, RTS_GTS,MICEX_UNLIST_BONDS, N13, FORTS, N15, MICEX_ARCH, FORTS_ARCH, RTS_ARCH, N19, RTS_BOARD, N21, N22, N23, GOODS, US_BATS, US_GOVBONDS, US_INDUSTRY, ETF, MICEX_PIF, WORLD_ECO_IDX, N31, N32, N33, N34, N35, N36, N37, RTS_STD}; 
    public enum Period {NONE, TICK, ONE_MIN, FIVE_MINS, TEN_MINS, FIFTEEN_MINS, THIRTY_MINS, ONE_HOUR, DAILY, WEEKLY, MONTHLY, HOUR1030 };
    public enum Format {NONE, TPDTOHLCV, TPDTOHLC, TPDTCV, TPDTC, DTOHLCV};
    public HashMap<Market, String[]> instrument = new HashMap<Market, String[]>() {{
        put(Market.MICEX_STOCKS, new String[] {"LKOH", "GAZP", "SBER", "GMKN"});
        put(Market.FORTS, new String[] {"RIZ1", "RIZ2", "RIH2", "RIM2", "RIM5"});
    }};

    private static String SERVER = "http://195.128.78.52/";
    private static String getFileName(Calendar from, Calendar to, String inst) {
        DateFormat df = new SimpleDateFormat("yyMMdd");
        return inst + "_" + df.format(from.getTime()) + "_" + df.format(to.getTime()) + ".txt";
    }
    private static String getParams(Calendar from, Calendar to, Market market, String inst, Period period, Format format) {
        return "d=d&market=" + market.ordinal() + 
               "&em=16842&df=" + from.get(Calendar.DAY_OF_MONTH) + 
               "&mf=" + from.get(Calendar.MONTH) + 
               "&yf=" + from.get(Calendar.YEAR) + 
               "&dt=" + to.get(Calendar.DAY_OF_MONTH) + 
               "&mt=" + to.get(Calendar.MONTH) + 
               "&yt=" + to.get(Calendar.YEAR) + 
               "&p=" + period.ordinal() +
               "&f=" + inst + from.getTimeInMillis() + to.getTimeInMillis() + 
               "&e=.txt&cn=" + inst + 
               "&dtf=1&tmf=1&MSOR=0&sep=1&sep2=1&datf=" + format.ordinal();
    }
    
	private static com.dukascopy.api.Period translatePeriod(Period p) {
        if (p == Period.TICK) return com.dukascopy.api.Period.TICK;
        else if (p == Period.ONE_MIN) return com.dukascopy.api.Period.ONE_MIN;
        else if (p == Period.FIVE_MINS) return com.dukascopy.api.Period.FIVE_MINS;
        else if (p == Period.TEN_MINS) return com.dukascopy.api.Period.TEN_MINS;
        else if (p == Period.FIFTEEN_MINS) return com.dukascopy.api.Period.FIFTEEN_MINS;
        else if (p == Period.THIRTY_MINS) return com.dukascopy.api.Period.THIRTY_MINS;
        else if (p == Period.ONE_HOUR) return com.dukascopy.api.Period.ONE_HOUR;
        else if (p == Period.DAILY) return com.dukascopy.api.Period.DAILY;
        else if (p == Period.WEEKLY) return com.dukascopy.api.Period.WEEKLY;
        else if (p == Period.MONTHLY) return com.dukascopy.api.Period.MONTHLY;
        else throw new UnsupportedOperationException("Period " + p + " is not supported");
    }	    
    
    public static CandleData getCandle(String quote, Period period) throws ParseException {
        StringTokenizer st = new StringTokenizer(quote, DELIM);
        Date date = DATE_FORMAT.parse(st.nextToken() + ',' + st.nextToken());
        date = new Date(DataCacheUtils.getCandleStartFast(translatePeriod(period), date.getTime()));
        Double open = Double.parseDouble(st.nextToken());
        Double high = Double.parseDouble(st.nextToken());
        Double low = Double.parseDouble(st.nextToken());
        Double close = Double.parseDouble(st.nextToken());
        Long vol = Long.parseLong(st.nextToken());
        return new IntraPeriodCandleData(false, date.getTime(), open, close, low, high, vol);
    }
    
    public static Data[] loadData(Calendar from, Calendar to, Market market, String inst, Period period) {
        logger.info("FINAM: " + DateFormat.getDateTimeInstance().format(from.getTime()) + "-" + DateFormat.getDateTimeInstance().format(to.getTime()) + " for " + inst + " " + period);
    	if (period == Period.TICK)
    		period = Period.ONE_MIN;
        String response = null;
    	try {
    	    response = HTTPRequestPoster.sendGetRequest(FinamDataLoader.SERVER + "/" + getFileName(from, to, inst), getParams(from, to, market, inst, period, Format.DTOHLCV));
    	}
    	catch (IOException e) {
            System.setProperty("http.proxyHost", "10.10.0.20");
            System.setProperty("http.proxyPort", "80");
            try {
                response = HTTPRequestPoster.sendGetRequest(FinamDataLoader.SERVER + "/" + getFileName(from, to, inst), getParams(from, to, market, inst, period, Format.DTOHLCV));
            }
            catch(Exception ex) {
                e.printStackTrace();
                return null;
            }
    	}
        ArrayList<Data> data = new ArrayList<Data>(); 
            StringTokenizer t = new StringTokenizer(response);
            String quote;
            while (t.hasMoreTokens()) {
            	quote = t.nextToken().trim();
              if (quote.length() == 0)
                  continue;
              try {
                  CandleData q = getCandle(quote, period);
                  data.add(q);
              }
              catch (Exception e) {
                  logger.error("Failed to parse quote: {}", quote);
                  System.out.println(new String(quote.getBytes(), Charset.forName("ISO8859_5")));
                  System.out.println(new String(quote.getBytes(), Charset.forName("Cp866")));
              }
          }
            return data.toArray(new Data[]{});
        }
}
