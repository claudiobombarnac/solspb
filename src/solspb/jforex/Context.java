package solspb.jforex;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IStrategies;
import com.dukascopy.api.IUserInterface;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.PriceRange;
import com.dukascopy.api.ReversalAmount;
import com.dukascopy.api.TickBarSize;
import com.dukascopy.api.feed.IBarFeedListener;
import com.dukascopy.api.feed.IPointAndFigureFeedListener;
import com.dukascopy.api.feed.IRangeBarFeedListener;
import com.dukascopy.api.feed.ITickBarFeedListener;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.charts.main.interfaces.DDSChartsController;
import com.dukascopy.charts.wrapper.DelegatableChartWrapper;
import com.dukascopy.dds2.greed.util.FilePathManager;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;

public class Context implements IContext {
    private StrategyProcessor strategyProcessor;
    private Engine forexEngineImpl;
    private History history;
    private IConsole console;
    private DDSChartsController chartsController;
    private IUserInterface userInterface;
    private IIndicators indicators;
    private File filesDir;

    // Referenced classes of package com.dukascopy.api.impl.connect:
//                StrategyProcessor, JForexTaskManager, JForexEngineImpl

        private final class StopStrategyPrivilegedAction
            implements PrivilegedExceptionAction
        {

            public Object run()
                throws Exception
            {
                strategyProcessor.getTaskManager().stopStrategy();
                return null;
            }

            private StopStrategyPrivilegedAction()
            {
                super();
            }

        }


        public Context(StrategyProcessor strategyProcessor, Engine forexEngineImpl, History history, IConsole console, DDSChartsController chartsController, IUserInterface userInterface)
        {
            this.strategyProcessor = null;
            this.forexEngineImpl = null;
            this.history = null;
            this.strategyProcessor = strategyProcessor;
            this.forexEngineImpl = forexEngineImpl;
            this.history = history;
            this.console = console;
            filesDir = FilePathManager.getInstance().getFilesForStrategiesDir();
            this.chartsController = chartsController;
            this.userInterface = userInterface;
        }

        public boolean isStopped()
        {
            return strategyProcessor.isStopping();
        }

        public StrategyProcessor getStrategyProcessor()
        {
            return strategyProcessor;
        }

        public IChart getChart(Instrument instrument)
        {
            if(chartsController != null)
            {
                IChart chart = chartsController.getIChartBy(instrument);
                if(chart == null)
                    return null;
                else
                    return new DelegatableChartWrapper(chart, strategyProcessor.getStrategy().getClass().getName());
            } else
            {
                return null;
            }
        }

        public Set getCharts(Instrument instrument)
        {
            if(chartsController != null)
            {
                Set charts = chartsController.getICharts(instrument);
                if(charts == null)
                    return null;
                Set result = new HashSet();
                DelegatableChartWrapper wrapper;
                for(Iterator i$ = charts.iterator(); i$.hasNext(); result.add(wrapper))
                {
                    IChart chart = (IChart)i$.next();
                    wrapper = new DelegatableChartWrapper(chart, strategyProcessor.getStrategy().getClass().getName());
                }

                return result;
            } else
            {
                return null;
            }
        }

        public IUserInterface getUserInterface()
        {
            return userInterface;
        }

        public IConsole getConsole()
        {
            return console;
        }

        public IEngine getEngine()
        {
            return forexEngineImpl;
        }

        public IHistory getHistory()
        {
            return history;
        }

        public IIndicators getIndicators()
        {
            if(indicators == null)
                indicators = new Indicators(history);
            return indicators;
        }

        public IStrategies getStrategies()
        {
            return null;
        }

        public Future executeTask(Callable callable)
        {
            return strategyProcessor.executeTask(callable, false);
        }

        public boolean isFullAccessGranted()
        {
            return strategyProcessor.isFullAccessGranted();
        }

        public void stop()
        {
            try
            {
                AccessController.doPrivileged(new StopStrategyPrivilegedAction());
            }
            catch(PrivilegedActionException e)
            {
                Exception ex = e.getException();
                String error = StrategyWrapper.representError(new Long(strategyProcessor.getStrategyId()), ex);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage(error, ex, false);
            }
        }

        public File getFilesDir()
        {
            return filesDir;
        }

        public String toString()
        {
            return "JForex context";
        }

        public void pause()
        {
        }

        public void setSubscribedInstruments(Set instruments)
        {
            strategyProcessor.setSubscribedInstruments(instruments);
        }

        public Set getSubscribedInstruments()
        {
            return strategyProcessor.getSubscribedInstruments();
        }

        public void subscribeToBarsFeed(Instrument instrument, Period period, OfferSide offerSide, IBarFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void unsubscribeFromBarsFeed(IBarFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void subscribeToPointAndFigureFeed(Instrument instrument, OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, IPointAndFigureFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void unsubscribeFromPointAndFigureFeed(IPointAndFigureFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void subscribeToRangeBarFeed(Instrument instrument, OfferSide offerSide, PriceRange priceRange, IRangeBarFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void unsubscribeFromRangeBarFeed(IRangeBarFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void subscribeToTickBarFeed(Instrument instrument, OfferSide offerSide, TickBarSize tickBarSize, ITickBarFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }

        public void unsubscribeFromTickBarFeed(ITickBarFeedListener listener)
        {
            throw new UnsupportedOperationException();
        }
        
        public IAccount getAccount() {
        	throw new UnsupportedOperationException();
        }

}
