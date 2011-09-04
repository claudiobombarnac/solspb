package solspb.jforex;

import com.dukascopy.transport.common.msg.ProtocolMessage;
import com.dukascopy.transport.common.msg.request.CurrencyMarket;

public class ADCurrencyMarket extends CurrencyMarket implements ADMessage {

	public ADCurrencyMarket(ProtocolMessage message) {
		super(message);
	}

}
