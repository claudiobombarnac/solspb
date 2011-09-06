package solspb.jforex;

import com.dukascopy.api.Instrument;
import com.dukascopy.charts.data.datacache.TickData;
import artist.api.ContextLoader;
import artist.api.Data;
import artist.api.IContext;
import artist.api.IDataQueue;
import artist.api.beans.Quote;

public class ADFeedDataProvider {
	private static ADFeedDataProvider instance = new ADFeedDataProvider();
	private static IContext context = ContextLoader.getInstance();
	public static ADFeedDataProvider getDefaultInstance() {
		return instance;
	}
	public TickData getLastTick(Instrument instrument) {
		IDataQueue q = context.getQueue(Quote.class);
		if (q.size() != 0) {
			Quote quote = (Quote)q.pop();
			return new TickData(System.currentTimeMillis(), quote.getHi(), quote.getLow(), quote.getVol(), quote.getVol());
		}
		else
			return null;
	}
	public boolean isSubscribedToInstrument(Instrument instrument) {
		return true;
	}};