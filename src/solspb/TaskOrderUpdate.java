// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskOrderUpdate.java

package solspb;

import com.dukascopy.api.*;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.connect.*;
import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.api.system.IStrategyExceptionHandler;
import com.dukascopy.dds2.greed.util.INotificationUtils;
import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;
import com.dukascopy.transport.common.model.type.Money;
import com.dukascopy.transport.common.model.type.OrderState;
import com.dukascopy.transport.common.msg.group.OrderGroupMessage;
import com.dukascopy.transport.common.msg.group.OrderMessage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskOrderUpdate
    implements Task
{

    public TaskOrderUpdate(TaskManager taskManager, IStrategy strategy, OrderMessage orderMessage)
    {
        this.strategy = strategy;
        this.orderMessage = orderMessage;
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
            LOGGER.debug((new StringBuilder()).append("Starting processing of order message [").append(orderMessage).append("]").toString());
        return null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrderUpdate.class);
    private IStrategy strategy;
    private OrderMessage orderMessage;
    private TaskManager taskManager;

}