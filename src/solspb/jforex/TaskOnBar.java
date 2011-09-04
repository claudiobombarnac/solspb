// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskOnBar.java

package solspb.jforex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskOnBar
    implements Task
{

    public TaskOnBar(TaskManager taskManager, StrategyProcessor strategyProcessor, Instrument instrument, Period period, IBar askBar, IBar bidBar, IStrategyExceptionHandler exceptionHandler)
    {
        this.strategyProcessor = null;
        this.instrument = null;
        this.period = null;
        this.askBar = null;
        this.bidBar = null;
        this.strategyProcessor = strategyProcessor;
        this.instrument = instrument;
        this.period = period;
        this.askBar = askBar;
        this.bidBar = bidBar;
        this.exceptionHandler = exceptionHandler;
        this.taskManager = taskManager;
    }

    public Task.Type getType()
    {
        return Task.Type.BAR;
    }

    public Object call()
        throws Exception
    {
        if(taskManager.isStrategyStopping())
            return null;
        if(strategyProcessor.isOnBarImplemented())
            try
            {
                strategyProcessor.getStrategy().onBar(instrument, period, askBar, bidBar);
            }
            catch(AbstractMethodError abstractMethodError)
            {
                strategyProcessor.setOnBarImplemented(false);
            }
            catch(Throwable t)
            {
                String msg = StrategyWrapper.representError(strategyProcessor, t);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage(msg, t, false);
                LOGGER.error(t.getMessage(), t);
                exceptionHandler.onException(taskManager.getStrategyId(), com.dukascopy.api.system.IStrategyExceptionHandler.Source.ON_BAR, t);
            }
        return null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOnBar.class);
    private StrategyProcessor strategyProcessor;
    private Instrument instrument;
    private Period period;
    private IBar askBar;
    private IBar bidBar;
    private IStrategyExceptionHandler exceptionHandler;
    private TaskManager taskManager;

}