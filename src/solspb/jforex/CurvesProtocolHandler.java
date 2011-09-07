package solspb.jforex;

import java.util.List;

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

	@Override
	public Data[] loadData(Instrument arg0, Period arg1, OfferSide arg2,
			long arg3, long arg4, boolean arg5, LoadingProgressListener arg6)
			throws NotConnectedException, DataCacheException {
		// TODO Auto-generated method stub
		return null;
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
