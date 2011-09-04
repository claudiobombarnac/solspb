// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:07
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TesterClientImpl.java

package solspb.jforex;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.DataType;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.INewsFilter;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.impl.connect.AuthorizationClient;
import com.dukascopy.api.impl.connect.ITesterGuiImpl;
import com.dukascopy.api.impl.connect.PrintStreamNotificationUtils;
import com.dukascopy.api.impl.connect.TesterChartControllerImpl;
import com.dukascopy.api.system.Commissions;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.JFAuthenticationException;
import com.dukascopy.api.system.JFVersionException;
import com.dukascopy.api.system.Overnights;
import com.dukascopy.api.system.tester.ITesterExecution;
import com.dukascopy.api.system.tester.ITesterExecutionControl;
import com.dukascopy.api.system.tester.ITesterUserInterface;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.FeedDataProvider;
import com.dukascopy.charts.data.datacache.IFeedDataProvider;
import com.dukascopy.charts.data.datacache.JForexPeriod;
import com.dukascopy.charts.main.interfaces.DDSChartsController;
import com.dukascopy.charts.persistence.ChartBean;
import com.dukascopy.dds2.greed.agent.compiler.JFXPack;
import com.dukascopy.dds2.greed.agent.strategy.tester.ExecutionControl;
import com.dukascopy.dds2.greed.agent.strategy.tester.IStrategyRunner;
import com.dukascopy.dds2.greed.agent.strategy.tester.StrategyReport;
import com.dukascopy.dds2.greed.agent.strategy.tester.StrategyRunner;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterAccount;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterChartData;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterDataLoader;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterFeedDataProvider;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterOrdersProvider;
import com.dukascopy.dds2.greed.agent.strategy.tester.TesterReportData;
import com.dukascopy.dds2.greed.util.AbstractCurrencyConverter;
import com.dukascopy.dds2.greed.util.FilePathManager;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;

// Referenced classes of package com.dukascopy.api.impl.connect:
//            PrintStreamNotificationUtils, IndicatorsSettingsStorage, ITesterGuiImpl, TesterChartControllerImpl, 
//            AuthorizationClient

public class TesterClient
    implements ITesterClient
{
    private class TesterExecutionControl
        implements ITesterExecutionControl
    {

        public void pauseExecution()
        {
            executionControl.pause();
        }

        public boolean isExecutionPaused()
        {
            return executionControl.isPaused();
        }

        public void continueExecution()
        {
            executionControl.run();
        }

        public void cancelExecution()
        {
            executionControl.stopExecuting(true);
        }

        public boolean isExecutionCanceled()
        {
            return !executionControl.isExecuting();
        }

        ExecutionControl executionControl;

        public TesterExecutionControl(ExecutionControl executionControl)
        {
            super();
            this.executionControl = null;
            this.executionControl = executionControl;
        }
    }

    private static class StrategyStuff
    {

        private long processId;
        private IStrategy strategies[];
        private TesterReportData reportDatas[];
        private IStrategyRunner strategyRunner;
        private TesterLoadingProgressListener loadingProgressListener;
        private File reportFiles[];













        private StrategyStuff()
        {
        }

    }

    private class TesterLoadingProgressListener
        implements com.dukascopy.charts.data.datacache.LoadingProgressListener
    {

        public void dataLoaded(long startTime, long endTime, long currentTime, String information)
        {
            if(testerProgressListener != null)
                testerProgressListener.dataLoaded(startTime, endTime, currentTime, information);
        }

        public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, 
                Exception ex)
        {
            synchronized(this)
            {
                if(!allDataLoaded && ex != null)
                    LOGGER.error(ex.getMessage(), ex);
                if((testerProgressListener == null || !testerProgressListener.stopJob()) && !strategy.strategyRunner.wasCanceled())
                {
                    strategy.reportFiles = new File[strategy.strategies.length];
                    for(int i = 0; i < strategy.strategies.length; i++)
                        try
                        {
                            strategy.reportFiles[i] = saveReportToFile(strategy.reportDatas[i], accountCurrency, eventLogEnabled);
                        }
                        catch(IOException e)
                        {
                            LOGGER.error(e.getMessage(), e);
                        }

                }
                strategy.strategies = null;
                strategy.reportDatas = null;
                strategy.loadingProgressListener = null;
                strategy.strategyRunner = null;
                if(systemListener != null)
                    systemListener.onStop(strategy.processId);
                if(testerProgressListener != null)
                {
                    testerProgressListener.loadingFinished(allDataLoaded, startTime, endTime, currentTime);
                    testerProgressListener = null;
                }
            }
        }

        public boolean stopJob()
        {
            return cancel || testerProgressListener != null && testerProgressListener.stopJob() || testerExecutionControl != null && testerExecutionControl.isExecutionCanceled();
        }

        public void cancel()
        {
            cancel = true;
        }

        private boolean cancel;
        private StrategyStuff strategy;
        private LoadingProgressListener testerProgressListener;
        private TesterExecutionControl testerExecutionControl;
        private Currency accountCurrency;
        private boolean eventLogEnabled;

        public TesterLoadingProgressListener(StrategyStuff strategy, LoadingProgressListener testerProgressListener, Currency accountCurrency, boolean eventLogEnabled)
        {
            super();
            this.accountCurrency = accountCurrency;
            this.eventLogEnabled = eventLogEnabled;
            this.strategy = strategy;
            this.testerProgressListener = testerProgressListener;
        }

        public TesterLoadingProgressListener(StrategyStuff strategy, LoadingProgressListener testerProgressListener, TesterExecutionControl testerExecutionControl, Currency accountCurrency, boolean eventLogEnabled)
        {
            super();
            this.accountCurrency = accountCurrency;
            this.eventLogEnabled = eventLogEnabled;
            this.strategy = strategy;
            this.testerProgressListener = testerProgressListener;
            this.testerExecutionControl = testerExecutionControl;
        }
    }

    private class DefaultStrategyExceptionHandler
        implements IStrategyExceptionHandler
    {

        public void onException(long strategyId, com.dukascopy.api.system.IStrategyExceptionHandler.Source source, Throwable t)
        {
            LOGGER.error((new StringBuilder()).append("Exception thrown while running ").append(source).append(" method: ").append(t.getMessage()).toString(), t);
            strategy.loadingProgressListener.cancel();
        }

        private final Logger LOGGER;
        private StrategyStuff strategy;

        private DefaultStrategyExceptionHandler(StrategyStuff strategy)
        {
            super();
            LOGGER = LoggerFactory.getLogger(DefaultStrategyExceptionHandler.class);
            this.strategy = strategy;
        }

    }


    public TesterClient()
    {
        chart_id = 0;
        instruments = new HashSet();
        connected = false;
        strategies = new HashMap();
        period = Period.TICK;
        offerSide = OfferSide.BID;
        accountCurrency = Instrument.EURUSD.getSecondaryCurrency();
        deposit = 50000D;
        leverage = 100;
        commission = new Commissions(false);
        overnights = new Overnights(false);
        marginCutLevel = 200;
        mcEquity = 0.0D;
        console = new IConsole() {

            public PrintStream getOut()
            {
                return out;
            }

            public PrintStream getErr()
            {
                return err;
            }

        };
        out = System.out;
        err = System.err;
        NotificationUtilsProvider.setNotificationUtils(new PrintStreamNotificationUtils(out, err));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        to = calendar.getTimeInMillis();
        calendar.add(6, -3);
        from = calendar.getTimeInMillis();
        dataLoadingMethod = com.dukascopy.api.system.ITesterClient.DataLoadingMethod.ALL_TICKS;
    }

    public synchronized void connect(String jnlpUrl, String username, String password)
        throws Exception
    {
        connect(jnlpUrl, username, password, null);
    }

    public synchronized void reconnect()
    {
    }

    public boolean isConnected()
    {
        return connected;
    }

    public synchronized void setSystemListener(final ISystemListener userSystemListener)
    {
        systemListener = new ISystemListener() {

            public void onStart(long processId)
            {
                userSystemListener.onStart(processId);
            }

            public void onStop(long processId)
            {
                userSystemListener.onStop(processId);
            }

            public void onConnect()
            {
                userSystemListener.onConnect();
            }

            public void onDisconnect()
            {
                userSystemListener.onDisconnect();
            }
        };
    }

    private void fireConnected()
    {
        ISystemListener systemListener = this.systemListener;
        if(systemListener != null)
            systemListener.onConnect();
        FeedDataProvider.getDefaultInstance().connected();
    }

    public synchronized long startStrategy(IStrategy strategy)
        throws IllegalStateException, IllegalArgumentException
    {
        if(!connected)
            throw new IllegalStateException("Not connected");
        if(strategy == null)
            throw new IllegalArgumentException("Strategy is null");
        else
            return startTest(strategy, null, null);
    }

    public synchronized long startStrategy(IStrategy strategy, LoadingProgressListener testerProgressListener)
        throws IllegalStateException, IllegalArgumentException
    {
        if(!connected)
            throw new IllegalStateException("Not connected");
        if(strategy == null)
            throw new IllegalArgumentException("Strategy is null");
        else
            return startTest(strategy, null, testerProgressListener);
    }

    public synchronized long startStrategy(IStrategy strategy, IStrategyExceptionHandler exceptionHandler)
        throws IllegalStateException, IllegalArgumentException
    {
        if(!connected)
            throw new IllegalStateException("Not connected");
        if(strategy == null)
            throw new IllegalArgumentException("Strategy is null");
        if(exceptionHandler == null)
            throw new IllegalArgumentException("Exception handler is null");
        else
            return startTest(strategy, exceptionHandler, null);
    }

    public synchronized long startStrategy(IStrategy strategy, IStrategyExceptionHandler exceptionHandler, LoadingProgressListener testerProgressListener)
        throws IllegalStateException, IllegalArgumentException
    {
        if(!connected)
            throw new IllegalStateException("Not connected");
        if(strategy == null)
            throw new IllegalArgumentException("Strategy is null");
        if(exceptionHandler == null)
            throw new IllegalArgumentException("Exception handler is null");
        else
            return startTest(strategy, exceptionHandler, testerProgressListener);
    }

    public synchronized long startStrategy(IStrategy strategy, LoadingProgressListener testerProgressListener, ITesterExecution testerExecution, ITesterUserInterface testerUserInterface)
        throws IllegalStateException, IllegalArgumentException
    {
        if(!connected)
            throw new IllegalStateException("Not connected");
        if(strategy == null)
            throw new IllegalArgumentException("Strategy is null");
        if(testerProgressListener == null)
            throw new IllegalArgumentException("TesterProgressListener is null");
        if(testerUserInterface == null)
            throw new IllegalArgumentException("TesterUserInterface is null");
        if(testerExecution == null)
            throw new IllegalArgumentException("TesterExecution is null");
        else
            return startTest(strategy, testerProgressListener, testerExecution, testerUserInterface);
    }

    private long startTest(IStrategy strategy, IStrategyExceptionHandler exceptionHandler, LoadingProgressListener testerProgressListener)
    {
        StrategyStuff strategyStuff = new StrategyStuff();
        strategyIdCounter++;
        strategyStuff.processId = strategyIdCounter;
        strategyStuff.loadingProgressListener = new TesterLoadingProgressListener(strategyStuff, testerProgressListener, accountCurrency, eventLogEnabled);
        strategyStuff.strategies = (new IStrategy[] {
            strategy
        });
        TesterOrdersProvider testerOrdersProvider = new TesterOrdersProvider();
        TesterAccount account = new TesterAccount(accountCurrency, deposit, leverage, marginCutLevel, mcEquity, commission, overnights, "");
        if(exceptionHandler == null)
            exceptionHandler = new DefaultStrategyExceptionHandler(strategyStuff);
        strategyStuff.strategyRunner = new StrategyRunner(null, strategy.getClass().getName(), strategy, true, period, offerSide, interpolationMethod, dataLoadingMethod, from, to, new HashSet(instruments), strategyStuff.loadingProgressListener, NotificationUtilsProvider.getNotificationUtils(), 1000D, account, null, testerOrdersProvider, new HashMap(0), new ExecutionControl() {

            public void pause()
            {
            }

            public void setSpeed(int i)
            {
            }

        }, processingStatsEnabled, exceptionHandler);
        strategyStuff.reportDatas = (new TesterReportData[] {
            ((StrategyRunner)strategyStuff.strategyRunner).getReportData()
        });
        strategies.put(Long.valueOf(strategyStuff.processId), strategyStuff);
        if(systemListener != null)
            systemListener.onStart(strategyStuff.processId);
        ((StrategyRunner)strategyStuff.strategyRunner).start();
        return strategyStuff.processId;
    }

    private long startTest(IStrategy strategy, LoadingProgressListener testerProgressListener, ITesterExecution testerExecution, ITesterUserInterface testerUserInterface)
    {
        TesterOrdersProvider testerOrdersProvider = new TesterOrdersProvider();
        TesterFeedDataProvider testerFeedDataProvider = null;
        try
        {
            testerFeedDataProvider = new TesterFeedDataProvider("jnlp.client.mode", testerOrdersProvider);
        }
        catch(DataCacheException e)
        {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException((new StringBuilder()).append("Cannot create TesterFeedDataProvider in the TestClient:").append(e.getMessage()).toString());
        }
        testerFeedDataProvider.setInstrumentsSubscribed(instruments);
        JForexPeriod jForexPeriod = getJForexPeriod();
        DDSChartsController ddsChartsController = getDDSChartsController();
        java.util.List chartBeans = createChartBeans(ddsChartsController, testerFeedDataProvider, jForexPeriod);
        Map chartsPanels = createChartsPanels(ddsChartsController, chartBeans);
        testerUserInterface.setChartPanels(chartsPanels);
        ExecutionControl executionControl = new ExecutionControl();
        executionControl.startExecuting(true);
        TesterExecutionControl testerExecutionControl = new TesterExecutionControl(executionControl);
        testerExecution.setExecutionControl(testerExecutionControl);
        Map testerChartsData = createTesterChartData(ddsChartsController, chartBeans);
        StrategyStuff strategyStuff = new StrategyStuff();
        strategyIdCounter++;
        strategyStuff.processId = strategyIdCounter;
        strategyStuff.loadingProgressListener = new TesterLoadingProgressListener(strategyStuff, testerProgressListener, testerExecutionControl, accountCurrency, eventLogEnabled);
        strategyStuff.strategies = (new IStrategy[] {
            strategy
        });
        TesterAccount account = new TesterAccount(accountCurrency, deposit, leverage, marginCutLevel, mcEquity, commission, overnights, "");
        IStrategyExceptionHandler exceptionHandler = new DefaultStrategyExceptionHandler(strategyStuff);
        strategyStuff.strategyRunner = new StrategyRunner(null, strategy.getClass().getName(), strategy, true, period, offerSide, interpolationMethod, dataLoadingMethod, from, to, new HashSet(instruments), strategyStuff.loadingProgressListener, NotificationUtilsProvider.getNotificationUtils(), 1000D, account, testerFeedDataProvider, testerOrdersProvider, testerChartsData, executionControl, processingStatsEnabled, exceptionHandler);
        strategyStuff.reportDatas = (new TesterReportData[] {
            ((StrategyRunner)strategyStuff.strategyRunner).getReportData()
        });
        strategies.put(Long.valueOf(strategyStuff.processId), strategyStuff);
        if(systemListener != null)
            systemListener.onStart(strategyStuff.processId);
        ((StrategyRunner)strategyStuff.strategyRunner).start();
        return strategyStuff.processId;
    }

    private java.util.List createChartBeans(final DDSChartsController ddsChartsController, IFeedDataProvider feedDataProvider, JForexPeriod jForexPeriod)
    {
        java.util.List chartBeans = new ArrayList();
        ChartBean chartBean;
        for(Iterator i$ = instruments.iterator(); i$.hasNext(); chartBeans.add(chartBean))
        {
            Instrument instrument = (Instrument)i$.next();
            chartBean = new ChartBean(getNextChartId(), instrument, jForexPeriod, OfferSide.BID);
            chartBean.setHistoricalTesterChart(true);
            chartBean.setReadOnly(true);
            chartBean.setFeedDataProvider(feedDataProvider);

            final ChartBean cb = chartBean;

            Runnable dataLoadingStarter = new Runnable() {

                public void run()
                {
                    if(ddsChartsController != null)
                        ddsChartsController.startLoadingData(Integer.valueOf(cb.getId()), cb.getAutoShiftActiveAsBoolean(), cb.getChartShiftInPx());
                }
            };
            chartBean.setStartLoadingDataRunnable(dataLoadingStarter);
        }

        return chartBeans;
    }

    private Map createChartsPanels(DDSChartsController ddsChartsController, java.util.List chartBeans)
    {
        Map chartPanels = new HashMap();
        ITesterGuiImpl chartGuiImpl;
        com.dukascopy.api.IChart chart;
        for(Iterator i$ = chartBeans.iterator(); i$.hasNext(); chartPanels.put(chart, chartGuiImpl))
        {
            ChartBean chartBean = (ChartBean)i$.next();
            chartGuiImpl = new ITesterGuiImpl();
            javax.swing.JPanel chartPanel = ddsChartsController.createNewChartOrGetById(chartBean);
            chartGuiImpl.setChartPanel(chartPanel);
            com.dukascopy.api.system.tester.ITesterChartController testerChartControl = new TesterChartControllerImpl(ddsChartsController, chartBean.getId());
            chartGuiImpl.setTesterChartControl(testerChartControl);
            chart = ddsChartsController.getIChartBy(Integer.valueOf(chartBean.getId()));
        }

        return chartPanels;
    }

    private Map createTesterChartData(DDSChartsController ddsChartsController, java.util.List chartBeans)
    {
        Map testerChartsData = new HashMap();
        ChartBean chartBean;
        TesterChartData chartData;
        for(Iterator i$ = chartBeans.iterator(); i$.hasNext(); testerChartsData.put(chartBean.getInstrument(), chartData))
        {
            chartBean = (ChartBean)i$.next();
            com.dukascopy.api.IChart chart = ddsChartsController.getIChartBy(Integer.valueOf(chartBean.getId()));
            chartData = new TesterChartData();
            chartData.instrument = chartBean.getInstrument();
            chartData.jForexPeriod = chartBean.getJForexPeriod();
            chartData.offerSide = chartBean.getOfferSide();
            chartData.feedDataProvider = chartBean.getFeedDataProvider();
            chartData.chartPanelId = chartBean.getId();
            chartData.chart = chart;
        }

        return testerChartsData;
    }

    private DDSChartsController getDDSChartsController()
    {
        DDSChartsController ddsChartsController = null;
        try
        {
            Class ddsChartsControllerImpl = Thread.currentThread().getContextClassLoader().loadClass("com.dukascopy.charts.main.DDSChartsControllerImpl");
            Method method = ddsChartsControllerImpl.getMethod("getInstance", null);
            ddsChartsController = (DDSChartsController)method.invoke(ddsChartsControllerImpl, null);
        }
        catch(Exception e)
        {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException((new StringBuilder()).append("Cannot create DDSChartsController:").append(e.getMessage()).toString());
        }
        if(ddsChartsController == null)
            throw new IllegalArgumentException("DDSChartsController is null");
        else
            return ddsChartsController;
    }

    private JForexPeriod getJForexPeriod()
    {
        JForexPeriod jForexPeriod = new JForexPeriod();
        long HOURS = 0x36ee80L;
        long DAYS = HOURS * 24L;
        long range = Math.abs(to - from);
        if(range < HOURS * 22L)
        {
            jForexPeriod.setPeriod(Period.FIVE_MINS);
            jForexPeriod.setDataType(DataType.TIME_PERIOD_AGGREGATION);
        } else
        if(range < DAYS * 6L)
        {
            jForexPeriod.setPeriod(Period.TEN_MINS);
            jForexPeriod.setDataType(DataType.TIME_PERIOD_AGGREGATION);
        } else
        if(range < DAYS * 25L)
        {
            jForexPeriod.setPeriod(Period.THIRTY_MINS);
            jForexPeriod.setDataType(DataType.TIME_PERIOD_AGGREGATION);
        } else
        {
            jForexPeriod.setPeriod(Period.ONE_HOUR);
            jForexPeriod.setDataType(DataType.TIME_PERIOD_AGGREGATION);
        }
        return jForexPeriod;
    }

    public void setEventLogEnabled(boolean eventLogEnabled)
    {
        this.eventLogEnabled = eventLogEnabled;
    }

    public boolean getEventLogEnabled()
    {
        return eventLogEnabled;
    }

    public void setProcessingStatsEnabled(boolean processingStats)
    {
        processingStatsEnabled = processingStats;
    }

    public boolean getProcessingStats()
    {
        return processingStatsEnabled;
    }

    public IStrategy loadStrategy(File strategyBinaryFile)
        throws Exception
    {
        JFXPack jfxPack = JFXPack.loadFromPack(strategyBinaryFile);
        return (IStrategy)jfxPack.getTarget();
    }

    public synchronized void stopStrategy(long processId)
    {
        if(!strategies.containsKey(Long.valueOf(processId)))
        {
            return;
        } else
        {
            ((StrategyStuff)strategies.remove(Long.valueOf(processId))).loadingProgressListener.cancel();
            return;
        }
    }

    public synchronized ISystemListener getSystemListener()
    {
        return systemListener;
    }

    public synchronized Map getStartedStrategies()
    {
        HashMap startedStrategies = new HashMap();
        Iterator i$ = strategies.values().iterator();
        do
        {
            if(!i$.hasNext())
                break;
            StrategyStuff strategy = (StrategyStuff)i$.next();
            if(strategy.strategyRunner != null)
                startedStrategies.put(Long.valueOf(strategy.processId), strategy.strategies[0]);
        } while(true);
        return startedStrategies;
    }

    public synchronized void setSubscribedInstruments(Set instruments)
    {
        Instrument instrument;
        for(Iterator i$ = (new HashSet(instruments)).iterator(); i$.hasNext(); instruments.addAll(AbstractCurrencyConverter.getConversionDeps(instrument.getSecondaryCurrency(), Instrument.EURUSD.getSecondaryCurrency())))
            instrument = (Instrument)i$.next();

        instruments.addAll(AbstractCurrencyConverter.getConversionDeps(Instrument.EURUSD.getSecondaryCurrency(), accountCurrency));
        this.instruments = instruments;
    }

    public synchronized INewsFilter getNewsFilter(com.dukascopy.api.INewsFilter.NewsSource newsSource)
    {
        return null;
    }

    public synchronized INewsFilter removeNewsFilter(com.dukascopy.api.INewsFilter.NewsSource newsSource)
    {
        return null;
    }

    public synchronized Set getSubscribedInstruments()
    {
        return Collections.unmodifiableSet(instruments);
    }

    public synchronized void addNewsFilter(INewsFilter inewsfilter)
    {
    }

    public synchronized void setOut(PrintStream out)
    {
        this.out = out;
        NotificationUtilsProvider.setNotificationUtils(new PrintStreamNotificationUtils(out, err));
    }

    public synchronized void setErr(PrintStream err)
    {
        this.err = err;
        NotificationUtilsProvider.setNotificationUtils(new PrintStreamNotificationUtils(out, err));
    }

    public void setDataInterval(Period period, OfferSide offerSide, com.dukascopy.api.system.ITesterClient.InterpolationMethod interpolationMethod, long from, long to)
    {
        if(period == null)
            this.period = Period.TICK;
        if(period == Period.TICK)
        {
            setDataInterval(com.dukascopy.api.system.ITesterClient.DataLoadingMethod.ALL_TICKS, from, to);
            return;
        }
        if(offerSide == null)
            offerSide = OfferSide.BID;
        if(period != Period.TICK && interpolationMethod == null)
        {
            throw new IllegalArgumentException("InterpolationMethod is null");
        } else
        {
            this.period = period;
            this.offerSide = offerSide;
            this.interpolationMethod = interpolationMethod;
            this.from = DataCacheUtils.getCandleStartFast(period, from);
            this.to = DataCacheUtils.getCandleStartFast(period, to);
            dataLoadingMethod = null;
            return;
        }
    }

    public void setDataInterval(com.dukascopy.api.system.ITesterClient.DataLoadingMethod dataLoadingMethod, long from, long to)
    {
        this.dataLoadingMethod = dataLoadingMethod;
        this.from = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, from);
        this.to = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, to);
        interpolationMethod = null;
    }

    public Future downloadData(final LoadingProgressListener loadingProgressListener)
    {
        final TesterDataLoader testerDataLoader = new TesterDataLoader(from, to, instruments, new com.dukascopy.charts.data.datacache.LoadingProgressListener() {

            public void dataLoaded(long startTime, long endTime, long currentTime, String information)
            {
                if(loadingProgressListener != null)
                    loadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
            }

            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, 
                    Exception e)
            {
                if(loadingProgressListener != null)
                    loadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, currentTime);
            }

            public boolean stopJob()
            {
                return loadingProgressListener != null && loadingProgressListener.stopJob();
            }
        });
        FutureTask future = new FutureTask(new Runnable() {

            public void run()
            {
                testerDataLoader.loadData();
            }

        }, null);
        Thread thread = new Thread(future, "Data loading thread");
        thread.start();
        return future;
    }

    public void setGatherReportData(boolean gatherReportData)
    {
        this.gatherReportData = gatherReportData;
    }

    public boolean getGatherReportData()
    {
        return gatherReportData;
    }

    public synchronized void createReport(File file)
        throws IOException, IllegalStateException
    {
        StrategyStuff strategy;
        InputStream is = null;
        OutputStream os = null;
        try {
            strategy = (StrategyStuff)strategies.get(Long.valueOf(1L));
            if(strategy == null || strategy.reportFiles == null)
                throw new IllegalStateException("Report data is not available");
            os = new BufferedOutputStream(new FileOutputStream(file));
            is = new BufferedInputStream(new FileInputStream(strategy.reportFiles[0]));
            byte buff[] = new byte[8192];
            int i;
            while((i = is.read(buff)) != -1) 
                os.write(buff, 0, i);
            is.close();
            os.close();
        }
        catch (Exception e) {
            if (is != null)is.close();
            if (os != null)os.close();
            throw new IllegalStateException("Report data is not available");
        }
    }

    public synchronized void createReport(long processId, File file)
        throws IOException, IllegalStateException
    {
        StrategyStuff strategy;
        InputStream is = null;
        OutputStream os = null;
        try {
            strategy = (StrategyStuff)strategies.get(Long.valueOf(processId));
            if(strategy == null || strategy.reportFiles == null)
                throw new IllegalStateException("Report data is not available");
            os = new BufferedOutputStream(new FileOutputStream(file));
            is = new BufferedInputStream(new FileInputStream(strategy.reportFiles[0]));
            byte buff[] = new byte[8192];
            int i;
            while((i = is.read(buff)) != -1) 
                os.write(buff, 0, i);
            is.close();
            os.close();
        }
        catch (Exception e) {
            if (is != null)is.close();
            if (os != null)os.close();
            throw new IllegalStateException("Report data is not available");
        }
    }

    public File saveReportToFile(TesterReportData reportData, Currency accountCurrency, boolean eventLogEnabled)
        throws IOException
    {
        File tempFile = File.createTempFile("jfrep", null);
        tempFile.deleteOnExit();
        StrategyReport.createReport(tempFile, reportData, accountCurrency, eventLogEnabled);
        return tempFile;
    }

    public void setInitialDeposit(Currency currency, double deposit)
        throws IllegalArgumentException
    {
        Set majors = AbstractCurrencyConverter.getMajors();
        if(currency == null)
        {
            accountCurrency = Instrument.EURUSD.getSecondaryCurrency();
        } else
        {
            if(!majors.contains(currency))
                throw new IllegalArgumentException((new StringBuilder()).append("Currency [").append(currency.getCurrencyCode()).append("] cannot be set as an account currency").toString());
            accountCurrency = currency;
        }
        setSubscribedInstruments(instruments);
        this.deposit = deposit;
    }

    public double getInitialDeposit()
    {
        return deposit;
    }

    public Currency getInitialDepositCurrency()
    {
        return accountCurrency;
    }

    public void setLeverage(int leverage)
    {
        this.leverage = leverage;
    }

    public int getLeverage()
    {
        return leverage;
    }

    public void setCommissions(Commissions commission)
    {
        this.commission = commission;
    }

    public Commissions getCommissions()
    {
        return commission;
    }

    public void setOvernights(Overnights overnights)
    {
        this.overnights = overnights;
    }

    public Overnights getOvernights()
    {
        return overnights;
    }

    public void setMarginCutLevel(int marginCutLevel)
    {
        this.marginCutLevel = marginCutLevel;
    }

    public int getMarginCutLevel()
    {
        return marginCutLevel;
    }

    public void setMCEquity(double mcEquity)
    {
        this.mcEquity = mcEquity;
    }

    public double getMCEquity()
    {
        return mcEquity;
    }

    public void setCacheDirectory(File cacheDirectory)
    {
        if(!cacheDirectory.exists())
        {
            LOGGER.warn((new StringBuilder()).append("Cache directory [").append(cacheDirectory).append("] doesn't exist, trying to create").toString());
            if(!cacheDirectory.mkdirs())
            {
                LOGGER.error((new StringBuilder()).append("Cannot create cache directory [").append(cacheDirectory).append("], default cache directory will be used").toString());
                return;
            }
        }
        FilePathManager.getInstance().setCacheFolderPath(cacheDirectory.getAbsolutePath());
    }

    private int getNextChartId()
    {
        return chart_id++;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TesterClient.class);
    public static final String HISTORY_SERVER_URL = "history.server.url";
    public static final String ENCRYPTION_KEY = "encryptionKey";
    private int chart_id;
    private volatile ISystemListener systemListener;
    private Set instruments;
    private PrintStream out;
    private PrintStream err;
    private Properties serverProperties;
    private AuthorizationClient authorizationClient;
    private String sessionID;
    private String captchaId;
    private boolean connected;
    private long strategyIdCounter;
    private Map strategies;
    private Period period;
    private OfferSide offerSide;
    private com.dukascopy.api.system.ITesterClient.InterpolationMethod interpolationMethod;
    private com.dukascopy.api.system.ITesterClient.DataLoadingMethod dataLoadingMethod;
    private long from;
    private long to;
    private Currency accountCurrency;
    private double deposit;
    private int leverage;
    private Commissions commission;
    private Overnights overnights;
    private boolean gatherReportData;
    private boolean eventLogEnabled;
    private boolean processingStatsEnabled;
    private int marginCutLevel;
    private double mcEquity;
    private IConsole console;
    @Override
    public void connect(String jnlp, String username, String password,
            String pin) throws JFAuthenticationException, JFVersionException,
            Exception {
        System.err.println("TODO: implement connect");
        this.connected = true;
    }

    @Override
    public BufferedImage getCaptchaImage(String jnlp) throws Exception {
        throw new UnsupportedOperationException();
    }
}