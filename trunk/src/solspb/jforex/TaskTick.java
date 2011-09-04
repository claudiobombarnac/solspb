// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskTick.java

package solspb.jforex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;


// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskTick extends com.dukascopy.api.impl.execution.TaskTick
    implements Task
{

    public TaskTick(TaskManager taskManager, IStrategy strategy, Instrument instrument, ITick tick, IStrategyExceptionHandler exceptionHandler)
    {
    	super(null, null, null, tick, exceptionHandler);
        this.strategy = null;
        this.strategy = strategy;
        this.instrument = instrument;
        this.tick = tick;
        this.exceptionHandler = exceptionHandler;
        addedTime = System.currentTimeMillis();
        this.taskManager = taskManager;
    }

    public long getAddedTime()
    {
        return addedTime;
    }

    public Task.Type getType()
    {
        return Task.Type.TICK;
    }

    public Instrument getInstrument()
    {
        return instrument;
    }

    public ITick getTick()
    {
        return tick;
    }

    public Object call()
        throws Exception
    {
        if(taskManager.isStrategyStopping())
            return null;
        try
        {
            strategy.onTick(instrument, tick);
        }
        catch(Throwable t)
        {
            String msg = StrategyWrapper.representError(strategy, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage(msg, t, false);
            LOGGER.error(t.getMessage(), t);
            exceptionHandler.onException(taskManager.getStrategyId(), com.dukascopy.api.system.IStrategyExceptionHandler.Source.ON_TICK, t);
        }
        return null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTick.class);
    private IStrategy strategy;
    private final Instrument instrument;
    private final ITick tick;
    private IStrategyExceptionHandler exceptionHandler;
    private long addedTime;
    private TaskManager taskManager;

}