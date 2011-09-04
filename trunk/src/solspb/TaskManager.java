// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:06
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   JForexTaskManager.java

package solspb;

import com.dukascopy.api.*;
import com.dukascopy.api.impl.StrategyMessages;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.connect.ISystemListenerExtended;
import com.dukascopy.api.impl.connect.OrdersInternalCollection;
import com.dukascopy.api.impl.connect.PlatformAccountImpl;
import com.dukascopy.api.impl.connect.PlatformMessageImpl;
import com.dukascopy.api.impl.connect.PlatformOrderImpl;
import com.dukascopy.api.impl.connect.StratTickData;
import com.dukascopy.api.impl.connect.StrategiesControl;
import com.dukascopy.api.impl.execution.TaskFlush;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.charts.data.datacache.*;
import com.dukascopy.charts.data.orders.ExposureData;
import com.dukascopy.charts.data.orders.OrdersProvider;
import com.dukascopy.charts.main.interfaces.DDSChartsController;
import com.dukascopy.dds2.greed.util.INotificationUtils;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;
import com.dukascopy.transport.client.TransportClient;
import com.dukascopy.transport.common.model.type.*;
import com.dukascopy.transport.common.msg.group.*;
import com.dukascopy.transport.common.msg.news.*;
import com.dukascopy.transport.common.msg.request.*;
import com.dukascopy.transport.common.msg.response.ErrorResponseMessage;
import com.dukascopy.transport.common.msg.response.NotificationMessage;
import java.math.BigDecimal;
import java.util.*;
import java.util.Currency;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Referenced classes of package com.dukascopy.api.impl.connect:
//            OrdersInternalCollection, PlatformAccountImpl, JForexEngineImpl, PlatformOrderImpl, 
//            PlatformCalendarMessageImpl, PlatformNewsMessageImpl, StratTickData, StrategyProcessor, 
//            InstrumentStatusMessageImpl, ConnectionStatusMessageImpl, ISystemListenerExtended, PlatformMessageImpl, 
//            StrategiesControl, JForexContextImpl

public class TaskManager
    implements LiveCandleListener
{
    class StopCallable
        implements Callable
    {

        public Object call()
            throws Exception
        {
            if(runningStrategy != null)
            {
                IStrategy strategy = runningStrategy.getStrategy();
                try
                {
                    runningStrategy.onStop();
                    fireOnStop(strategy);
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String msg = StrategyWrapper.representError(strategy, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage(msg, t, false);
                }
            }
            return null;
        }
    }

    public static enum Environment {LOCAL_JFOREX, LOCAL_EMBEDDED, REMOTE};

    public TaskManager(Environment environment, boolean live, String accountName, IConsole console, TransportClient transportClient, DDSChartsController ddsChartsController, IUserInterface userInterface, 
            IStrategyExceptionHandler exceptionHandler, AccountInfoMessage lastAccountInfo, String externalIP, String internalIP, String sessionID)
    {
        strategyId = 0x8000000000000000L;
        strategyListeners = new ArrayList();
        requiredInstruments = new HashSet();
        
        {
            history = new History(0);
            this.console = console;
            this.transportClient = transportClient;
            this.ddsChartsController = ddsChartsController;
            this.userInterface = userInterface;
            this.exceptionHandler = exceptionHandler;
            forexEngineImpl = new Engine(this, accountName, live);
            this.environment = environment;
            this.externalIP = externalIP;
            this.internalIP = internalIP;
            this.sessionID = sessionID;
            return;
        }
    }

    public Future executeTask(Callable callable)
    {
        if(runningStrategy != null)
            return runningStrategy.executeTask(callable, false);
        else
            return null;
    }

    public void onNewsMessage(NewsStoryMessage newsStoryMessage)
    {
    }

    public void onBroadcastMessage(String transactionId, IStrategyBroadcastMessage message)
    {
        if(!getUID().equals(transactionId))
            syncMessage(message);
    }

    public void onErrorMessage(ErrorResponseMessage errorResponseMessage, PlatformOrderImpl platformOrderImpl)
    {
        if(runningStrategy != null && !isStrategyStopping())
            runningStrategy.updateOrder(platformOrderImpl, errorResponseMessage);
    }

    public void onNotifyMessage(NotificationMessage notificationMessage)
    {
        if(runningStrategy == null || isStrategyStopping())
        {
            return;
        } else
        {
            runningStrategy.updateOrder(notificationMessage);
            return;
        }
    }

    public void onOrderGroupReceived(OrderGroupMessage orderGroupMessage)
    {
        if(runningStrategy == null || isStrategyStopping())
        {
            return;
        } else
        {
            runningStrategy.updateOrder(orderGroupMessage);
            return;
        }
    }

    public void onOrderReceived(OrderMessage orderMessage)
    {
        if(runningStrategy == null || isStrategyStopping())
        {
            return;
        } else
        {
            runningStrategy.updateOrder(orderMessage);
            return;
        }
    }

    public void onOrdersMergedMessage(MergePositionsMessage mergePositionsMessage)
    {
        if(runningStrategy == null || isStrategyStopping())
        {
            return;
        } else
        {
            runningStrategy.updateOrder(mergePositionsMessage);
            return;
        }
    }

    public void onMessage(PlatformMessageImpl platformMessageImpl)
    {
        if(runningStrategy != null && !isStrategyStopping())
            runningStrategy.onMessage(platformMessageImpl, true);
    }

    private void syncMessage(IMessage message)
    {
        if(runningStrategy != null && !isStrategyStopping())
            runningStrategy.onMessage(message, false);
    }

    public ITick onMarketState(ADMessage market)
    {
        if(runningStrategy == null || isStrategyStopping())
            return null;
        Instrument instrument = Instrument.fromString(market.getInstrument());
        TickData tick = ADFeedDataProvider.getDefaultInstance().getLastTick(instrument);
        if(tick == null)
        {
            LOGGER.warn((new StringBuilder()).append("Got tick for instrument [").append(instrument).append("] that was not processed by FeedDataProvider... Instrument subscription status [").append(ADFeedDataProvider.getDefaultInstance().isSubscribedToInstrument(instrument)).append("] MarketState [").append(market).append("]").toString());
            return null;
        } else
        {
            tick = new StratTickData(tick, market.getTotalLiquidityAsk().divide(ONE_MILLION).doubleValue(), market.getTotalLiquidityBid().divide(ONE_MILLION).doubleValue());
            runningStrategy.onMarket(instrument, tick);
            return tick;
        }
    }

    public void waitForUpdate(PlatformOrderImpl platformOrderImpl, long timeout, TimeUnit unit)
        throws InterruptedException
    {
        if(runningStrategy != null && !isStrategyStopping())
            runningStrategy.waitForUpdate(platformOrderImpl, timeout, unit);
    }

    public boolean flushQueue(long timeout)
    {
        System.err.println("FlushQueue is not implemented");
        return true;
    }

    public long startStrategy(IStrategy strategy, final IStrategyListener listener, final String strategyKey, boolean fullAccessGranted)
    {
        long rc = 0L;
        final StrategyProcessor strategyProcessor = new StrategyProcessor(this, strategy, fullAccessGranted);
        Future future = strategyProcessor.executeTask(new Callable() {

            public Long call()
                throws Exception
            {
                Context forexContextImpl = new Context(strategyProcessor, forexEngineImpl, new History(0), console, ddsChartsController, userInterface);
                strategyId = strategyProcessor.getStrategyId();
                runningStrategy = strategyProcessor;
                LOGGER.info("Strategy is started: " + runningStrategy.getStrategy());
                requiredInstruments = new HashSet(0);
                try
                {
                    strategyId = strategyProcessor.onStart(forexContextImpl);
                }
                catch(Throwable t)
                {
                    strategyId = 0x8000000000000000L;
                    LOGGER.error(t.getMessage(), t);
                }
                if(strategyId > 0L && !forexContextImpl.isStopped())
                {
                    if(listener != null)
                        strategyListeners.add(listener);
                    fireOnStart();
                } else
                {
                    stopStrategy();
                }
                return Long.valueOf(strategyId);
            }
        }, false);
        try
        {
            rc = ((Long)future.get()).longValue();
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
        }
        return rc;
    }

    public void stopStrategy()
    {
        if(!strategyStopping.compareAndSet(false, true))
            return;
        if(runningStrategy != null)
        {
            LOGGER.info("Stopping strategy: " + runningStrategy.getStrategy());
            StopCallable cc = new StopCallable();
            try
            {
                IStrategy strategy = runningStrategy.getStrategy();
                runningStrategy.executeStop(cc);
                final IStrategy s = strategy;
                Thread haltThread = new Thread() {

                    public void run()
                    {
                        try
                        {
                            Thread.sleep(15000L);
                        }
                        catch(InterruptedException e)
                        {
                            LOGGER.error(e.getMessage(), e);
                        }
                        runningStrategy.halt();
                        long sId = strategyId;
                        if(sId != 0x8000000000000000L)
                            fireOnStop(s);
                    }

                };
                haltThread.start();
            }
            catch(Exception e)
            {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void fireOnStart()
    {
        ISystemListenerExtended systemListener = this.systemListener;
        if(systemListener != null)
            systemListener.onStart(strategyId);
        IStrategyListener listeners[] = (IStrategyListener[])strategyListeners.toArray(new IStrategyListener[strategyListeners.size()]);
        IStrategyListener arr$[] = listeners;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            IStrategyListener listener = arr$[i$];
            listener.onStart(strategyId);
        }

    }

    private void fireOnStop(IStrategy strategy)
    {
        ISystemListenerExtended systemListener = this.systemListener;
        if(systemListener != null)
            systemListener.onStop(strategyId);
        IStrategyListener listeners[] = (IStrategyListener[])strategyListeners.toArray(new IStrategyListener[strategyListeners.size()]);
        IStrategyListener arr$[] = listeners;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            IStrategyListener listener = arr$[i$];
            listener.onStop(strategyId);
        }

        strategyId = 0x8000000000000000L;
        LOGGER.info("Strategy is stopped: " + strategy);
    }

    public boolean isStrategyStopping()
    {
        return strategyStopping.get();
    }

    public boolean isThreadOk(long id)
    {
        return strategyId == id;
    }

    public void newCandle(Instrument instrument, Period period, CandleData askCandle, CandleData bidCandle)
    {
        CandleData askBar = new CandleData(askCandle.time, askCandle.open, askCandle.close, askCandle.low, askCandle.high, askCandle.vol);
        CandleData bidBar = new CandleData(bidCandle.time, bidCandle.open, bidCandle.close, bidCandle.low, bidCandle.high, bidCandle.vol);
        if(runningStrategy != null && !isStrategyStopping())
            runningStrategy.onBar(instrument, period, askBar, bidBar);
    }

    public boolean isConnected()
    {
        return connected == null || connected.booleanValue();
    }

    public void setSystemListener(ISystemListenerExtended systemListener)
    {
        this.systemListener = systemListener;
    }

    public TransportClient getTransportClient()
    {
        return transportClient;
    }

    public IStrategyExceptionHandler getExceptionHandler()
    {
        return exceptionHandler;
    }

    public long getStrategyId()
    {
        return strategyId;
    }

    public String getStrategyKey()
    {
        return strategyKey;
    }

    public String getUID()
    {
        return String.format("%1$s:%2$s:%3$s", new Object[] {
            Integer.valueOf(hashCode()), sessionID, strategyKey
        });
    }

    public void setSubscribedInstruments(Set requiredInstruments)
    {
        if(requiredInstruments == null)
            requiredInstruments = new HashSet(0);
        this.requiredInstruments = requiredInstruments;
        ISystemListenerExtended systemListener = this.systemListener;
        if(systemListener != null)
            systemListener.subscribeToInstruments(requiredInstruments);
    }

    public Set getSubscribedInstruments()
    {
        ISystemListenerExtended systemListener = this.systemListener;
        if(systemListener != null)
            return systemListener.getSubscribedInstruments();
        else
            return new HashSet(0);
    }

    public Set getRequiredInstruments()
    {
        if(runningStrategy != null && !isStrategyStopping())
            return requiredInstruments;
        else
            return new HashSet(0);
    }

    public String getStrategyName()
    {
        return runningStrategy.getStrategy().getClass().getSimpleName();
    }

    public Environment getEnvironment()
    {
        return environment;
    }

    public String getSessionID()
    {
        return sessionID;
    }

    public String getInternalIP()
    {
        return internalIP;
    }

    public String getExternalIP()
    {
        return externalIP;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);
    private static final String UID_FORMAT = "%1$s:%2$s:%3$s";
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(0xf4240L);
    private long strategyId;
    private volatile StrategyProcessor runningStrategy;
    private String strategyKey;
    private final AtomicBoolean strategyStopping = new AtomicBoolean(false);
    private final Engine forexEngineImpl;
    private final History history;
    private final IConsole console;
    private final DDSChartsController ddsChartsController;
    private final IUserInterface userInterface;
    private volatile ISystemListenerExtended systemListener;
    private List strategyListeners;
    private final TransportClient transportClient;
    private final IStrategyExceptionHandler exceptionHandler;
    private Set requiredInstruments;
    private final Environment environment;
    private Boolean connected;
    private final String externalIP;
    private final String internalIP;
    private final String sessionID;

}