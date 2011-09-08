package solspb.jforex;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import solspb.client.FinamDataLoader;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.charts.data.datacache.Data;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IAuthenticator;
import com.dukascopy.charts.data.datacache.ICurvesProtocolHandler;
import com.dukascopy.charts.data.datacache.LoadingProgressListener;
import com.dukascopy.charts.data.datacache.NotConnectedException;
import com.dukascopy.dds2.greed.gui.component.filechooser.CancelLoadingException;
import com.dukascopy.dds2.greed.gui.component.filechooser.FileProgressListener;
import com.dukascopy.transport.common.datafeed.FileAlreadyExistException;
import com.dukascopy.transport.common.datafeed.FileType;
import com.dukascopy.transport.common.datafeed.KeyNotFoundException;
import com.dukascopy.transport.common.datafeed.StorageException;
import com.dukascopy.transport.common.msg.strategy.FileItem;
import com.dukascopy.transport.common.msg.strategy.FileItem.AccessType;
import com.dukascopy.transport.common.msg.strategy.StrategyParameter;

public class CurvesProtocolHandler implements ICurvesProtocolHandler {

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connect(IAuthenticator arg0, String arg1, String arg2,
			String arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileItem downloadFile(long arg0, LoadingProgressListener arg1)
			throws StorageException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FileItem> getFileList(FileType arg0, AccessType arg1,
			FileProgressListener arg2) throws StorageException,
			CancelLoadingException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public List<StrategyParameter> listStrategyParameters(long arg0,
			LoadingProgressListener arg1) throws StorageException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Data[] loadCandles(String inst, Period period, long from, long to,
			LoadingProgressListener arg4) throws DataCacheException {
        Calendar fc = new GregorianCalendar();
        fc.setTimeInMillis(from);
        Calendar tc = new GregorianCalendar();
        tc.setTimeInMillis(to);

        return FinamDataLoader.loadData(fc, tc, translateMarket(Instrument.fromString(inst).getMarket()), inst, translatePeriod(period));
	}

	private FinamDataLoader.Period translatePeriod(Period p) {
        if (p == Period.TICK) return FinamDataLoader.Period.TICK;
        else if (p == Period.ONE_MIN) return FinamDataLoader.Period.ONE_MIN;
        else if (p == Period.FIVE_MINS) return FinamDataLoader.Period.FIVE_MINS;
        else if (p == Period.TEN_MINS) return FinamDataLoader.Period.TEN_MINS;
        else if (p == Period.FIFTEEN_MINS) return FinamDataLoader.Period.FIFTEEN_MINS;
        else if (p == Period.THIRTY_MINS) return FinamDataLoader.Period.THIRTY_MINS;
        else if (p == Period.ONE_HOUR) return FinamDataLoader.Period.ONE_HOUR;
        else if (p == Period.DAILY) return FinamDataLoader.Period.DAILY;
        else if (p == Period.WEEKLY) return FinamDataLoader.Period.WEEKLY;
        else if (p == Period.MONTHLY) return FinamDataLoader.Period.MONTHLY;
        else throw new UnsupportedOperationException("Period " + p + " is not supported");
    }	    
    private FinamDataLoader.Market translateMarket(String market) {
        if ("FOREX".equals(market))
            return FinamDataLoader.Market.FOREX;
        else if ("MICEX".equals(market))
            return FinamDataLoader.Market.MICEX_STOCKS;
        else if ("FORTS".equals(market))
            return FinamDataLoader.Market.FORTS;
        else throw new UnsupportedOperationException("Market " + market + " is not supported");
    }
	
	@Override
	public Data[] loadData(Instrument inst, Period period, OfferSide offerSide,
			long from, long to, boolean dontKnow, LoadingProgressListener lpListener)
			throws NotConnectedException, DataCacheException {
	    Calendar fc = new GregorianCalendar();
	    fc.setTimeInMillis(from);
        Calendar tc = new GregorianCalendar();
	    tc.setTimeInMillis(to);

		return FinamDataLoader.loadData(fc, tc, translateMarket(inst.getMarket()), inst.name(), translatePeriod(period));
	}

	@Override
	public Data[] loadFile(Instrument arg0, Period arg1, OfferSide arg2,
			long arg3, LoadingProgressListener arg4)
			throws NotConnectedException, DataCacheException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Data[] loadInProgressCandle(Instrument arg0, long arg1,
			LoadingProgressListener arg2) throws NotConnectedException,
			DataCacheException {
	    Calendar fc = new GregorianCalendar();
	    fc.add(Calendar.MONTH, -2);
        Calendar tc = new GregorianCalendar();
		Data[] monthly = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.MONTHLY));

		fc = new GregorianCalendar();
		fc.add(Calendar.WEEK_OF_YEAR, -2);
		Data[] weekly = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.WEEKLY));

		fc = new GregorianCalendar();
		fc.add(Calendar.DAY_OF_YEAR, -2);
		Data[] daily = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.DAILY));

		fc = new GregorianCalendar();
		fc.add(Calendar.HOUR_OF_DAY, -2);
		Data[] onehour = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.ONE_HOUR));

		fc = new GregorianCalendar();
		fc.add(Calendar.HOUR_OF_DAY, -1);
		Data[] thirtymin = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.THIRTY_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -30);
		Data[] fifteenmin = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.FIFTEEN_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -20);
		Data[] tenmin = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.TEN_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -10);
		Data[] fivemin = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.FIVE_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -2);
		Data[] onemin = FinamDataLoader.loadData(fc, tc, translateMarket(arg0.getMarket()), arg0.name(), translatePeriod(Period.ONE_MIN));
		
		return new Data[] {
				monthly.length > 0 ? monthly[0] : null, 
				monthly.length > 1 ? monthly[1] : null, 
				weekly.length > 0 ? weekly[0]: null, 
				weekly.length > 1 ? weekly[1]: null, 
				daily.length > 0 ? daily[0]: null, 
				daily.length > 1 ? daily[1]: null, 
				null, 
				null, 
				onehour.length > 0 ? onehour[0] : null, 
				onehour.length > 1 ? onehour[1] : null, 
				thirtymin.length > 0 ? thirtymin[0] : null, 
				thirtymin.length > 1 ? thirtymin[1] : null, 
				fifteenmin.length > 0 ? fifteenmin[0] : null, 
				fifteenmin.length > 1 ? fifteenmin[1] : null, 
				tenmin.length > 0 ? tenmin[0] : null, 
				tenmin.length > 1 ?	tenmin[1] : null, 
				fivemin.length > 0 ? fivemin[0] : null, 
				fivemin.length > 1 ? fivemin[1] : null, 
				onemin.length > 0 ? onemin[0] : null, 
				onemin.length > 1 ? onemin[1] : null, 
				null, 
				null};
	}

	public static void main(String[] args) {
		try {
			Data[] result = new CurvesProtocolHandler().loadInProgressCandle(Instrument.GAZP, 0, null);
			System.out.println(DateFormat.getDateTimeInstance().format(result[0].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.MONTHLY, result[0].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[2].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.WEEKLY, result[2].getTime()))));
		//	System.out.println(DateFormat.getDateTimeInstance().format(result[4].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.DAILY, result[4].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[6].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.FOUR_HOURS, result[6].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[8].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.ONE_HOUR, result[8].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[10].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.THIRTY_MINS, result[10].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[12].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.FIFTEEN_MINS, result[12].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[14].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.TEN_MINS, result[14].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[16].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.FIVE_MINS, result[16].getTime()))));
			System.out.println(DateFormat.getDateTimeInstance().format(result[18].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.ONE_MIN, result[18].getTime()))));
//			System.out.println(DateFormat.getDateTimeInstance().format(result[20].getTime()) + "X" + DateFormat.getDateTimeInstance().format(new Date(DataCacheUtils.getCandleStartFast(Period.ONE_MIN, result[20].getTime()))));	
			} catch (NotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DataCacheException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public OrdersDataStruct loadOrders(Instrument arg0, long arg1, long arg2,
			LoadingProgressListener arg3) throws NotConnectedException,
			DataCacheException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Long uploadFile(FileItem arg0, String arg1,
			LoadingProgressListener arg2) throws StorageException,
			FileAlreadyExistException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileItem useKey(String arg0, FileType arg1,
			LoadingProgressListener arg2, String arg3) throws StorageException,
			KeyNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
