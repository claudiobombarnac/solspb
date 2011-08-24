// Decompiled by DJ v3.9.9.91 Copyright 2005 Atanas Neshkov  Date: 24.08.2011 11:18:05
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   Indicators.java

package solspb;

import com.dukascopy.api.ConnectorIndicator;
import com.dukascopy.api.DataType;
import com.dukascopy.api.Filter;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.impl.CustIndicatorWrapper;
import com.dukascopy.api.impl.IndicatorContext;
import com.dukascopy.api.impl.IndicatorHolder;
import com.dukascopy.api.impl.IndicatorWrapper;
import com.dukascopy.api.impl.RateDataIndicatorCalculationResultWraper;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.TaLibException;
import com.dukascopy.api.indicators.*;
import com.dukascopy.charts.data.datacache.CandleData;
import com.dukascopy.charts.data.datacache.Data;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;
import com.dukascopy.charts.data.datacache.IntraperiodCandlesGenerator;
import com.dukascopy.charts.math.dataprovider.AbstractDataProvider;
import com.dukascopy.charts.math.dataprovider.CandleDataSequence;
import com.dukascopy.charts.math.dataprovider.IDataSequence;
import com.dukascopy.charts.math.dataprovider.TickDataSequence;
import com.dukascopy.charts.math.indicators.IndicatorsProvider;
import com.dukascopy.dds2.greed.util.*;
import java.io.File;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Referenced classes of package com.dukascopy.api.impl:
//            TaLibException, IndicatorHolder, RateDataIndicatorCalculationResultWraper, CustIndicatorWrapper, 
//            IndicatorContext, StrategyWrapper, History, IndicatorWrapper

public class Indicators
    implements IIndicators
{
    private static interface BarsLoadable
    {

        public abstract List load(IFeedDescriptor ifeeddescriptor)
            throws JFException;
    }


    public Indicators(History history)
    {
        dailyFilterPeriod = Period.DAILY;
        this.history = history;
    }

    public Collection getAllNames()
    {
        Collection result = new LinkedHashSet();
        if(!customIndicators.isEmpty())
            result.addAll(customIndicators.keySet());
        result.addAll(IndicatorsProvider.getInstance().getAllNames());
        return result;
    }

    public Collection getGroups()
    {
        Collection result = new LinkedHashSet();
        if(!customIndicators.isEmpty())
        {
            for(Iterator i$ = customIndicators.keySet().iterator(); i$.hasNext();)
            {
                String indicatorName = (String)i$.next();
                try
                {
                    IndicatorHolder cachedIndicator = getCachedIndicator(indicatorName);
                    result.add(cachedIndicator.getIndicator().getIndicatorInfo().getName());
                    cacheIndicator(indicatorName, cachedIndicator);
                }
                catch(Exception ex)
                {
                    LOGGER.error((new StringBuilder()).append("Error while getting cached indicator : ").append(indicatorName).toString(), ex);
                }
            }

        }
        result.addAll(IndicatorsProvider.getInstance().getGroups());
        return result;
    }

    public Collection getNames(String groupName)
    {
        Collection result = new LinkedHashSet();
        if(!customIndicators.isEmpty())
        {
            for(Iterator i$ = customIndicators.keySet().iterator(); i$.hasNext();)
            {
                String indicatorName = (String)i$.next();
                try
                {
                    IndicatorHolder cachedIndicator = getCachedIndicator(indicatorName);
                    IndicatorInfo indicatorInfo = cachedIndicator.getIndicator().getIndicatorInfo();
                    if(indicatorInfo.getGroupName().equalsIgnoreCase(groupName))
                        result.add(indicatorInfo.getName());
                    cacheIndicator(indicatorName, cachedIndicator);
                }
                catch(Exception ex)
                {
                    LOGGER.error((new StringBuilder()).append("Error while getting cached indicator : ").append(indicatorName).toString(), ex);
                }
            }

        }
        return IndicatorsProvider.getInstance().getNames(groupName);
    }

    public double acos(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ACOS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] acos(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ACOS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] acos(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ACOS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] ac(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AC", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] ac(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AC", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] ac(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AC", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double ad(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "AD", null, null, shift)[0]).doubleValue();
    }

    public double[] ad(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "AD", null, null, from, to)[0];
    }

    public double[] ad(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AD", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double add(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "ADD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), shift)[0]).doubleValue();
    }

    public double[] add(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "ADD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), from, to)[0];
    }

    public double[] add(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "ADD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double adOsc(Instrument instrument, Period period, OfferSide side, int fastPeriod, int slowPeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ADOSC", null, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] adOsc(Instrument instrument, Period period, OfferSide side, int fastPeriod, int slowPeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ADOSC", null, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod)
        }, from, to)[0];
    }

    public double[] adOsc(Instrument instrument, Period period, OfferSide side, int fastPeriod, int slowPeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ADOSC", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double adx(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ADX", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] adx(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ADX", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] adx(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ADX", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double adxr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ADXR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] adxr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ADXR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] adxr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ADXR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] alligator(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, 
            int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ALLIGATOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] alligator(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, 
            long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ALLIGATOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] alligator(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ALLIGATOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double apo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "APO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, shift)[0]).doubleValue();
    }

    public double[] apo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "APO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, from, to)[0];
    }

    public double[] apo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "APO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] aroon(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "AROON", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] aroon(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "AROON", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] aroon(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AROON", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double aroonOsc(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "AROONOSC", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] aroonOsc(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "AROONOSC", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] aroonOsc(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AROONOSC", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double asin(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ASIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] asin(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ASIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] asin(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ASIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double atan(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ATAN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] atan(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ATAN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] atan(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ATAN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double atr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ATR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] atr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ATR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] atr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ATR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double avgPrice(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "AVGPRICE", null, null, shift)[0]).doubleValue();
    }

    public double[] avgPrice(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "AVGPRICE", null, null, from, to)[0];
    }

    public double[] avgPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AVGPRICE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] awesome(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, com.dukascopy.api.IIndicators.MaType fasterMaType, int slowerMaTimePeriod, 
            com.dukascopy.api.IIndicators.MaType slowerMaType, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AWESOME", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] awesome(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, com.dukascopy.api.IIndicators.MaType fasterMaType, int slowerMaTimePeriod, 
            com.dukascopy.api.IIndicators.MaType slowerMaType, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AWESOME", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] awesome(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, com.dukascopy.api.IIndicators.MaType fasterMaType, int slowerMaTimePeriod, 
            com.dukascopy.api.IIndicators.MaType slowerMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "AWESOME", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[] bbands(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDevUp, 
            double nbDevDn, com.dukascopy.api.IIndicators.MaType maType, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "BBANDS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDevUp), Double.valueOf(nbDevDn), Integer.valueOf(maType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] bbands(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDevUp, 
            double nbDevDn, com.dukascopy.api.IIndicators.MaType maType, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "BBANDS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDevUp), Double.valueOf(nbDevDn), Integer.valueOf(maType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] bbands(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDevUp, 
            double nbDevDn, com.dukascopy.api.IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BBANDS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDevUp), Double.valueOf(nbDevDn), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double beta(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int timePeriod, 
            int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "BETA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] beta(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int timePeriod, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "BETA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] beta(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int timePeriod, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "BETA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double bear(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "BEARP", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] bear(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "BEARP", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] bear(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BEARP", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double bull(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "BULLP", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] bull(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "BULLP", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] bull(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BULLP", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] bwmfi(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BWMFI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[0], shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue()
        });
    }

    public double[][] bwmfi(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BWMFI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[0], from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3]
        });
    }

    public double[][] bwmfi(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BWMFI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[0], filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3]
        });
    }

    public double bop(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "BOP", null, null, shift)[0]).doubleValue();
    }

    public double[] bop(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "BOP", null, null, from, to)[0];
    }

    public double[] bop(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BOP", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double butterworthFilter(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BUTTERWORTH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return ((Double)ret[0]).doubleValue();
    }

    public double[] butterworthFilter(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BUTTERWORTH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (double[])(double[])ret[0];
    }

    public double[] butterworthFilter(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "BUTTERWORTH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (double[])(double[])ret[0];
    }

    public double[] camPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "CAMPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue()
        });
    }

    public double[][] camPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "CAMPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double[][] camPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "CAMPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double cci(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "CCI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] cci(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "CCI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] cci(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CCI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl2Crows(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL2CROWS", null, null, shift)[0]).intValue();
    }

    public int[] cdl2Crows(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL2CROWS", null, null, from, to)[0];
    }

    public int[] cdl2Crows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL2CROWS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl3BlackCrows(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL3BLACKCROWS", null, null, shift)[0]).intValue();
    }

    public int[] cdl3BlackCrows(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL3BLACKCROWS", null, null, from, to)[0];
    }

    public int[] cdl3BlackCrows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL3BLACKCROWS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl3Inside(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL3INSIDE", null, null, shift)[0]).intValue();
    }

    public int[] cdl3Inside(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL3INSIDE", null, null, from, to)[0];
    }

    public int[] cdl3Inside(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL3INSIDE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl3LineStrike(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL3LINESTRIKE", null, null, shift)[0]).intValue();
    }

    public int[] cdl3LineStrike(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL3LINESTRIKE", null, null, from, to)[0];
    }

    public int[] cdl3LineStrike(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL3LINESTRIKE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl3Outside(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL3OUTSIDE", null, null, shift)[0]).intValue();
    }

    public int[] cdl3Outside(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL3OUTSIDE", null, null, from, to)[0];
    }

    public int[] cdl3Outside(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL3OUTSIDE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl3StarsInSouth(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL3STARSINSOUTH", null, null, shift)[0]).intValue();
    }

    public int[] cdl3StarsInSouth(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL3STARSINSOUTH", null, null, from, to)[0];
    }

    public int[] cdl3StarsInSouth(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL3STARSINSOUTH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdl3WhiteSoldiers(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDL3WHITESOLDIERS", null, null, shift)[0]).intValue();
    }

    public int[] cdl3WhiteSoldiers(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDL3WHITESOLDIERS", null, null, from, to)[0];
    }

    public int[] cdl3WhiteSoldiers(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDL3WHITESOLDIERS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlAbandonedBaby(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLABANDONEDBABY", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlAbandonedBaby(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLABANDONEDBABY", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlAbandonedBaby(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLABANDONEDBABY", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlAdvanceBlock(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLADVANCEBLOCK", null, null, shift)[0]).intValue();
    }

    public int[] cdlAdvanceBlock(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLADVANCEBLOCK", null, null, from, to)[0];
    }

    public int[] cdlAdvanceBlock(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLADVANCEBLOCK", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlBeltHold(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLBELTHOLD", null, null, shift)[0]).intValue();
    }

    public int[] cdlBeltHold(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLBELTHOLD", null, null, from, to)[0];
    }

    public int[] cdlBeltHold(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLBELTHOLD", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlBreakAway(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLBREAKAWAY", null, null, shift)[0]).intValue();
    }

    public int[] cdlBreakAway(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLBREAKAWAY", null, null, from, to)[0];
    }

    public int[] cdlBreakAway(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLBREAKAWAY", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlClosingMarubozu(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLCLOSINGMARUBOZU", null, null, shift)[0]).intValue();
    }

    public int[] cdlClosingMarubozu(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLCLOSINGMARUBOZU", null, null, from, to)[0];
    }

    public int[] cdlClosingMarubozu(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLCLOSINGMARUBOZU", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlConcealBabySwall(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLCONCEALBABYSWALL", null, null, shift)[0]).intValue();
    }

    public int[] cdlConcealBabySwall(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLCONCEALBABYSWALL", null, null, from, to)[0];
    }

    public int[] cdlConcealBabySwall(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLCONCEALBABYSWALL", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlCounterattack(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLCOUNTERATTACK", null, null, shift)[0]).intValue();
    }

    public int[] cdlCounterattack(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLCOUNTERATTACK", null, null, from, to)[0];
    }

    public int[] cdlCounterattack(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLCOUNTERATTACK", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlDarkCloudCover(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLDARKCLOUDCOVER", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlDarkCloudCover(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLDARKCLOUDCOVER", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlDarkCloudCover(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLDARKCLOUDCOVER", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlDoji(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLDOJI", null, null, shift)[0]).intValue();
    }

    public int[] cdlDoji(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLDOJI", null, null, from, to)[0];
    }

    public int[] cdlDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLDOJI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlDojiStar(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLDOJISTAR", null, null, shift)[0]).intValue();
    }

    public int[] cdlDojiStar(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLDOJISTAR", null, null, from, to)[0];
    }

    public int[] cdlDojiStar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLDOJISTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlDragonflyDoji(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLDRAGONFLYDOJI", null, null, shift)[0]).intValue();
    }

    public int[] cdlDragonflyDoji(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLDRAGONFLYDOJI", null, null, from, to)[0];
    }

    public int[] cdlDragonflyDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLDRAGONFLYDOJI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlEngulfing(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLENGULFING", null, null, shift)[0]).intValue();
    }

    public int[] cdlEngulfing(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLENGULFING", null, null, from, to)[0];
    }

    public int[] cdlEngulfing(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLENGULFING", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlEveningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLEVENINGDOJISTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlEveningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLEVENINGDOJISTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlEveningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLEVENINGDOJISTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlEveningStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLEVENINGSTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlEveningStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLEVENINGSTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlEveningStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLEVENINGSTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlGapSideSideWhite(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLGAPSIDESIDEWHITE", null, null, shift)[0]).intValue();
    }

    public int[] cdlGapSideSideWhite(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLGAPSIDESIDEWHITE", null, null, from, to)[0];
    }

    public int[] cdlGapSideSideWhite(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLGAPSIDESIDEWHITE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlGravestoneDoji(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLGRAVESTONEDOJI", null, null, shift)[0]).intValue();
    }

    public int[] cdlGravestoneDoji(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLGRAVESTONEDOJI", null, null, from, to)[0];
    }

    public int[] cdlGravestoneDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLGRAVESTONEDOJI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHammer(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHAMMER", null, null, shift)[0]).intValue();
    }

    public int[] cdlHammer(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHAMMER", null, null, from, to)[0];
    }

    public int[] cdlHammer(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHAMMER", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHangingMan(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHANGINGMAN", null, null, shift)[0]).intValue();
    }

    public int[] cdlHangingMan(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHANGINGMAN", null, null, from, to)[0];
    }

    public int[] cdlHangingMan(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHANGINGMAN", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHarami(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHARAMI", null, null, shift)[0]).intValue();
    }

    public int[] cdlHarami(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHARAMI", null, null, from, to)[0];
    }

    public int[] cdlHarami(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHARAMI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHaramiCross(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHARAMICROSS", null, null, shift)[0]).intValue();
    }

    public int[] cdlHaramiCross(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHARAMICROSS", null, null, from, to)[0];
    }

    public int[] cdlHaramiCross(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHARAMICROSS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHighWave(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHIGHWAVE", null, null, shift)[0]).intValue();
    }

    public int[] cdlHighWave(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHIGHWAVE", null, null, from, to)[0];
    }

    public int[] cdlHighWave(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHIGHWAVE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHikkake(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHIKKAKE", null, null, shift)[0]).intValue();
    }

    public int[] cdlHikkake(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHIKKAKE", null, null, from, to)[0];
    }

    public int[] cdlHikkake(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHIKKAKE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHikkakeMod(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHIKKAKEMOD", null, null, shift)[0]).intValue();
    }

    public int[] cdlHikkakeMod(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHIKKAKEMOD", null, null, from, to)[0];
    }

    public int[] cdlHikkakeMod(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHIKKAKEMOD", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlHomingPigeon(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLHOMINGPIGEON", null, null, shift)[0]).intValue();
    }

    public int[] cdlHomingPigeon(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLHOMINGPIGEON", null, null, from, to)[0];
    }

    public int[] cdlHomingPigeon(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLHOMINGPIGEON", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlIdentical3Crows(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLIDENTICAL3CROWS", null, null, shift)[0]).intValue();
    }

    public int[] cdlIdentical3Crows(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLIDENTICAL3CROWS", null, null, from, to)[0];
    }

    public int[] cdlIdentical3Crows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLIDENTICAL3CROWS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlInNeck(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLINNECK", null, null, shift)[0]).intValue();
    }

    public int[] cdlInNeck(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLINNECK", null, null, from, to)[0];
    }

    public int[] cdlInNeck(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLINNECK", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlInvertedHammer(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLINVERTEDHAMMER", null, null, shift)[0]).intValue();
    }

    public int[] cdlInvertedHammer(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLINVERTEDHAMMER", null, null, from, to)[0];
    }

    public int[] cdlInvertedHammer(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLINVERTEDHAMMER", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlKicking(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLKICKING", null, null, shift)[0]).intValue();
    }

    public int[] cdlKicking(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLKICKING", null, null, from, to)[0];
    }

    public int[] cdlKicking(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLKICKING", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlKickingByLength(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLKICKINGBYLENGTH", null, null, shift)[0]).intValue();
    }

    public int[] cdlKickingByLength(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLKICKINGBYLENGTH", null, null, from, to)[0];
    }

    public int[] cdlKickingByLength(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLKICKINGBYLENGTH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlLadderBotton(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLLADDERBOTTOM", null, null, shift)[0]).intValue();
    }

    public int[] cdlLadderBotton(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLLADDERBOTTOM", null, null, from, to)[0];
    }

    public int[] cdlLadderBotton(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLLADDERBOTTOM", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlLongLeggedDoji(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLLONGLEGGEDDOJI", null, null, shift)[0]).intValue();
    }

    public int[] cdlLongLeggedDoji(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLLONGLEGGEDDOJI", null, null, from, to)[0];
    }

    public int[] cdlLongLeggedDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLLONGLEGGEDDOJI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlLongLine(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLLONGLINE", null, null, shift)[0]).intValue();
    }

    public int[] cdlLongLine(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLLONGLINE", null, null, from, to)[0];
    }

    public int[] cdlLongLine(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLLONGLINE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlMarubozu(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLMARUBOZU", null, null, shift)[0]).intValue();
    }

    public int[] cdlMarubozu(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLMARUBOZU", null, null, from, to)[0];
    }

    public int[] cdlMarubozu(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLMARUBOZU", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlMatchingLow(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLMATCHINGLOW", null, null, shift)[0]).intValue();
    }

    public int[] cdlMatchingLow(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLMATCHINGLOW", null, null, from, to)[0];
    }

    public int[] cdlMatchingLow(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLMATCHINGLOW", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlMathold(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLMATHOLD", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlMathold(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLMATHOLD", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlMathold(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLMATHOLD", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlMorningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLMORNINGDOJISTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlMorningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLMORNINGDOJISTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlMorningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLMORNINGDOJISTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlMorningStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLMORNINGSTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, shift)[0]).intValue();
    }

    public int[] cdlMorningStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLMORNINGSTAR", null, new Object[] {
            Double.valueOf(penetration)
        }, from, to)[0];
    }

    public int[] cdlMorningStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLMORNINGSTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Double.valueOf(penetration)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlOnNeck(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLONNECK", null, null, shift)[0]).intValue();
    }

    public int[] cdlOnNeck(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLONNECK", null, null, from, to)[0];
    }

    public int[] cdlOnNeck(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLONNECK", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlPiercing(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLPIERCING", null, null, shift)[0]).intValue();
    }

    public int[] cdlPiercing(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLPIERCING", null, null, from, to)[0];
    }

    public int[] cdlPiercing(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLPIERCING", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlRickshawMan(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLRICKSHAWMAN", null, null, shift)[0]).intValue();
    }

    public int[] cdlRickshawMan(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLRICKSHAWMAN", null, null, from, to)[0];
    }

    public int[] cdlRickshawMan(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLRICKSHAWMAN", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlRiseFall3Methods(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLRISEFALL3METHODS", null, null, shift)[0]).intValue();
    }

    public int[] cdlRiseFall3Methods(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLRISEFALL3METHODS", null, null, from, to)[0];
    }

    public int[] cdlRiseFall3Methods(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLRISEFALL3METHODS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlSeparatingLines(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLSEPARATINGLINES", null, null, shift)[0]).intValue();
    }

    public int[] cdlSeparatingLines(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLSEPARATINGLINES", null, null, from, to)[0];
    }

    public int[] cdlSeparatingLines(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLSEPARATINGLINES", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlShootingStar(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLSHOOTINGSTAR", null, null, shift)[0]).intValue();
    }

    public int[] cdlShootingStar(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLSHOOTINGSTAR", null, null, from, to)[0];
    }

    public int[] cdlShootingStar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLSHOOTINGSTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlShortLine(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLSHORTLINE", null, null, shift)[0]).intValue();
    }

    public int[] cdlShortLine(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLSHORTLINE", null, null, from, to)[0];
    }

    public int[] cdlShortLine(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLSHORTLINE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlSpinningTop(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLSPINNINGTOP", null, null, shift)[0]).intValue();
    }

    public int[] cdlSpinningTop(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLSPINNINGTOP", null, null, from, to)[0];
    }

    public int[] cdlSpinningTop(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLSPINNINGTOP", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlStalledPattern(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLSTALLEDPATTERN", null, null, shift)[0]).intValue();
    }

    public int[] cdlStalledPattern(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLSTALLEDPATTERN", null, null, from, to)[0];
    }

    public int[] cdlStalledPattern(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLSTALLEDPATTERN", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlStickSandwich(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLSTICKSANDWICH", null, null, shift)[0]).intValue();
    }

    public int[] cdlStickSandwich(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLSTICKSANDWICH", null, null, from, to)[0];
    }

    public int[] cdlStickSandwich(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLSTICKSANDWICH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlTakuri(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLTAKURI", null, null, shift)[0]).intValue();
    }

    public int[] cdlTakuri(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLTAKURI", null, null, from, to)[0];
    }

    public int[] cdlTakuri(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLTAKURI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlTasukiGap(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLTASUKIGAP", null, null, shift)[0]).intValue();
    }

    public int[] cdlTasukiGap(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLTASUKIGAP", null, null, from, to)[0];
    }

    public int[] cdlTasukiGap(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLTASUKIGAP", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlThrusting(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLTHRUSTING", null, null, shift)[0]).intValue();
    }

    public int[] cdlThrusting(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLTHRUSTING", null, null, from, to)[0];
    }

    public int[] cdlThrusting(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLTHRUSTING", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlTristar(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLTRISTAR", null, null, shift)[0]).intValue();
    }

    public int[] cdlTristar(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLTRISTAR", null, null, from, to)[0];
    }

    public int[] cdlTristar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLTRISTAR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlUnique3River(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLUNIQUE3RIVER", null, null, shift)[0]).intValue();
    }

    public int[] cdlUnique3River(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLUNIQUE3RIVER", null, null, from, to)[0];
    }

    public int[] cdlUnique3River(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLUNIQUE3RIVER", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlUpsideGap2Crows(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLUPSIDEGAP2CROWS", null, null, shift)[0]).intValue();
    }

    public int[] cdlUpsideGap2Crows(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLUPSIDEGAP2CROWS", null, null, from, to)[0];
    }

    public int[] cdlUpsideGap2Crows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLUPSIDEGAP2CROWS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int cdlXsideGap3Methods(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "CDLXSIDEGAP3METHODS", null, null, shift)[0]).intValue();
    }

    public int[] cdlXsideGap3Methods(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "CDLXSIDEGAP3METHODS", null, null, from, to)[0];
    }

    public int[] cdlXsideGap3Methods(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CDLXSIDEGAP3METHODS", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ceil(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "CEIL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] ceil(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "CEIL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] ceil(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CEIL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double cmo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "CMO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] cmo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "CMO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] cmo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "CMO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] cog(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int smoothPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            int shift)
        throws JFException
    {
        Object res[] = function(instrument, period, side, "COG", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(smoothPeriod), Integer.valueOf(maType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)res[0]).doubleValue(), ((Double)res[1]).doubleValue()
        });
    }

    public double[][] cog(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int smoothPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            long from, long to)
        throws JFException
    {
        Object res[] = function(instrument, period, side, "COG", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(smoothPeriod), Integer.valueOf(maType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])res[0], (double[])(double[])res[1]
        });
    }

    public double[][] cog(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int smoothPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object res[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "COG", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(smoothPeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])res[0], (double[])(double[])res[1]
        });
    }

    public double correl(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int timePeriod, 
            int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "CORREL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] correl(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int timePeriod, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "CORREL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] correl(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int timePeriod, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "CORREL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double cos(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "COS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] cos(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "COS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] cos(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "COS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double cosh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "COSH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] cosh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "COSH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] cosh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "COSH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double dema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "DEMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] dema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "DEMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] dema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "DEMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double div(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "DIV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), shift)[0]).doubleValue();
    }

    public double[] div(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "DIV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), from, to)[0];
    }

    public double[] div(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "DIV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] dmi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object res[] = function(instrument, period, side, "DMI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)res[0]).doubleValue(), ((Double)res[1]).doubleValue(), ((Double)res[2]).doubleValue()
        });
    }

    public double[][] dmi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object res[] = function(instrument, period, side, "DMI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])res[0], (double[])(double[])res[1], (double[])(double[])res[2]
        });
    }

    public double[][] dmi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object res[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "DMI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])res[0], (double[])(double[])res[1], (double[])(double[])res[2]
        });
    }

    public double[] donchian(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "DONCHIANCHANNEL", new com.dukascopy.api.IIndicators.AppliedPrice[0], new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] donchian(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "DONCHIANCHANNEL", new com.dukascopy.api.IIndicators.AppliedPrice[0], new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] donchian(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "DONCHIANCHANNEL", new com.dukascopy.api.IIndicators.AppliedPrice[0], new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double dx(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "DX", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] dx(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "DX", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] dx(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "DX", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "EMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] ema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "EMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] ema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "EMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] emaEnvelope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, 
            int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "EMAEnvelope", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] emaEnvelope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, 
            long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "EMAEnvelope", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] emaEnvelope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "EMAEnvelope", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double exp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "EXP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] exp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "EXP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] exp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "EXP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double floor(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "FLOOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] floor(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "FLOOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] floor(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FLOOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double force(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "FORCEI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, shift);
        return ((Double)ret[0]).doubleValue();
    }

    public double[] force(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "FORCEI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, from, to);
        return (double[])(double[])ret[0];
    }

    public double[] force(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "FORCEI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (double[])(double[])ret[0];
    }

    public double[] fractal(Instrument instrument, Period period, OfferSide side, int barsOnSides, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FRACTAL", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(barsOnSides)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] fractal(Instrument instrument, Period period, OfferSide side, int barsOnSides, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FRACTAL", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(barsOnSides)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] fractal(Instrument instrument, Period period, OfferSide side, int barsOnSides, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FRACTAL", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(barsOnSides)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] fractalLines(Instrument instrument, Period period, OfferSide side, int barsOnSides, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FRACTALLINES", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(barsOnSides)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] fractalLines(Instrument instrument, Period period, OfferSide side, int barsOnSides, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FRACTALLINES", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(barsOnSides)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] fractalLines(Instrument instrument, Period period, OfferSide side, int barsOnSides, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "FRACTALLINES", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(barsOnSides)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] fibPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "FIBPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue()
        });
    }

    public double[][] fibPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "FIBPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double[][] fibPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "FIBPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double[] gator(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, 
            int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "GATOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] gator(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, 
            long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "GATOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] gator(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "GATOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] heikenAshi(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        Object ret = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HeikinAshi", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), shift)[0];
        return (double[])(double[])ret;
    }

    public double[][] heikenAshi(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HeikinAshi", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), from, to);
        double retD[][] = new double[((Object[])(Object[])ret[0]).length][];
        System.arraycopy(ret[0], 0, retD, 0, retD.length);
        return retD;
    }

    public double[][] heikenAshi(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HeikinAshi", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        double retD[][] = new double[((Object[])(Object[])ret[0]).length][];
        System.arraycopy(ret[0], 0, retD, 0, retD.length);
        return retD;
    }

    public double[] heikinAshi(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        Object ret = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HeikinAshi", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), shift)[0];
        return (double[])(double[])ret;
    }

    public double[][] heikinAshi(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HeikinAshi", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), from, to);
        double retD[][] = new double[((Object[])(Object[])ret[0]).length][];
        System.arraycopy(ret[0], 0, retD, 0, retD.length);
        return retD;
    }

    public double[][] heikinAshi(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HeikinAshi", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        double retD[][] = new double[((Object[])(Object[])ret[0]).length][];
        System.arraycopy(ret[0], 0, retD, 0, retD.length);
        return retD;
    }

    public double hma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "HMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] hma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "HMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] hma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ht_dcperiod(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "HT_DCPERIOD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] ht_dcperiod(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "HT_DCPERIOD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] ht_dcperiod(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HT_DCPERIOD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ht_dcphase(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "HT_DCPHASE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] ht_dcphase(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "HT_DCPHASE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] ht_dcphase(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HT_DCPHASE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] ht_phasor(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "HT_PHASOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] ht_phasor(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "HT_PHASOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] ht_phasor(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HT_PHASOR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] ht_sine(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "HT_SINE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] ht_sine(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "HT_SINE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] ht_sine(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HT_SINE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double ht_trendline(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "HT_TRENDLINE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] ht_trendline(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "HT_TRENDLINE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] ht_trendline(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HT_TRENDLINE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int ht_trendmode(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "HT_TRENDMODE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).intValue();
    }

    public int[] ht_trendmode(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "HT_TRENDMODE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public int[] ht_trendmode(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "HT_TRENDMODE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] ichimoku(Instrument instrument, Period period, OfferSide side, int tenkan, int kijun, int senkou, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ICHIMOKU", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(tenkan), Integer.valueOf(kijun), Integer.valueOf(senkou)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), Double.valueOf(((double[])(double[])ret[5])[0]).doubleValue(), Double.valueOf(((double[])(double[])ret[5])[1]).doubleValue()
        });
    }

    public double[][] ichimoku(Instrument instrument, Period period, OfferSide side, int tenkan, int kijun, int senkou, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ICHIMOKU", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(tenkan), Integer.valueOf(kijun), Integer.valueOf(senkou)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])((Object[])(Object[])ret[5])[0], (double[])(double[])((Object[])(Object[])ret[5])[0]
        });
    }

    public double[][] ichimoku(Instrument instrument, Period period, OfferSide side, int tenkan, int kijun, int senkou, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ICHIMOKU", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(tenkan), Integer.valueOf(kijun), Integer.valueOf(senkou)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])((Object[])(Object[])ret[5])[0], (double[])(double[])((Object[])(Object[])ret[5])[0]
        });
    }

    public double kairi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "KAIRI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, shift);
        return ((Double)ret[0]).doubleValue();
    }

    public double[] kairi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "KAIRI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, from, to);
        return (double[])(double[])ret[0];
    }

    public double[] kairi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "KAIRI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (double[])(double[])ret[0];
    }

    public double kama(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "KAMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] kama(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "KAMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] kama(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "KAMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] keltner(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "KELTNER", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] keltner(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "KELTNER", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] keltner(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "KELTNER", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double lasacs1(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int ma, double gamma, 
            int lookback, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LAGACS1", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(ma), Double.valueOf(gamma), Integer.valueOf(lookback)
        }, shift)[0]).doubleValue();
    }

    public double[] lasacs1(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int ma, double gamma, 
            int lookback, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LAGACS1", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(ma), Double.valueOf(gamma), Integer.valueOf(lookback)
        }, from, to)[0];
    }

    public double[] lasacs1(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int ma, double gamma, 
            int lookback, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LAGACS1", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(ma), Double.valueOf(gamma), Integer.valueOf(lookback)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double linearReg(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LINEARREG", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] linearReg(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LINEARREG", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] linearReg(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LINEARREG", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double linearRegAngle(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LINEARREG_ANGLE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] linearRegAngle(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LINEARREG_ANGLE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] linearRegAngle(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LINEARREG_ANGLE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double linearRegIntercept(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LINEARREG_INTERCEPT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] linearRegIntercept(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LINEARREG_INTERCEPT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] linearRegIntercept(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LINEARREG_INTERCEPT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double linearRegSlope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LINEARREG_SLOPE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] linearRegSlope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LINEARREG_SLOPE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] linearRegSlope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LINEARREG_SLOPE", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ln(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] ln(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] ln(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double log10(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LOG10", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] log10(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LOG10", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] log10(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LOG10", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double lwma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "LWMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] lwma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "LWMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] lwma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "LWMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, shift)[0]).doubleValue();
    }

    public double[] ma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, from, to)[0];
    }

    public double[] ma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, com.dukascopy.api.IIndicators.MaType maType, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] macd(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int signalPeriod, 
            int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MACD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(signalPeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] macd(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int signalPeriod, 
            long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MACD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(signalPeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] macd(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int signalPeriod, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MACD", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(signalPeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[] macdExt(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, com.dukascopy.api.IIndicators.MaType fastMaType, int slowPeriod, 
            com.dukascopy.api.IIndicators.MaType slowMaType, int signalPeriod, com.dukascopy.api.IIndicators.MaType signalMaType, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MACDEXT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(fastMaType.ordinal()), Integer.valueOf(slowPeriod), Integer.valueOf(slowMaType.ordinal()), Integer.valueOf(signalPeriod), Integer.valueOf(signalMaType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] macdExt(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, com.dukascopy.api.IIndicators.MaType fastMaType, int slowPeriod, 
            com.dukascopy.api.IIndicators.MaType slowMaType, int signalPeriod, com.dukascopy.api.IIndicators.MaType signalMaType, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MACDEXT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(fastMaType.ordinal()), Integer.valueOf(slowPeriod), Integer.valueOf(slowMaType.ordinal()), Integer.valueOf(signalPeriod), Integer.valueOf(signalMaType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] macdExt(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, com.dukascopy.api.IIndicators.MaType fastMaType, int slowPeriod, 
            com.dukascopy.api.IIndicators.MaType slowMaType, int signalPeriod, com.dukascopy.api.IIndicators.MaType signalMaType, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MACDEXT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(fastMaType.ordinal()), Integer.valueOf(slowPeriod), Integer.valueOf(slowMaType.ordinal()), Integer.valueOf(signalPeriod), Integer.valueOf(signalMaType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[] macdFix(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int signalPeriod, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MACDFIX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(signalPeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] macdFix(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int signalPeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MACDFIX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(signalPeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] macdFix(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int signalPeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MACDFIX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(signalPeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[] maEnvelope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, 
            int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MAEnvelope", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] maEnvelope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, 
            long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MAEnvelope", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] maEnvelope(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MAEnvelope", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] mama(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, double fastLimit, double slowLimit, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MAMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Double.valueOf(fastLimit), Double.valueOf(slowLimit)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] mama(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, double fastLimit, double slowLimit, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MAMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Double.valueOf(fastLimit), Double.valueOf(slowLimit)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] mama(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, double fastLimit, double slowLimit, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MAMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Double.valueOf(fastLimit), Double.valueOf(slowLimit)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public Object[] wsmTime(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return function(instrument, period, side, "WSMTIME", new com.dukascopy.api.IIndicators.AppliedPrice[0], new Object[0], shift);
    }

    public Object[] wsmTime(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return function(instrument, period, side, "WSMTIME", new com.dukascopy.api.IIndicators.AppliedPrice[0], new Object[0], from, to);
    }

    public Object[] wsmTime(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WSMTIME", new com.dukascopy.api.IIndicators.AppliedPrice[0], new Object[0], filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
    }

    public double mavp(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int minPeriod, 
            int maxPeriod, com.dukascopy.api.IIndicators.MaType maType, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "MAVP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(minPeriod), Integer.valueOf(maxPeriod), Integer.valueOf(maType.ordinal())
        }, shift)[0]).doubleValue();
    }

    public double[] mavp(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int minPeriod, 
            int maxPeriod, com.dukascopy.api.IIndicators.MaType maType, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "MAVP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(minPeriod), Integer.valueOf(maxPeriod), Integer.valueOf(maType.ordinal())
        }, from, to)[0];
    }

    public double[] mavp(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int minPeriod, 
            int maxPeriod, com.dukascopy.api.IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "MAVP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, new Object[] {
            Integer.valueOf(minPeriod), Integer.valueOf(maxPeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double max(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MAX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] max(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MAX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] max(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MAX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int maxIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "MAXINDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).intValue();
    }

    public int[] maxIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "MAXINDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public int[] maxIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MAXINDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double medPrice(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MEDPRICE", null, null, shift)[0]).doubleValue();
    }

    public double[] medPrice(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MEDPRICE", null, null, from, to)[0];
    }

    public double[] medPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MEDPRICE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double mfi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MFI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] mfi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MFI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] mfi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MFI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double midPoint(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MIDPOINT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] midPoint(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MIDPOINT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] midPoint(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MIDPOINT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double midPrice(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MIDPRICE", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] midPrice(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MIDPRICE", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] midPrice(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MIDPRICE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double min(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] min(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] min(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public int minIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Integer)function(instrument, period, side, "MININDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).intValue();
    }

    public int[] minIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (int[])(int[])function(instrument, period, side, "MININDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public int[] minIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (int[])(int[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MININDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] minMax(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MINMAX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] minMax(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MINMAX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] minMax(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MINMAX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public int[] minMaxIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MINMAXINDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new int[] {
            ((Integer)ret[0]).intValue(), ((Integer)ret[1]).intValue()
        });
    }

    public int[][] minMaxIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "MINMAXINDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new int[][] {
            (int[])(int[])ret[0], (int[])(int[])ret[1]
        });
    }

    public int[][] minMaxIndex(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MINMAXINDEX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new int[][] {
            (int[])(int[])ret[0], (int[])(int[])ret[1]
        });
    }

    public double minusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MINUS_DI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] minusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MINUS_DI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] minusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MINUS_DI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double minusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MINUS_DM", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] minusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MINUS_DM", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] minusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MINUS_DM", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double mom(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "MOM", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] mom(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "MOM", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] mom(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MOM", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double mult(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "MULT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), shift)[0]).doubleValue();
    }

    public double[] mult(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "MULT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), from, to)[0];
    }

    public double[] mult(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "MULT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] murrey(Instrument instrument, Period period, OfferSide side, int nPeriod, int timePeriod, int stepBack, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MURRCH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(nPeriod), Integer.valueOf(timePeriod), Integer.valueOf(stepBack)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue(), ((Double)ret[7]).doubleValue(), ((Double)ret[8]).doubleValue(), ((Double)ret[9]).doubleValue(), 
            ((Double)ret[10]).doubleValue(), ((Double)ret[11]).doubleValue(), ((Double)ret[12]).doubleValue()
        });
    }

    public double[][] murrey(Instrument instrument, Period period, OfferSide side, int nPeriod, int timePeriod, int stepBack, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MURRCH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(nPeriod), Integer.valueOf(timePeriod), Integer.valueOf(stepBack)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6], (double[])(double[])ret[7], (double[])(double[])ret[8], (double[])(double[])ret[9], 
            (double[])(double[])ret[10], (double[])(double[])ret[11], (double[])(double[])ret[12]
        });
    }

    public double[][] murrey(Instrument instrument, Period period, OfferSide side, int nPeriod, int timePeriod, int stepBack, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "MURRCH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(nPeriod), Integer.valueOf(timePeriod), Integer.valueOf(stepBack)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6], (double[])(double[])ret[7], (double[])(double[])ret[8], (double[])(double[])ret[9], 
            (double[])(double[])ret[10], (double[])(double[])ret[11], (double[])(double[])ret[12]
        });
    }

    public double natr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "NATR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] natr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "NATR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] natr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "NATR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double obv(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, OfferSide sideForPriceV, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side, sideForPriceV
        }, "OBV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), shift)[0]).doubleValue();
    }

    public double[] obv(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, OfferSide sideForPriceV, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side, sideForPriceV
        }, "OBV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), from, to)[0];
    }

    public double[] obv(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, OfferSide sideForPriceV, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side, sideForPriceV
        }, "OBV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double osma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fast_ema_period, int slow_ema_period, int signal_period, 
            int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "OsMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fast_ema_period), Integer.valueOf(slow_ema_period), Integer.valueOf(signal_period)
        }, shift);
        return ((Double)ret[0]).doubleValue();
    }

    public double[] osma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fast_ema_period, int slow_ema_period, int signal_period, 
            long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "OsMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fast_ema_period), Integer.valueOf(slow_ema_period), Integer.valueOf(signal_period)
        }, from, to);
        return (double[])(double[])ret[0];
    }

    public double[] osma(Instrument instrument, Period period, OfferSide side, int fast_ema_period, int slow_ema_period, int signal_period, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "OsMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fast_ema_period), Integer.valueOf(slow_ema_period), Integer.valueOf(signal_period)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (double[])(double[])ret[0];
    }

    public double[] pivot(Instrument instrument, Period period, OfferSide side, int timePeriod, boolean showHistoricalLevels, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "PIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod), Boolean.valueOf(showHistoricalLevels)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue()
        });
    }

    public double[][] pivot(Instrument instrument, Period period, OfferSide side, int timePeriod, boolean showHistoricalLevels, long from, 
            long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "PIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod), Boolean.valueOf(showHistoricalLevels)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double[][] pivot(Instrument instrument, Period period, OfferSide side, int timePeriod, boolean showHistoricalLevels, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "PIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod), Boolean.valueOf(showHistoricalLevels)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double plusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "PLUS_DI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] plusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "PLUS_DI", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] plusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "PLUS_DI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double plusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "PLUS_DM", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] plusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "PLUS_DM", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] plusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "PLUS_DM", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ppo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "PPO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, shift)[0]).doubleValue();
    }

    public double[] ppo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "PPO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, from, to)[0];
    }

    public double[] ppo(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "PPO", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double prchannel(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "PCHANNEL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, shift)[0]).doubleValue();
    }

    public double[] prchannel(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "PCHANNEL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, from, to)[0];
    }

    public double[] prchannel(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, com.dukascopy.api.IIndicators.MaType maType, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "PCHANNEL", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double rci(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "RCI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] rci(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "RCI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] rci(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "RCI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double rmi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int momentumPeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "RMI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(momentumPeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] rmi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int momentumPeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "RMI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(momentumPeriod)
        }, from, to)[0];
    }

    public double[] rmi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int momentumPeriod, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "RMI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(momentumPeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double roc(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ROC", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] roc(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ROC", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] roc(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ROC", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double rocp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ROCP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] rocp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ROCP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] rocp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ROCP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double rocr(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ROCR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] rocr(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ROCR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] rocr(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ROCR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double rocr100(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ROCR100", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] rocr100(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ROCR100", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] rocr100(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ROCR100", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double rsi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "RSI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] rsi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "RSI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] rsi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "RSI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] rvi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "RVI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] rvi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "RVI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] rvi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "RVI", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] sar(Instrument instrument, Period period, OfferSide side, double acceleration, double maximum, 
            long from, long to)
        throws JFException
    {
        double values[] = sarExt(instrument, period, side, 0.0D, 0.0D, acceleration, acceleration, maximum, acceleration, acceleration, maximum, from, to);
        for(int i = 0; i < values.length; i++)
        {
            double value = values[i];
            if(value < 0.0D)
                values[i] = -value;
        }

        return values;
    }

    public double[] sar(Instrument instrument, Period period, OfferSide side, double acceleration, double maximum, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        double values[] = sarExt(instrument, period, side, 0.0D, 0.0D, acceleration, acceleration, maximum, acceleration, acceleration, maximum, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        for(int i = 0; i < values.length; i++)
        {
            double value = values[i];
            if(value < 0.0D)
                values[i] = -value;
        }

        return values;
    }

    public double[] sarExt(Instrument instrument, Period period, OfferSide side, double startValue, double offsetOnReverse, 
            double accelerationInitLong, double accelerationLong, double accelerationMaxLong, double accelerationInitShort, double accelerationShort, double accelerationMaxShort, long from, 
            long to)
        throws JFException
    {
        IndicatorHolder indicatorHolder;
        checkSide(period, new OfferSide[] {
            side
        });
        checkIntervalValid(period, from, to);
        indicatorHolder = getCachedIndicator("SAREXT");
        double ad1[];
        IIndicator indicator = indicatorHolder.getIndicator();
        setOptParams(indicator, new Object[] {
            Double.valueOf(startValue), Double.valueOf(offsetOnReverse), Double.valueOf(accelerationInitLong), Double.valueOf(accelerationLong), Double.valueOf(accelerationMaxLong), Double.valueOf(accelerationInitShort), Double.valueOf(accelerationShort), Double.valueOf(accelerationMaxShort)
        });
        int indicatorLookback;
        int lookback = indicatorLookback = indicator.getLookback();
        boolean hasPeriodChange = false;
        int previousLength = 0;
        double doubles[] = new double[0];
        IndicatorResult result = null;
        do
        {
            lookback += lookback * 4 >= 100 ? lookback * 4 : 100;
            double inputData[][] = null;
            try {
                inputData = (double[][])(double[][])getInputData(instrument, period, side, com.dukascopy.api.indicators.InputParameterInfo.Type.PRICE, null, from, to, lookback, 0, false, 0);
            } catch (DataCacheException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            int length = inputData[0].length;
            if(length <= previousLength)
                break;
            previousLength = length;
            indicator.setInputParameter(0, inputData);
            doubles = new double[length - indicatorLookback];
            indicator.setOutputParameter(0, doubles);
            result = indicator.calculate(0, length - 1);
            if(result.getNumberOfElements() == 0)
                break;
            boolean positive = doubles[0] > 0.0D;
            int i = 1;
            do
            {
                if(i >= result.getNumberOfElements())
                    break;
                if((doubles[i] > 0.0D) != positive)
                {
                    hasPeriodChange = true;
                    break;
                }
                i++;
            } while(true);
        } while(!hasPeriodChange);
        int outCount = DataCacheUtils.getCandlesCountBetweenFast(period != Period.TICK ? period : Period.ONE_SEC, from, to) <= result.getNumberOfElements() ? DataCacheUtils.getCandlesCountBetweenFast(period != Period.TICK ? period : Period.ONE_SEC, from, to) : result.getNumberOfElements();
        double retDoubles[] = new double[outCount];
        System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
        ad1 = retDoubles;
        cacheIndicator("SAREXT", indicatorHolder);
        return ad1;
    }

    public double[] sarExt(Instrument instrument, Period period, OfferSide side, double startValue, double offsetOnReverse, 
            double accelerationInitLong, double accelerationLong, double accelerationMaxLong, double accelerationInitShort, double accelerationShort, double accelerationMaxShort, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        IndicatorHolder indicatorHolder;
        checkNotTick(period);
        checkSide(period, side);
        checkIntervalValid(numberOfCandlesBefore, numberOfCandlesAfter);
        indicatorHolder = getCachedIndicator("SAREXT");
        double ad1[];
        IIndicator indicator = indicatorHolder.getIndicator();
        setOptParams(indicator, new Object[] {
            Double.valueOf(startValue), Double.valueOf(offsetOnReverse), Double.valueOf(accelerationInitLong), Double.valueOf(accelerationLong), Double.valueOf(accelerationMaxLong), Double.valueOf(accelerationInitShort), Double.valueOf(accelerationShort), Double.valueOf(accelerationMaxShort)
        });
        int indicatorLookback;
        int lookback = indicatorLookback = indicator.getLookback();
        boolean hasPeriodChange = false;
        int previousLength = 0;
        double doubles[] = new double[0];
        IndicatorResult result = null;
        do
        {
            lookback += lookback * 4 >= 100 ? lookback * 4 : 100;
            double inputData[][] = null;
            try {
                inputData = (double[][])(double[][])getInputData(instrument, period, side, com.dukascopy.api.indicators.InputParameterInfo.Type.PRICE, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, lookback, 0, 0);
            } catch (DataCacheException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            int length = inputData[0].length;
            if(length <= previousLength)
                break;
            previousLength = length;
            indicator.setInputParameter(0, inputData);
            doubles = new double[length - indicatorLookback];
            indicator.setOutputParameter(0, doubles);
            result = indicator.calculate(0, length - 1);
            if(result.getNumberOfElements() == 0)
                break;
            boolean positive = doubles[0] > 0.0D;
            int i = 1;
            do
            {
                if(i >= result.getNumberOfElements())
                    break;
                if((doubles[i] > 0.0D) != positive)
                {
                    hasPeriodChange = true;
                    break;
                }
                i++;
            } while(true);
        } while(!hasPeriodChange);
        int outCount = numberOfCandlesBefore + numberOfCandlesAfter <= result.getNumberOfElements() ? numberOfCandlesBefore + numberOfCandlesAfter : result.getNumberOfElements();
        double retDoubles[] = new double[outCount];
        System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
        ad1 = retDoubles;
        cacheIndicator("SAREXT", indicatorHolder);
        return ad1;
    }

    public double sin(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "SIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] sin(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "SIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] sin(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "SIN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double sinh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "SINH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] sinh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "SINH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] sinh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "SINH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double sma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "SMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] sma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "SMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] sma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "SMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double smma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "SMMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] smma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "SMMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] smma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "SMMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double sqrt(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "SQRT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] sqrt(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "SQRT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] sqrt(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "SQRT", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double stdDev(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, 
            int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "STDDEV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDev)
        }, shift)[0]).doubleValue();
    }

    public double[] stdDev(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "STDDEV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDev)
        }, from, to)[0];
    }

    public double[] stdDev(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "STDDEV", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDev)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] stoch(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int slowKPeriod, com.dukascopy.api.IIndicators.MaType slowKMaType, int slowDPeriod, 
            com.dukascopy.api.IIndicators.MaType slowDMaType, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "STOCH", null, new Object[] {
            Integer.valueOf(fastKPeriod), Integer.valueOf(slowKPeriod), Integer.valueOf(slowKMaType.ordinal()), Integer.valueOf(slowDPeriod), Integer.valueOf(slowDMaType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] stoch(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int slowKPeriod, com.dukascopy.api.IIndicators.MaType slowKMaType, int slowDPeriod, 
            com.dukascopy.api.IIndicators.MaType slowDMaType, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "STOCH", null, new Object[] {
            Integer.valueOf(fastKPeriod), Integer.valueOf(slowKPeriod), Integer.valueOf(slowKMaType.ordinal()), Integer.valueOf(slowDPeriod), Integer.valueOf(slowDMaType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] stoch(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int slowKPeriod, com.dukascopy.api.IIndicators.MaType slowKMaType, int slowDPeriod, 
            com.dukascopy.api.IIndicators.MaType slowDMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "STOCH", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(fastKPeriod), Integer.valueOf(slowKPeriod), Integer.valueOf(slowKMaType.ordinal()), Integer.valueOf(slowDPeriod), Integer.valueOf(slowDMaType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] stochF(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int fastDPeriod, com.dukascopy.api.IIndicators.MaType fastDMaType, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "STOCHF", null, new Object[] {
            Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] stochF(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int fastDPeriod, com.dukascopy.api.IIndicators.MaType fastDMaType, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "STOCHF", null, new Object[] {
            Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] stochF(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int fastDPeriod, com.dukascopy.api.IIndicators.MaType fastDMaType, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "STOCHF", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[] stochRsi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int fastKPeriod, int fastDPeriod, 
            com.dukascopy.api.IIndicators.MaType fastDMaType, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "STOCHRSI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal())
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] stochRsi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int fastKPeriod, int fastDPeriod, 
            com.dukascopy.api.IIndicators.MaType fastDMaType, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "STOCHRSI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal())
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] stochRsi(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int fastKPeriod, int fastDPeriod, 
            com.dukascopy.api.IIndicators.MaType fastDMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "STOCHRSI", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal())
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double sub(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "SUB", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), shift)[0]).doubleValue();
    }

    public double[] sub(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "SUB", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), from, to)[0];
    }

    public double[] sub(Instrument instrument, Period period, OfferSide side1, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice1, OfferSide side2, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice2, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side1, side2
        }, "SUB", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice1, appliedPrice2
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double sum(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "SUM", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] sum(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "SUM", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] sum(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "SUM", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] supportResistance(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "S&R", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue()
        });
    }

    public double[][] supportResistance(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "S&R", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double[][] supportResistance(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "S&R", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1]
        });
    }

    public double t3(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double factor, 
            int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "T3", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(factor)
        }, shift)[0]).doubleValue();
    }

    public double[] t3(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double factor, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "T3", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(factor)
        }, from, to)[0];
    }

    public double[] t3(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double factor, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "T3", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(factor)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double tan(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TAN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] tan(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TAN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] tan(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TAN", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double tanh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TANH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, shift)[0]).doubleValue();
    }

    public double[] tanh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TANH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, null, from, to)[0];
    }

    public double[] tanh(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TANH", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] tbp(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "THRUSTBAR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), shift);
        return (new double[] {
            (double)((Integer)ret[0]).intValue(), (double)((Integer)ret[1]).intValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue()
        });
    }

    public double[] tbop(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "THRUSTOUTSIDEBAR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, ((Object []) (null)), shift);
        return (new double[] {
            (double)((Integer)ret[0]).intValue(), (double)((Integer)ret[1]).intValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue()
        });
    }

    public double[] td_i(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "TD_I", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue()
        });
    }

    public double[][] td_i(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "TD_I", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public double[][] td_i(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TD_I", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2]
        });
    }

    public int[] td_s(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "TD_S", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new int[] {
            ((Integer)ret[0]).intValue(), ((Integer)ret[1]).intValue()
        });
    }

    public int[][] td_s(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = function(instrument, period, side, "TD_S", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new int[][] {
            (int[])(int[])ret[0], (int[])(int[])ret[1]
        });
    }

    public int[][] td_s(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TD_S", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new int[][] {
            (int[])(int[])ret[0], (int[])(int[])ret[1]
        });
    }

    public double tema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TEMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] tema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TEMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] tema(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TEMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double trange(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TRANGE", null, null, shift)[0]).doubleValue();
    }

    public double[] trange(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TRANGE", null, null, from, to)[0];
    }

    public double[] trange(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TRANGE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] trendEnv(Instrument instrument, Period period, OfferSide side, int timePeriod, double deviation, int shift)
        throws JFException
    {
        Object res[] = function(instrument, period, side, "TrendEnvelopes", null, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, shift);
        return (new double[] {
            ((Double)res[0]).doubleValue(), ((Double)res[1]).doubleValue()
        });
    }

    public double[][] trendEnv(Instrument instrument, Period period, OfferSide side, int timePeriod, double deviation, long from, long to)
        throws JFException
    {
        Object res[] = function(instrument, period, side, "TrendEnvelopes", null, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, from, to);
        return (new double[][] {
            (double[])(double[])res[0], (double[])(double[])res[1]
        });
    }

    public double[][] trendEnv(Instrument instrument, Period period, OfferSide side, int timePeriod, double deviation, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object res[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TrendEnvelopes", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(deviation)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])res[0], (double[])(double[])res[1]
        });
    }

    public double trima(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TRIMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] trima(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TRIMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] trima(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TRIMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double trix(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TRIX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] trix(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TRIX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] trix(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TRIX", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double tsf(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TSF", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] tsf(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TSF", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] tsf(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TSF", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double tvs(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TVS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] tvs(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TVS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] tvs(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TVS", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double typPrice(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "TYPPRICE", null, null, shift)[0]).doubleValue();
    }

    public double[] typPrice(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "TYPPRICE", null, null, from, to)[0];
    }

    public double[] typPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "TYPPRICE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double ultOsc(Instrument instrument, Period period, OfferSide side, int timePeriod1, int timePeriod2, int timePeriod3, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "ULTOSC", null, new Object[] {
            Integer.valueOf(timePeriod1), Integer.valueOf(timePeriod2), Integer.valueOf(timePeriod3)
        }, shift)[0]).doubleValue();
    }

    public double[] ultOsc(Instrument instrument, Period period, OfferSide side, int timePeriod1, int timePeriod2, int timePeriod3, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "ULTOSC", null, new Object[] {
            Integer.valueOf(timePeriod1), Integer.valueOf(timePeriod2), Integer.valueOf(timePeriod3)
        }, from, to)[0];
    }

    public double[] ultOsc(Instrument instrument, Period period, OfferSide side, int timePeriod1, int timePeriod2, int timePeriod3, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ULTOSC", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod1), Integer.valueOf(timePeriod2), Integer.valueOf(timePeriod3)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double var(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, 
            int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "VAR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDev)
        }, shift)[0]).doubleValue();
    }

    public double[] var(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, 
            long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "VAR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDev)
        }, from, to)[0];
    }

    public double[] var(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, 
            Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "VAR", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod), Double.valueOf(nbDev)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double volume(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "Volume", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), shift);
        return ((Double)ret[0]).doubleValue();
    }

    public double[] volume(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "Volume", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), from, to);
        return (double[])(double[])ret[0];
    }

    public double[] volume(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "Volume", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (double[])(double[])ret[0];
    }

    public double volumeWAP(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "VolumeWAP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return ((Double)ret[0]).doubleValue();
    }

    public double[] volumeWAP(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "VolumeWAP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (double[])(double[])ret[0];
    }

    public double[] volumeWAP(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "VolumeWAP", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice, appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (double[])(double[])ret[0];
    }

    public double waddahAttar(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WADDAHAT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), shift)[0]).doubleValue();
    }

    public double[] waddahAttar(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WADDAHAT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), from, to)[0];
    }

    public double[] waddahAttar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WADDAHAT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double wclPrice(Instrument instrument, Period period, OfferSide side, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "WCLPRICE", null, null, shift)[0]).doubleValue();
    }

    public double[] wclPrice(Instrument instrument, Period period, OfferSide side, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "WCLPRICE", null, null, from, to)[0];
    }

    public double[] wclPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, 
            int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WCLPRICE", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), ((Object []) (null)), filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double willr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "WILLR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] willr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "WILLR", null, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] willr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WILLR", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double wma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift)
        throws JFException
    {
        return ((Double)function(instrument, period, side, "WMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, shift)[0]).doubleValue();
    }

    public double[] wma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, 
            long to)
        throws JFException
    {
        return (double[])(double[])function(instrument, period, side, "WMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to)[0];
    }

    public double[] wma(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, 
            long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "WMA", new com.dukascopy.api.IIndicators.AppliedPrice[] {
            appliedPrice
        }, new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    public double[] woodPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "WOODPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, shift);
        return (new double[] {
            ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue()
        });
    }

    public double[][] woodPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "WOODPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, from, to);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double[][] woodPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        Object ret[] = calculateIndicator(instrument, period, new OfferSide[] {
            side, side
        }, "WOODPIVOT", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(timePeriod)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return (new double[][] {
            (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6]
        });
    }

    public double zigzag(Instrument instrument, Period period, OfferSide side, int extDepth, int extDeviation, int extBackstep, int shift)
        throws JFException
    {
        return ((Double)calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ZIGZAG", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(extDepth), Integer.valueOf(extDeviation), Integer.valueOf(extBackstep)
        }, shift)[0]).doubleValue();
    }

    public double[] zigzag(Instrument instrument, Period period, OfferSide side, int extDepth, int extDeviation, int extBackstep, long from, long to)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ZIGZAG", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(extDepth), Integer.valueOf(extDeviation), Integer.valueOf(extBackstep)
        }, from, to)[0];
    }

    public double[] zigzag(Instrument instrument, Period period, OfferSide side, int extDepth, int extDeviation, int extBackstep, Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        return (double[])(double[])calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, "ZIGZAG", ((com.dukascopy.api.IIndicators.AppliedPrice []) (null)), new Object[] {
            Integer.valueOf(extDepth), Integer.valueOf(extDeviation), Integer.valueOf(extBackstep)
        }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0];
    }

    protected IndicatorHolder getCachedIndicator(String name)
        throws JFException
    {
        IndicatorHolder indicatorHolder = (IndicatorHolder)cachedIndicators.remove(name);
        if(indicatorHolder == null)
        {
            if(indicatorHolder == null)
                indicatorHolder = IndicatorsProvider.getInstance().getIndicatorHolder(name);
        }
        return indicatorHolder;
    }

    protected void cacheIndicator(String name, IndicatorHolder indicator)
    {
        cachedIndicators.put(name, indicator);
    }

    private Object[] function(Instrument instrument, Period period, OfferSide side, String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], int shift)
        throws JFException
    {
        return calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, functionName, inputTypes, optParams, shift);
    }

    public Object[] calculateIndicator(Instrument instrument, Period period, OfferSide side[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], int shift)
        throws JFException
    {
        IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
        try {
            Object aobj[];
            checkNotTick(period);
            checkSide(period, side);
            checkShiftPositive(shift);
            if(indicatorHolder == null)
                throw new JFException((new StringBuilder()).append("Indicator with name ").append(functionName).append(" was not found").toString());
            int lookSides[] = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
            aobj = calculateIndicator(instrument, period, side, inputTypes, shift, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
            if(indicatorHolder != null)
            {
                indicatorHolder.getIndicatorContext().setChartInfo(null, null, null);
                cacheIndicator(functionName, indicatorHolder);
            }
            return aobj;
        }
        catch (Exception e) {
            if(indicatorHolder != null)
            {
                indicatorHolder.getIndicatorContext().setChartInfo(null, null, null);
                cacheIndicator(functionName, indicatorHolder);
            }
        throw new JFException(e);
        }
    }

    protected int[] calculateLookbackLookforward(IIndicator indicator, Object optParams[])
        throws JFException
    {
        setOptParams(indicator, optParams);
        int lookback;
        int indicatorLookback;
        try
        {
            lookback = indicatorLookback = indicator.getLookback();
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        int lookforward;
        try
        {
            lookforward = indicator.getLookforward();
        }
        catch(AbstractMethodError e)
        {
            lookforward = 0;
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        if(indicatorInfo.isUnstablePeriod())
            lookback += lookback * 4 >= 100 ? lookback * 4 : 100;
        return (new int[] {
            indicatorLookback, lookback, lookforward
        });
    }

    protected Object[] calculateIndicator(Instrument instrument, Period period, OfferSide side[], com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], int shift, IndicatorHolder indicatorHolder, int indicatorLookback, 
            int lookback, int lookforward)
        throws JFException, DataCacheException
    {
        int inputLength = 0x80000000;
        IIndicator indicator = indicatorHolder.getIndicator();
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
        indicatorContext.setChartInfo(instrument, period, indicatorInfo.getNumberOfInputs() <= 0 ? OfferSide.BID : side[0]);
        int i = 0;
        for(int j = indicatorInfo.getNumberOfInputs(); i < j; i++)
        {
            InputParameterInfo info = indicator.getInputParameterInfo(i);
            Instrument currentInstrument = info.getInstrument() != null ? info.getInstrument() : instrument;
            Period currentPeriod = info.getPeriod() != null ? info.getPeriod() : period;
            OfferSide currentSide = info.getOfferSide() != null ? info.getOfferSide() : side[i];
            Object inputData;
            if(info.getInstrument() == null && info.getPeriod() == null)
            {
                inputData = getInputData(currentInstrument, currentPeriod, currentSide, info.getType(), info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i], shift, lookback, lookforward);
            } else
            {
                if(period.getInterval() > currentPeriod.getInterval())
                    throw new JFException((new StringBuilder()).append("Indicator [").append(indicatorInfo.getName()).append("] cannot be run with periods longer than ").append(currentPeriod).toString());
                int currentShift = calculateShift(instrument, currentPeriod, period, shift);
                int currentLookforward = currentShift >= 0 ? currentShift < lookforward ? currentShift : lookforward : 0;
                inputData = getInputData(currentInstrument, currentPeriod, currentSide, info.getType(), info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i], currentShift, lookback, currentLookforward);
            }
            if(inputData == null)
            {
                Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
                int k = 0;
                for(int l = indicatorInfo.getNumberOfOutputs(); k < l; k++)
                {
                    OutputParameterInfo outInfo = indicator.getOutputParameterInfo(k);

                    switch(outInfo.getType().ordinal())
                    {
                    case 1: // '\001'
                        ret[k] = Double.valueOf((0.0D / 0.0D));
                        break;

                    case 2: // '\002'
                        ret[k] = Integer.valueOf(0x80000000);
                        break;

                    case 3: // '\003'
                        ret[k] = null;
                        break;
                    }
                }

                return ret;
            }
            if(info.getInstrument() == null && info.getPeriod() == null)
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.PRICE)
                    inputLength = ((double[][])(double[][])inputData)[0].length;
                else
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE)
                    inputLength = ((double[])(double[])inputData).length;
                else
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.BAR)
                    inputLength = ((IBar[])(IBar[])inputData).length;
            try
            {
                indicator.setInputParameter(i, inputData);
            }
            catch(Throwable t)
            {
                LOGGER.error(t.getMessage(), t);
                String error = StrategyWrapper.representError(indicator, t);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                throw new JFException(t);
            }
        }

        if(inputLength == 0x80000000)
            if(indicatorInfo.getNumberOfInputs() == 0)
            {
                inputLength = 0;
            } else
            {
                Object inputData = getInputData(instrument, period, OfferSide.BID, com.dukascopy.api.indicators.InputParameterInfo.Type.BAR, null, shift, lookback, lookforward);
                inputLength = ((IBar[])(IBar[])inputData).length;
            }
        return calculateIndicator(indicator, indicatorLookback, lookback, lookforward, inputLength);
    }

    protected int calculateShift(Instrument instrument, Period currentPeriod, Period period, int shift)
        throws JFException
    {
        if(period == currentPeriod)
            return shift;
        if(shift == 0)
            return 0;
        long currentBarStartTime = history.getStartTimeOfCurrentBar(instrument, period);
        long requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, DataCacheUtils.getPreviousCandleStartFast(period, currentBarStartTime), shift);
        currentBarStartTime = history.getStartTimeOfCurrentBar(instrument, currentPeriod);
        requestedBarStartTime = DataCacheUtils.getCandleStartFast(currentPeriod, requestedBarStartTime);
        if(requestedBarStartTime <= currentBarStartTime)
            return DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, requestedBarStartTime, currentBarStartTime) - 1;
        else
            return -(DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentBarStartTime, requestedBarStartTime) - 1);
    }

    protected Object[] calculateIndicator(IIndicator indicator, int indicatorLookback, int lookback, int lookforward, int inputLength)
        throws JFException
    {
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        int i;
        if(inputLength <= indicatorLookback + lookforward)
        {
            LOGGER.warn("There is no enough data to calculate value");
            Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
            i = 0;
            for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
            {
                OutputParameterInfo info = indicator.getOutputParameterInfo(i);
                switch(info.getType().ordinal())
                {
                case 1: // '\001'
                    ret[i] = Double.valueOf((0.0D / 0.0D));
                    break;

                case 2: // '\002'
                    ret[i] = Integer.valueOf(0x80000000);
                    break;

                case 3: // '\003'
                    ret[i] = null;
                    break;
                }
            }

            return ret;
        }
        Object outputs[] = new Object[indicatorInfo.getNumberOfOutputs()];
        i = 0;
        for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
        {
            OutputParameterInfo info = indicator.getOutputParameterInfo(i);
            switch(info.getType().ordinal())
            {
            default:
                break;

            case 1: // '\001'
                double doubles[] = new double[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = doubles;
                try
                {
                    indicator.setOutputParameter(i, doubles);
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }
                break;

            case 2: // '\002'
                int ints[] = new int[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = ints;
                try
                {
                    indicator.setOutputParameter(i, ints);
                    break;
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }

            case 3: // '\003'
                Object objects[] = new Object[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = ((Object) (objects));
                try
                {
                    indicator.setOutputParameter(i, ((Object) (objects)));
                    break;
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }
            }
        }

        IndicatorResult result;
        try
        {
            result = indicator.calculate(0, inputLength - 1);
        }
        catch(TaLibException e)
        {
            Throwable t = e.getCause();
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        if(result.getLastValueIndex() == 0x80000000 && result.getNumberOfElements() != 0)
        {
            if(lookforward != 0)
            {
                String error = (new StringBuilder()).append("calculate() method of indicator [").append(indicator.getIndicatorInfo().getName()).append("] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero").toString();
                LOGGER.error(error);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), true);
                throw new JFException(error);
            }
            result.setLastValueIndex(inputLength - 1);
        }
        if((result.getLastValueIndex() + 1) - result.getFirstValueIndex() < inputLength - lookback - lookforward || result.getNumberOfElements() < inputLength - lookback - lookforward)
        {
            String error = (new StringBuilder()).append("calculate() method of indicator [").append(indicator.getIndicatorInfo().getName()).append("] returned less values than expected").toString();
            LOGGER.error(error);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), true);
            throw new JFException(error);
        }
        Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
        i = 0;
        for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
        {
            OutputParameterInfo info = indicator.getOutputParameterInfo(i);
            if(result.getNumberOfElements() == 0)
                throw new JFException("Indicator didn't return any value");
            switch(info.getType().ordinal())
            {
            case 1: // '\001'
                double doubles[] = (double[])(double[])outputs[i];
                ret[i] = Double.valueOf(doubles[result.getNumberOfElements() - 1]);
                break;

            case 2: // '\002'
                int ints[] = (int[])(int[])outputs[i];
                ret[i] = Integer.valueOf(ints[result.getNumberOfElements() - 1]);
                break;

            case 3: // '\003'
                Object objects[] = (Object[])(Object[])outputs[i];
                ret[i] = objects[result.getNumberOfElements() - 1];
                break;
            }
        }

        return ret;
    }

    private Object[] function(Instrument instrument, Period period, OfferSide side, String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], long from, long to)
        throws JFException
    {
        return calculateIndicator(instrument, period, new OfferSide[] {
            side
        }, functionName, inputTypes, optParams, from, to);
    }

    public Object[] calculateIndicator(Instrument instrument, Period period, OfferSide side[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], long from, long to)
        throws JFException
    {
        IndicatorHolder indicatorHolder;
        checkSide(period, side);
        checkIntervalValid(period, from, to);
        indicatorHolder = getCachedIndicator(functionName);
        if(indicatorHolder == null)
            throw new JFException((new StringBuilder()).append("Indicator with name ").append(functionName).append(" was not found").toString());
        Object aobj[] = null;
        setOptParams(indicatorHolder.getIndicator(), optParams);
        int lookSides[] = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
        try {
            aobj = calculateIndicator(instrument, period, side, inputTypes, from, to, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
        } catch (DataCacheException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        indicatorHolder.getIndicatorContext().setChartInfo(null, null, null);
        cacheIndicator(functionName, indicatorHolder);
        return aobj;
    }

    protected Object[] calculateIndicator(Instrument instrument, Period period, OfferSide side[], com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], long from, long to, IndicatorHolder indicatorHolder, int indicatorLookback, int lookback, int lookforward)
        throws JFException, DataCacheException
    {
        IIndicator indicator = indicatorHolder.getIndicator();
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
        indicatorContext.setChartInfo(instrument, period, indicatorInfo.getNumberOfInputs() <= 0 ? OfferSide.BID : side[0]);
        int length = 0x80000000;
        int i = 0;
        for(int j = indicatorInfo.getNumberOfInputs(); i < j; i++)
        {
            InputParameterInfo info = indicator.getInputParameterInfo(i);
            OfferSide currentSide = info.getOfferSide() != null ? info.getOfferSide() : side[i];
            Period currentPeriod = info.getPeriod() != null ? info.getPeriod() : period;
            Instrument currentInstrument = info.getInstrument() != null ? info.getInstrument() : instrument;
            if(period.getInterval() > currentPeriod.getInterval())
                throw new JFException((new StringBuilder()).append("Indicator [").append(indicatorInfo.getName()).append("] cannot be run with periods longer than ").append(currentPeriod).toString());
            int flats = 0;
            long currentTo = to;
            boolean addCurrentCandle = false;
            if(instrument != currentInstrument)
            {
                long lastCandleTime = DataCacheUtils.getPreviousCandleStartFast(period, history.getStartTimeOfCurrentBar(instrument, period));
                if(currentTo > lastCandleTime)
                    currentTo = lastCandleTime;
                currentTo = DataCacheUtils.getCandleStartFast(currentPeriod, currentTo);
                long lastFormedCandleTime = DataCacheUtils.getPreviousCandleStartFast(currentPeriod, history.getStartTimeOfCurrentBar(currentInstrument, currentPeriod));
                if(currentTo > lastFormedCandleTime)
                    addCurrentCandle = true;
                lastCandleTime = history.getStartTimeOfCurrentBar(currentInstrument, currentPeriod);
                if(currentTo > lastCandleTime)
                    flats = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, lastCandleTime, currentTo) - 1;
                if(currentTo > lastFormedCandleTime)
                    currentTo = lastFormedCandleTime;
            }
            Object inputData = getInputData(currentInstrument, currentPeriod, currentSide, info.getType(), info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i], from, currentTo, lookback, lookforward, addCurrentCandle, flats);
            if(info.getInstrument() == null && info.getPeriod() == null)
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.PRICE)
                    length = ((double[][])(double[][])inputData)[0].length;
                else
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE)
                    length = ((double[])(double[])inputData).length;
                else
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.BAR)
                    length = ((IBar[])(IBar[])inputData).length;
            try
            {
                indicator.setInputParameter(i, inputData);
            }
            catch(Throwable t)
            {
                LOGGER.error(t.getMessage(), t);
                String error = StrategyWrapper.representError(indicator, t);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                throw new JFException(t);
            }
        }

        if(length == 0x80000000)
            if(indicatorInfo.getNumberOfInputs() == 0)
            {
                length = 0;
            } else
            {
                Object inputData = getInputData(instrument, period, OfferSide.BID, com.dukascopy.api.indicators.InputParameterInfo.Type.BAR, null, from, to, lookback, lookforward, false, 0);
                length = ((IBar[])(IBar[])inputData).length;
            }
        return calculateIndicator(period, from, to, indicator, indicatorLookback, lookback, lookforward, length);
    }

    protected Object[] calculateIndicator(Period period, long from, long to, IIndicator indicator, int indicatorLookback, 
            int lookback, int lookforward, int inputLength)
        throws JFException
    {
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        int i;
        if(inputLength <= indicatorLookback + lookforward)
        {
            LOGGER.warn("There is no enough data to calculate values");
            Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
            i = 0;
            for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
            {
                OutputParameterInfo info = indicator.getOutputParameterInfo(i);
                switch(info.getType().ordinal())
                {
                case 1: // '\001'
                    ret[i] = new double[0];
                    break;

                case 2: // '\002'
                    ret[i] = new int[0];
                    break;

                case 3: // '\003'
                    ret[i] = ((Object) (new Object[0]));
                    break;
                }
            }

            return ret;
        }
        Object outputs[] = new Object[indicatorInfo.getNumberOfOutputs()];
        i = 0;
        for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
        {
            OutputParameterInfo info = indicator.getOutputParameterInfo(i);
            switch(info.getType().ordinal())
            {
            default:
                break;

            case 1: // '\001'
                double doubles[] = new double[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = doubles;
                try
                {
                    indicator.setOutputParameter(i, doubles);
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }
                break;

            case 2: // '\002'
                int ints[] = new int[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = ints;
                try
                {
                    indicator.setOutputParameter(i, ints);
                    break;
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }

            case 3: // '\003'
                Object objects[] = new Object[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = ((Object) (objects));
                try
                {
                    indicator.setOutputParameter(i, ((Object) (objects)));
                    break;
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }
            }
        }

        IndicatorResult result;
        try
        {
            result = indicator.calculate(0, inputLength - 1);
        }
        catch(TaLibException e)
        {
            Throwable t = e.getCause();
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        if(result.getLastValueIndex() == 0x80000000 && result.getNumberOfElements() != 0)
        {
            if(lookforward != 0)
            {
                String error = (new StringBuilder()).append("calculate() method of indicator [").append(indicator.getIndicatorInfo().getName()).append("] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero").toString();
                LOGGER.error(error);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), true);
                throw new JFException(error);
            }
            result.setLastValueIndex(inputLength - 1);
        }
        if((result.getLastValueIndex() + 1) - result.getFirstValueIndex() < inputLength - lookback - lookforward || result.getNumberOfElements() < inputLength - lookback - lookforward)
        {
            String error = (new StringBuilder()).append("calculate() method of indicator [").append(indicator.getIndicatorInfo().getName()).append("] returned less values than expected").toString();
            LOGGER.error(error);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), true);
            throw new JFException(error);
        }
        Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
        i = 0;
        for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
        {
            OutputParameterInfo info = indicator.getOutputParameterInfo(i);
            int outCount;
            if(period != null)
                outCount = DataCacheUtils.getCandlesCountBetweenFast(period != Period.TICK ? period : Period.ONE_SEC, from, to) <= result.getNumberOfElements() ? DataCacheUtils.getCandlesCountBetweenFast(period != Period.TICK ? period : Period.ONE_SEC, from, to) : result.getNumberOfElements();
            else
                outCount = inputLength - lookback - lookforward;
            switch(info.getType().ordinal())
            {
            case 1: // '\001'
                double doubles[] = (double[])(double[])outputs[i];
                double retDoubles[] = new double[outCount];
                System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
                ret[i] = retDoubles;
                break;

            case 2: // '\002'
                int ints[] = (int[])(int[])outputs[i];
                int retInts[] = new int[outCount];
                System.arraycopy(ints, result.getNumberOfElements() - outCount, retInts, 0, outCount);
                ret[i] = retInts;
                break;

            case 3: // '\003'
                Object objects[] = (Object[])(Object[])outputs[i];
                Object retObjs[] = new Object[outCount];
                System.arraycopy(((Object) (objects)), result.getNumberOfElements() - outCount, ((Object) (retObjs)), 0, outCount);
                ret[i] = ((Object) (retObjs));
                break;
            }
        }

        return ret;
    }

    public Object[] calculateIndicator(Instrument instrument, Period period, OfferSide side[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        RateDataIndicatorCalculationResultWraper wraper = calculateIndicatorReturnSourceData(instrument, period, side, functionName, inputTypes, optParams, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
        return wraper.getIndicatorCalculationResult();
    }

    public static int calculateLookback(IIndicator indicator)
    {
        return calculateLookback(indicator, indicator.getLookback());
    }

    private static int calculateLookback(IIndicator indicator, int initialLookback)
    {
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        if(indicatorInfo.isUnstablePeriod())
            initialLookback += initialLookback * 4 >= 100 ? initialLookback * 4 : 100;
        return initialLookback;
    }

    public RateDataIndicatorCalculationResultWraper calculateIndicatorReturnSourceData(Instrument instrument, Period period, OfferSide side[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], Filter filter, 
            int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
        throws JFException
    {
        RateDataIndicatorCalculationResultWraper calculationResultWraper = new RateDataIndicatorCalculationResultWraper();
        IndicatorHolder indicatorHolder;
        checkNotTick(period);
        checkSide(period, side);
        checkIntervalValid(numberOfCandlesBefore, numberOfCandlesAfter);
        indicatorHolder = getCachedIndicator(functionName);
        RateDataIndicatorCalculationResultWraper ratedataindicatorcalculationresultwraper;
        if(indicatorHolder == null)
            throw new JFException((new StringBuilder()).append("Indicator with name ").append(functionName).append(" was not found").toString());
        IIndicator indicator = indicatorHolder.getIndicator();
        setOptParams(indicator, optParams);
        int lookback;
        int indicatorLookback;
        try
        {
            lookback = indicatorLookback = indicator.getLookback();
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        int lookforward;
        try
        {
            lookforward = indicator.getLookforward();
        }
        catch(AbstractMethodError e)
        {
            lookforward = 0;
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        if(indicatorInfo.isUnstablePeriod())
            lookback = calculateLookback(indicator, lookback);
        IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
        indicatorContext.setChartInfo(instrument, period, indicatorInfo.getNumberOfInputs() <= 0 ? OfferSide.BID : side[0]);
        int length = 0x80000000;
        int i = 0;
        for(int j = indicatorInfo.getNumberOfInputs(); i < j; i++)
        {
            InputParameterInfo info = indicator.getInputParameterInfo(i);
            Object inputData;
            List sourceBars = null;
            if(info.getInstrument() == null && info.getPeriod() == null)
            {
                com.dukascopy.api.indicators.InputParameterInfo.Type inputType = info.getType();
                com.dukascopy.api.IIndicators.AppliedPrice appliedPrice = info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i];
                try {
                    sourceBars = getInputBars(instrument, period, side[i], inputType, appliedPrice, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, lookback, lookforward, 0);
                } catch (DataCacheException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                inputData = barsToReal(sourceBars, inputType, appliedPrice);
            } else
            {
                OfferSide currentSide = info.getOfferSide() != null ? info.getOfferSide() : side[i];
                Instrument currentInstrument = info.getInstrument() != null ? info.getInstrument() : instrument;
                Period currentPeriod = info.getPeriod() != null ? info.getPeriod() : period;
                if(period.getInterval() > currentPeriod.getInterval())
                    throw new JFException((new StringBuilder()).append("Indicator [").append(indicatorInfo.getName()).append("] cannot be run with periods longer than ").append(currentPeriod).toString());
                if(currentPeriod != period)
                {
                    IBar bars[] = null;
                    try {
                        bars = (IBar[])(IBar[])getInputData(instrument, period, side[i], com.dukascopy.api.indicators.InputParameterInfo.Type.BAR, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, 0, 0, 0);
                    } catch (DataCacheException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(bars != null && bars.length > 0)
                    {
                        long currentFrom = DataCacheUtils.getCandleStartFast(currentPeriod, bars[0].getTime());
                        long currentTo = DataCacheUtils.getCandleStartFast(currentPeriod, bars[bars.length - 1].getTime());
                        int before = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentFrom, currentTo);
                        long currentBarTime = history.getStartTimeOfCurrentBar(currentInstrument, currentPeriod);
                        int flats = 0;
                        if(currentTo > currentBarTime)
                        {
                            flats = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentBarTime, currentTo) - 1;
                            currentTo = currentBarTime;
                        }
                        com.dukascopy.api.indicators.InputParameterInfo.Type inputType = info.getType();
                        com.dukascopy.api.IIndicators.AppliedPrice appliedPrice = info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i];
                        try {
                            sourceBars = getInputBars(currentInstrument, currentPeriod, currentSide, info.getType(), appliedPrice, filter, before, currentTo, 0, lookback - flats >= 0 ? lookback - flats : 0, lookforward, flats);
                        } catch (DataCacheException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        inputData = barsToReal(sourceBars, inputType, appliedPrice);
                    } else
                    {
                        inputData = bars;
                        sourceBars = Arrays.asList(bars);
                    }
                } else
                {
                    long currentBarTime = history.getStartTimeOfCurrentBar(currentInstrument, period);
                    int flats = 0;
                    long currentTo = time;
                    if(currentTo > currentBarTime)
                    {
                        flats = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentBarTime, currentTo) - 1;
                        currentTo = currentBarTime;
                    }
                    com.dukascopy.api.indicators.InputParameterInfo.Type inputType = info.getType();
                    com.dukascopy.api.IIndicators.AppliedPrice appliedPrice = info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i];
                    try {
                        sourceBars = getInputBars(currentInstrument, currentPeriod, currentSide, inputType, appliedPrice, filter, numberOfCandlesBefore, currentTo, numberOfCandlesAfter, lookback - flats >= 0 ? lookback - flats : 0, flats <= 0 ? lookforward : 0, flats);
                    } catch (DataCacheException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    inputData = barsToReal(sourceBars, inputType, appliedPrice);
                }
            }
            if(info.getInstrument() == null && info.getPeriod() == null)
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.PRICE)
                    length = ((double[][])(double[][])inputData)[0].length;
                else
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE)
                    length = ((double[])(double[])inputData).length;
                else
                if(info.getType() == com.dukascopy.api.indicators.InputParameterInfo.Type.BAR)
                    length = ((IBar[])(IBar[])inputData).length;
            try
            {
                indicator.setInputParameter(i, inputData);
                calculationResultWraper.setSourceData(sourceBars);
            }
            catch(Throwable t)
            {
                LOGGER.error(t.getMessage(), t);
                String error = StrategyWrapper.representError(indicator, t);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                throw new JFException(t);
            }
        }

        if(length == 0x80000000)
            if(indicatorInfo.getNumberOfInputs() == 0)
            {
                length = 0;
            } else
            {
                Object inputData = null;
                try {
                    inputData = getInputData(instrument, period, OfferSide.BID, com.dukascopy.api.indicators.InputParameterInfo.Type.BAR, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, lookback, lookforward, 0);
                } catch (DataCacheException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                length = ((IBar[])(IBar[])inputData).length;
            }
        Object ret[] = calculateIndicator(numberOfCandlesBefore, time, numberOfCandlesAfter, indicator, indicatorLookback, indicatorLookback, lookforward, length);
        calculationResultWraper.setIndicatorCalculationResult(ret);
        ratedataindicatorcalculationresultwraper = calculationResultWraper;
        cacheIndicator(functionName, indicatorHolder);
        return ratedataindicatorcalculationresultwraper;
    }

    protected Object[] calculateIndicator(int numberOfCandlesBefore, long time, int numberOfCandlesAfter, IIndicator indicator, int indicatorLookback, int lookback, 
            int lookforward, int inputLength)
        throws JFException
    {
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        int i;
        if(inputLength < numberOfCandlesBefore + numberOfCandlesAfter + indicatorLookback + lookforward)
        {
            LOGGER.warn("There is no enough data to calculate values");
            Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
            i = 0;
            for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
            {
                OutputParameterInfo info = indicator.getOutputParameterInfo(i);
                switch(info.getType().ordinal())
                {
                case 1: // '\001'
                    ret[i] = new double[0];
                    break;

                case 2: // '\002'
                    ret[i] = new int[0];
                    break;

                case 3: // '\003'
                    ret[i] = ((Object) (new Object[0]));
                    break;
                }
            }

            return ret;
        }
        Object outputs[] = new Object[indicatorInfo.getNumberOfOutputs()];
        i = 0;
        for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
        {
            OutputParameterInfo info = indicator.getOutputParameterInfo(i);
            switch(info.getType().ordinal())
            {
            default:
                break;

            case 1: // '\001'
                double doubles[] = new double[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = doubles;
                try
                {
                    indicator.setOutputParameter(i, doubles);
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }
                break;

            case 2: // '\002'
                int ints[] = new int[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = ints;
                try
                {
                    indicator.setOutputParameter(i, ints);
                    break;
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }

            case 3: // '\003'
                Object objects[] = new Object[inputLength - (indicatorLookback + lookforward)];
                outputs[i] = ((Object) (objects));
                try
                {
                    indicator.setOutputParameter(i, ((Object) (objects)));
                    break;
                }
                catch(Throwable t)
                {
                    LOGGER.error(t.getMessage(), t);
                    String error = StrategyWrapper.representError(indicator, t);
                    NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                    throw new JFException(t);
                }
            }
        }

        IndicatorResult result;
        try
        {
            result = indicator.calculate(0, inputLength - 1);
        }
        catch(TaLibException e)
        {
            Throwable t = e.getCause();
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        catch(Throwable t)
        {
            LOGGER.error(t.getMessage(), t);
            String error = StrategyWrapper.representError(indicator, t);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
            throw new JFException(t);
        }
        if(result.getLastValueIndex() == 0x80000000 && result.getNumberOfElements() != 0)
        {
            if(lookforward != 0)
            {
                String error = (new StringBuilder()).append("calculate() method of indicator [").append(indicator.getIndicatorInfo().getName()).append("] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero").toString();
                LOGGER.error(error);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), true);
                throw new JFException(error);
            }
            result.setLastValueIndex(inputLength - 1);
        }
        if((result.getLastValueIndex() + 1) - result.getFirstValueIndex() < inputLength - lookback - lookforward || result.getNumberOfElements() < inputLength - lookback - lookforward)
        {
            String error = (new StringBuilder()).append("calculate() method of indicator [").append(indicator.getIndicatorInfo().getName()).append("] returned less values than expected").toString();
            LOGGER.error(error);
            NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), true);
            throw new JFException(error);
        }
        Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
        i = 0;
        for(int j = indicatorInfo.getNumberOfOutputs(); i < j; i++)
        {
            OutputParameterInfo info = indicator.getOutputParameterInfo(i);
            switch(info.getType().ordinal())
            {
            case 1: // '\001'
            {
                double doubles[] = (double[])(double[])outputs[i];
                int outCount = numberOfCandlesBefore + numberOfCandlesAfter <= result.getNumberOfElements() ? numberOfCandlesBefore + numberOfCandlesAfter : result.getNumberOfElements();
                double retDoubles[] = new double[outCount];
                System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
                ret[i] = retDoubles;
                break;
            }

            case 2: // '\002'
            {
                int ints[] = (int[])(int[])outputs[i];
                int outCount = numberOfCandlesBefore + numberOfCandlesAfter <= result.getNumberOfElements() ? numberOfCandlesBefore + numberOfCandlesAfter : result.getNumberOfElements();
                int retInts[] = new int[outCount];
                System.arraycopy(ints, result.getNumberOfElements() - outCount, retInts, 0, outCount);
                ret[i] = retInts;
                break;
            }

            case 3: // '\003'
            {
                Object objects[] = (Object[])(Object[])outputs[i];
                int outCount = numberOfCandlesBefore + numberOfCandlesAfter <= result.getNumberOfElements() ? numberOfCandlesBefore + numberOfCandlesAfter : result.getNumberOfElements();
                Object retObjs[] = new Object[outCount];
                System.arraycopy(((Object) (objects)), result.getNumberOfElements() - outCount, ((Object) (retObjs)), 0, outCount);
                ret[i] = ((Object) (retObjs));
                break;
            }
            }
        }

        return ret;
    }

    protected void setOptParams(IIndicator indicator, Object optParams[])
        throws JFException
    {
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        int i = 0;
        for(int j = indicatorInfo.getNumberOfOptionalInputs(); i < j; i++)
            try
            {
                indicator.setOptInputParameter(i, optParams[i]);
            }
            catch(Throwable t)
            {
                LOGGER.error(t.getMessage(), t);
                String error = StrategyWrapper.representError(indicator, t);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                throw new JFException(t);
            }

    }

    Object getInputData(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.indicators.InputParameterInfo.Type inputType, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, int shift, int lookback, 
            int lookforward)
        throws DataCacheException, JFException
    {
        if(lookback == 0 && lookforward == 0)
        {
            IBar bar;
            if(shift < 0)
            {
                bar = history.getCurrentBar(instrument, period, side);
                if(bar == null)
                    return null;
                double price = bar.getClose();
                bar = new CandleData(DataCacheUtils.getTimeForNCandlesForwardFast(period, bar.getTime(), -shift + 1), price, price, price, price, 0.0D);
            } else
            {
                bar = history.getBar(instrument, period, side, shift);
                if(bar == null)
                    return null;
            }
            List bars = new ArrayList(1);
            bars.add(bar);
            return barsToReal(bars, inputType, appliedPrice);
        }
        int flats;
        IBar currentBar;
        long currentBarStartTime;
        long requestedBarStartTime;
        if(lookback == 0)
        {
            flats = shift >= 0 ? 0 : -shift;
            shift = shift >= 0 ? shift : 0;
            currentBar = null;
            currentBarStartTime = history.getStartTimeOfCurrentBar(instrument, period);
            requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, DataCacheUtils.getPreviousCandleStart(period, currentBarStartTime), shift);
            int count = DataCacheUtils.getCandlesCountBetweenFast(period, requestedBarStartTime, currentBarStartTime) - 1;
            long lookforwardStartTime;
            if(count > lookforward)
                lookforwardStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, count + 1);
            else
            if(count == lookforward)
            {
                lookforwardStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, count);
                currentBar = history.getCurrentBar(instrument, period, side);
            } else
            {
                return null;
            }
            List bars = history.getBars(instrument, period, side, requestedBarStartTime, lookforwardStartTime);
            if(currentBar != null)
                bars.add(currentBar);
            if(flats > 0 && !bars.isEmpty())
            {
                IBar lastCandle = (IBar)bars.get(bars.size() - 1);
                double price = lastCandle.getClose();
                int i = 0;
                for(long time = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < flats; time = DataCacheUtils.getNextCandleStartFast(period, time))
                {
                    bars.add(new CandleData(time, price, price, price, price, 0.0D));
                    i++;
                }

            }
            return barsToReal(bars, inputType, appliedPrice);
        }
        flats = shift >= 0 ? 0 : -shift;
        shift = shift >= 0 ? shift : 0;
        currentBar = null;
        if(shift == 0)
        {
            if(lookforward > 0)
                return null;
            shift = 1;
            lookback--;
            currentBar = history.getCurrentBar(instrument, period, side);
            if(currentBar == null)
                return null;
        }
        currentBarStartTime = history.getStartTimeOfCurrentBar(instrument, period);
        requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, DataCacheUtils.getPreviousCandleStart(period, currentBarStartTime), shift);
        long lookbackStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, requestedBarStartTime, lookback + 1);
        if(lookforward > 0)
        {
            int count = DataCacheUtils.getCandlesCountBetweenFast(period, requestedBarStartTime, currentBarStartTime) - 1;
            if(count > lookforward)
                requestedBarStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, lookforward + 1);
            else
            if(count == lookforward)
            {
                requestedBarStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, count);
                currentBar = history.getCurrentBar(instrument, period, side);
            } else
            {
                return null;
            }
        }
        List bars = history.getBars(instrument, period, side, lookbackStartTime, requestedBarStartTime);
        if(currentBar != null)
            bars.add(currentBar);
        if(flats > 0 && !bars.isEmpty())
        {
            IBar lastCandle = (IBar)bars.get(bars.size() - 1);
            double price = lastCandle.getClose();
            int i = 0;
            for(long time = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < flats; time = DataCacheUtils.getNextCandleStartFast(period, time))
            {
                bars.add(new CandleData(time, price, price, price, price, 0.0D));
                i++;
            }

        }
        return barsToReal(bars, inputType, appliedPrice);
    }

    Object getInputData(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.indicators.InputParameterInfo.Type inputType, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, long from, 
            long to, int lookback, int lookforward, boolean addCurrentCandle, int addFlatsAtTheEnd)
        throws JFException, DataCacheException
    {
        if(period == Period.TICK)
        {
            from = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, from);
            from -= lookback * 1000;
            long candleTo = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, to);
            long timeOfLastTick = history.getTimeOfLastTick(instrument);
            if(lookforward > 0)
            {
                long lastCandle = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, timeOfLastTick);
                int count = DataCacheUtils.getCandlesCountBetweenFast(Period.ONE_SEC, candleTo, lastCandle) - 1;
                count = count <= lookforward ? count : lookforward;
                to = DataCacheUtils.getTimeForNCandlesForwardFast(Period.ONE_SEC, candleTo, count + 1);
                candleTo = to;
                if(candleTo + 999L <= timeOfLastTick)
                    to = candleTo + 999L;
                else
                if(candleTo <= timeOfLastTick)
                    to = timeOfLastTick;
            } else
            if(candleTo + 999L <= timeOfLastTick)
                to = candleTo + 999L;
            List ticks = history.getTicks(instrument, from, to);
            List bars = new ArrayList((int)((to - from) / 1000L + 1L));
            if(ticks.isEmpty() || DataCacheUtils.getCandleStartFast(Period.ONE_SEC, ((ITick)ticks.get(0)).getTime()) != from)
            {
                ITick tickBefore = history.getLastTickBefore(instrument, from - 1L);
                double price = side != OfferSide.ASK ? tickBefore.getBid() : tickBefore.getAsk();
                long timeOfLastFlatCandle = ticks.isEmpty() ? DataCacheUtils.getCandleStartFast(Period.ONE_SEC, to) : DataCacheUtils.getPreviousCandleStartFast(Period.ONE_SEC, DataCacheUtils.getCandleStartFast(Period.ONE_SEC, ((ITick)ticks.get(0)).getTime()));
                for(long time = from; time <= timeOfLastFlatCandle; time += 1000L)
                    bars.add(new CandleData(time, price, price, price, price, 0.0D));

            }
            createCandlesFromTicks(ticks, bars, side);
            return barsToReal(bars, inputType, appliedPrice);
        }
        from = DataCacheUtils.getCandleStartFast(period, from);
        long lookbackStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, from, lookback + 1);
        if(lookforward > 0)
        {
            to = DataCacheUtils.getCandleStartFast(period, to);
            long lastCandle = DataCacheUtils.getPreviousCandleStartFast(period, history.getStartTimeOfCurrentBar(instrument, period));
            int count = DataCacheUtils.getCandlesCountBetweenFast(period, to, lastCandle) - 1;
            count = count <= lookforward ? count : lookforward;
            to = DataCacheUtils.getTimeForNCandlesForwardFast(period, to, count + 1);
        }
        List bars = history.getBars(instrument, period, side, lookbackStartTime, to);
        if(addCurrentCandle)
        {
            IBar currentBar = history.getCurrentBar(instrument, period, side);
            bars.add(currentBar);
        }
        if(addFlatsAtTheEnd > 0 && !bars.isEmpty())
        {
            IBar lastCandle = (IBar)bars.get(bars.size() - 1);
            double price = lastCandle.getClose();
            int i = 0;
            for(long lastTime = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < addFlatsAtTheEnd; lastTime = DataCacheUtils.getNextCandleStartFast(period, lastTime))
            {
                bars.add(new CandleData(lastTime, price, price, price, price, 0.0D));
                i++;
            }

        }
        return barsToReal(bars, inputType, appliedPrice);
    }

    Object getInputData(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.indicators.InputParameterInfo.Type inputType, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int before, 
            long time, int after, int lookback, int lookforward, int addFlatsAtTheEnd)
        throws JFException, DataCacheException
    {
        List bars = getInputBars(instrument, period, side, inputType, appliedPrice, filter, before, time, after, lookback, lookforward, addFlatsAtTheEnd);
        return barsToReal(bars, inputType, appliedPrice);
    }

    private List getInputBars(Instrument instrument, Period period, OfferSide side, com.dukascopy.api.indicators.InputParameterInfo.Type inputType, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, Filter filter, int before, 
            long time, int after, int lookback, int lookforward, int addFlatsAtTheEnd)
        throws JFException, DataCacheException
    {
        time = DataCacheUtils.getCandleStartFast(period, time);
        before += lookback;
        after += lookforward;
        List bars = history.getBars(instrument, period, side, filter, before, time, after);
        if(addFlatsAtTheEnd > 0 && !bars.isEmpty())
        {
            IBar lastCandle = (IBar)bars.get(bars.size() - 1);
            double price = lastCandle.getClose();
            int i = 0;
            for(long lastTime = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < addFlatsAtTheEnd; lastTime = DataCacheUtils.getNextCandleStartFast(period, lastTime))
            {
                bars.add(new CandleData(lastTime, price, price, price, price, 0.0D));
                i++;
            }

        }
        return bars;
    }

    protected void checkSide(Period period, OfferSide side[])
        throws JFException
    {
        if(period == Period.TICK)
        {
            return;
        } else
        {
            checkSide(side);
            return;
        }
    }

    protected void checkSide(OfferSide side[])
        throws JFException
    {
        if(side == null)
            throw new JFException("Side parameter cannot be null");
        OfferSide arr$[] = side;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            OfferSide offerSide = arr$[i$];
            if(offerSide == null)
                throw new JFException("Side parameter cannot be null");
        }

    }

    private void checkSide(Period period, OfferSide side)
        throws JFException
    {
        if(period == Period.TICK)
            return;
        if(side == null)
            throw new JFException("Side parameter cannot be null");
        else
            return;
    }

    protected void checkIntervalValid(Period period, long from, long to)
        throws JFException
    {
        if(!history.isIntervalValid(period, from, to))
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            throw new JFException((new StringBuilder()).append("Interval from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("] GMT is not valid").toString());
        } else
        {
            return;
        }
    }

    private void checkIntervalValid(int before, int after)
        throws JFException
    {
        if(before <= 0 && after <= 0)
            throw new IllegalArgumentException("Negative or zero number of candles requested");
        else
            return;
    }

    protected void checkNotTick(Period period)
        throws JFException
    {
        if(period == Period.TICK)
            throw new JFException("Functions with shift parameter doesn't support ticks");
        else
            return;
    }

    protected void checkShiftPositive(int shift)
        throws JFException
    {
        if(shift < 0)
            throw new JFException("Shift parameter must not be negative");
        else
            return;
    }

    protected Object barsToReal(List bars, com.dukascopy.api.indicators.InputParameterInfo.Type inputType, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice)
        throws JFException
    {
        int i = 0;
        switch(inputType.ordinal())
        {
        case 1: // '\001'
            double retDouble[] = new double[bars.size()];
            for(Iterator i$ = bars.iterator(); i$.hasNext();)
            {
                IBar bar = (IBar)i$.next();
                retDouble[i] = barToReal(bar, appliedPrice);
                i++;
            }

            return retDouble;

        case 2: // '\002'
            double retPrice[][] = new double[5][bars.size()];
            for(Iterator i$ = bars.iterator(); i$.hasNext();)
            {
                IBar bar = (IBar)i$.next();
                retPrice[0][i] = barToReal(bar, com.dukascopy.api.IIndicators.AppliedPrice.OPEN);
                retPrice[1][i] = barToReal(bar, com.dukascopy.api.IIndicators.AppliedPrice.CLOSE);
                retPrice[2][i] = barToReal(bar, com.dukascopy.api.IIndicators.AppliedPrice.HIGH);
                retPrice[3][i] = barToReal(bar, com.dukascopy.api.IIndicators.AppliedPrice.LOW);
                retPrice[4][i] = barToReal(bar, com.dukascopy.api.IIndicators.AppliedPrice.VOLUME);
                i++;
            }

            return retPrice;

        case 3: // '\003'
            return ((Object) (bars.toArray(new IBar[bars.size()])));
        }
        throw new RuntimeException((new StringBuilder()).append("Unexpected input type [").append(inputType).append("]").toString());
    }

    private double barToReal(IBar bar, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice)
        throws JFException
    {
        switch(appliedPrice.ordinal())
        {
        case 1: // '\001'
            return bar.getClose();

        case 2: // '\002'
            return bar.getHigh();

        case 3: // '\003'
            return bar.getLow();

        case 4: // '\004'
            return (bar.getHigh() + bar.getLow()) / 2D;

        case 5: // '\005'
            return bar.getOpen();

        case 6: // '\006'
            return (bar.getHigh() + bar.getLow() + bar.getClose()) / 3D;

        case 7: // '\007'
            return (bar.getHigh() + bar.getLow() + bar.getClose() + bar.getClose()) / 4D;

        case 8: // '\b'
            return (double)bar.getTime();

        case 9: // '\t'
            return bar.getVolume();
        }
        throw new JFException("Parameter should not be ASK, BID, ASK_VOLUME or BID_VOLUME for bars");
    }

    private void createCandlesFromTicks(List timeData, List tickCandles, OfferSide side)
    {
        if(timeData.isEmpty())
            return;
        IntraPeriodCandleData generatedCandles[][] = new IntraPeriodCandleData[Period.values().length][];
        generatedCandles[Period.ONE_SEC.ordinal()] = new IntraPeriodCandleData[0];
        List formedCandles = new ArrayList();
        ITick tickData;
        for(Iterator i$ = timeData.iterator(); i$.hasNext(); IntraperiodCandlesGenerator.addTickToCandles(tickData.getTime(), tickData.getAsk(), tickData.getBid(), tickData.getAskVolume(), tickData.getBidVolume(), null, generatedCandles, formedCandles, null))
            tickData = (ITick)i$.next();

        long nextCandleTime = DataCacheUtils.getNextCandleStartFast(Period.ONE_SEC, DataCacheUtils.getCandleStartFast(Period.ONE_SEC, ((ITick)timeData.get(timeData.size() - 1)).getTime()));
        IntraperiodCandlesGenerator.addTickToCandles(nextCandleTime, 0.0D, 0.0D, 0.0D, 0.0D, null, generatedCandles, formedCandles, null);
        IBar bar;
        for(Iterator i$ = formedCandles.iterator(); i$.hasNext(); tickCandles.add(bar))
        {
            Object formedCandlesData[] = (Object[])i$.next();
            IntraPeriodCandleData candle;
            if(side == OfferSide.ASK)
                candle = (IntraPeriodCandleData)formedCandlesData[1];
            else
                candle = (IntraPeriodCandleData)formedCandlesData[2];
            bar = new CandleData(candle.time, candle.open, candle.close, candle.low, candle.high, candle.vol);
        }

    }

    public void registerCustomIndicator(final File compiledCustomIndcatorFile)
        throws JFException
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run()
                    throws Exception
                {
                    registerCustomIndicatorSecured(compiledCustomIndcatorFile);
                    return null;
                }

            });
        }
        catch(PrivilegedActionException e)
        {
            Exception ex = e.getException();
            if(ex instanceof JFException)
                throw (JFException)ex;
            if(ex instanceof RuntimeException)
            {
                throw (RuntimeException)ex;
            } else
            {
                LOGGER.error(ex.getMessage(), ex);
                throw new JFException(ex);
            }
        }
    }

    public void registerCustomIndicator(final Class indicatorClass)
        throws JFException
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run()
                    throws Exception
                {
                    registerCustomIndicatorSecured(indicatorClass);
                    return null;
                }

            });
        }
        catch(PrivilegedActionException e)
        {
            Exception ex = e.getException();
            if(ex instanceof JFException)
                throw (JFException)ex;
            if(ex instanceof RuntimeException)
            {
                throw (RuntimeException)ex;
            } else
            {
                LOGGER.error(ex.getMessage(), ex);
                throw new JFException(ex);
            }
        }
    }

    private void registerCustomIndicatorSecured(File compiledCustomIndicatorFile)
        throws JFException
    {
        if(!compiledCustomIndicatorFile.exists())
            throw new JFException("Indicator file doesn't exists");
        IndicatorsProvider indicatorsProvider = IndicatorsProvider.getInstance();
        CustIndicatorWrapper indicatorWrapper = new CustIndicatorWrapper();
        indicatorWrapper.setBinaryFile(compiledCustomIndicatorFile);
        if(indicatorsProvider.enableIndicator(indicatorWrapper, NotificationUtilsProvider.getNotificationUtils()) == null)
            throw new JFException("Cannot register indicator");
        else
            return;
    }

    private void registerCustomIndicatorSecured(Class indicatorClass)
        throws JFException
    {
        if(indicatorClass == null)
            throw new JFException("Indicator class is null");
        IIndicator indicator = null;
        try
        {
            indicator = (IIndicator)indicatorClass.newInstance();
        }
        catch(Exception ex)
        {
            throw new JFException((new StringBuilder()).append("Cannot create indicator [").append(indicatorClass).append("] instance.").toString());
        }
        try
        {
            indicator.onStart(IndicatorHelper.createIndicatorContext());
        }
        catch(Throwable th)
        {
            throw new JFException("Exception in onStart method");
        }
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        if(indicatorInfo == null)
            throw new JFException("Indicator info is null");
        String indicatorName = indicatorInfo.getName();
        if(indicatorName != null && !indicatorName.trim().isEmpty())
        {
            customIndicators.put(indicatorName, indicatorClass);
            cacheIndicator(indicatorName, new IndicatorHolder(indicator, IndicatorHelper.createIndicatorContext()));
        } else
        {
            throw new JFException("Indicator name is empty");
        }
    }

    private Period noFilterPeriod(Period period)
    {
        if(period == null)
            return null;
        if(period == Period.DAILY_SKIP_SUNDAY || period == Period.DAILY_SUNDAY_IN_MONDAY)
            return Period.DAILY;
        else
            return period;
    }

    private Period dailyFilterPeriod(Period period)
    {
        if(period == Period.DAILY || period == Period.DAILY_SKIP_SUNDAY || period == Period.DAILY_SUNDAY_IN_MONDAY)
            return period;
        else
            return null;
    }

    private Object getIndicatorInputData(InputParameterInfo inputParameterInfo, com.dukascopy.api.IIndicators.AppliedPrice appliedPrice, CandleData timeData[])
    {
        int dataSize = timeData.length;
        switch(inputParameterInfo.getType().ordinal())
        {
        case 2: // '\002'
            double open[] = new double[dataSize];
            double high[] = new double[dataSize];
            double low[] = new double[dataSize];
            double close[] = new double[dataSize];
            double volume[] = new double[dataSize];
            int k = 0;
            CandleData arr$[] = timeData;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                CandleData candle = arr$[i$];
                open[k] = candle.open;
                high[k] = candle.high;
                low[k] = candle.low;
                close[k] = candle.close;
                volume[k] = candle.vol;
                k++;
            }

            return (new double[][] {
                open, close, high, low, volume
            });

        case 1: // '\001'
            double data[] = new double[dataSize];
            switch(appliedPrice.ordinal())
            {
            default:
                break;

            case 1: // '\001'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = candle.close;
                    k++;
                }

                break;
            }

            case 2: // '\002'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = candle.high;
                    k++;
                }

                break;
            }

            case 3: // '\003'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = candle.low;
                    k++;
                }

                break;
            }

            case 5: // '\005'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = candle.open;
                    k++;
                }

                break;
            }

            case 4: // '\004'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = (candle.high + candle.low) / 2D;
                    k++;
                }

                break;
            }

            case 6: // '\006'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = (candle.high + candle.low + candle.close) / 3D;
                    k++;
                }

                break;
            }

            case 7: // '\007'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = (candle.high + candle.low + candle.close + candle.close) / 4D;
                    k++;
                }

                break;
            }

            case 8: // '\b'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = candle.time;
                    k++;
                }

                break;
            }

            case 9: // '\t'
            {
                k = 0;
                arr$ = timeData;
                len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    CandleData candle = arr$[i$];
                    data[k] = candle.vol;
                    k++;
                }

                break;
            }
            }
            return data;

        case 3: // '\003'
            return timeData;
        }
        return null;
    }

    protected final CandleData[] putDataInListFromToIndexes(int from, int to, CandleData buffer[])
    {
        CandleData data[] = new CandleData[(to - from) + 1];
        System.arraycopy(buffer, from, data, 0, (to - from) + 1);
        return data;
    }

    private double[][][] calculateIndicatorInputs(Instrument instrument, Period period, OfferSide side, Collection formulasToRecalculate, CandleData timeDataAsk[], CandleData timeDataBid[], double doubleInputs[][][], 
            boolean isTicksDataType)
    {
        double priceInput[][][] = (double[][][])null;
        Iterator i$ = formulasToRecalculate.iterator();
        do
        {
            if(!i$.hasNext())
                break;
            com.dukascopy.charts.math.dataprovider.AbstractDataProvider.IndicatorData formulaData = (com.dukascopy.charts.math.dataprovider.AbstractDataProvider.IndicatorData)i$.next();
            if(!formulaData.disabledIndicator)
            {
                IIndicator indicator = formulaData.indicatorWrapper.getIndicator();
                OfferSide tickOfferSides[] = formulaData.indicatorWrapper.getOfferSidesForTicks();
                com.dukascopy.api.IIndicators.AppliedPrice appliedPrices[] = formulaData.indicatorWrapper.getAppliedPricesForCandles();
                if(timeDataAsk != null || timeDataBid != null)
                {
                    if(indicator instanceof ConnectorIndicator)
                    {
                        ((ConnectorIndicator)indicator).setCurrentInstrument(instrument);
                        ((ConnectorIndicator)indicator).setCurrentPeriod(period);
                    }
                    int i = 0;
                    int j = indicator.getIndicatorInfo().getNumberOfInputs();
                    while(i < j) 
                    {
                        InputParameterInfo inputParameterInfo = indicator.getInputParameterInfo(i);
                        if(inputParameterInfo.getOfferSide() == null && inputParameterInfo.getPeriod() == null && inputParameterInfo.getInstrument() == null || (inputParameterInfo.getOfferSide() == null || inputParameterInfo.getOfferSide() == (isTicksDataType ? tickOfferSides[i] : side)) && ((inputParameterInfo.getPeriod() == null || noFilterPeriod(inputParameterInfo.getPeriod()) == period && dailyFilterPeriod(inputParameterInfo.getPeriod()) == dailyFilterPeriod(dailyFilterPeriod)) && (inputParameterInfo.getInstrument() == null || inputParameterInfo.getInstrument() == instrument)))
                            switch(inputParameterInfo.getType().ordinal())
                            {
                            default:
                                break;

                            case 2: // '\002'
                                if(priceInput != null && (!isTicksDataType || priceInput[tickOfferSides[i].ordinal()] != null))
                                    break;
                                if(priceInput == null)
                                    priceInput = new double[2][][];
                                priceInput[isTicksDataType ? tickOfferSides[i].ordinal() : 0] = (double[][])(double[][])getIndicatorInputData(inputParameterInfo, appliedPrices[i], isTicksDataType && tickOfferSides[i] != OfferSide.ASK ? timeDataBid : timeDataAsk);
                                break;

                            case 1: // '\001'
                                if(doubleInputs[isTicksDataType ? tickOfferSides[i].ordinal() : 0][appliedPrices[i].ordinal()] == null)
                                    doubleInputs[isTicksDataType ? tickOfferSides[i].ordinal() : 0][appliedPrices[i].ordinal()] = (double[])(double[])getIndicatorInputData(inputParameterInfo, appliedPrices[i], isTicksDataType && tickOfferSides[i] != OfferSide.ASK ? timeDataBid : timeDataAsk);
                                break;
                            }
                        i++;
                    }
                }
            }
        } while(true);
        return priceInput;
    }

    protected void copyToIndicatorOutput(int from, int to, int recalculateStart, Object outputData, int firstValueIndex, int numberOfElements, Object inputData, 
            com.dukascopy.api.indicators.OutputParameterInfo.Type type, int lastIndex, int bufferLength)
    {
        int toSkip;
        if(recalculateStart <= from)
            toSkip = from - recalculateStart;
        else
            toSkip = (bufferLength - recalculateStart) + from;
        int valuesNA = firstValueIndex - toSkip;
        int outputBufferCounter;
        int inputBufferCounter;
        if(valuesNA > 0)
        {
            inputBufferCounter = 0;
            if((lastIndex - from) + 1 >= valuesNA)
                outputBufferCounter = from + valuesNA;
            else
                throw new RuntimeException("Cannot happen");
            switch(type.ordinal())
            {
            case 2: // '\002'
                int outputDataInt[] = (int[])(int[])outputData;
                Arrays.fill(outputDataInt, from, outputBufferCounter, 0x80000000);
                break;

            case 1: // '\001'
                double outputDataDouble[] = (double[])(double[])outputData;
                Arrays.fill(outputDataDouble, from, outputBufferCounter, (0.0D / 0.0D));
                break;

            case 3: // '\003'
                Object outputDataObject[] = (Object[])(Object[])outputData;
                Arrays.fill(outputDataObject, from, outputBufferCounter, null);
                break;
            }
        } else
        {
            outputBufferCounter = from;
            inputBufferCounter = toSkip - firstValueIndex;
            numberOfElements -= inputBufferCounter;
        }
        if((lastIndex - outputBufferCounter) + 1 >= numberOfElements)
            System.arraycopy(inputData, inputBufferCounter, outputData, outputBufferCounter, numberOfElements);
        else
            throw new RuntimeException("Cannot happen");
        if((to - (outputBufferCounter + numberOfElements)) + 1 > 0)
            switch(type.ordinal())
            {
            case 2: // '\002'
                int outputDataInt[] = (int[])(int[])outputData;
                Arrays.fill(outputDataInt, outputBufferCounter + numberOfElements, to + 1, 0x80000000);
                break;

            case 1: // '\001'
                double outputDataDouble[] = (double[])(double[])outputData;
                Arrays.fill(outputDataDouble, outputBufferCounter + numberOfElements, to + 1, (0.0D / 0.0D));
                break;

            case 3: // '\003'
                Object outputDataObject[] = (Object[])(Object[])outputData;
                Arrays.fill(outputDataObject, outputBufferCounter + numberOfElements, to + 1, null);
                break;
            }
    }

    private boolean populateIndicatorInputFromDataProvider(Period period, com.dukascopy.charts.math.dataprovider.AbstractDataProvider.IndicatorData indicatorData, int inputIndex, long from, long to, 
            int finalLookback, int finalLookforward, int maxNumberOfCandles, int bufferSizeMultiplier)
    {
        IIndicator indicator = indicatorData.indicatorWrapper.getIndicator();
        if(indicatorData.inputDataProviders[inputIndex] == null)
            throw new AssertionError("Input data provider is null");
        Period inputPeriod = indicatorData.inputPeriods[inputIndex] != null ? indicatorData.inputPeriods[inputIndex] : period;
        Period inputCandlePeriod = inputPeriod != Period.TICK ? inputPeriod : Period.ONE_SEC;
        long latestTo = to = DataCacheUtils.getCandleStartFast(inputCandlePeriod, to);
        from = DataCacheUtils.getCandleStartFast(inputCandlePeriod, from);
        long latestDataTime = indicatorData.inputDataProviders[inputIndex].getLastLoadedDataTime();
        if(latestDataTime == 0x8000000000000000L)
        {
            LOGGER.debug("WARN: Indicator data provider doesn't have any data");
            return false;
        }
        if(to > latestDataTime)
            to = DataCacheUtils.getCandleStartFast(inputCandlePeriod, latestDataTime);
        if(from > to)
            return false;
        int candlesBefore = DataCacheUtils.getCandlesCountBetweenFast(inputCandlePeriod, from, to) + finalLookback;
        if(candlesBefore == 0)
        {
            LOGGER.debug("WARN: Nothing to request from indicator data provider");
            return false;
        }
        if(candlesBefore + finalLookforward > maxNumberOfCandles * bufferSizeMultiplier)
            candlesBefore = maxNumberOfCandles * bufferSizeMultiplier - finalLookforward;
        IDataSequence dataSequence = indicatorData.inputDataProviders[inputIndex].getDataSequence(candlesBefore, to, finalLookforward);
        CandleData data[];
        if(inputPeriod == Period.TICK)
        {
            TickDataSequence tickSequence = (TickDataSequence)dataSequence;
            OfferSide offerSide = indicator.getInputParameterInfo(inputIndex).getOfferSide();
            OfferSide tickOfferSides[] = indicatorData.indicatorWrapper.getOfferSidesForTicks();
            if(offerSide != null && offerSide == OfferSide.ASK || offerSide == null && tickOfferSides[inputIndex] == OfferSide.ASK)
                data = tickSequence.getOneSecCandlesAsk();
            else
                data = tickSequence.getOneSecCandlesBid();
            if(tickSequence.getOneSecExtraBefore() > 0 || tickSequence.getOneSecExtraAfter() > 0)
            {
                CandleData newData[] = new CandleData[data.length - tickSequence.getOneSecExtraBefore() - tickSequence.getOneSecExtraAfter()];
                System.arraycopy(data, tickSequence.getOneSecExtraBefore(), newData, 0, newData.length);
                data = newData;
            }
        } else
        {
            CandleDataSequence candleSequence = (CandleDataSequence)dataSequence;
            data = (CandleData[])candleSequence.getData();
            if(dataSequence.getExtraBefore() > 0 || dataSequence.getExtraAfter() > 0)
            {
                CandleData newData[] = new CandleData[data.length - dataSequence.getExtraBefore() - dataSequence.getExtraAfter()];
                System.arraycopy(data, dataSequence.getExtraBefore(), newData, 0, newData.length);
                data = newData;
            }
        }
        if(data.length == 0)
        {
            indicator.setInputParameter(inputIndex, getIndicatorInputData(indicatorData.indicatorWrapper.getIndicator().getInputParameterInfo(inputIndex), indicatorData.indicatorWrapper.getAppliedPricesForCandles()[inputIndex], new CandleData[0]));
            return true;
        }
        int flats = 0;
        if(latestTo > to)
            flats = DataCacheUtils.getCandlesCountBetweenFast(inputCandlePeriod, to, latestTo) - 1;
        CandleData correctData[];
        if(flats > 0)
        {
            correctData = new CandleData[data.length + flats];
            System.arraycopy(data, 0, correctData, 0, data.length);
            CandleData lastCandle = correctData[data.length - 1];
            int i = 1;
            for(long time = DataCacheUtils.getNextCandleStartFast(inputCandlePeriod, lastCandle.time); i <= flats; time = DataCacheUtils.getNextCandleStartFast(inputCandlePeriod, time))
            {
                correctData[(data.length - 1) + i] = new CandleData(time, lastCandle.close, lastCandle.close, lastCandle.close, lastCandle.close, 0.0D);
                i++;
            }

        } else
        {
            correctData = data;
        }
        indicator.setInputParameter(inputIndex, getIndicatorInputData(indicatorData.indicatorWrapper.getIndicator().getInputParameterInfo(inputIndex), indicatorData.indicatorWrapper.getAppliedPricesForCandles()[inputIndex], correctData));
        return true;
    }

    public static class IndicatorData
    {

        public IndicatorWrapper indicatorWrapper;
        public double outputDataDouble[][];
        public int outputDataInt[][];
        public Object outputDataObject[][];
        public int lookback;
        public int lookforward;
        public boolean disabledIndicator;
        public long lastTime;
        public Object lastValues[];
        public OfferSide inputSides[];
        public Period inputPeriods[];
        public Instrument inputInstruments[];
        public Filter inputFilters[];
        public AbstractDataProvider inputDataProviders[];

        public IndicatorData()
        {
            lastTime = -1L;
        }
    }

    public void calculateIndicators(Instrument instrument, Period period, OfferSide side, int from, int to, Collection formulasToRecalculate, int lastIndex, 
            CandleData bufferAsk[], CandleData bufferBid[], boolean isTicksDataType, int maxNumberOfCandles, int bufferSizeMultiplier, Data firstData)
    {
        throw new UnsupportedOperationException();
    }

    public Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide offerSides[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], long from, 
            long to)
        throws JFException
    {
        if(feedDescriptor == null)
            throw new JFException("FeedDescriptor is null");
        if(feedDescriptor.getInstrument() == null)
            throw new JFException("Instrument is null");
        if(DataType.TICKS.equals(feedDescriptor.getDataType()) || DataType.TIME_PERIOD_AGGREGATION.equals(feedDescriptor.getDataType()))
            return calculateIndicator(feedDescriptor.getInstrument(), feedDescriptor.getPeriod(), offerSides, functionName, inputTypes, optParams, from, to);
        IndicatorHolder indicatorHolder;
        checkSide(offerSides);
        history.validateTimeInterval(feedDescriptor.getInstrument(), from, to);
        indicatorHolder = getCachedIndicator(functionName);
        if(indicatorHolder == null)
            throw new JFException((new StringBuilder()).append("Indicator with name ").append(functionName).append(" was not found").toString());
        Object aobj[] = null;
        setOptParams(indicatorHolder.getIndicator(), optParams);
        int lookSides[] = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
        try {
            aobj = calculateIndicator(feedDescriptor, offerSides, inputTypes, from, to, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
        } catch (DataCacheException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        indicatorHolder.getIndicatorContext().setFeedDescriptor(null);
        cacheIndicator(functionName, indicatorHolder);
        return aobj;
    }

    protected Object[] calculateIndicator(final IFeedDescriptor feedDescriptor, OfferSide offerSides[], com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], final long from, final long to, 
            IndicatorHolder indicatorHolder, int indicatorLookback, final int lookBack, final int lookForward)
        throws JFException, DataCacheException
    {
        int inputLength = feedDataToIndicator(new BarsLoadable() {

            public List load(IFeedDescriptor fd)
                throws JFException
            {
                return loadBarsFromTo(feedDescriptor, from, to, lookBack, lookForward);
            }
        }, feedDescriptor, offerSides, inputTypes, indicatorHolder, indicatorLookback, lookBack, lookForward);
        IIndicator indicator = indicatorHolder.getIndicator();
        if(inputLength <= 0)
            return getEmptyIndicatorCalculationResult(indicator);
        else
            return calculateIndicator(((Period) (null)), from, to, indicator, indicatorLookback, lookBack, lookForward, inputLength);
    }

    private List loadBarsFromTo(final IFeedDescriptor feedDescriptor, final long from, final long to, final int lookBack, final int lookForward)
        throws JFException
    {
        DataType dataType = feedDescriptor.getDataType();
        List converedResult;
        if(DataType.PRICE_RANGE_AGGREGATION.equals(dataType))
        {
            List result = loadBarsFromTo(new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), from, to);
                }

            }, new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), lookBack, DataCacheUtils.getPreviousPriceAggregationBarStart(from), 0);
                }

            }, new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), 0, DataCacheUtils.getNextPriceAggregationBarStart(to), lookForward);
                }

            }, lookBack, lookForward);
            converedResult = new ArrayList(result);
        } else
        if(DataType.POINT_AND_FIGURE.equals(dataType))
        {
            List result = loadBarsFromTo(new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), from, to);
                }

            }, new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), lookBack, DataCacheUtils.getPreviousPriceAggregationBarStart(from), 0);
                }
            }, new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), 0, DataCacheUtils.getNextPriceAggregationBarStart(to), lookForward);
                }

            }, lookBack, lookForward);
            converedResult = new ArrayList(result);
        } else
        if(DataType.TICK_BAR.equals(dataType))
        {
            List result = loadBarsFromTo(new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), from, to);
                }

            }, new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), lookBack, DataCacheUtils.getPreviousPriceAggregationBarStart(from), 0);
                }

            }, new BarsLoadable() {

                public List load(IFeedDescriptor fd)
                    throws JFException
                {
                    return history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), 0, DataCacheUtils.getNextPriceAggregationBarStart(to), lookForward);
                }

            }, lookBack, lookForward);
            converedResult = new ArrayList(result);
        } else
        {
            throw new JFException((new StringBuilder()).append("Unsupported data type [").append(dataType).append("]").toString());
        }
        return converedResult;
    }

    private List loadBarsFromTo(BarsLoadable fromToLoadable, BarsLoadable lookBackLoadable, BarsLoadable lookForwardLoadable, int lookBack, int lookForward)
        throws JFException
    {
        List rangeBars = fromToLoadable.load(null);
        List lookBackBars = null;
        List lookForwardBars = null;
        if(lookBack > 0)
            lookBackBars = lookBackLoadable.load(null);
        if(lookForward > 0)
            lookForwardBars = lookForwardLoadable.load(null);
        List result = new ArrayList();
        if(lookBackBars != null)
            result.addAll(lookBackBars);
        result.addAll(rangeBars);
        if(lookForwardBars != null)
            result.addAll(lookForwardBars);
        return result;
    }

    public Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide offerSides[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], int shift)
        throws JFException
    {
        if(feedDescriptor == null)
            throw new JFException("FeedDescriptor is null");
        if(DataType.TICKS.equals(feedDescriptor.getDataType()) || DataType.TIME_PERIOD_AGGREGATION.equals(feedDescriptor.getDataType()))
            return calculateIndicator(feedDescriptor.getInstrument(), feedDescriptor.getPeriod(), offerSides, functionName, inputTypes, optParams, shift);
        IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
        Object aobj[] = null;
        checkSide(offerSides);
        checkShiftPositive(shift);
        if(indicatorHolder == null)
            throw new JFException((new StringBuilder()).append("Indicator with name ").append(functionName).append(" was not found").toString());
        int lookSides[] = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
        try {
            aobj = calculateIndicator(feedDescriptor, offerSides, inputTypes, shift, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
        } catch (DataCacheException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(indicatorHolder != null)
        {
            indicatorHolder.getIndicatorContext().setFeedDescriptor(null);
            cacheIndicator(functionName, indicatorHolder);
        }
        return aobj;
    }

    protected Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide offerSides[], com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], final int shift, IndicatorHolder indicatorHolder, int indicatorLookback, final int lookback, 
            final int lookforward)
        throws JFException, DataCacheException
    {
        int inputLength = feedDataToIndicator(new BarsLoadable() {

            public List load(IFeedDescriptor feedDescriptor)
                throws JFException
            {
                return loadBarsByShift(feedDescriptor, shift, lookback, lookforward);
            }

        }, feedDescriptor, offerSides, inputTypes, indicatorHolder, indicatorLookback, lookback, lookforward);
        IIndicator indicator = indicatorHolder.getIndicator();
        if(inputLength <= 0)
            return getEmptyIndicatorCalculationResult(indicator);
        else
            return calculateIndicator(indicator, indicatorLookback, lookback, lookforward, inputLength);
    }

    private List loadBarsByShift(IFeedDescriptor feedDescriptor, int shift, int lookback, int lookforward)
        throws JFException
    {
        if(lookforward > shift)
            return null;
        int finalShift = shift - lookforward;
        IBar shiftedBar = null;
        if(DataType.PRICE_RANGE_AGGREGATION.equals(feedDescriptor.getDataType()))
            shiftedBar = history.getRangeBar(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), finalShift);
        else
        if(DataType.POINT_AND_FIGURE.equals(feedDescriptor.getDataType()))
            shiftedBar = history.getPointAndFigure(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), finalShift);
        else
        if(DataType.TICK_BAR.equals(feedDescriptor.getDataType()))
            shiftedBar = history.getTickBar(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), finalShift);
        else
            throw new JFException((new StringBuilder()).append("Unsupported data type [").append(feedDescriptor.getDataType()).append("]").toString());
        if(shiftedBar == null)
            return null;
        List result = new ArrayList();
        if(lookback + lookforward <= 0)
        {
            result.add(shiftedBar);
        } else
        {
            long time = shiftedBar.getTime();
            int beforeCount = lookback + lookforward + 1;
            if(DataType.PRICE_RANGE_AGGREGATION.equals(feedDescriptor.getDataType()))
            {
                List bars = history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), beforeCount, time, 0);
                result.addAll(bars);
            } else
            if(DataType.POINT_AND_FIGURE.equals(feedDescriptor.getDataType()))
            {
                List bars = history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), beforeCount, time, 0);
                result.addAll(bars);
            } else
            if(DataType.TICK_BAR.equals(feedDescriptor.getDataType()))
            {
                List bars = history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), beforeCount, time, 0);
                result.addAll(bars);
            }
        }
        return result.isEmpty() ? null : result;
    }

    public Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide offerSides[], String functionName, com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], Object optParams[], int numberOfBarsBefore, long time, int numberOfBarsAfter)
        throws JFException
    {
        if(feedDescriptor == null)
            throw new JFException("FeedDescriptor is null");
        if(feedDescriptor.getInstrument() == null)
            throw new JFException("Instrument is null");
        if(DataType.TICKS.equals(feedDescriptor.getDataType()) || DataType.TIME_PERIOD_AGGREGATION.equals(feedDescriptor.getDataType()))
            return calculateIndicator(feedDescriptor.getInstrument(), feedDescriptor.getPeriod(), offerSides, functionName, inputTypes, optParams, Filter.NO_FILTER, numberOfBarsBefore, time, numberOfBarsAfter);
        IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
        Object aobj[];
        checkSide(offerSides);
        history.validateBeforeTimeAfter(feedDescriptor.getInstrument(), numberOfBarsBefore, time, numberOfBarsAfter);
        if(indicatorHolder == null)
            throw new JFException((new StringBuilder()).append("Indicator with name ").append(functionName).append(" was not found").toString());
        int lookSides[] = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
        aobj = calculateIndicator(feedDescriptor, offerSides, inputTypes, numberOfBarsBefore, time, numberOfBarsAfter, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
        if(indicatorHolder != null)
        {
            indicatorHolder.getIndicatorContext().setFeedDescriptor(null);
            cacheIndicator(functionName, indicatorHolder);
        }
        return aobj;
    }

    private Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide offerSides[], com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], final int numberOfBarsBefore, final long time, final int numberOfBarsAfter, 
            IndicatorHolder indicatorHolder, int indicatorLookback, final int lookback, final int lookforward)
        throws JFException
    {
        int inputLength = feedDataToIndicator(new BarsLoadable() {

            public List load(IFeedDescriptor feedDescriptor)
                throws JFException
            {
                return loadBarsBeforeTimeAfter(feedDescriptor, numberOfBarsBefore, time, numberOfBarsAfter, lookback, lookforward);
            }

        }, feedDescriptor, offerSides, inputTypes, indicatorHolder, indicatorLookback, lookback, lookforward);
        IIndicator indicator = indicatorHolder.getIndicator();
        if(inputLength <= 0)
            return getEmptyIndicatorCalculationResult(indicator);
        else
            return calculateIndicator(numberOfBarsBefore, time, numberOfBarsAfter, indicator, indicatorLookback, lookback, lookforward, inputLength);
    }

    private List loadBarsBeforeTimeAfter(IFeedDescriptor feedDescriptor, int numberOfBarsBefore, long time, int numberOfBarsAfter, int lookback, int lookforward)
        throws JFException
    {
        List data = new ArrayList();
        int beforeCount = numberOfBarsBefore + lookback;
        int afterCount = numberOfBarsAfter + lookforward;
        if(DataType.PRICE_RANGE_AGGREGATION.equals(feedDescriptor.getDataType()))
        {
            List bars = history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), beforeCount, time, afterCount);
            data.addAll(bars);
        } else
        if(DataType.POINT_AND_FIGURE.equals(feedDescriptor.getDataType()))
        {
            List bars = history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), beforeCount, time, afterCount);
            data.addAll(bars);
        } else
        if(DataType.TICK_BAR.equals(feedDescriptor.getDataType()))
        {
            List bars = history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), beforeCount, time, afterCount);
            data.addAll(bars);
        } else
        {
            throw new JFException((new StringBuilder()).append("Unsupported data type [").append(feedDescriptor.getDataType()).append("]").toString());
        }
        List result = data.size() >= beforeCount + afterCount ? data : null;
        return result;
    }

    private Object[] getEmptyIndicatorCalculationResult(IIndicator indicator)
    {
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        Object ret[] = new Object[indicatorInfo.getNumberOfOutputs()];
        int k = 0;
        for(int l = indicatorInfo.getNumberOfOutputs(); k < l; k++)
        {
            OutputParameterInfo outInfo = indicator.getOutputParameterInfo(k);
            switch(outInfo.getType().ordinal())
            {
            case 1: // '\001'
                ret[k] = Double.valueOf((0.0D / 0.0D));
                break;

            case 2: // '\002'
                ret[k] = Integer.valueOf(0x80000000);
                break;

            case 3: // '\003'
                ret[k] = null;
                break;
            }
        }

        return ret;
    }

    private int feedDataToIndicator(BarsLoadable loadable, IFeedDescriptor feedDescriptor, OfferSide offerSides[], com.dukascopy.api.IIndicators.AppliedPrice inputTypes[], IndicatorHolder indicatorHolder, int indicatorLookback, int lookback, 
            int lookforward)
        throws JFException
    {
        if(feedDescriptor == null)
            throw new JFException("FeedDescriptor is null");
        int inputLength = 0x80000000;
        IIndicator indicator = indicatorHolder.getIndicator();
        IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
        IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
        OfferSide offerSide = indicatorInfo.getNumberOfInputs() <= 0 ? OfferSide.BID : offerSides[0];
        feedDescriptor.setOfferSide(offerSide);
        indicatorContext.setFeedDescriptor(feedDescriptor);
        int i = 0;
        for(int j = indicatorInfo.getNumberOfInputs(); i < j; i++)
        {
            InputParameterInfo info = indicator.getInputParameterInfo(i);
            if(info.getOfferSide() != null || info.getInstrument() != null || info.getPeriod() != null)
                throw new JFException((new StringBuilder()).append("Indicator [").append(indicatorInfo.getName()).append("] cannot be calculated for data type ").append(feedDescriptor.getDataType()).toString());
            OfferSide currentOfferSide = offerSides[i];
            IFeedDescriptor fd = new FeedDescriptor(feedDescriptor);
            fd.setOfferSide(currentOfferSide);
            List bars = loadable.load(fd);
            Object inputData = null;
            if(bars == null || bars.isEmpty())
                return 0;
            inputLength = bars.size();
            inputData = barsToReal(bars, info.getType(), info.getType() != com.dukascopy.api.indicators.InputParameterInfo.Type.DOUBLE ? null : inputTypes[i]);
            try
            {
                indicator.setInputParameter(i, inputData);
            }
            catch(Throwable t)
            {
                LOGGER.error(t.getMessage(), t);
                String error = StrategyWrapper.representError(indicator, t);
                NotificationUtilsProvider.getNotificationUtils().postErrorMessage((new StringBuilder()).append("Error in indicator: ").append(error).toString(), t, true);
                throw new JFException(t);
            }
        }

        return inputLength;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Indicators.class);
    private static final int UNSTABLE_PERIOD_LOOKBACK = 100;
    protected History history;
    protected final Map cachedIndicators = Collections.synchronizedMap(new LinkedHashMap(50, 0.75F, true) {

        protected boolean removeEldestEntry(java.util.Map.Entry eldest)
        {
            return size() > 15;
        }

    });
    private final Map customIndicators = Collections.synchronizedMap(new LinkedHashMap());
    protected Period dailyFilterPeriod;
    @Override
    public IIndicator getIndicator(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double sar(Instrument instrument, Period period, OfferSide side,
            double acceleration, double maximum, int shift) throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double sarExt(Instrument instrument, Period period, OfferSide side,
            double startValue, double offsetOnReverse,
            double accelerationInitLong, double accelerationLong,
            double accelerationMaxLong, double accelerationInitShort,
            double accelerationShort, double accelerationMaxShort, int shift)
            throws JFException {
        // TODO Auto-generated method stub
        return 0;
    }
}