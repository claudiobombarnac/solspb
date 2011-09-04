package solspb.jforex;

import com.dukascopy.api.Instrument;
import com.dukascopy.charts.data.datacache.TickData;

public class ADFeedDataProvider {
	private static ADFeedDataProvider instance = new ADFeedDataProvider();
	public static ADFeedDataProvider getDefaultInstance() {
		return instance;
	}
	public TickData getLastTick(Instrument instrument) {
		return new TickData(System.currentTimeMillis(), 100, 100, 100, 100);
	}
	public boolean isSubscribedToInstrument(Instrument instrument) {
		return true;
	}};