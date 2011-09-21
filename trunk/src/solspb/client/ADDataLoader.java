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
import java.util.List;
	import java.util.StringTokenizer;
import java.util.TimeZone;

	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;

import artist.api.BrokerInt;
import artist.api.Constants;
import artist.api.ContextLoader;

import com.dukascopy.api.Instrument;
	import com.dukascopy.charts.data.datacache.CandleData;
	import com.dukascopy.charts.data.datacache.Data;
import com.dukascopy.charts.data.datacache.DataCacheException;
	import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;
import com.sun.org.apache.bcel.internal.generic.NEW;

	public class ADDataLoader {
	    private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	    private static final String DELIM = "|";
	    
	    private static Logger logger = LoggerFactory.getLogger(ADDataLoader.class);
	    private static BrokerInt broker = ContextLoader.getInstance().getBroker();
        private static int localOffset = broker.sessionTime().getHours() - new GregorianCalendar().get(Calendar.HOUR_OF_DAY);
//        private static int gmtOffset = broker.sessionTime().getHours() - new GregorianCalendar(TimeZone.getTimeZone("GMT")).get(Calendar.HOUR_OF_DAY);
	    
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
	    
	    public static CandleData getCandle(String quote, int period, boolean applyTimezoneOffset) throws ParseException {
	        StringTokenizer st = new StringTokenizer(quote, DELIM);
	        Date date;
	        String dateStr = st.nextToken();
	        
            Date d;
	        if (period < 6) {
	            d = DATETIME_FORMAT.parse(dateStr);
	        }
	        else {
                d = DATE_FORMAT.parse(dateStr);
	        }
	        long offset;
	        if (applyTimezoneOffset)
	            offset = periodOffset(period) + localOffset;
	        else
	            offset = periodOffset(period);
            date = new Date(DataCacheUtils.getCandleStartFast(translatePeriod(period), d.getTime() - offset));

	        Double open = Double.parseDouble(st.nextToken().replace(",", "."));
	        Double high = Double.parseDouble(st.nextToken().replace(",", "."));
	        Double low = Double.parseDouble(st.nextToken().replace(",", "."));
	        Double close = Double.parseDouble(st.nextToken().replace(",", "."));
	        Long vol = Long.parseLong(st.nextToken());
            System.out.println(SimpleDateFormat.getInstance().format(date) + " " + open + " " + close  + " " + low  + " " + high  + " " + vol);	        
	        return new IntraPeriodCandleData(false, date.getTime(), open, close, low, high, vol);
	    }
	    
	    private static long periodOffset(int period) {
            int periodOffset = 0;
            if (period == 0) periodOffset = 60 * 1000;
            else if (period == 1) periodOffset = 300 * 1000;
            else if (period == 2) periodOffset = 600 * 1000;
            else if (period == 3) periodOffset = 900 * 1000;
            else if (period == 4) periodOffset = 1800 * 1000;
            else if (period == 5) periodOffset = 3600 * 1000;
            else if (period == 6) periodOffset = 3600 * 24 * 1000;
            else if (period == 6) periodOffset = 3600 * 24 * 7 * 1000;
            return periodOffset;
        }
	    
        public static void main(String[] args) {
	    	loadData(new GregorianCalendar(2011,8,17, 8, 00), new GregorianCalendar(2011,8,18, 8,00), Constants.PLACE_CODE, "GAZP", 0, false);
            logger.info(broker.lastResultMessage());
	    }
	    
        public static Data[] loadData(Calendar from, Calendar to, String market, String inst, int period) {
            return loadData(from, to, market, inst, period, true);
        }
        public static Data[] loadData(Calendar from, Calendar to, String market, String inst, int period, boolean applyTimezoneOffset) {
	        Calendar fc = (Calendar)from.clone();
	        Calendar tc = (Calendar)to.clone();
	        if (applyTimezoneOffset) {
    	        fc.add(Calendar.HOUR_OF_DAY, localOffset);
    	        tc.add(Calendar.HOUR_OF_DAY, localOffset);
	        }
	        logger.info("AD: " + DateFormat.getDateTimeInstance().format(from.getTime()) + " - " + DateFormat.getDateTimeInstance().format(to.getTime()) + " for " + inst + " " + period);
	        String response = broker.getArchiveFinInfo(market, Instrument.fromString(inst).toString(), period, fc.getTime(), tc.getTime(), 3, 50);
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
	                  CandleData q = getCandle(quote, period, applyTimezoneOffset);
	                  data.add(q);
	              }
	              catch (Exception e) {
	                  logger.error("Failed to parse quote: {}, period: {}", quote, period);
	              }
	          }
	          return fillMissingData(from, to, market, inst, period, data);
	        }
        private static Data[] fillMissingData(Calendar from, Calendar to,
                String market, String inst, int period, ArrayList<Data> data) {
            try {
                if (data != null && data.size() != 0)
                    return data.toArray(new Data[]{});
                else {
                    int nPeriods = DataCacheUtils.getCandlesCountBetween(translatePeriod(period), from.getTimeInMillis(), to.getTimeInMillis());
                    ArrayList<Data> result = new ArrayList<Data>();
                    Data lastMeaningCandle = getLastMeaningCandle(from, market, inst, period);
                    for (int i = 0; i < nPeriods; i++) {
                        result.add(lastMeaningCandle);
                    }
                    return result.toArray(new Data[]{});
                }
            }
            catch (DataCacheException e) {
                e.printStackTrace();
                return null;
            }
        }
        private static Data getLastMeaningCandleForts(Calendar from, String inst, int period) {
            if (from.get(Calendar.HOUR_OF_DAY) == 23 && from.get(Calendar.MINUTE) > 45) {
                from.set(Calendar.HOUR_OF_DAY, 23);
                from.set(Calendar.MINUTE, 45);
            }
            else if (from.get(Calendar.HOUR_OF_DAY) < 10) {
                from.set(Calendar.HOUR_OF_DAY, 10);
                from.set(Calendar.MINUTE, 0);
            }
            if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    from.add(Calendar.DAY_OF_MONTH, -1);
                    from.set(Calendar.HOUR_OF_DAY, 23);
                    from.set(Calendar.MINUTE, 45);                    
            }
            else if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    from.add(Calendar.DAY_OF_MONTH, -2);
                    from.set(Calendar.HOUR_OF_DAY, 23);
                    from.set(Calendar.MINUTE, 45);                    
            }
            Calendar to = new GregorianCalendar();
            if (from.after(to)) //in the future
                from.setTimeInMillis(from.getTimeInMillis() - periodOffset(period));
            else
                to.setTimeInMillis(from.getTimeInMillis() + periodOffset(period));
            Data[] result = loadData(from, to, "FORTS", inst, period, false);
            if (result == null || result.length != 1) {
                System.err.println("Can't calculate last meaning candle " + from + inst + period);
                return null;
            }
            else
                return result[0];
        }

        private static Data getLastMeaningCandleMicex(Calendar from, String inst, int period) {
            if (from.get(Calendar.HOUR_OF_DAY) == 18 && from.get(Calendar.MINUTE) > 45 || from.get(Calendar.HOUR_OF_DAY) > 18) {
                from.set(Calendar.HOUR_OF_DAY, 18);
                from.set(Calendar.MINUTE, 45);
            }
            else if (from.get(Calendar.HOUR_OF_DAY) < 10 || from.get(Calendar.HOUR_OF_DAY) == 10 && from.get(Calendar.MINUTE) < 30) {
                from.set(Calendar.HOUR_OF_DAY, 10);
                from.set(Calendar.MINUTE, 30);
            }
            if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    from.add(Calendar.DAY_OF_MONTH, -1);
                    from.set(Calendar.HOUR_OF_DAY, 18);
                    from.set(Calendar.MINUTE, 45);                    
            }
            else if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    from.add(Calendar.DAY_OF_MONTH, -2);
                    from.set(Calendar.HOUR_OF_DAY, 18);
                    from.set(Calendar.MINUTE, 45);                    
            }
            Calendar to = new GregorianCalendar();
            if (from.after(to)) //in the future
                from.setTimeInMillis(from.getTimeInMillis() - periodOffset(period));
            else
                to.setTimeInMillis(from.getTimeInMillis() + periodOffset(period));
            Data[] result = loadData(from, to, "MICEX_SHR", inst, period, false);
            if (result == null) {
                System.err.println("Can't calculate last meaning candle " + from + inst + period);
                return null;
            }
            else
                return result[0];
        }

        
        private static Data getLastMeaningCandle(Calendar from, String market, String inst, int period) {
                if ("FORTS".equals(market)) {
                    return getLastMeaningCandleForts(from, inst, period);
                }
                else if ("MICEX_SHR".equals(market)) {
                    return getLastMeaningCandleMicex(from, inst, period);
                }
                else
                    throw new RuntimeException("Can't process market: " + market);
            }
        }