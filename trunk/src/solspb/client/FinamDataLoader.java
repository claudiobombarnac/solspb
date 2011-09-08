package solspb.client;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class FinamDataLoader {
    public enum Market {NONE, MICEX_STOCKS, MICEX_BONDS, RTS, N4, FOREX, WORLD_IDX, US_FUTURES, ADR, SPFB_ARCH, RTS_GAZ, RTS_GTS,MICEX_UNLIST_BONDS, N13, FORTS, N15, MICEX_ARCH, FORTS_ARCH, RTS_ARCH, N19, RTS_BOARD, N21, N22, N23, GOODS, US_BATS, US_GOVBONDS, US_INDUSTRY, ETF, MICEX_PIF, WORLD_ECO_IDX, N31, N32, N33, N34, N35, N36, N37, RTS_STD}; 
    public enum Period {NONE, TICK, MIN, FIVE_MIN, TEN_MIN, FIFTEEN_MIN, THIRTI_MIN, HOUR, DAY, WEEK, MONTH, HOUR1030 };
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
    
    public static String loadData(Calendar from, Calendar to, Market market, String inst, Period period, Format format) {
        return HTTPRequestPoster.sendGetRequest(FinamDataLoader.SERVER + "/" + getFileName(from, to, inst), getParams(from, to, market, inst, period, format));
    }
}
