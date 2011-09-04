// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskOrdersMerged.java

package solspb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.impl.execution.Task;
import com.dukascopy.transport.common.msg.request.MergePositionsMessage;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskOrdersMerged
    implements Task
{

    public TaskOrdersMerged(TaskManager taskManager, IStrategy strategy, MergePositionsMessage mergePositionsMessage)
    {
        this.strategy = strategy;
        this.mergePositionsMessage = mergePositionsMessage;
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
            LOGGER.debug((new StringBuilder()).append("Starting processing of merge message [").append(mergePositionsMessage).append("]").toString());
        return null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrdersMerged.class);
    private IStrategy strategy;
    private MergePositionsMessage mergePositionsMessage;
    private TaskManager taskManager;

}