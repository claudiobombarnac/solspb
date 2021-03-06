// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskOrderError.java

package solspb.jforex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.connect.PlatformOrderImpl;
import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;
import com.dukascopy.transport.common.msg.response.ErrorResponseMessage;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskOrderError
    implements Task
{

    public TaskOrderError(TaskManager taskManager, IStrategy strategy, ErrorResponseMessage errorResponseMessage, PlatformOrderImpl order, IStrategyExceptionHandler exceptionHandler)
    {
        this.strategy = strategy;
        this.errorResponseMessage = errorResponseMessage;
        this.order = order;
        this.exceptionHandler = exceptionHandler;
        this.taskManager = taskManager;
    }

    public Task.Type getType()
    {
        return Task.Type.MESSAGE;
    }

    public Object call()
        throws Exception
    {
        if(taskManager.isStrategyStopping())
            return null;
        if(LOGGER.isDebugEnabled())
            LOGGER.debug((new StringBuilder()).append("Starting order [").append(order.getLabel()).append("] update process, error response message [").append(errorResponseMessage).append("]").toString());
        try
        {
            com.dukascopy.api.IMessage platformMessageImpl = order.update(errorResponseMessage);
            if(platformMessageImpl != null)
                strategy.onMessage(platformMessageImpl);
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrderError.class);
    private IStrategy strategy;
    private ErrorResponseMessage errorResponseMessage;
    private PlatformOrderImpl order;
    private IStrategyExceptionHandler exceptionHandler;
    private TaskManager taskManager;

}