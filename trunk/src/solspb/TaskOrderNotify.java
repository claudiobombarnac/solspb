// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskOrderNotify.java

package solspb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.transport.common.msg.response.NotificationMessage;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskOrderNotify
    implements Task
{

    public TaskOrderNotify(TaskManager taskManager, IStrategy strategy, NotificationMessage notificationMessage)
    {
        this.strategy = strategy;
        this.notificationMessage = notificationMessage;
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
            LOGGER.debug((new StringBuilder()).append("Starting processing of notify message [").append(notificationMessage).append("]").toString());
        return null;
    }

    public String toString()
    {
        return (new StringBuilder()).append("TaskOrderNotify [").append(notificationMessage).append("]").toString();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrderNotify.class);
    private TaskManager taskManager;
    private IStrategy strategy;
    private NotificationMessage notificationMessage;

}