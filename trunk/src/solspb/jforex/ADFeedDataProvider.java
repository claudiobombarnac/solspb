package solspb.jforex;

import com.dukascopy.api.Instrument;
import com.dukascopy.charts.data.datacache.TickData;

public class ADFeedDataProvider {
	private static ADFeedDataProvider instance = new ADFeedDataProvider();
	public static ADFeedDataProvider getDefaultInstance() {
		return instance;
	}
	public TickData getLastTick(Instrument instrument) {
		return new TickData(System.currentTimeMillis(), Math.random() * 100, Math.random() * 100, Math.random() * 100, Math.random() * 100);
	}
	public boolean isSubscribedToInstrument(Instrument instrument) {
		return true;
	}};