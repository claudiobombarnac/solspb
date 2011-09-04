package solspb;

import java.math.BigDecimal;

public class ADStockMarket implements ADMessage {

	private String instrument;
	private BigDecimal ask;
	private BigDecimal bid;

	public ADStockMarket(String instrument, BigDecimal ask, BigDecimal bid) {
		this.instrument = instrument;
		this.ask = ask;
		this.bid = bid;
	}
	@Override
	public String getInstrument() {
		return instrument;
	}

	@Override
	public BigDecimal getTotalLiquidityAsk() {
	return ask;
	}

	@Override
	public BigDecimal getTotalLiquidityBid() {
		return bid;
	}

}
