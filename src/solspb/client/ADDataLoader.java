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

import artist.api.BrokerInt;
import artist.api.Constants;
import artist.api.ContextLoader;

	import com.dukascopy.charts.data.datacache.CandleData;
	import com.dukascopy.charts.data.datacache.Data;
	import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;

	public class ADDataLoader {
	    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	    private static final String DELIM = "|";
	    
	    private static Logger logger = LoggerFactory.getLogger(ADDataLoader.class);
	    private static BrokerInt broker = ContextLoader.getInstance().getBroker();
	    
	    
		public static int translatePeriod(com.dukascopy.api.Period p) {
	        if (p == com.dukascopy.api.Period.TICK) return 0;
	        else if (p == com.dukascopy.api.Period.ONE_MIN) return 0;
	        else if (p == com.dukascopy.api.Period.FIVE_MINS) return 1;
	        else if (p == com.dukascopy.api.Period.TEN_MINS) return 2;
	        else if (p == com.dukascopy.api.Period.FIFTEEN_MINS) return 3;
	        else if (p == com.dukascopy.api.Period.THIRTY_MINS) return 4;
	        else if (p == com.dukascopy.api.Period.ONE_HOUR) return 5;
	        else if (p == com.dukascopy.api.Period.DAILY) return 6;
	        else if (p == com.dukascopy.api.Period.WEEKLY) return 7;
	        else if (p == com.dukascopy.api.Period.MONTHLY) return 8;
	        else throw new UnsupportedOperationException("Period " + p + " is not supported");
	    }	    
	    public static String translateMarket(String market) {
	        if ("FOREX".equals(market))
	            return "FOREX";
	        else if ("MICEX".equals(market))
	            return "MICEX_SHR";
	        else if ("FORTS".equals(market))
	            return "FORTS";
	        else throw new UnsupportedOperationException("Market " + market + " is not supported");
	    }
	    
		private static com.dukascopy.api.Period translatePeriod(int p) {
	        if (p == 0) return com.dukascopy.api.Period.ONE_MIN;
	        else if (p == 1) return com.dukascopy.api.Period.FIVE_MINS;
	        else if (p == 2) return com.dukascopy.api.Period.TEN_MINS;
	        else if (p == 3) return com.dukascopy.api.Period.FIFTEEN_MINS;
	        else if (p == 4) return com.dukascopy.api.Period.THIRTY_MINS;
	        else if (p == 5) return com.dukascopy.api.Period.ONE_HOUR;
	        else if (p == 6) return com.dukascopy.api.Period.DAILY;
	        else if (p == 7) return com.dukascopy.api.Period.WEEKLY;
	        else if (p == 8) return com.dukascopy.api.Period.MONTHLY;
	        else throw new UnsupportedOperationException("Period " + p + " is not supported");
	    }	    
	    
	    public static CandleData getCandle(String quote, int period) throws ParseException {
	        StringTokenizer st = new StringTokenizer(quote, DELIM);
	        Date date = DATE_FORMAT.parse(st.nextToken());
	        date = new Date(DataCacheUtils.getCandleStartFast(translatePeriod(period), date.getTime()));
	        Double open = Double.parseDouble(st.nextToken().replace(",", "."));
	        Double high = Double.parseDouble(st.nextToken().replace(",", "."));
	        Double low = Double.parseDouble(st.nextToken().replace(",", "."));
	        Double close = Double.parseDouble(st.nextToken().replace(",", "."));
	        Long vol = Long.parseLong(st.nextToken());
	        return new IntraPeriodCandleData(false, date.getTime(), open, close, low, high, vol);
	    }
	    
	    public static void main(String[] args) {
	    	loadData(new GregorianCalendar(2011,8,1), new GregorianCalendar(2011,8,30), Constants.PLACE_CODE, "SBER3", 0);
	    }
	    
	    public static Data[] loadData(Calendar from, Calendar to, String market, String inst, int period) {
	    	from.add(Calendar.MONTH, -1);
	        logger.info("AD: " + DateFormat.getDateTimeInstance().format(from.getTime()) + "-" + DateFormat.getDateTimeInstance().format(to.getTime()) + " for " + inst + " " + period);
	        String response = broker.getArchiveFinInfo(market, inst, period, from.getTime(), to.getTime(), 3, 50);
	        ArrayList<Data> data = new ArrayList<Data>(); 
	        if (response == null) return
	        		new Data[0];
	            StringTokenizer t = new StringTokenizer(response, "\r\n");
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
	              }
	          }
	            return data.toArray(new Data[]{});
	        }
	}
