package jforex;
import java.util.*;
import com.dukascopy.api.*;

public class May implements IStrategy {
	private IEngine engine;
	private IConsole console;
	private IHistory history;
	private IContext context;
	private IIndicators indicators;
	private IUserInterface userInterface;
    private IAccount account;
	
    @Configurable("SL")          public int    SL = 40;
    @Configurable("TP")          public int    TP = 40;    
    @Configurable("Target")      public double target = 150000;
    @Configurable("ShortPeriod") public int ShortPeriod = 15;
    @Configurable("LongPeriod")  public int LongPeriod = 150;
    @Configurable("prc")         public double prc = 99.0;

    public Instrument instrument = Instrument.EURUSD;
    public Period period = Period.ONE_MIN;
    public double SLIPPAGE = 10;
    public int counter = 0;
            
	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole();
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		this.userInterface = context.getUserInterface();               
	}

	public void onAccount(IAccount account) throws JFException {
        this.account = account;
	}

	public void onMessage(IMessage message) throws JFException {
	}

    public void onStop() throws JFException {    
        for (IOrder order : engine.getOrders())   
            order.close();                
    }
    
	public void onTick(Instrument instrument, ITick tick) throws JFException 	{       
	}
	
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException 
    {
        if (instrument != this.instrument) return;
        if (period != this.period) return;
           
        double PIP = instrument.getPipValue();        
        int pos=0;
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED) {
                if (order.isLong())  pos=1;                                 
                else                 pos=-1;                                                                                  
                if (SL>0 && order.getStopLossPrice()==0) order.setStopLossPrice(order.getOpenPrice() - pos*PIP*SL);                
                if (TP>0 && order.getTakeProfitPrice()==0) order.setTakeProfitPrice(order.getOpenPrice() + pos*PIP*TP);
            }    
        }                
                     
        if (pos==0) {
           double eq = this.account.getEquity();                      
           if  (eq>target) return;           
           double ma_long  = indicators.ma(instrument,period,OfferSide.BID,IIndicators.AppliedPrice.CLOSE,LongPeriod,IIndicators.MaType.SMA,0);
           double ma_short = indicators.ma(instrument,period,OfferSide.BID,IIndicators.AppliedPrice.CLOSE,ShortPeriod,IIndicators.MaType.SMA,0);           
           double Lot = eq/bidBar.getOpen()/10000*prc/100;
           Date datetime = new Date(askBar.getTime());
           int day  = datetime.getDate();
           if (ma_short>ma_long)  engine.submitOrder("B"+(day*100+counter++), instrument, IEngine.OrderCommand.BUY, Lot, 0, SLIPPAGE);              
           else                   engine.submitOrder("S"+(day*100+counter++), instrument, IEngine.OrderCommand.SELL, Lot, 0, SLIPPAGE);              
        }

    }
    
}