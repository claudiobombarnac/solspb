package solspb.jforex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.dukascopy.transport.common.msg.ProtocolMessage;
import com.dukascopy.transport.common.msg.request.CurrencyMarket;
import com.dukascopy.transport.common.msg.request.CurrencyOffer;

public class ADCurrencyMarket implements ADMessage {
	private String instrument;
	List<CurrencyOffer> bid;
	List<CurrencyOffer> ask;
	private AtomicInteger timestamp = new AtomicInteger((int)System.currentTimeMillis());
	
	public ADCurrencyMarket(final String instrument, final double bid, final double ask) {
		this.instrument = instrument;
		this.bid = new ArrayList<CurrencyOffer>() {{add(new CurrencyOffer(String.valueOf(bid), "999999", instrument, "BID"));}};
		this.ask = new ArrayList<CurrencyOffer>() {{add(new CurrencyOffer(String.valueOf(ask), "999999", instrument, "ASK"));}};
	}
	@Override
	public String getInstrument() {
		return instrument;
	}
	@Override
	public BigDecimal getTotalLiquidityAsk() {
		return BigDecimal.valueOf(999999);
	}
	@Override
	public BigDecimal getTotalLiquidityBid() {
		return BigDecimal.valueOf(999999);
	}
	public AtomicInteger getCreationTimestamp() {
		return timestamp;
	}
	public boolean isBackup() {
		return true;
	}
	public CurrencyOffer getBestOffer(
			com.dukascopy.transport.common.model.type.OfferSide side) {
		if (com.dukascopy.transport.common.model.type.OfferSide.ASK == side)
			return ask.get(0);
		else
			return bid.get(0);
	}
	public List<CurrencyOffer> getAsks() {
		return ask;
	}
	public List<CurrencyOffer> getBids() {
		return bid;
	}

}
