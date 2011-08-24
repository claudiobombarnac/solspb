// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:04
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   History.java

package solspb;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.LoadingDataListener;
import com.dukascopy.api.LoadingOrdersListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.PriceRange;
import com.dukascopy.api.ReversalAmount;
import com.dukascopy.api.TickBarSize;
import com.dukascopy.api.feed.IPointAndFigure;
import com.dukascopy.api.feed.IPointAndFigureFeedListener;
import com.dukascopy.api.feed.IRangeBar;
import com.dukascopy.api.feed.IRangeBarFeedListener;
import com.dukascopy.api.feed.ITickBar;
import com.dukascopy.api.feed.ITickBarFeedListener;
import com.dukascopy.api.impl.HistoryOrder;
import com.dukascopy.api.impl.util.HistoryUtils;
import com.dukascopy.charts.data.datacache.CandleData;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.FeedDataProvider;
import com.dukascopy.charts.data.datacache.LiveFeedListener;
import com.dukascopy.charts.data.datacache.LoadingProgressAdapter;
import com.dukascopy.charts.data.datacache.LoadingProgressListener;
import com.dukascopy.charts.data.datacache.OrderHistoricalData;
import com.dukascopy.charts.data.datacache.OrdersListener;
import com.dukascopy.charts.data.datacache.TickData;
import com.dukascopy.charts.data.datacache.pnf.PointAndFigureData;
import com.dukascopy.charts.data.datacache.pnf.PointAndFigureLiveFeedAdapter;
import com.dukascopy.charts.data.datacache.priceaggregation.AbstractPriceAggregationData;
import com.dukascopy.charts.data.datacache.rangebar.PriceRangeData;
import com.dukascopy.charts.data.datacache.rangebar.PriceRangeLiveFeedAdapter;
import com.dukascopy.charts.data.datacache.tickbar.TickBarData;
import com.dukascopy.charts.data.datacache.tickbar.TickBarLiveFeedAdapter;
import com.dukascopy.charts.data.orders.IOrdersProvider;
import java.math.BigDecimal;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Referenced classes of package com.dukascopy.api.impl:
//            HistoryOrder

public class History
    implements IHistory {

    @Override
    public IBar getBar(Instrument instrument, Period period, OfferSide side,
            int shift) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getBarStart(Period period, long time) throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<IBar> getBars(Instrument instrument, Period period,
            OfferSide side, long from, long to) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<IBar> getBars(Instrument instrument, Period period,
            OfferSide side, Filter filter, int numberOfCandlesBefore,
            long time, int numberOfCandlesAfter) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getEquity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ITick getLastTick(Instrument instrument) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getNextBarStart(Period period, long barTime) throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<IOrder> getOrdersHistory(Instrument instrument, long from,
            long to) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPointAndFigure getPointAndFigure(Instrument instrument,
            OfferSide offerSide, PriceRange boxSize,
            ReversalAmount reversalAmount, int shift) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<IPointAndFigure> getPointAndFigures(Instrument instrument,
            OfferSide offerSide, PriceRange boxSize,
            ReversalAmount reversalAmount, long from, long to)
            throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<IPointAndFigure> getPointAndFigures(Instrument instrument,
            OfferSide offerSide, PriceRange boxSize,
            ReversalAmount reversalAmount, int numberOfBarsBefore, long time,
            int numberOfBarsAfter) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getPreviousBarStart(Period period, long barTime)
            throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IRangeBar getRangeBar(Instrument instrument, OfferSide offerSide,
            PriceRange priceRange, int shift) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<IRangeBar> getRangeBars(Instrument instrument,
            OfferSide offerSide, PriceRange priceRange, long from, long to)
            throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<IRangeBar> getRangeBars(Instrument instrument,
            OfferSide offerSide, PriceRange priceRange, int numberOfBarsBefore,
            long time, int numberOfBarsAfter) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getStartTimeOfCurrentBar(Instrument instrument, Period period)
            throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ITick getTick(Instrument instrument, int shift) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITickBar getTickBar(Instrument instrument, OfferSide offerSide,
            TickBarSize tickBarSize, int shift) throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ITickBar> getTickBars(Instrument instrument,
            OfferSide offerSide, TickBarSize tickBarSize, long from, long to)
            throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ITickBar> getTickBars(Instrument instrument,
            OfferSide offerSide, TickBarSize tickBarSize,
            int numberOfBarsBefore, long time, int numberOfBarsAfter)
            throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ITick> getTicks(Instrument instrument, long from, long to)
            throws JFException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getTimeForNBarsBack(Period period, long to, int numberOfBars)
            throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTimeForNBarsForward(Period period, long from,
            int numberOfBars) throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTimeOfLastTick(Instrument instrument) throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void readBars(Instrument instrument, Period period, OfferSide side,
            long from, long to, LoadingDataListener barListener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readBars(Instrument instrument, Period period, OfferSide side,
            Filter filter, int numberOfCandlesBefore, long time,
            int numberOfCandlesAfter, LoadingDataListener barListener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readOrdersHistory(Instrument instrument, long from, long to,
            LoadingOrdersListener ordersListener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readPointAndFigures(Instrument instrument, OfferSide offerSide,
            PriceRange boxSize, ReversalAmount reversalAmount, long from,
            long to, IPointAndFigureFeedListener listener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readPointAndFigures(Instrument instrument, OfferSide offerSide,
            PriceRange boxSize, ReversalAmount reversalAmount,
            int numberOfBarsBefore, long time, int numberOfBarsAfter,
            IPointAndFigureFeedListener listener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readRangeBars(Instrument instrument, OfferSide offerSide,
            PriceRange priceRange, long from, long to,
            IRangeBarFeedListener listener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readRangeBars(Instrument instrument, OfferSide offerSide,
            PriceRange priceRange, int numberOfBarsBefore, long time,
            int numberOfBarsAfter, IRangeBarFeedListener listener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readTickBars(Instrument instrument, OfferSide offerSide,
            TickBarSize tickBarSize, long from, long to,
            ITickBarFeedListener listener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readTickBars(Instrument instrument, OfferSide offerSide,
            TickBarSize tickBarSize, int numberOfBarsBefore, long time,
            int numberOfBarsAfter, ITickBarFeedListener listener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void readTicks(Instrument instrument, long from, long to,
            LoadingDataListener tickListener,
            com.dukascopy.api.LoadingProgressListener loadingProgress)
            throws JFException {
        // TODO Auto-generated method stub
        
    }

    public IBar getCurrentBar(Instrument instrument, Period period,
            OfferSide side) {
        // TODO Auto-generated method stub
        return null;
    }

    public ITick getLastTickBefore(Instrument instrument, long l) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isIntervalValid(Period period, long from, long to) {
        // TODO Auto-generated method stub
        return false;
    }

    public void validateTimeInterval(Instrument instrument, long from, long to) {
        // TODO Auto-generated method stub
        
    }

    public void validateBeforeTimeAfter(Instrument instrument,
            int numberOfBarsBefore, long time, int numberOfBarsAfter) {
        // TODO Auto-generated method stub
        
    }
}