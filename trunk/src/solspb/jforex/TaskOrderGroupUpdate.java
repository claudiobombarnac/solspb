// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskOrderGroupUpdate.java

package solspb.jforex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.transport.common.msg.group.OrderGroupMessage;


// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskOrderGroupUpdate
    implements Task
{

    public TaskOrderGroupUpdate(TaskManager taskManager, IStrategy strategy, OrderGroupMessage orderGroupMessage)
    {
        this.strategy = strategy;
        this.orderGroupMessage = orderGroupMessage;
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
            LOGGER.debug((new StringBuilder()).append("Starting processing of order group message [").append(orderGroupMessage).append("]").toString());
        return null;
    }

    public String toString()
    {
        return (new StringBuilder()).append("TaskOrderGroupUpdate [").append(orderGroupMessage).append("]").toString();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrderGroupUpdate.class);
    private IStrategy strategy;
    private OrderGroupMessage orderGroupMessage;
    private TaskManager taskManager;

}