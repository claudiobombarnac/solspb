package solspb;

import java.util.concurrent.TimeUnit;

import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;

public class SubmitOrder implements IOrder {

	@Override
	public Instrument getInstrument() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCreationTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCloseTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public OrderCommand getOrderCommand() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLong() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getFillTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAmount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRequestedAmount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getOpenPrice() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getClosePrice() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getStopLossPrice() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTakeProfitPrice() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setStopLossPrice(double price) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStopLossPrice(double price, OfferSide side)
			throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStopLossPrice(double price, OfferSide side,
			double trailingStep) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public OfferSide getStopLossSide() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getTrailingStep() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTakeProfitPrice(double price) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRequestedAmount(double amount) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOpenPrice(double price) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close(double amount, double price, double slippage)
			throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close(double amount, double price) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close(double amount) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setGoodTillTime(long goodTillTime) throws JFException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getGoodTillTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void waitForUpdate(long timeoutMills) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IMessage waitForUpdate(long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getProfitLossInPips() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getProfitLossInUSD() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getProfitLossInAccountCurrency() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCommission() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCommissionInUSD() {
		// TODO Auto-generated method stub
		return 0;
	}
}
