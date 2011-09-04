// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskStop.java

package solspb.jforex;


import com.dukascopy.api.impl.execution.Task;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskStop
    implements Task
{

    public TaskStop(IStrategy strategy)
    {
        this.strategy = null;
        this.strategy = strategy;
    }

    public Task.Type getType()
    {
        return Task.Type.STOP;
    }

    public Object call()
        throws Exception
    {
        if(strategy != null)
            strategy.onStop();
        return null;
    }

    private IStrategy strategy;
}