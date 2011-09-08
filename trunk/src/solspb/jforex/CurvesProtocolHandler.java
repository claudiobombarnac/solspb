package solspb.jforex;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import solspb.client.FinamDataLoader;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.charts.data.datacache.Data;
import com.dukascopy.charts.data.datacache.DataCacheException;
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
		return null;
	}

	@Override
	public List<FileItem> getFileList(FileType arg0, AccessType arg1,
			FileProgressListener arg2) throws StorageException,
			CancelLoadingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<StrategyParameter> listStrategyParameters(long arg0,
			LoadingProgressListener arg1) throws StorageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Data[] loadCandles(String arg0, Period arg1, long arg2, long arg3,
			LoadingProgressListener arg4) throws DataCacheException {
		// TODO Auto-generated method stub
		return null;
	}

	private FinamDataLoader.Period translatePeriod(Period p) {
        if (p == Period.TICK) return FinamDataLoader.Period.TICK;
        else if (p == Period.ONE_MIN) return FinamDataLoader.Period.MIN;
        else if (p == Period.FIVE_MINS) return FinamDataLoader.Period.FIVE_MIN;
        else if (p == Period.TEN_MINS) return FinamDataLoader.Period.TEN_MIN;
        else if (p == Period.FIFTEEN_MINS) return FinamDataLoader.Period.FIFTEEN_MIN;
        else if (p == Period.THIRTY_MINS) return FinamDataLoader.Period.THIRTY_MIN;
        else if (p == Period.ONE_HOUR) return FinamDataLoader.Period.HOUR;
        else if (p == Period.DAILY) return FinamDataLoader.Period.DAY;
        else if (p == Period.WEEKLY) return FinamDataLoader.Period.WEEK;
        else if (p == Period.MONTHLY) return FinamDataLoader.Period.MONTH;
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
		return null;
	}

	@Override
	public Data[] loadInProgressCandle(Instrument arg0, long arg1,
			LoadingProgressListener arg2) throws NotConnectedException,
			DataCacheException {
		return null;
	}

	@Override
	public OrdersDataStruct loadOrders(Instrument arg0, long arg1, long arg2,
			LoadingProgressListener arg3) throws NotConnectedException,
			DataCacheException {
		// TODO Auto-generated method stub
		return null;
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
