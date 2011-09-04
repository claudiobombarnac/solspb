package solspb;

import java.math.BigDecimal;

public interface ADMessage {
	public String getInstrument();
	public BigDecimal getTotalLiquidityAsk();
	public BigDecimal getTotalLiquidityBid();
}
