package solspb.jforex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import solspb.client.ADDataLoader;
import solspb.client.FinamDataLoader;

import SevenZip.Compression.LZMA.Decoder;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.charts.data.datacache.CurvesProtocolUtil;
import com.dukascopy.charts.data.datacache.Data;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IAuthenticator;
import com.dukascopy.charts.data.datacache.ICurvesProtocolHandler;
import com.dukascopy.charts.data.datacache.LoadingProgressListener;
import com.dukascopy.charts.data.datacache.NotConnectedException;
import com.dukascopy.dds2.greed.agent.strategy.StratUtils;
import com.dukascopy.dds2.greed.gui.component.filechooser.CancelLoadingException;
import com.dukascopy.dds2.greed.gui.component.filechooser.FileProgressListener;
import com.dukascopy.dds2.greed.util.FilePathManager;
import com.dukascopy.transport.common.datafeed.FileAlreadyExistException;
import com.dukascopy.transport.common.datafeed.FileType;
import com.dukascopy.transport.common.datafeed.KeyNotFoundException;
import com.dukascopy.transport.common.datafeed.StorageException;
import com.dukascopy.transport.common.msg.strategy.FileItem;
import com.dukascopy.transport.common.msg.strategy.FileItem.AccessType;
import com.dukascopy.transport.common.msg.strategy.StrategyParameter;

public class CurvesProtocolHandler implements ICurvesProtocolHandler {
	private Logger LOGGER = LoggerFactory.getLogger(CurvesProtocolHandler.class);

	private String historyServerUrl = "file:///C:\\Users\\Sony\\Local Settings\\JForex\\.cache\\";

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

        return ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(Instrument.fromString(inst).getMarket()), inst, ADDataLoader.translatePeriod(period));
	}

	@Override
	public Data[] loadData(Instrument inst, Period period, OfferSide offerSide,
			long from, long to, boolean dontKnow, LoadingProgressListener lpListener)
			throws NotConnectedException, DataCacheException {
	    Calendar fc = new GregorianCalendar();
	    fc.setTimeInMillis(from);
        Calendar tc = new GregorianCalendar();
	    tc.setTimeInMillis(to);

		return ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(inst.getMarket()), inst.name(), ADDataLoader.translatePeriod(period));
	}

	@Override
	public Data[] loadFile(Instrument instrument, Period period, OfferSide side,
			long chunkStart, LoadingProgressListener loadingProgress)
			throws NotConnectedException, DataCacheException {
	    {
	        long firstChunkCandle;
	        String fileName;
	        firstChunkCandle = DataCacheUtils.getFirstCandleInChunkFast(period, chunkStart);
	        if(DataCacheUtils.getChunkEnd(period, firstChunkCandle) < FeedDataProvider.getDefaultInstance().getTimeOfFirstCandle(instrument, period))
	            return new Data[0];
	        fileName = DataCacheUtils.getChunkFile(instrument, period, side, firstChunkCandle, 5).getPath();
	        Data dataArray[];
	        byte bytesData[] = new byte[0];
//	        try {
//		        ByteArrayOutputStream os = new ByteArrayOutputStream();
//		        URL fileUrl = new URL((new StringBuilder()).append(historyServerUrl).append(fileName.substring(FilePathManager.getInstance().getCacheDirectory().length()).replace('\\', '/')).toString());
//				StratUtils.returnURL(fileUrl, os);
//		        bytesData = os.toByteArray();
//		        os.close();
		        bytesData = StratUtils.readFile(fileName);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	        if(bytesData.length != 0)
	        {
	            ByteArrayInputStream is = new ByteArrayInputStream(bytesData);
	            int propertiesSize = 5;
	            byte properties[] = new byte[propertiesSize];
	            int readBytes;
	            int i;
	            for(readBytes = 0; (i = is.read(properties, readBytes, properties.length - readBytes)) > -1 && readBytes < properties.length; readBytes += i);
	            if(readBytes != propertiesSize)
	                throw new DataCacheException("7ZIP: input .lzma file is too short");
	            Decoder decoder = new Decoder();
	            if(!decoder.SetDecoderProperties(properties))
	                throw new DataCacheException("7ZIP: Incorrect stream properties");
	            long outSize = 0L;
	            for(i = 0; i < 8; i++)
	            {
	                int v = is.read();
	                if(v < 0)
	                    throw new DataCacheException("7ZIP: Can't read stream size");
	                outSize |= (long)v << 8 * i;
	            }

	            ByteArrayOutputStream bos = new ByteArrayOutputStream((int)outSize);
	            try {
					if(!decoder.Code(is, bos, outSize))
					    throw new DataCacheException((new StringBuilder()).append("Cannot decode 7zip compressed file [").append(fileName).append("]").toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            bytesData = bos.toByteArray();
	        }
	        dataArray = CurvesProtocolUtil.bytesToChunkData(bytesData, period, 5, firstChunkCandle, instrument.getPipValue());
	        if(LOGGER.isTraceEnabled())
	        {
	            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
	            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	            String logMessage = (new StringBuilder()).append("Data file downloaded, size [").append(dataArray.length).append("]").toString();
	            if(dataArray.length > 0)
	                logMessage = (new StringBuilder()).append(logMessage).append(", first data time [").append(dateFormat.format(new Date(dataArray[0].time))).append("], last data time [").append(dateFormat.format(new Date(dataArray[dataArray.length - 1].time))).append("]").toString();
	            LOGGER.trace(logMessage);
	        }
	        if(dataArray.length > 0)
	        {
	            long lastTime = dataArray[0].time;
	            for(int i = 1; i < dataArray.length; i++)
	                if(dataArray[i].time < lastTime)
	                    throw new DataCacheException("Data consistency error, not sorted");

	        }
	        return dataArray;
	    }
	}

	@Override
	public Data[] loadInProgressCandle(Instrument arg0, long arg1,
			LoadingProgressListener arg2) throws NotConnectedException,
			DataCacheException {
	    Calendar fc = new GregorianCalendar();
	    fc.add(Calendar.MONTH, -2);
        Calendar tc = new GregorianCalendar();
		Data[] monthly = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.MONTHLY));

		fc = new GregorianCalendar();
		fc.add(Calendar.WEEK_OF_YEAR, -2);
		Data[] weekly = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.WEEKLY));

		fc = new GregorianCalendar();
		fc.add(Calendar.DAY_OF_YEAR, -2);
		Data[] daily = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.DAILY));

		fc = new GregorianCalendar();
		fc.add(Calendar.HOUR_OF_DAY, -2);
		Data[] onehour = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.ONE_HOUR));

		fc = new GregorianCalendar();
		fc.add(Calendar.HOUR_OF_DAY, -1);
		Data[] thirtymin = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.THIRTY_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -30);
		Data[] fifteenmin = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.FIFTEEN_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -20);
		Data[] tenmin = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.TEN_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -10);
		Data[] fivemin = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.FIVE_MINS));

		fc = new GregorianCalendar();
		fc.add(Calendar.MINUTE, -2);
		Data[] onemin = ADDataLoader.loadData(fc, tc, ADDataLoader.translateMarket(arg0.getMarket()), arg0.name(), ADDataLoader.translatePeriod(Period.ONE_MIN));
		
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
				new CurvesProtocolHandler().loadFile(Instrument.GAZP, Period.TICK, OfferSide.ASK, 0, null);
			
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
		System.out.println("UploadFile");
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
