// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskStart.java

package solspb.jforex;


import com.dukascopy.api.IContext;
import com.dukascopy.api.impl.execution.Task;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskStart
    implements Task
{

    public TaskStart(IContext context, IStrategy strategy)
    {
        this.context = null;
        this.strategy = null;
        this.context = context;
        this.strategy = strategy;
    }

    public Task.Type getType()
    {
        return Task.Type.START;
    }

    public Object call()
        throws Exception
    {
        strategy.onStart(context);
        return null;
    }

    private IContext context;
    private IStrategy strategy;
}