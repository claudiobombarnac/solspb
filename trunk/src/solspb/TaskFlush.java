// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskFlush.java

package solspb;

import com.dukascopy.api.impl.execution.Task;


// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task

public class TaskFlush
    implements Task
{

    public TaskFlush(Object notifyObject)
    {
        this.notifyObject = notifyObject;
    }

    public Task.Type getType()
    {
        return Task.Type.CUSTOM;
    }

    public Object call()
        throws Exception
    {
        synchronized(notifyObject)
        {
            notifyObject.notifyAll();
        }
        return null;
    }

    private Object notifyObject;
}