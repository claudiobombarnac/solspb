// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskAccount.java

package solspb.jforex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskAccount
    implements Task
{

    public TaskAccount(TaskManager taskManager, IStrategy strategy, IAccount account, IStrategyExceptionHandler exceptionHandler)
    {
        this.strategy = null;
        this.account = null;
        this.strategy = strategy;
        this.account = account;
        this.exceptionHandler = exceptionHandler;
        this.taskManager = taskManager;
    }

    public Task.Type getType()
    {
        return Task.Type.ACCOUNT;
    }

    public Object call()
        throws Exception
    {
        if(taskManager.isStrategyStopping())
            return null;
        try
        {
            strategy.onAccount(account);
        }
        catch(Throwable t)
        {
            String msg = StrategyWrapper.representError(strategy, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage(msg, t, false);
            LOGGER.error(t.getMessage(), t);
            exceptionHandler.onException(taskManager.getStrategyId(), com.dukascopy.api.system.IStrategyExceptionHandler.Source.ON_ACCOUNT_INFO, t);
        }
        return null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAccount.class);
    private IStrategy strategy;
    private IAccount account;
    private IStrategyExceptionHandler exceptionHandler;
    private TaskManager taskManager;

}