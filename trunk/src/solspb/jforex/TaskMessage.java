// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskMessage.java

package solspb.jforex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IMessage;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;


// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskMessage
    implements Task
{

    public TaskMessage(TaskManager taskManager, IStrategy strategy, IMessage message, IStrategyExceptionHandler exceptionHandler)
    {
        release = false;
        this.strategy = strategy;
        this.message = message;
        this.exceptionHandler = exceptionHandler;
        this.taskManager = taskManager;
    }

    public Task.Type getType()
    {
        return Task.Type.MESSAGE;
    }

    public boolean release()
    {
        return release;
    }

    public void setRelease(boolean release)
    {
        this.release = release;
    }

    public Object call()
        throws Exception
    {
        if(taskManager.isStrategyStopping())
            return null;
        try
        {
            strategy.onMessage(message);
        }
        catch(Throwable t)
        {
            String msg = StrategyWrapper.representError(strategy, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage(msg, t, false);
            LOGGER.error(t.getMessage(), t);
            exceptionHandler.onException(taskManager.getStrategyId(), com.dukascopy.api.system.IStrategyExceptionHandler.Source.ON_MESSAGE, t);
        }
        return null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessage.class);
    private final TaskManager taskManager;
    private final IStrategy strategy;
    private final IMessage message;
    private final IStrategyExceptionHandler exceptionHandler;
    private boolean release;

}