// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskCustom.java

package solspb.jforex;

import java.util.concurrent.Callable;

import com.dukascopy.api.impl.execution.Task;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskCustom
    implements Task
{

    public TaskCustom(TaskManager taskManager, Callable callable, boolean force)
    {
        this.callable = null;
        this.callable = callable;
        this.taskManager = taskManager;
        this.force = force;
    }

    public Task.Type getType()
    {
        return Task.Type.CUSTOM;
    }

    public Object call()
        throws Exception
    {
        if(taskManager.isStrategyStopping() && !force)
            return null;
        else
            return callable.call();
    }

    private Callable callable;
    private TaskManager taskManager;
    private boolean force;
}