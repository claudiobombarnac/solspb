package solspb.jforex;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;

public interface IStrategy {

    /**
     * Called on strategy start
     * 
     * @param context allows access to all system functionality
     * @throws JFException when strategy author ignores exceptions 
     */
    void onStart(IContext context) throws JFException;

    /**
     * Called on every tick of every instrument that application is subscribed on
     * 
     * @param instrument instrument of the tick
     * @param tick tick data
     */
    void onTick(Instrument instrument, ITick tick) throws JFException;

    /**
     * Called on every bar for every basic period and instrument that application is subscribed on
     * 
     * @param instrument instrument of the bar
     * @param period period of the bar
     * @param askBar bar created of ask side of the ticks
     * @param bidBar bar created of bid side of the ticks
     */
    void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException;

    /**
     * Called when new message is received
     * 
     * @param message message
     */
    void onMessage(IMessage message) throws JFException;

    /**
     * Called when account information update is received
     * 
     * @param account updated account information
     */
    void onAccount(IAccount account) throws JFException;
    
    /**
     * Called before strategy is stopped
     */
    void onStop() throws JFException;
}
