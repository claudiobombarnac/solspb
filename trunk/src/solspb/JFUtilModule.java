package solspb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import artist.api.BaseThread;
import artist.api.IDataQueue;
import artist.api.beans.Queue;

public class JFUtilModule {
	public class FuturesArbitrage extends BaseThread {
		private Logger logger = LoggerFactory.getLogger(FuturesArbitrage.class);
		private double coef = 1.01;
		private IDataQueue<Queue> dataQueue;
		private int stockNo;
		private int futuresNo;
		private double priceStock = 0;
		private boolean signal = false;
		private double priceFutures = 0;
		
		public FuturesArbitrage(int stock, int futures, IDataQueue<Queue> dq) {
			this.dataQueue = dq;
			this.stockNo = stock;
			this.futuresNo = futures;
		}

		@Override
		public void tick() throws InterruptedException {
			if (dataQueue.size() == 0)
				synchronized(dataQueue) {
					dataQueue.wait();
				}
			while (dataQueue.size() != 0) {
				Queue q = dataQueue.pop();
		        logger.info("Size" + dataQueue.size() + " price " + q.getPrice());
				if (q.getPaperNo() == stockNo && q.getSellQty() > 0 && q.getPrice() > priceStock) {
					priceStock = q.getPrice();
				}
				else if (q.getPaperNo() == stockNo && q.getBuyQty() > 0 && q.getPrice() < priceStock) {
					priceStock = q.getPrice();
				}
				else if (q.getPaperNo() == futuresNo && q.getSellQty() > 0 && q.getPrice() > priceFutures) {
					priceFutures = q.getPrice();
				}
				else if (q.getPaperNo() == futuresNo && q.getBuyQty() > 0 && q.getPrice() < priceFutures) {
					priceFutures = q.getPrice();
				}
				logger.info("stock {} futures{}", priceStock, priceFutures);
			}
		}
		
		public void tack() {
			setSignal(priceStock < priceFutures * coef);
			synchronized (this) {
				notifyAll();		
			}
		}
		
		public boolean isSignal() {
			return signal;
		}

		private void setSignal(boolean signal) {
			this.signal = signal;
		}

		public double getPriceStock() {
			return priceStock;
		}

		private void setPriceStock(double priceStock) {
			this.priceStock = priceStock;
		}

		public double getPriceFutures() {
			return priceFutures;
		}

		private void setPriceFutures(double priceFutures) {
			this.priceFutures = priceFutures;
		}
	}

}
