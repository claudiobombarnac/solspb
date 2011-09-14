// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:06
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   JForexEngineImpl.java

package solspb.jforex;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import artist.api.BrokerInt;
import artist.api.Constants;
import artist.api.ContextLoader;
import artist.api.OrderInt.OrderType;
import artist.api.beans.LimitOrder;
import artist.api.beans.StopOrder;

import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.impl.AbstractEngine;
import com.dukascopy.api.impl.connect.PlatformOrderImpl;
import com.dukascopy.transport.common.msg.ProtocolMessage;
import com.dukascopy.transport.common.msg.response.ErrorResponseMessage;
import com.dukascopy.transport.common.msg.strategy.StrategyBroadcastMessage;

// Referenced classes of package com.dukascopy.api.impl.connect:
//            PlatformOrderImpl, PlatformMessageImpl, JForexTaskManager, OrdersInternalCollection, 
//            JForexAPI

class Engine extends AbstractEngine
    implements IEngine
{
	private BrokerInt broker;
    private List<IOrder> orders = new ArrayList<IOrder>();

	public Engine(TaskManager taskManager, String accountName, boolean live)
    {
        myType = com.dukascopy.api.IEngine.Type.DEMO;
        this.accountName = null;
        lastBroadcastTime = 0L;
        if(live)
            myType = com.dukascopy.api.IEngine.Type.LIVE;
        this.taskManager = taskManager;
        this.accountName = accountName;
        this.broker = ContextLoader.getInstance().getBroker();
    }

    public IOrder getOrder(String label)
        throws JFException
    {
        List allOrders = getOrders();
        IOrder rc = null;
        Iterator i$ = allOrders.iterator();
        do
        {
            if(!i$.hasNext())
                break;
            IOrder order = (IOrder)i$.next();
            if(order.getLabel() == null || !order.getLabel().equals(label))
                continue;
            rc = order;
            break;
        } while(true);
        return rc;
    }

    public List getOrders(Instrument instrument)
        throws JFException
    {
        List rc = new ArrayList();
        Iterator i$ = getOrders().iterator();
        do
        {
            if(!i$.hasNext())
                break;
            IOrder order = (IOrder)i$.next();
            if(instrument == order.getInstrument())
                rc.add(order);
        } while(true);
        return rc;
    }

    public List getOrders()
        throws JFException
    {
        return orders ;
    }

    public com.dukascopy.api.IEngine.Type getType()
    {
        return myType;
    }

    private static int orderId = 1;
    public IOrder submitOrder(String label, Instrument instrument, com.dukascopy.api.IEngine.OrderCommand orderCommand, double amount, double price, 
            double slippage, double stopLossPrice, double takeProfitPrice, long goodTillTime, String comment)
        throws JFException
    {
    	SubmitOrder order = new SubmitOrder(instrument, String.valueOf(orderId++));
        System.out.println(orderCommand + " STOP-LOSS: " + stopLossPrice + " TAKE-PROFIT: " + takeProfitPrice);
        Date expiration = new Date();
        expiration.setDate(expiration.getDate() + 1);
        if (orderCommand == OrderCommand.BUY) {
        	int no = broker.createOrder(new StopOrder(Constants.ACCOUNT, Constants.PLACE_CODE, instrument.toString(), expiration, Constants.CURRENCY, OrderType.B, (int)amount, price, stopLossPrice, slippage, Constants.ORDER_TIMEOUT));
            System.out.println("Order " + no + ": " + broker.lastResultMessage());
        	no = broker.createOrder(new LimitOrder(Constants.ACCOUNT, Constants.PLACE_CODE, instrument.toString(), expiration, Constants.CURRENCY, OrderType.S, (int)amount, takeProfitPrice, Constants.ORDER_TIMEOUT));
            System.out.println("Order " + no + ": " + broker.lastResultMessage());
        }
        else {
        	int no = broker.createOrder(new StopOrder(Constants.ACCOUNT, Constants.PLACE_CODE, instrument.toString(), expiration, Constants.CURRENCY, OrderType.S, (int)amount, price, stopLossPrice, slippage, Constants.ORDER_TIMEOUT));
            System.out.println("Order " + no + ": " + broker.lastResultMessage());
        	no = broker.createOrder(new LimitOrder(Constants.ACCOUNT, Constants.PLACE_CODE, instrument.toString(), expiration, Constants.CURRENCY, OrderType.B, (int)amount, takeProfitPrice, Constants.ORDER_TIMEOUT));
            System.out.println("Order " + no + ": " + broker.lastResultMessage());
        }
        System.out.println(broker.lastResultMessage());
        orders.add(order);
        return order;
    }

    public IOrder submitOrder(String label, Instrument instrument, com.dukascopy.api.IEngine.OrderCommand orderCommand, double amount, double price, 
            double slippage, double stopLossPrice, double takeProfitPrice, long goodTillTime)
        throws JFException
    {
        return submitOrder(label, instrument, orderCommand, amount, price, slippage, stopLossPrice, takeProfitPrice, goodTillTime, null);
    }

    public IOrder submitOrder(String label, Instrument instrument, com.dukascopy.api.IEngine.OrderCommand orderCommand, double amount, double price, 
            double slippage, double stopLossPrice, double takeProfitPrice)
        throws JFException
    {
        return submitOrder(label, instrument, orderCommand, amount, price, slippage, stopLossPrice, takeProfitPrice, 0L);
    }

    public IOrder submitOrder(String label, Instrument instrument, com.dukascopy.api.IEngine.OrderCommand orderCommand, double amount, double price, 
            double slippage)
        throws JFException
    {
        return submitOrder(label, instrument, orderCommand, amount, price, slippage, 0.0D, 0.0D);
    }

    public IOrder submitOrder(String label, Instrument instrument, com.dukascopy.api.IEngine.OrderCommand orderCommand, double amount, double price)
        throws JFException
    {
        double slippage = 5D;
        if(orderCommand != null && (orderCommand == com.dukascopy.api.IEngine.OrderCommand.BUYLIMIT || orderCommand == com.dukascopy.api.IEngine.OrderCommand.BUYLIMIT_BYBID || orderCommand == com.dukascopy.api.IEngine.OrderCommand.SELLLIMIT || orderCommand == com.dukascopy.api.IEngine.OrderCommand.SELLLIMIT_BYASK))
            slippage = 0.0D;
        return submitOrder(label, instrument, orderCommand, amount, price, slippage);
    }

    public IOrder submitOrder(String label, Instrument instrument, com.dukascopy.api.IEngine.OrderCommand orderCommand, double amount)
        throws JFException
    {
        return submitOrder(label, instrument, orderCommand, amount, 0.0D);
    }

    public PlatformOrderImpl getOrderById(String positionId)
    {
        throw new UnsupportedOperationException();
    }

    public String toString()
    {
        return "Platform engine";
    }

    public String getAccount()
    {
        return accountName;
    }

    public void broadcast(String topic, String message)
        throws JFException
    {
        if(topic == null || topic.isEmpty())
            throw new JFException("Illegal broadcast usage : topic is empty");
        if(topic.length() > 100)
            throw new JFException((new StringBuilder()).append("Broadcast topic max length exceeded : ").append(topic.length()).append(" / ").append(100).toString());
        if(message == null || message.isEmpty())
            throw new JFException("Illegal broadcast usage : message is empty");
        if(message.length() > 1000)
            throw new JFException((new StringBuilder()).append("Broadcast message max length exceeded : ").append(message.length()).append(" / ").append(1000).toString());
        long period = System.currentTimeMillis() - lastBroadcastTime;
        if(period >= 0L && period < BROADCAST_MIN_PERIOD)
            throw new JFException((new StringBuilder()).append("Broadcast min period exceeded : ").append(period).append(" / ").append(BROADCAST_MIN_PERIOD).toString());
        lastBroadcastTime = System.currentTimeMillis();
        try
        {
            StrategyBroadcastMessage broadcastMessage = new StrategyBroadcastMessage();
            broadcastMessage.setTopic(topic);
            broadcastMessage.setMessage(message);
            broadcastMessage.setTransactionId(taskManager.getUID());
            LOGGER.debug((new StringBuilder()).append("Sending broadcast message : ").append(broadcastMessage).toString());
            ProtocolMessage response = taskManager.getTransportClient().controlRequest(broadcastMessage);
            if(response instanceof ErrorResponseMessage)
                throw new JFException((new StringBuilder()).append("Unable to send broadcast message : ").append(((ErrorResponseMessage)response).getReason()).toString());
        }
        catch(Exception ex)
        {
            throw new JFException("Broadcast error", ex);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);
    private static final int BROADCAST_TOPIC_MAX_LENGTH = 100;
    private static final int BROADCAST_MESSAGE_MAX_LENGTH = 1000;
    private static final long BROADCAST_MIN_PERIOD;
    static final double DEFAULT_SLIPPAGE = 5D;
    private com.dukascopy.api.IEngine.Type myType;
    private final TaskManager taskManager;
    private String accountName;
    private volatile long lastBroadcastTime;

    static 
    {
        BROADCAST_MIN_PERIOD = TimeUnit.SECONDS.toMillis(1L);
    }

    @Override
    public void closeOrders(IOrder... orders) throws JFException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mergeOrders(IOrder... orders) throws JFException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOrder mergeOrders(String label, IOrder... orders)
            throws JFException {
        throw new UnsupportedOperationException();
    }
}