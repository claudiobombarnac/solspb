// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:08
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TaskParameter.java

package solspb.jforex;

import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.impl.StrategyMessages;
import com.dukascopy.api.impl.execution.IControlUI;
import com.dukascopy.api.impl.execution.Task;

// Referenced classes of package com.dukascopy.api.impl.execution:
//            Task, IControlUI

public class TaskParameter
    implements Task
{

    public TaskParameter(IStrategy strategy, IControlUI parametersDialog, List componentsList)
    {
        this.parametersDialog = parametersDialog;
        this.componentsList = componentsList;
        this.strategy = strategy;
    }

    public TaskParameter()
    {
        parametersDialog = null;
        componentsList = null;
    }

    public Task.Type getType()
    {
        return Task.Type.PARAMETER;
    }

    public Object call()
        throws Exception
    {
        try {
            if(parametersDialog != null && componentsList != null)
            {
                JComponent component;
                for(Iterator i$ = componentsList.iterator(); i$.hasNext(); parametersDialog.setControlField(component, false))
                    component = (JComponent)i$.next();
    
                if(printChangeMessage && parametersDialog != null && componentsList != null)
                    StrategyMessages.strategyIsModified(strategy);
            }
            return Boolean.valueOf(true);
        }
        catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            return Boolean.valueOf(false);
        }
    }

    public void setPrintChangeMessage(boolean printChangeMessage)
    {
        this.printChangeMessage = printChangeMessage;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskParameter.class);
    private IControlUI parametersDialog;
    List componentsList;
    private IStrategy strategy;
    private boolean printChangeMessage;

}