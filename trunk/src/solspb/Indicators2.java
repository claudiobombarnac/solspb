package solspb;
	/*      */ 
	/*      */ import com.dukascopy.api.ConnectorIndicator;
	/*      */ import com.dukascopy.api.DataType;
	/*      */ import com.dukascopy.api.Filter;
	/*      */ import com.dukascopy.api.IBar;
	/*      */ import com.dukascopy.api.IIndicators;
	/*      */ import com.dukascopy.api.IIndicators.AppliedPrice;
	/*      */ import com.dukascopy.api.IIndicators.MaType;
	/*      */ import com.dukascopy.api.ITick;
	/*      */ import com.dukascopy.api.Instrument;
	/*      */ import com.dukascopy.api.JFException;
	/*      */ import com.dukascopy.api.OfferSide;
	/*      */ import com.dukascopy.api.Period;
	/*      */ import com.dukascopy.api.feed.FeedDescriptor;
	/*      */ import com.dukascopy.api.feed.IFeedDescriptor;
	/*      */ import com.dukascopy.api.feed.IPointAndFigure;
	/*      */ import com.dukascopy.api.feed.IRangeBar;
	/*      */ import com.dukascopy.api.feed.ITickBar;
import com.dukascopy.api.impl.CustIndicatorWrapper;
import com.dukascopy.api.impl.IndicatorContext;
import com.dukascopy.api.impl.IndicatorHolder;
import com.dukascopy.api.impl.RateDataIndicatorCalculationResultWraper;
import com.dukascopy.api.impl.StrategyWrapper;
import com.dukascopy.api.impl.TaLibException;
	/*      */ import com.dukascopy.api.indicators.IIndicator;
	/*      */ import com.dukascopy.api.indicators.IndicatorInfo;
	/*      */ import com.dukascopy.api.indicators.IndicatorResult;
	/*      */ import com.dukascopy.api.indicators.InputParameterInfo;
	/*      */ import com.dukascopy.api.indicators.InputParameterInfo.Type;
	/*      */ import com.dukascopy.api.indicators.OutputParameterInfo;
	/*      */ import com.dukascopy.charts.data.datacache.CandleData;
	/*      */ import com.dukascopy.charts.data.datacache.Data;
	/*      */ import com.dukascopy.charts.data.datacache.DataCacheException;
	/*      */ import com.dukascopy.charts.data.datacache.DataCacheUtils;
	/*      */ import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;
	/*      */ import com.dukascopy.charts.data.datacache.IntraperiodCandlesGenerator;
	/*      */ import com.dukascopy.charts.math.dataprovider.AbstractDataProvider;
	/*      */ import com.dukascopy.charts.math.dataprovider.AbstractDataProvider.IndicatorData;
	/*      */ import com.dukascopy.charts.math.dataprovider.CandleDataSequence;
	/*      */ import com.dukascopy.charts.math.dataprovider.IDataSequence;
	/*      */ import com.dukascopy.charts.math.dataprovider.TickDataSequence;
	/*      */ import com.dukascopy.charts.math.indicators.IndicatorsProvider;
	/*      */ import com.dukascopy.dds2.greed.util.INotificationUtils;
	/*      */ import com.dukascopy.dds2.greed.util.IndicatorHelper;
	/*      */ import com.dukascopy.dds2.greed.util.NotificationUtilsProvider;
	/*      */ import java.io.File;
	/*      */ import java.security.AccessController;
	/*      */ import java.security.PrivilegedActionException;
	/*      */ import java.security.PrivilegedExceptionAction;
	/*      */ import java.text.SimpleDateFormat;
	/*      */ import java.util.ArrayList;
	/*      */ import java.util.Arrays;
	/*      */ import java.util.Collection;
	/*      */ import java.util.Collections;
	/*      */ import java.util.Date;
	/*      */ import java.util.LinkedHashMap;
	/*      */ import java.util.LinkedHashSet;
	/*      */ import java.util.List;
	/*      */ import java.util.Map;
	/*      */ import java.util.Map.Entry;
	/*      */ import java.util.TimeZone;
	/*      */ import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	/*      */ 
	/*      */ public class Indicators2
	/*      */   implements IIndicators
	/*      */ {
	/*      */   private static final Logger LOGGER = LoggerFactory.getLogger(Indicators2.class);
	/*      */   private static final int UNSTABLE_PERIOD_LOOKBACK = 100;
	/*      */   protected History history;
	/*   66 */   protected final Map<String, IndicatorHolder> cachedIndicators = Collections.synchronizedMap(new LinkedHashMap<String, IndicatorHolder>(50, 0.75F, true) {
	/*      */     protected boolean removeEldestEntry(Map.Entry<String, IndicatorHolder> eldest) {
	/*   68 */       return (size() > 15);
	/*      */     }
	/*      */   });
	/*      */ 
	/*   72 */   private final Map<String, Class<? extends IIndicator>> customIndicators = Collections.synchronizedMap(new LinkedHashMap());
	/*      */ 
	/* 4738 */   protected Period dailyFilterPeriod = Period.DAILY;
	/*      */ 
	/*      */   public Indicators2(History history)
	/*      */   {
	/*   75 */     this.history = history;
	/*      */   }
	/*      */ 
	/*      */   public Collection<String> getAllNames() {
	/*   79 */     Collection result = new LinkedHashSet();
	/*   80 */     if (!(this.customIndicators.isEmpty())) {
	/*   81 */       result.addAll(this.customIndicators.keySet());
	/*      */     }
	/*   83 */     result.addAll(IndicatorsProvider.getInstance().getAllNames());
	/*   84 */     return result;
	/*      */   }
	/*      */ 
	/*      */   public Collection<String> getGroups() {
	/*   88 */     Collection result = new LinkedHashSet();
	/*   89 */     if (!(this.customIndicators.isEmpty())) {
	/*   90 */       for (String indicatorName : this.customIndicators.keySet()) {
	/*      */         try {
	/*   92 */           IndicatorHolder cachedIndicator = getCachedIndicator(indicatorName);
	/*   93 */           result.add(cachedIndicator.getIndicator().getIndicatorInfo().getName());
	/*   94 */           cacheIndicator(indicatorName, cachedIndicator);
	/*      */         } catch (Exception ex) {
	/*   96 */           LOGGER.error("Error while getting cached indicator : " + indicatorName, ex);
	/*      */         }
	/*      */       }
	/*      */     }
	/*  100 */     result.addAll(IndicatorsProvider.getInstance().getGroups());
	/*  101 */     return result;
	/*      */   }
	/*      */ 
	/*      */   public IIndicator getIndicator(String name) {
	/*      */     try {
	/*  106 */       IIndicator indicator = getCustomIndicator(name);
	/*  107 */       if (indicator != null)
	/*  108 */         return indicator;
	/*      */     }
	/*      */     catch (Exception ex) {
	/*  111 */       LOGGER.error("Error while getting custom indicator : " + name, ex);
	/*      */     }
	/*  113 */     return IndicatorsProvider.getInstance().getIndicator(name);
	/*      */   }
	/*      */ 
	/*      */   public Collection<String> getNames(String groupName) {
	/*  117 */     Collection result = new LinkedHashSet();
	/*  118 */     if (!(this.customIndicators.isEmpty())) {
	/*  119 */       for (String indicatorName : this.customIndicators.keySet()) {
	/*      */         try {
	/*  121 */           IndicatorHolder cachedIndicator = getCachedIndicator(indicatorName);
	/*  122 */           IndicatorInfo indicatorInfo = cachedIndicator.getIndicator().getIndicatorInfo();
	/*  123 */           if (indicatorInfo.getGroupName().equalsIgnoreCase(groupName)) {
	/*  124 */             result.add(indicatorInfo.getName());
	/*      */           }
	/*  126 */           cacheIndicator(indicatorName, cachedIndicator);
	/*      */         } catch (Exception ex) {
	/*  128 */           LOGGER.error("Error while getting cached indicator : " + indicatorName, ex);
	/*      */         }
	/*      */       }
	/*      */     }
	/*  132 */     return IndicatorsProvider.getInstance().getNames(groupName);
	/*      */   }
	/*      */ 
	/*      */   public double acos(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/*  136 */     return ((Double)function(instrument, period, side, "ACOS", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] acos(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException {
	/*  140 */     return ((double[])(double[])function(instrument, period, side, "ACOS", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] acos(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/*  145 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ACOS", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ac(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int shift) throws JFException {
	/*  149 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AC", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod) }, shift);
	/*  150 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ac(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, long from, long to) throws JFException {
	/*  154 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AC", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod) }, from, to);
	/*  155 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ac(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  159 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AC", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  160 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double ad(Instrument instrument, Period period, OfferSide side, int shift) throws JFException
	/*      */   {
	/*  165 */     return ((Double)function(instrument, period, side, "AD", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ad(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  169 */     return ((double[])(double[])function(instrument, period, side, "AD", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ad(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  173 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "AD", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double add(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int shift) throws JFException {
	/*  177 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "ADD", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] add(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, long from, long to) throws JFException {
	/*  181 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "ADD", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] add(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  185 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "ADD", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double adOsc(Instrument instrument, Period period, OfferSide side, int fastPeriod, int slowPeriod, int shift) throws JFException {
	/*  189 */     return ((Double)function(instrument, period, side, "ADOSC", null, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] adOsc(Instrument instrument, Period period, OfferSide side, int fastPeriod, int slowPeriod, long from, long to) throws JFException {
	/*  193 */     return ((double[])(double[])function(instrument, period, side, "ADOSC", null, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] adOsc(Instrument instrument, Period period, OfferSide side, int fastPeriod, int slowPeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  197 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ADOSC", null, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double adx(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  201 */     return ((Double)function(instrument, period, side, "ADX", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] adx(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  205 */     return ((double[])(double[])function(instrument, period, side, "ADX", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] adx(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  209 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ADX", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double adxr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  213 */     return ((Double)function(instrument, period, side, "ADXR", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] adxr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  217 */     return ((double[])(double[])function(instrument, period, side, "ADXR", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] adxr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  221 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ADXR", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] alligator(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, int shift) throws JFException
	/*      */   {
	/*  226 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "ALLIGATOR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod) }, shift);
	/*  227 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] alligator(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, long from, long to) throws JFException
	/*      */   {
	/*  232 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "ALLIGATOR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod) }, from, to);
	/*  233 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] alligator(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/*  238 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "ALLIGATOR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  239 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double apo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, int shift) throws JFException {
	/*  243 */     return ((Double)function(instrument, period, side, "APO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] apo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/*  247 */     return ((double[])(double[])function(instrument, period, side, "APO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] apo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  251 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "APO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] aroon(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  255 */     Object[] ret = function(instrument, period, side, "AROON", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/*  256 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] aroon(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  260 */     Object[] ret = function(instrument, period, side, "AROON", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/*  261 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] aroon(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  265 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AROON", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  266 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double aroonOsc(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  270 */     return ((Double)function(instrument, period, side, "AROONOSC", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] aroonOsc(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  274 */     return ((double[])(double[])function(instrument, period, side, "AROONOSC", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] aroonOsc(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  278 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "AROONOSC", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double asin(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/*  282 */     return ((Double)function(instrument, period, side, "ASIN", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] asin(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException {
	/*  286 */     return ((double[])(double[])function(instrument, period, side, "ASIN", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] asin(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  290 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ASIN", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double atan(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/*  294 */     return ((Double)function(instrument, period, side, "ATAN", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] atan(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException {
	/*  298 */     return ((double[])(double[])function(instrument, period, side, "ATAN", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] atan(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  302 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ATAN", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double atr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  306 */     return ((Double)function(instrument, period, side, "ATR", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] atr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  310 */     return ((double[])(double[])function(instrument, period, side, "ATR", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] atr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  314 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ATR", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double avgPrice(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  318 */     return ((Double)function(instrument, period, side, "AVGPRICE", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] avgPrice(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  322 */     return ((double[])(double[])function(instrument, period, side, "AVGPRICE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] avgPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  326 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "AVGPRICE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] awesome(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, IIndicators.MaType fasterMaType, int slowerMaTimePeriod, IIndicators.MaType slowerMaType, int shift) throws JFException
	/*      */   {
	/*  331 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AWESOME", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal()) }, shift);
	/*  332 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] awesome(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, IIndicators.MaType fasterMaType, int slowerMaTimePeriod, IIndicators.MaType slowerMaType, long from, long to) throws JFException
	/*      */   {
	/*  337 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AWESOME", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal()) }, from, to);
	/*  338 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] awesome(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, IIndicators.MaType fasterMaType, int slowerMaTimePeriod, IIndicators.MaType slowerMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/*  343 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AWESOME", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  344 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[] bbands(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDevUp, double nbDevDn, IIndicators.MaType maType, int shift) throws JFException {
	/*  348 */     Object[] ret = function(instrument, period, side, "BBANDS", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDevUp), Double.valueOf(nbDevDn), Integer.valueOf(maType.ordinal()) }, shift);
	/*  349 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] bbands(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDevUp, double nbDevDn, IIndicators.MaType maType, long from, long to) throws JFException {
	/*  353 */     Object[] ret = function(instrument, period, side, "BBANDS", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDevUp), Double.valueOf(nbDevDn), Integer.valueOf(maType.ordinal()) }, from, to);
	/*  354 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] bbands(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDevUp, double nbDevDn, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  358 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BBANDS", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDevUp), Double.valueOf(nbDevDn), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  359 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double beta(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int timePeriod, int shift) throws JFException {
	/*  363 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "BETA", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] beta(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int timePeriod, long from, long to) throws JFException {
	/*  367 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "BETA", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] beta(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  371 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "BETA", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double bear(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  375 */     return ((Double)function(instrument, period, side, "BEARP", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] bear(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  379 */     return ((double[])(double[])function(instrument, period, side, "BEARP", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] bear(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  383 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "BEARP", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double bull(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  387 */     return ((Double)function(instrument, period, side, "BULLP", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] bull(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException
	/*      */   {
	/*  392 */     return ((double[])(double[])function(instrument, period, side, "BULLP", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] bull(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  396 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "BULLP", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] bwmfi(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  400 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BWMFI", null, new Object[0], shift);
	/*  401 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] bwmfi(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  405 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BWMFI", null, new Object[0], from, to);
	/*  406 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] bwmfi(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  410 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BWMFI", null, new Object[0], filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  411 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3] };
	/*      */   }
	/*      */ 
	/*      */   public double bop(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  415 */     return ((Double)function(instrument, period, side, "BOP", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] bop(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  419 */     return ((double[])(double[])function(instrument, period, side, "BOP", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] bop(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  423 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "BOP", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double butterworthFilter(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/*  427 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BUTTERWORTH", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/*  428 */     return ((Double)ret[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] butterworthFilter(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/*  432 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BUTTERWORTH", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/*  433 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] butterworthFilter(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  437 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "BUTTERWORTH", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  438 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] camPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  442 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "CAMPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/*  443 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] camPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  447 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "CAMPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/*  448 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] camPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  452 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "CAMPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*  453 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double cci(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/*  457 */     return ((Double)function(instrument, period, side, "CCI", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] cci(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/*  461 */     return ((double[])(double[])function(instrument, period, side, "CCI", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] cci(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  465 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CCI", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl2Crows(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  469 */     return ((Integer)function(instrument, period, side, "CDL2CROWS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl2Crows(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  473 */     return ((int[])(int[])function(instrument, period, side, "CDL2CROWS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl2Crows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  477 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL2CROWS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl3BlackCrows(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  481 */     return ((Integer)function(instrument, period, side, "CDL3BLACKCROWS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3BlackCrows(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  485 */     return ((int[])(int[])function(instrument, period, side, "CDL3BLACKCROWS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3BlackCrows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  489 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL3BLACKCROWS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl3Inside(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  493 */     return ((Integer)function(instrument, period, side, "CDL3INSIDE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3Inside(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  497 */     return ((int[])(int[])function(instrument, period, side, "CDL3INSIDE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3Inside(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  501 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL3INSIDE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl3LineStrike(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  505 */     return ((Integer)function(instrument, period, side, "CDL3LINESTRIKE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3LineStrike(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  509 */     return ((int[])(int[])function(instrument, period, side, "CDL3LINESTRIKE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3LineStrike(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  513 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL3LINESTRIKE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl3Outside(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  517 */     return ((Integer)function(instrument, period, side, "CDL3OUTSIDE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3Outside(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  521 */     return ((int[])(int[])function(instrument, period, side, "CDL3OUTSIDE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3Outside(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  525 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL3OUTSIDE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl3StarsInSouth(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  529 */     return ((Integer)function(instrument, period, side, "CDL3STARSINSOUTH", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3StarsInSouth(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  533 */     return ((int[])(int[])function(instrument, period, side, "CDL3STARSINSOUTH", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3StarsInSouth(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  537 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL3STARSINSOUTH", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdl3WhiteSoldiers(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  541 */     return ((Integer)function(instrument, period, side, "CDL3WHITESOLDIERS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3WhiteSoldiers(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  545 */     return ((int[])(int[])function(instrument, period, side, "CDL3WHITESOLDIERS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdl3WhiteSoldiers(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  549 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDL3WHITESOLDIERS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlAbandonedBaby(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  553 */     return ((Integer)function(instrument, period, side, "CDLABANDONEDBABY", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlAbandonedBaby(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException {
	/*  557 */     return ((int[])(int[])function(instrument, period, side, "CDLABANDONEDBABY", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlAbandonedBaby(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  561 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLABANDONEDBABY", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlAdvanceBlock(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  565 */     return ((Integer)function(instrument, period, side, "CDLADVANCEBLOCK", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlAdvanceBlock(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  569 */     return ((int[])(int[])function(instrument, period, side, "CDLADVANCEBLOCK", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlAdvanceBlock(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  573 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLADVANCEBLOCK", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlBeltHold(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  577 */     return ((Integer)function(instrument, period, side, "CDLBELTHOLD", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlBeltHold(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  581 */     return ((int[])(int[])function(instrument, period, side, "CDLBELTHOLD", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlBeltHold(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  585 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLBELTHOLD", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlBreakAway(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  589 */     return ((Integer)function(instrument, period, side, "CDLBREAKAWAY", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlBreakAway(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  593 */     return ((int[])(int[])function(instrument, period, side, "CDLBREAKAWAY", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlBreakAway(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  597 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLBREAKAWAY", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlClosingMarubozu(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  601 */     return ((Integer)function(instrument, period, side, "CDLCLOSINGMARUBOZU", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlClosingMarubozu(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  605 */     return ((int[])(int[])function(instrument, period, side, "CDLCLOSINGMARUBOZU", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlClosingMarubozu(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  609 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLCLOSINGMARUBOZU", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlConcealBabySwall(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  613 */     return ((Integer)function(instrument, period, side, "CDLCONCEALBABYSWALL", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlConcealBabySwall(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  617 */     return ((int[])(int[])function(instrument, period, side, "CDLCONCEALBABYSWALL", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlConcealBabySwall(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  621 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLCONCEALBABYSWALL", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlCounterattack(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  625 */     return ((Integer)function(instrument, period, side, "CDLCOUNTERATTACK", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlCounterattack(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  629 */     return ((int[])(int[])function(instrument, period, side, "CDLCOUNTERATTACK", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlCounterattack(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  633 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLCOUNTERATTACK", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlDarkCloudCover(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  637 */     return ((Integer)function(instrument, period, side, "CDLDARKCLOUDCOVER", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDarkCloudCover(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException {
	/*  641 */     return ((int[])(int[])function(instrument, period, side, "CDLDARKCLOUDCOVER", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDarkCloudCover(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  645 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLDARKCLOUDCOVER", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlDoji(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  649 */     return ((Integer)function(instrument, period, side, "CDLDOJI", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDoji(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  653 */     return ((int[])(int[])function(instrument, period, side, "CDLDOJI", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  657 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLDOJI", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlDojiStar(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  661 */     return ((Integer)function(instrument, period, side, "CDLDOJISTAR", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDojiStar(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  665 */     return ((int[])(int[])function(instrument, period, side, "CDLDOJISTAR", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDojiStar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  669 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLDOJISTAR", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlDragonflyDoji(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  673 */     return ((Integer)function(instrument, period, side, "CDLDRAGONFLYDOJI", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDragonflyDoji(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  677 */     return ((int[])(int[])function(instrument, period, side, "CDLDRAGONFLYDOJI", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlDragonflyDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  681 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLDRAGONFLYDOJI", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlEngulfing(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  685 */     return ((Integer)function(instrument, period, side, "CDLENGULFING", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlEngulfing(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  689 */     return ((int[])(int[])function(instrument, period, side, "CDLENGULFING", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlEngulfing(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  693 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLENGULFING", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlEveningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  697 */     return ((Integer)function(instrument, period, side, "CDLEVENINGDOJISTAR", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlEveningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException {
	/*  701 */     return ((int[])(int[])function(instrument, period, side, "CDLEVENINGDOJISTAR", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlEveningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  705 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLEVENINGDOJISTAR", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlEveningStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  709 */     return ((Integer)function(instrument, period, side, "CDLEVENINGSTAR", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlEveningStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException
	/*      */   {
	/*  714 */     return ((int[])(int[])function(instrument, period, side, "CDLEVENINGSTAR", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlEveningStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/*  719 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLEVENINGSTAR", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlGapSideSideWhite(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  723 */     return ((Integer)function(instrument, period, side, "CDLGAPSIDESIDEWHITE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlGapSideSideWhite(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  727 */     return ((int[])(int[])function(instrument, period, side, "CDLGAPSIDESIDEWHITE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlGapSideSideWhite(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  731 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLGAPSIDESIDEWHITE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlGravestoneDoji(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  735 */     return ((Integer)function(instrument, period, side, "CDLGRAVESTONEDOJI", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlGravestoneDoji(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  739 */     return ((int[])(int[])function(instrument, period, side, "CDLGRAVESTONEDOJI", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlGravestoneDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  743 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLGRAVESTONEDOJI", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHammer(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  747 */     return ((Integer)function(instrument, period, side, "CDLHAMMER", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHammer(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  751 */     return ((int[])(int[])function(instrument, period, side, "CDLHAMMER", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHammer(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  755 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHAMMER", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHangingMan(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  759 */     return ((Integer)function(instrument, period, side, "CDLHANGINGMAN", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHangingMan(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  763 */     return ((int[])(int[])function(instrument, period, side, "CDLHANGINGMAN", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHangingMan(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  767 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHANGINGMAN", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHarami(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  771 */     return ((Integer)function(instrument, period, side, "CDLHARAMI", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHarami(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  775 */     return ((int[])(int[])function(instrument, period, side, "CDLHARAMI", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHarami(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  779 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHARAMI", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHaramiCross(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  783 */     return ((Integer)function(instrument, period, side, "CDLHARAMICROSS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHaramiCross(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  787 */     return ((int[])(int[])function(instrument, period, side, "CDLHARAMICROSS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHaramiCross(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  791 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHARAMICROSS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHighWave(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  795 */     return ((Integer)function(instrument, period, side, "CDLHIGHWAVE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHighWave(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  799 */     return ((int[])(int[])function(instrument, period, side, "CDLHIGHWAVE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHighWave(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  803 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHIGHWAVE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHikkake(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  807 */     return ((Integer)function(instrument, period, side, "CDLHIKKAKE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHikkake(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  811 */     return ((int[])(int[])function(instrument, period, side, "CDLHIKKAKE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHikkake(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  815 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHIKKAKE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHikkakeMod(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  819 */     return ((Integer)function(instrument, period, side, "CDLHIKKAKEMOD", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHikkakeMod(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  823 */     return ((int[])(int[])function(instrument, period, side, "CDLHIKKAKEMOD", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHikkakeMod(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  827 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHIKKAKEMOD", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlHomingPigeon(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  831 */     return ((Integer)function(instrument, period, side, "CDLHOMINGPIGEON", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHomingPigeon(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  835 */     return ((int[])(int[])function(instrument, period, side, "CDLHOMINGPIGEON", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlHomingPigeon(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  839 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLHOMINGPIGEON", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlIdentical3Crows(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  843 */     return ((Integer)function(instrument, period, side, "CDLIDENTICAL3CROWS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlIdentical3Crows(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  847 */     return ((int[])(int[])function(instrument, period, side, "CDLIDENTICAL3CROWS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlIdentical3Crows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  851 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLIDENTICAL3CROWS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlInNeck(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  855 */     return ((Integer)function(instrument, period, side, "CDLINNECK", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlInNeck(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  859 */     return ((int[])(int[])function(instrument, period, side, "CDLINNECK", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlInNeck(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  863 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLINNECK", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlInvertedHammer(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  867 */     return ((Integer)function(instrument, period, side, "CDLINVERTEDHAMMER", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlInvertedHammer(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  871 */     return ((int[])(int[])function(instrument, period, side, "CDLINVERTEDHAMMER", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlInvertedHammer(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  875 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLINVERTEDHAMMER", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlKicking(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  879 */     return ((Integer)function(instrument, period, side, "CDLKICKING", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlKicking(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  883 */     return ((int[])(int[])function(instrument, period, side, "CDLKICKING", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlKicking(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  887 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLKICKING", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlKickingByLength(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  891 */     return ((Integer)function(instrument, period, side, "CDLKICKINGBYLENGTH", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlKickingByLength(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  895 */     return ((int[])(int[])function(instrument, period, side, "CDLKICKINGBYLENGTH", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlKickingByLength(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  899 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLKICKINGBYLENGTH", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlLadderBotton(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  903 */     return ((Integer)function(instrument, period, side, "CDLLADDERBOTTOM", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlLadderBotton(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  907 */     return ((int[])(int[])function(instrument, period, side, "CDLLADDERBOTTOM", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlLadderBotton(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  911 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLLADDERBOTTOM", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlLongLeggedDoji(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  915 */     return ((Integer)function(instrument, period, side, "CDLLONGLEGGEDDOJI", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlLongLeggedDoji(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  919 */     return ((int[])(int[])function(instrument, period, side, "CDLLONGLEGGEDDOJI", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlLongLeggedDoji(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  923 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLLONGLEGGEDDOJI", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlLongLine(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  927 */     return ((Integer)function(instrument, period, side, "CDLLONGLINE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlLongLine(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  931 */     return ((int[])(int[])function(instrument, period, side, "CDLLONGLINE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlLongLine(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  935 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLLONGLINE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlMarubozu(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  939 */     return ((Integer)function(instrument, period, side, "CDLMARUBOZU", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMarubozu(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  943 */     return ((int[])(int[])function(instrument, period, side, "CDLMARUBOZU", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMarubozu(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  947 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLMARUBOZU", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlMatchingLow(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/*  951 */     return ((Integer)function(instrument, period, side, "CDLMATCHINGLOW", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMatchingLow(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/*  955 */     return ((int[])(int[])function(instrument, period, side, "CDLMATCHINGLOW", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMatchingLow(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  959 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLMATCHINGLOW", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlMathold(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  963 */     return ((Integer)function(instrument, period, side, "CDLMATHOLD", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMathold(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException {
	/*  967 */     return ((int[])(int[])function(instrument, period, side, "CDLMATHOLD", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMathold(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  971 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLMATHOLD", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlMorningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  975 */     return ((Integer)function(instrument, period, side, "CDLMORNINGDOJISTAR", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMorningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException {
	/*  979 */     return ((int[])(int[])function(instrument, period, side, "CDLMORNINGDOJISTAR", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMorningDojiStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/*  983 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLMORNINGDOJISTAR", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlMorningStar(Instrument instrument, Period period, OfferSide side, double penetration, int shift) throws JFException {
	/*  987 */     return ((Integer)function(instrument, period, side, "CDLMORNINGSTAR", null, new Object[] { Double.valueOf(penetration) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMorningStar(Instrument instrument, Period period, OfferSide side, double penetration, long from, long to) throws JFException
	/*      */   {
	/*  992 */     return ((int[])(int[])function(instrument, period, side, "CDLMORNINGSTAR", null, new Object[] { Double.valueOf(penetration) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlMorningStar(Instrument instrument, Period period, OfferSide side, double penetration, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/*  997 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLMORNINGSTAR", null, new Object[] { Double.valueOf(penetration) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlOnNeck(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1001 */     return ((Integer)function(instrument, period, side, "CDLONNECK", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlOnNeck(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1005 */     return ((int[])(int[])function(instrument, period, side, "CDLONNECK", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlOnNeck(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1009 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLONNECK", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlPiercing(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1013 */     return ((Integer)function(instrument, period, side, "CDLPIERCING", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlPiercing(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1017 */     return ((int[])(int[])function(instrument, period, side, "CDLPIERCING", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlPiercing(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1021 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLPIERCING", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlRickshawMan(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1025 */     return ((Integer)function(instrument, period, side, "CDLRICKSHAWMAN", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlRickshawMan(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1029 */     return ((int[])(int[])function(instrument, period, side, "CDLRICKSHAWMAN", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlRickshawMan(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1033 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLRICKSHAWMAN", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlRiseFall3Methods(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1037 */     return ((Integer)function(instrument, period, side, "CDLRISEFALL3METHODS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlRiseFall3Methods(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1041 */     return ((int[])(int[])function(instrument, period, side, "CDLRISEFALL3METHODS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlRiseFall3Methods(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1045 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLRISEFALL3METHODS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlSeparatingLines(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1049 */     return ((Integer)function(instrument, period, side, "CDLSEPARATINGLINES", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlSeparatingLines(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1053 */     return ((int[])(int[])function(instrument, period, side, "CDLSEPARATINGLINES", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlSeparatingLines(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1057 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLSEPARATINGLINES", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlShootingStar(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1061 */     return ((Integer)function(instrument, period, side, "CDLSHOOTINGSTAR", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlShootingStar(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1065 */     return ((int[])(int[])function(instrument, period, side, "CDLSHOOTINGSTAR", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlShootingStar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1069 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLSHOOTINGSTAR", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlShortLine(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1073 */     return ((Integer)function(instrument, period, side, "CDLSHORTLINE", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlShortLine(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1077 */     return ((int[])(int[])function(instrument, period, side, "CDLSHORTLINE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlShortLine(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1081 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLSHORTLINE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlSpinningTop(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1085 */     return ((Integer)function(instrument, period, side, "CDLSPINNINGTOP", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlSpinningTop(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1089 */     return ((int[])(int[])function(instrument, period, side, "CDLSPINNINGTOP", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlSpinningTop(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1093 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLSPINNINGTOP", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlStalledPattern(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1097 */     return ((Integer)function(instrument, period, side, "CDLSTALLEDPATTERN", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlStalledPattern(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1101 */     return ((int[])(int[])function(instrument, period, side, "CDLSTALLEDPATTERN", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlStalledPattern(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1105 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLSTALLEDPATTERN", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlStickSandwich(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1109 */     return ((Integer)function(instrument, period, side, "CDLSTICKSANDWICH", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlStickSandwich(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1113 */     return ((int[])(int[])function(instrument, period, side, "CDLSTICKSANDWICH", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlStickSandwich(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1117 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLSTICKSANDWICH", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlTakuri(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1121 */     return ((Integer)function(instrument, period, side, "CDLTAKURI", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlTakuri(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1125 */     return ((int[])(int[])function(instrument, period, side, "CDLTAKURI", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlTakuri(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1129 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLTAKURI", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlTasukiGap(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1133 */     return ((Integer)function(instrument, period, side, "CDLTASUKIGAP", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlTasukiGap(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1137 */     return ((int[])(int[])function(instrument, period, side, "CDLTASUKIGAP", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlTasukiGap(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1141 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLTASUKIGAP", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlThrusting(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1145 */     return ((Integer)function(instrument, period, side, "CDLTHRUSTING", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlThrusting(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1149 */     return ((int[])(int[])function(instrument, period, side, "CDLTHRUSTING", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlThrusting(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1153 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLTHRUSTING", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlTristar(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1157 */     return ((Integer)function(instrument, period, side, "CDLTRISTAR", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlTristar(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1161 */     return ((int[])(int[])function(instrument, period, side, "CDLTRISTAR", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlTristar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1165 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLTRISTAR", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlUnique3River(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1169 */     return ((Integer)function(instrument, period, side, "CDLUNIQUE3RIVER", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlUnique3River(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1173 */     return ((int[])(int[])function(instrument, period, side, "CDLUNIQUE3RIVER", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlUnique3River(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1177 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLUNIQUE3RIVER", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlUpsideGap2Crows(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1181 */     return ((Integer)function(instrument, period, side, "CDLUPSIDEGAP2CROWS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlUpsideGap2Crows(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1185 */     return ((int[])(int[])function(instrument, period, side, "CDLUPSIDEGAP2CROWS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlUpsideGap2Crows(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1189 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLUPSIDEGAP2CROWS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int cdlXsideGap3Methods(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1193 */     return ((Integer)function(instrument, period, side, "CDLXSIDEGAP3METHODS", null, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlXsideGap3Methods(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1197 */     return ((int[])(int[])function(instrument, period, side, "CDLXSIDEGAP3METHODS", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] cdlXsideGap3Methods(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1201 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CDLXSIDEGAP3METHODS", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ceil(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1205 */     return ((Double)function(instrument, period, side, "CEIL", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ceil(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1210 */     return ((double[])(double[])function(instrument, period, side, "CEIL", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ceil(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1215 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CEIL", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double cmo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1219 */     return ((Double)function(instrument, period, side, "CMO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] cmo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1223 */     return ((double[])(double[])function(instrument, period, side, "CMO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] cmo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1227 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "CMO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] cog(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int smoothPeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 1231 */     Object[] res = function(instrument, period, side, "COG", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(smoothPeriod), Integer.valueOf(maType.ordinal()) }, shift);
	/* 1232 */     return new double[] { ((Double)res[0]).doubleValue(), ((Double)res[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] cog(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int smoothPeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 1236 */     Object[] res = function(instrument, period, side, "COG", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(smoothPeriod), Integer.valueOf(maType.ordinal()) }, from, to);
	/* 1237 */     return new double[][] { (double[])(double[])res[0], (double[])(double[])res[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] cog(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int smoothPeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1241 */     Object[] res = calculateIndicator(instrument, period, new OfferSide[] { side }, "COG", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(smoothPeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1242 */     return new double[][] { (double[])(double[])res[0], (double[])(double[])res[1] };
	/*      */   }
	/*      */ 
	/*      */   public double correl(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int timePeriod, int shift) throws JFException {
	/* 1246 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "CORREL", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] correl(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int timePeriod, long from, long to) throws JFException {
	/* 1250 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "CORREL", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] correl(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1254 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "CORREL", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double cos(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1258 */     return ((Double)function(instrument, period, side, "COS", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] cos(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1263 */     return ((double[])(double[])function(instrument, period, side, "COS", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] cos(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1268 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "COS", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double cosh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1272 */     return ((Double)function(instrument, period, side, "COSH", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] cosh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1277 */     return ((double[])(double[])function(instrument, period, side, "COSH", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] cosh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1282 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "COSH", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double dema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1286 */     return ((Double)function(instrument, period, side, "DEMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] dema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1290 */     return ((double[])(double[])function(instrument, period, side, "DEMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] dema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1294 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "DEMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double div(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int shift) throws JFException
	/*      */   {
	/* 1299 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "DIV", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] div(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, long from, long to) throws JFException
	/*      */   {
	/* 1304 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "DIV", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] div(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1309 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "DIV", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] dmi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 1313 */     Object[] res = function(instrument, period, side, "DMI", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 1314 */     return new double[] { ((Double)res[0]).doubleValue(), ((Double)res[1]).doubleValue(), ((Double)res[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] dmi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 1318 */     Object[] res = function(instrument, period, side, "DMI", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 1319 */     return new double[][] { (double[])(double[])res[0], (double[])(double[])res[1], (double[])(double[])res[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] dmi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1323 */     Object[] res = calculateIndicator(instrument, period, new OfferSide[] { side }, "DMI", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1324 */     return new double[][] { (double[])(double[])res[0], (double[])(double[])res[1], (double[])(double[])res[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[] donchian(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 1328 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "DONCHIANCHANNEL", new IIndicators.AppliedPrice[0], new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 1329 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] donchian(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 1333 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "DONCHIANCHANNEL", new IIndicators.AppliedPrice[0], new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 1334 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] donchian(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1338 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "DONCHIANCHANNEL", new IIndicators.AppliedPrice[0], new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1339 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double dx(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 1343 */     return ((Double)function(instrument, period, side, "DX", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] dx(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 1347 */     return ((double[])(double[])function(instrument, period, side, "DX", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] dx(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1351 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "DX", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1355 */     return ((Double)function(instrument, period, side, "EMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1359 */     return ((double[])(double[])function(instrument, period, side, "EMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1363 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "EMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] emaEnvelope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, int shift) throws JFException
	/*      */   {
	/* 1368 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "EMAEnvelope", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, shift);
	/* 1369 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] emaEnvelope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, long from, long to) throws JFException
	/*      */   {
	/* 1374 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "EMAEnvelope", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, from, to);
	/* 1375 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] emaEnvelope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1380 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "EMAEnvelope", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1381 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double exp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1385 */     return ((Double)function(instrument, period, side, "EXP", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] exp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1390 */     return ((double[])(double[])function(instrument, period, side, "EXP", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] exp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1395 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "EXP", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double floor(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1399 */     return ((Double)function(instrument, period, side, "FLOOR", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] floor(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1404 */     return ((double[])(double[])function(instrument, period, side, "FLOOR", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] floor(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1409 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "FLOOR", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double force(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 1413 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "FORCEI", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, shift);
	/* 1414 */     return ((Double)ret[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] force(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 1418 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "FORCEI", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, from, to);
	/* 1419 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] force(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
	/*      */     throws JFException
	/*      */   {
	/* 1426 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "FORCEI", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1427 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] fractal(Instrument instrument, Period period, OfferSide side, int barsOnSides, int shift) throws JFException {
	/* 1431 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "FRACTAL", null, new Object[] { Integer.valueOf(barsOnSides) }, shift);
	/* 1432 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] fractal(Instrument instrument, Period period, OfferSide side, int barsOnSides, long from, long to) throws JFException {
	/* 1436 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "FRACTAL", null, new Object[] { Integer.valueOf(barsOnSides) }, from, to);
	/* 1437 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] fractal(Instrument instrument, Period period, OfferSide side, int barsOnSides, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1441 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "FRACTAL", null, new Object[] { Integer.valueOf(barsOnSides) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1442 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] fractalLines(Instrument instrument, Period period, OfferSide side, int barsOnSides, int shift) throws JFException {
	/* 1446 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "FRACTALLINES", null, new Object[] { Integer.valueOf(barsOnSides) }, shift);
	/* 1447 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] fractalLines(Instrument instrument, Period period, OfferSide side, int barsOnSides, long from, long to) throws JFException {
	/* 1451 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "FRACTALLINES", null, new Object[] { Integer.valueOf(barsOnSides) }, from, to);
	/* 1452 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] fractalLines(Instrument instrument, Period period, OfferSide side, int barsOnSides, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1456 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "FRACTALLINES", null, new Object[] { Integer.valueOf(barsOnSides) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1457 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] fibPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 1461 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "FIBPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 1462 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] fibPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 1466 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "FIBPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 1467 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] fibPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1471 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "FIBPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1472 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double[] gator(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, int shift) throws JFException
	/*      */   {
	/* 1477 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "GATOR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod) }, shift);
	/* 1478 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] gator(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, long from, long to) throws JFException {
	/* 1482 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "GATOR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod) }, from, to);
	/* 1483 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] gator(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int jawTimePeriod, int teethTimePeriod, int lipsTimePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1487 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "GATOR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(jawTimePeriod), Integer.valueOf(teethTimePeriod), Integer.valueOf(lipsTimePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1488 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] heikenAshi(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1492 */     Object ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HeikinAshi", null, null, shift)[0];
	/* 1493 */     return ((double[])(double[])ret);
	/*      */   }
	/*      */ 
	/*      */   public double[][] heikenAshi(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException
	/*      */   {
	/* 1498 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HeikinAshi", null, null, from, to);
	/* 1499 */     double[][] retD = new double[((Object[])(Object[])ret[0]).length][];
	/* 1500 */     System.arraycopy(ret[0], 0, retD, 0, retD.length);
	/* 1501 */     return retD;
	/*      */   }
	/*      */ 
	/*      */   public double[][] heikenAshi(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1506 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HeikinAshi", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1507 */     double[][] retD = new double[((Object[])(Object[])ret[0]).length][];
	/* 1508 */     System.arraycopy(ret[0], 0, retD, 0, retD.length);
	/* 1509 */     return retD;
	/*      */   }
	/*      */ 
	/*      */   public double[] heikinAshi(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1513 */     Object ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HeikinAshi", null, null, shift)[0];
	/* 1514 */     return ((double[])(double[])ret);
	/*      */   }
	/*      */ 
	/*      */   public double[][] heikinAshi(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException
	/*      */   {
	/* 1519 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HeikinAshi", null, null, from, to);
	/* 1520 */     double[][] retD = new double[((Object[])(Object[])ret[0]).length][];
	/* 1521 */     System.arraycopy(ret[0], 0, retD, 0, retD.length);
	/* 1522 */     return retD;
	/*      */   }
	/*      */ 
	/*      */   public double[][] heikinAshi(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1527 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HeikinAshi", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1528 */     double[][] retD = new double[((Object[])(Object[])ret[0]).length][];
	/* 1529 */     System.arraycopy(ret[0], 0, retD, 0, retD.length);
	/* 1530 */     return retD;
	/*      */   }
	/*      */ 
	/*      */   public double hma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1534 */     return ((Double)function(instrument, period, side, "HMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] hma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1538 */     return ((double[])(double[])function(instrument, period, side, "HMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] hma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1542 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "HMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ht_dcperiod(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1546 */     return ((Double)function(instrument, period, side, "HT_DCPERIOD", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_dcperiod(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1551 */     return ((double[])(double[])function(instrument, period, side, "HT_DCPERIOD", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_dcperiod(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1556 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "HT_DCPERIOD", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ht_dcphase(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1560 */     return ((Double)function(instrument, period, side, "HT_DCPHASE", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_dcphase(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1565 */     return ((double[])(double[])function(instrument, period, side, "HT_DCPHASE", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_dcphase(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1570 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "HT_DCPHASE", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_phasor(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1574 */     Object[] ret = function(instrument, period, side, "HT_PHASOR", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift);
	/* 1575 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ht_phasor(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1580 */     Object[] ret = function(instrument, period, side, "HT_PHASOR", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to);
	/* 1581 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ht_phasor(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1586 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HT_PHASOR", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1587 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_sine(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1591 */     Object[] ret = function(instrument, period, side, "HT_SINE", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift);
	/* 1592 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ht_sine(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1597 */     Object[] ret = function(instrument, period, side, "HT_SINE", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to);
	/* 1598 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ht_sine(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1603 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "HT_SINE", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1604 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double ht_trendline(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1608 */     return ((Double)function(instrument, period, side, "HT_TRENDLINE", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_trendline(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1613 */     return ((double[])(double[])function(instrument, period, side, "HT_TRENDLINE", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ht_trendline(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1618 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "HT_TRENDLINE", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int ht_trendmode(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1622 */     return ((Integer)function(instrument, period, side, "HT_TRENDMODE", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] ht_trendmode(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 1627 */     return ((int[])(int[])function(instrument, period, side, "HT_TRENDMODE", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] ht_trendmode(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1632 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "HT_TRENDMODE", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ichimoku(Instrument instrument, Period period, OfferSide side, int tenkan, int kijun, int senkou, int shift) throws JFException {
	/* 1636 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "ICHIMOKU", null, new Object[] { Integer.valueOf(tenkan), Integer.valueOf(kijun), Integer.valueOf(senkou) }, shift);
	/* 1637 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), Double.valueOf(((double[])(double[])ret[5])[0]).doubleValue(), Double.valueOf(((double[])(double[])ret[5])[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ichimoku(Instrument instrument, Period period, OfferSide side, int tenkan, int kijun, int senkou, long from, long to) throws JFException {
	/* 1641 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "ICHIMOKU", null, new Object[] { Integer.valueOf(tenkan), Integer.valueOf(kijun), Integer.valueOf(senkou) }, from, to);
	/* 1642 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])((Object[])(Object[])ret[5])[0], (double[])(double[])((Object[])(Object[])ret[5])[0] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] ichimoku(Instrument instrument, Period period, OfferSide side, int tenkan, int kijun, int senkou, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1646 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "ICHIMOKU", null, new Object[] { Integer.valueOf(tenkan), Integer.valueOf(kijun), Integer.valueOf(senkou) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1647 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])((Object[])(Object[])ret[5])[0], (double[])(double[])((Object[])(Object[])ret[5])[0] };
	/*      */   }
	/*      */ 
	/*      */   public double kairi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 1651 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "KAIRI", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, shift);
	/* 1652 */     return ((Double)ret[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] kairi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 1656 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "KAIRI", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, from, to);
	/* 1657 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] kairi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1661 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "KAIRI", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1662 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double kama(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1666 */     return ((Double)function(instrument, period, side, "KAMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] kama(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1670 */     return ((double[])(double[])function(instrument, period, side, "KAMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] kama(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1674 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "KAMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] keltner(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1678 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "KELTNER", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 1679 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] keltner(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1683 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "KELTNER", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 1684 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] keltner(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1688 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "KELTNER", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1689 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double lasacs1(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int ma, double gamma, int lookback, int shift) throws JFException {
	/* 1693 */     return ((Double)function(instrument, period, side, "LAGACS1", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(ma), Double.valueOf(gamma), Integer.valueOf(lookback) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] lasacs1(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int ma, double gamma, int lookback, long from, long to) throws JFException {
	/* 1697 */     return ((double[])(double[])function(instrument, period, side, "LAGACS1", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(ma), Double.valueOf(gamma), Integer.valueOf(lookback) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] lasacs1(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int ma, double gamma, int lookback, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1701 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LAGACS1", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(ma), Double.valueOf(gamma), Integer.valueOf(lookback) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double linearReg(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1705 */     return ((Double)function(instrument, period, side, "LINEARREG", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] linearReg(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1709 */     return ((double[])(double[])function(instrument, period, side, "LINEARREG", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] linearReg(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1713 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LINEARREG", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double linearRegAngle(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1717 */     return ((Double)function(instrument, period, side, "LINEARREG_ANGLE", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] linearRegAngle(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1721 */     return ((double[])(double[])function(instrument, period, side, "LINEARREG_ANGLE", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] linearRegAngle(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1725 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LINEARREG_ANGLE", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double linearRegIntercept(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1729 */     return ((Double)function(instrument, period, side, "LINEARREG_INTERCEPT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] linearRegIntercept(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1733 */     return ((double[])(double[])function(instrument, period, side, "LINEARREG_INTERCEPT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] linearRegIntercept(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1737 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LINEARREG_INTERCEPT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double linearRegSlope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1741 */     return ((Double)function(instrument, period, side, "LINEARREG_SLOPE", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] linearRegSlope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1745 */     return ((double[])(double[])function(instrument, period, side, "LINEARREG_SLOPE", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] linearRegSlope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1749 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LINEARREG_SLOPE", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ln(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1753 */     return ((Double)function(instrument, period, side, "LN", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ln(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException {
	/* 1757 */     return ((double[])(double[])function(instrument, period, side, "LN", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ln(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1761 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LN", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double log10(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 1765 */     return ((Double)function(instrument, period, side, "LOG10", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] log10(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException {
	/* 1769 */     return ((double[])(double[])function(instrument, period, side, "LOG10", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] log10(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1773 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LOG10", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double lwma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1777 */     return ((Double)function(instrument, period, side, "LWMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] lwma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1781 */     return ((double[])(double[])function(instrument, period, side, "LWMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] lwma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1785 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "LWMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 1789 */     return ((Double)function(instrument, period, side, "MA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 1793 */     return ((double[])(double[])function(instrument, period, side, "MA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1797 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] macd(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int signalPeriod, int shift) throws JFException {
	/* 1801 */     Object[] ret = function(instrument, period, side, "MACD", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(signalPeriod) }, shift);
	/* 1802 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] macd(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int signalPeriod, long from, long to) throws JFException {
	/* 1806 */     Object[] ret = function(instrument, period, side, "MACD", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(signalPeriod) }, from, to);
	/* 1807 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] macd(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, int signalPeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1811 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MACD", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(signalPeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1812 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[] macdExt(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, IIndicators.MaType fastMaType, int slowPeriod, IIndicators.MaType slowMaType, int signalPeriod, IIndicators.MaType signalMaType, int shift) throws JFException {
	/* 1816 */     Object[] ret = function(instrument, period, side, "MACDEXT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(fastMaType.ordinal()), Integer.valueOf(slowPeriod), Integer.valueOf(slowMaType.ordinal()), Integer.valueOf(signalPeriod), Integer.valueOf(signalMaType.ordinal()) }, shift);
	/* 1817 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] macdExt(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, IIndicators.MaType fastMaType, int slowPeriod, IIndicators.MaType slowMaType, int signalPeriod, IIndicators.MaType signalMaType, long from, long to) throws JFException {
	/* 1821 */     Object[] ret = function(instrument, period, side, "MACDEXT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(fastMaType.ordinal()), Integer.valueOf(slowPeriod), Integer.valueOf(slowMaType.ordinal()), Integer.valueOf(signalPeriod), Integer.valueOf(signalMaType.ordinal()) }, from, to);
	/* 1822 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] macdExt(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, IIndicators.MaType fastMaType, int slowPeriod, IIndicators.MaType slowMaType, int signalPeriod, IIndicators.MaType signalMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1826 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MACDEXT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(fastMaType.ordinal()), Integer.valueOf(slowPeriod), Integer.valueOf(slowMaType.ordinal()), Integer.valueOf(signalPeriod), Integer.valueOf(signalMaType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1827 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[] macdFix(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int signalPeriod, int shift) throws JFException {
	/* 1831 */     Object[] ret = function(instrument, period, side, "MACDFIX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(signalPeriod) }, shift);
	/* 1832 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] macdFix(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int signalPeriod, long from, long to) throws JFException {
	/* 1836 */     Object[] ret = function(instrument, period, side, "MACDFIX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(signalPeriod) }, from, to);
	/* 1837 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] macdFix(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int signalPeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1841 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MACDFIX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(signalPeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1842 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[] maEnvelope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, int shift) throws JFException {
	/* 1846 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MAEnvelope", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, shift);
	/* 1847 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] maEnvelope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, long from, long to) throws JFException
	/*      */   {
	/* 1852 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MAEnvelope", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, from, to);
	/* 1853 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] maEnvelope(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double deviation, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 1858 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MAEnvelope", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1859 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] mama(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, double fastLimit, double slowLimit, int shift) throws JFException {
	/* 1863 */     Object[] ret = function(instrument, period, side, "MAMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Double.valueOf(fastLimit), Double.valueOf(slowLimit) }, shift);
	/* 1864 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] mama(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, double fastLimit, double slowLimit, long from, long to) throws JFException {
	/* 1868 */     Object[] ret = function(instrument, period, side, "MAMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Double.valueOf(fastLimit), Double.valueOf(slowLimit) }, from, to);
	/* 1869 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] mama(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, double fastLimit, double slowLimit, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1873 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MAMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Double.valueOf(fastLimit), Double.valueOf(slowLimit) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 1874 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public Object[] wsmTime(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1878 */     return function(instrument, period, side, "WSMTIME", new IIndicators.AppliedPrice[0], new Object[0], shift);
	/*      */   }
	/*      */ 
	/*      */   public Object[] wsmTime(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1882 */     return function(instrument, period, side, "WSMTIME", new IIndicators.AppliedPrice[0], new Object[0], from, to);
	/*      */   }
	/*      */ 
	/*      */   public Object[] wsmTime(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1886 */     return calculateIndicator(instrument, period, new OfferSide[] { side }, "WSMTIME", new IIndicators.AppliedPrice[0], new Object[0], filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*      */   }
	/*      */ 
	/*      */   public double mavp(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int minPeriod, int maxPeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 1890 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "MAVP", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(minPeriod), Integer.valueOf(maxPeriod), Integer.valueOf(maType.ordinal()) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] mavp(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int minPeriod, int maxPeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 1894 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "MAVP", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(minPeriod), Integer.valueOf(maxPeriod), Integer.valueOf(maType.ordinal()) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] mavp(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int minPeriod, int maxPeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1898 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "MAVP", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, new Object[] { Integer.valueOf(minPeriod), Integer.valueOf(maxPeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double max(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1902 */     return ((Double)function(instrument, period, side, "MAX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] max(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1906 */     return ((double[])(double[])function(instrument, period, side, "MAX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] max(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1910 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MAX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int maxIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1914 */     return ((Integer)function(instrument, period, side, "MAXINDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] maxIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1918 */     return ((int[])(int[])function(instrument, period, side, "MAXINDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] maxIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1922 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MAXINDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double medPrice(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 1926 */     return ((Double)function(instrument, period, side, "MEDPRICE", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] medPrice(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 1930 */     return ((double[])(double[])function(instrument, period, side, "MEDPRICE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] medPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1934 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MEDPRICE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double mfi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 1938 */     return ((Double)function(instrument, period, side, "MFI", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] mfi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 1942 */     return ((double[])(double[])function(instrument, period, side, "MFI", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] mfi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1946 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MFI", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double midPoint(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1950 */     return ((Double)function(instrument, period, side, "MIDPOINT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] midPoint(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1954 */     return ((double[])(double[])function(instrument, period, side, "MIDPOINT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] midPoint(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1958 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MIDPOINT", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double midPrice(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 1962 */     return ((Double)function(instrument, period, side, "MIDPRICE", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] midPrice(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 1966 */     return ((double[])(double[])function(instrument, period, side, "MIDPRICE", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] midPrice(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1970 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MIDPRICE", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double min(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1974 */     return ((Double)function(instrument, period, side, "MIN", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] min(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1978 */     return ((double[])(double[])function(instrument, period, side, "MIN", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] min(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1982 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MIN", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int minIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1986 */     return ((Integer)function(instrument, period, side, "MININDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).intValue();
	/*      */   }
	/*      */ 
	/*      */   public int[] minIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 1990 */     return ((int[])(int[])function(instrument, period, side, "MININDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public int[] minIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 1994 */     return ((int[])(int[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MININDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] minMax(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 1998 */     Object[] ret = function(instrument, period, side, "MINMAX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 1999 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] minMax(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2003 */     Object[] ret = function(instrument, period, side, "MINMAX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2004 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] minMax(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2008 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MINMAX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2009 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public int[] minMaxIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2013 */     Object[] ret = function(instrument, period, side, "MINMAXINDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 2014 */     return new int[] { ((Integer)ret[0]).intValue(), ((Integer)ret[1]).intValue() };
	/*      */   }
	/*      */ 
	/*      */   public int[][] minMaxIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2018 */     Object[] ret = function(instrument, period, side, "MINMAXINDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2019 */     return new int[][] { (int[])(int[])ret[0], (int[])(int[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public int[][] minMaxIndex(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2023 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MINMAXINDEX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2024 */     return new int[][] { (int[])(int[])ret[0], (int[])(int[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double minusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2028 */     return ((Double)function(instrument, period, side, "MINUS_DI", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] minusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2032 */     return ((double[])(double[])function(instrument, period, side, "MINUS_DI", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] minusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2036 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MINUS_DI", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double minusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2040 */     return ((Double)function(instrument, period, side, "MINUS_DM", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] minusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2044 */     return ((double[])(double[])function(instrument, period, side, "MINUS_DM", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] minusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2048 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MINUS_DM", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double mom(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2052 */     return ((Double)function(instrument, period, side, "MOM", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] mom(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2056 */     return ((double[])(double[])function(instrument, period, side, "MOM", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] mom(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2060 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "MOM", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double mult(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int shift) throws JFException
	/*      */   {
	/* 2065 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "MULT", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] mult(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, long from, long to) throws JFException
	/*      */   {
	/* 2070 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "MULT", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] mult(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2075 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "MULT", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] murrey(Instrument instrument, Period period, OfferSide side, int nPeriod, int timePeriod, int stepBack, int shift) throws JFException {
	/* 2079 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MURRCH", null, new Object[] { Integer.valueOf(nPeriod), Integer.valueOf(timePeriod), Integer.valueOf(stepBack) }, shift);
	/* 2080 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue(), ((Double)ret[7]).doubleValue(), ((Double)ret[8]).doubleValue(), ((Double)ret[9]).doubleValue(), ((Double)ret[10]).doubleValue(), ((Double)ret[11]).doubleValue(), ((Double)ret[12]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] murrey(Instrument instrument, Period period, OfferSide side, int nPeriod, int timePeriod, int stepBack, long from, long to) throws JFException {
	/* 2084 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MURRCH", null, new Object[] { Integer.valueOf(nPeriod), Integer.valueOf(timePeriod), Integer.valueOf(stepBack) }, from, to);
	/* 2085 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6], (double[])(double[])ret[7], (double[])(double[])ret[8], (double[])(double[])ret[9], (double[])(double[])ret[10], (double[])(double[])ret[11], (double[])(double[])ret[12] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] murrey(Instrument instrument, Period period, OfferSide side, int nPeriod, int timePeriod, int stepBack, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2089 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "MURRCH", null, new Object[] { Integer.valueOf(nPeriod), Integer.valueOf(timePeriod), Integer.valueOf(stepBack) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2090 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6], (double[])(double[])ret[7], (double[])(double[])ret[8], (double[])(double[])ret[9], (double[])(double[])ret[10], (double[])(double[])ret[11], (double[])(double[])ret[12] };
	/*      */   }
	/*      */ 
	/*      */   public double natr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException
	/*      */   {
	/* 2095 */     return ((Double)function(instrument, period, side, "NATR", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] natr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2099 */     return ((double[])(double[])function(instrument, period, side, "NATR", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] natr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2103 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "NATR", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double obv(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, OfferSide sideForPriceV, int shift) throws JFException
	/*      */   {
	/* 2108 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side, sideForPriceV }, "OBV", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] obv(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, OfferSide sideForPriceV, long from, long to) throws JFException
	/*      */   {
	/* 2113 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side, sideForPriceV }, "OBV", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] obv(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, OfferSide sideForPriceV, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2118 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side, sideForPriceV }, "OBV", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double osma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fast_ema_period, int slow_ema_period, int signal_period, int shift) throws JFException {
	/* 2122 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "OsMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fast_ema_period), Integer.valueOf(slow_ema_period), Integer.valueOf(signal_period) }, shift);
	/* 2123 */     return ((Double)ret[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] osma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fast_ema_period, int slow_ema_period, int signal_period, long from, long to) throws JFException {
	/* 2127 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "OsMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fast_ema_period), Integer.valueOf(slow_ema_period), Integer.valueOf(signal_period) }, from, to);
	/* 2128 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] osma(Instrument instrument, Period period, OfferSide side, int fast_ema_period, int slow_ema_period, int signal_period, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2133 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "OsMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fast_ema_period), Integer.valueOf(slow_ema_period), Integer.valueOf(signal_period) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2134 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] pivot(Instrument instrument, Period period, OfferSide side, int timePeriod, boolean showHistoricalLevels, int shift) throws JFException {
	/* 2138 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "PIVOT", null, new Object[] { Integer.valueOf(timePeriod), Boolean.valueOf(showHistoricalLevels) }, shift);
	/* 2139 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] pivot(Instrument instrument, Period period, OfferSide side, int timePeriod, boolean showHistoricalLevels, long from, long to) throws JFException {
	/* 2143 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "PIVOT", null, new Object[] { Integer.valueOf(timePeriod), Boolean.valueOf(showHistoricalLevels) }, from, to);
	/* 2144 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] pivot(Instrument instrument, Period period, OfferSide side, int timePeriod, boolean showHistoricalLevels, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2148 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "PIVOT", null, new Object[] { Integer.valueOf(timePeriod), Boolean.valueOf(showHistoricalLevels) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2149 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double plusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2153 */     return ((Double)function(instrument, period, side, "PLUS_DI", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] plusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2157 */     return ((double[])(double[])function(instrument, period, side, "PLUS_DI", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] plusDi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2161 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "PLUS_DI", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double plusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2165 */     return ((Double)function(instrument, period, side, "PLUS_DM", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] plusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2169 */     return ((double[])(double[])function(instrument, period, side, "PLUS_DM", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] plusDm(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2173 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "PLUS_DM", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ppo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 2177 */     return ((Double)function(instrument, period, side, "PPO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ppo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 2181 */     return ((double[])(double[])function(instrument, period, side, "PPO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ppo(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2185 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "PPO", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double prchannel(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, int shift) throws JFException {
	/* 2189 */     return ((Double)function(instrument, period, side, "PCHANNEL", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] prchannel(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, long from, long to) throws JFException {
	/* 2193 */     return ((double[])(double[])function(instrument, period, side, "PCHANNEL", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] prchannel(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fastPeriod, int slowPeriod, IIndicators.MaType maType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2197 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "PCHANNEL", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fastPeriod), Integer.valueOf(slowPeriod), Integer.valueOf(maType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double rci(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2201 */     return ((Double)function(instrument, period, side, "RCI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] rci(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2205 */     return ((double[])(double[])function(instrument, period, side, "RCI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rci(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2209 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "RCI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double rmi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int momentumPeriod, int shift) throws JFException {
	/* 2213 */     return ((Double)function(instrument, period, side, "RMI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(momentumPeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] rmi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int momentumPeriod, long from, long to) throws JFException {
	/* 2217 */     return ((double[])(double[])function(instrument, period, side, "RMI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(momentumPeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rmi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int momentumPeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2221 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "RMI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(momentumPeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double roc(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2225 */     return ((Double)function(instrument, period, side, "ROC", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] roc(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2229 */     return ((double[])(double[])function(instrument, period, side, "ROC", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] roc(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2233 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ROC", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double rocp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2237 */     return ((Double)function(instrument, period, side, "ROCP", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] rocp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2241 */     return ((double[])(double[])function(instrument, period, side, "ROCP", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rocp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2245 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ROCP", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double rocr(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2249 */     return ((Double)function(instrument, period, side, "ROCR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] rocr(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2253 */     return ((double[])(double[])function(instrument, period, side, "ROCR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rocr(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2257 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ROCR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double rocr100(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2261 */     return ((Double)function(instrument, period, side, "ROCR100", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] rocr100(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2265 */     return ((double[])(double[])function(instrument, period, side, "ROCR100", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rocr100(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2269 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ROCR100", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double rsi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2273 */     return ((Double)function(instrument, period, side, "RSI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] rsi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2277 */     return ((double[])(double[])function(instrument, period, side, "RSI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rsi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2281 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "RSI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] rvi(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException
	/*      */   {
	/* 2286 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "RVI", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 2287 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] rvi(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException
	/*      */   {
	/* 2292 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "RVI", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2293 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] rvi(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2298 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "RVI", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2299 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double sar(Instrument instrument, Period period, OfferSide side, double acceleration, double maximum, int shift) throws JFException {
	/* 2303 */     double value = sarExt(instrument, period, side, 0.0D, 0.0D, acceleration, acceleration, maximum, acceleration, acceleration, maximum, shift);
	/* 2304 */     if (value < 0.0D) {
	/* 2305 */       return (-value);
	/*      */     }
	/* 2307 */     return value;
	/*      */   }
	/*      */ 
	/*      */   public double[] sar(Instrument instrument, Period period, OfferSide side, double acceleration, double maximum, long from, long to) throws JFException
	/*      */   {
	/* 2312 */     double[] values = sarExt(instrument, period, side, 0.0D, 0.0D, acceleration, acceleration, maximum, acceleration, acceleration, maximum, from, to);
	/* 2313 */     for (int i = 0; i < values.length; ++i) {
	/* 2314 */       double value = values[i];
	/* 2315 */       if (value < 0.0D) {
	/* 2316 */         values[i] = (-value);
	/*      */       }
	/*      */     }
	/* 2319 */     return values;
	/*      */   }
	/*      */ 
	/*      */   public double[] sar(Instrument instrument, Period period, OfferSide side, double acceleration, double maximum, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2323 */     double[] values = sarExt(instrument, period, side, 0.0D, 0.0D, acceleration, acceleration, maximum, acceleration, acceleration, maximum, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2324 */     for (int i = 0; i < values.length; ++i) {
	/* 2325 */       double value = values[i];
	/* 2326 */       if (value < 0.0D) {
	/* 2327 */         values[i] = (-value);
	/*      */       }
	/*      */     }
	/* 2330 */     return values;
	/*      */   }
	/*      */ 
	/*      */   public double sarExt(Instrument instrument, Period period, OfferSide side, double startValue, double offsetOnReverse, double accelerationInitLong, double accelerationLong, double accelerationMaxLong, double accelerationInitShort, double accelerationShort, double accelerationMaxShort, int shift) throws JFException {
	/*      */     try {
	/* 2335 */       checkNotTick(period);
	/* 2336 */       checkSide(period, new OfferSide[] { side });
	/*      */ 
	/* 2338 */       IndicatorHolder indicatorHolder = getCachedIndicator("SAREXT");
	/*      */       try {
	/* 2340 */         IIndicator indicator = indicatorHolder.getIndicator();
	/* 2341 */         setOptParams(indicator, new Object[] { Double.valueOf(startValue), Double.valueOf(offsetOnReverse), Double.valueOf(accelerationInitLong), Double.valueOf(accelerationLong), Double.valueOf(accelerationMaxLong), Double.valueOf(accelerationInitShort), Double.valueOf(accelerationShort), Double.valueOf(accelerationMaxShort) });
	/*      */         int indicatorLookback;
	/* 2345 */         int lookback = indicatorLookback = indicator.getLookback();
	/* 2346 */         boolean hasPeriodChange = false;
	/* 2347 */         int previousLength = 0;
	/* 2348 */         double[] doubles = new double[0];
	/* 2349 */         IndicatorResult result = null;
	/*      */         do
	/*      */         {
	/* 2352 */           lookback += ((lookback * 4 < 100) ? 100 : lookback * 4);
	/* 2353 */           double[][] inputData = (double[][])getInputData(instrument, period, side, InputParameterInfo.Type.PRICE, null, shift, lookback, 0);
	/* 2354 */           if (inputData == null) {
	/* 2355 */             double d = (0.0D / 0.0D);
	/*      */             return d;
	/*      */           }
	/* 2357 */           int length = inputData[0].length;
	/* 2358 */           if (length <= previousLength) {
	/*      */             break;
	/*      */           }
	/*      */ 
	/* 2362 */           previousLength = length;
	/* 2363 */           indicator.setInputParameter(0, inputData);
	/* 2364 */           doubles = new double[length - indicatorLookback];
	/* 2365 */           indicator.setOutputParameter(0, doubles);
	/* 2366 */           result = indicator.calculate(0, length - 1);
	/* 2367 */           if (result.getNumberOfElements() == 0) {
	/*      */             break;
	/*      */           }
	/*      */ 
	/* 2371 */           boolean positive = doubles[0] > 0.0D;
	/* 2372 */           for (int i = 1; i < result.getNumberOfElements(); ++i)
	/* 2373 */             if (doubles[i] > 0.0D != positive) {
	/* 2374 */               hasPeriodChange = true;
	/* 2375 */               break;
	/*      */             }
	/*      */         }
	/* 2378 */         while (!(hasPeriodChange));
	/* 2379 */         double inputData = doubles[(result.getNumberOfElements() - 1)];
	/*      */ 
	/* 2381 */         return inputData; } finally { cacheIndicator("SAREXT", indicatorHolder);
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 2384 */       throw e;
	/*      */     } catch (TaLibException e) {
	/* 2386 */       Throwable t = e.getCause();
	/* 2387 */       throw new JFException(t);
	/*      */     } catch (RuntimeException e) {
	/* 2389 */       throw e;
	/*      */     } catch (Exception e) {
	/* 2391 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   public double[] sarExt(Instrument instrument, Period period, OfferSide side, double startValue, double offsetOnReverse, double accelerationInitLong, double accelerationLong, double accelerationMaxLong, double accelerationInitShort, double accelerationShort, double accelerationMaxShort, long from, long to) throws JFException {
	/*      */     try {
	/* 2397 */       checkSide(period, new OfferSide[] { side });
	/* 2398 */       checkIntervalValid(period, from, to);
	/*      */ 
	/* 2400 */       IndicatorHolder indicatorHolder = getCachedIndicator("SAREXT");
	/*      */       try {
	/* 2402 */         IIndicator indicator = indicatorHolder.getIndicator();
	/* 2403 */         setOptParams(indicator, new Object[] { Double.valueOf(startValue), Double.valueOf(offsetOnReverse), Double.valueOf(accelerationInitLong), Double.valueOf(accelerationLong), Double.valueOf(accelerationMaxLong), Double.valueOf(accelerationInitShort), Double.valueOf(accelerationShort), Double.valueOf(accelerationMaxShort) });
	/*      */         int indicatorLookback;
	/* 2407 */         int lookback = indicatorLookback = indicator.getLookback();
	/* 2408 */         boolean hasPeriodChange = false;
	/* 2409 */         int previousLength = 0;
	/* 2410 */         double[] doubles = new double[0];
	/* 2411 */         IndicatorResult result = null;
	/*      */         do
	/*      */         {
	/* 2414 */           lookback += ((lookback * 4 < 100) ? 100 : lookback * 4);
	/* 2415 */           double[][] inputData = (double[][])(double[][])getInputData(instrument, period, side, InputParameterInfo.Type.PRICE, null, from, to, lookback, 0, false, 0);
	/* 2416 */           int length = inputData[0].length;
	/* 2417 */           if (length <= previousLength) {
	/*      */             break;
	/*      */           }
	/*      */ 
	/* 2421 */           previousLength = length;
	/* 2422 */           indicator.setInputParameter(0, inputData);
	/* 2423 */           doubles = new double[length - indicatorLookback];
	/* 2424 */           indicator.setOutputParameter(0, doubles);
	/* 2425 */           result = indicator.calculate(0, length - 1);
	/* 2426 */           if (result.getNumberOfElements() == 0) {
	/*      */             break;
	/*      */           }
	/*      */ 
	/* 2430 */           boolean positive = doubles[0] > 0.0D;
	/* 2431 */           for (int i = 1; i < result.getNumberOfElements(); ++i)
	/* 2432 */             if (doubles[i] > 0.0D != positive) {
	/* 2433 */               hasPeriodChange = true;
	/* 2434 */               break;
	/*      */             }
	/*      */         }
	/* 2437 */         while (!(hasPeriodChange));
	/*      */ 
	/* 2439 */         int outCount = (DataCacheUtils.getCandlesCountBetweenFast((period == Period.TICK) ? Period.ONE_SEC : period, from, to) > result.getNumberOfElements()) ? result.getNumberOfElements() : DataCacheUtils.getCandlesCountBetweenFast((period == Period.TICK) ? Period.ONE_SEC : period, from, to);
	/*      */ 
	/* 2441 */         double[] retDoubles = new double[outCount];
	/* 2442 */         System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
	/*      */ 
	/* 2445 */         return retDoubles; } finally { cacheIndicator("SAREXT", indicatorHolder);
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 2448 */       throw e;
	/*      */     } catch (TaLibException e) {
	/* 2450 */       Throwable t = e.getCause();
	/* 2451 */       throw new JFException(t);
	/*      */     } catch (RuntimeException e) {
	/* 2453 */       throw e;
	/*      */     } catch (Exception e) {
	/* 2455 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   public double[] sarExt(Instrument instrument, Period period, OfferSide side, double startValue, double offsetOnReverse, double accelerationInitLong, double accelerationLong, double accelerationMaxLong, double accelerationInitShort, double accelerationShort, double accelerationMaxShort, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
	/*      */     throws JFException
	/*      */   {
	/*      */     try
	/*      */     {
	/* 2465 */       checkNotTick(period);
	/* 2466 */       checkSide(period, side);
	/* 2467 */       checkIntervalValid(numberOfCandlesBefore, numberOfCandlesAfter);
	/*      */ 
	/* 2469 */       IndicatorHolder indicatorHolder = getCachedIndicator("SAREXT");
	/*      */       try {
	/* 2471 */         IIndicator indicator = indicatorHolder.getIndicator();
	/* 2472 */         setOptParams(indicator, new Object[] { Double.valueOf(startValue), Double.valueOf(offsetOnReverse), Double.valueOf(accelerationInitLong), Double.valueOf(accelerationLong), Double.valueOf(accelerationMaxLong), Double.valueOf(accelerationInitShort), Double.valueOf(accelerationShort), Double.valueOf(accelerationMaxShort) });
	/*      */         int indicatorLookback;
	/* 2476 */         int lookback = indicatorLookback = indicator.getLookback();
	/* 2477 */         boolean hasPeriodChange = false;
	/* 2478 */         int previousLength = 0;
	/* 2479 */         double[] doubles = new double[0];
	/* 2480 */         IndicatorResult result = null;
	/*      */         do
	/*      */         {
	/* 2483 */           lookback += ((lookback * 4 < 100) ? 100 : lookback * 4);
	/* 2484 */           double[][] inputData = (double[][])(double[][])getInputData(instrument, period, side, InputParameterInfo.Type.PRICE, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, lookback, 0, 0);
	/* 2485 */           int length = inputData[0].length;
	/* 2486 */           if (length <= previousLength) {
	/*      */             break;
	/*      */           }
	/*      */ 
	/* 2490 */           previousLength = length;
	/* 2491 */           indicator.setInputParameter(0, inputData);
	/* 2492 */           doubles = new double[length - indicatorLookback];
	/* 2493 */           indicator.setOutputParameter(0, doubles);
	/* 2494 */           result = indicator.calculate(0, length - 1);
	/* 2495 */           if (result.getNumberOfElements() == 0) {
	/*      */             break;
	/*      */           }
	/*      */ 
	/* 2499 */           boolean positive = doubles[0] > 0.0D;
	/* 2500 */           for (int i = 1; i < result.getNumberOfElements(); ++i)
	/* 2501 */             if (doubles[i] > 0.0D != positive) {
	/* 2502 */               hasPeriodChange = true;
	/* 2503 */               break;
	/*      */             }
	/*      */         }
	/* 2506 */         while (!(hasPeriodChange));
	/*      */ 
	/* 2508 */         int outCount = (numberOfCandlesBefore + numberOfCandlesAfter > result.getNumberOfElements()) ? result.getNumberOfElements() : numberOfCandlesBefore + numberOfCandlesAfter;
	/*      */ 
	/* 2510 */         double[] retDoubles = new double[outCount];
	/* 2511 */         System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
	/*      */ 
	/* 2514 */         return retDoubles; } finally { cacheIndicator("SAREXT", indicatorHolder);
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 2517 */       throw e;
	/*      */     } catch (TaLibException e) {
	/* 2519 */       Throwable t = e.getCause();
	/* 2520 */       throw new JFException(t);
	/*      */     } catch (RuntimeException e) {
	/* 2522 */       throw e;
	/*      */     } catch (Exception e) {
	/* 2524 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   public double sin(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2529 */     return ((Double)function(instrument, period, side, "SIN", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] sin(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 2534 */     return ((double[])(double[])function(instrument, period, side, "SIN", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] sin(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2539 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "SIN", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double sinh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2543 */     return ((Double)function(instrument, period, side, "SINH", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] sinh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 2548 */     return ((double[])(double[])function(instrument, period, side, "SINH", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] sinh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2553 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "SINH", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double sma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2557 */     return ((Double)function(instrument, period, side, "SMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] sma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2561 */     return ((double[])(double[])function(instrument, period, side, "SMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] sma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2565 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "SMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double smma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException
	/*      */   {
	/* 2570 */     return ((Double)function(instrument, period, side, "SMMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] smma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException
	/*      */   {
	/* 2575 */     return ((double[])(double[])function(instrument, period, side, "SMMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] smma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2580 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "SMMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double sqrt(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2584 */     return ((Double)function(instrument, period, side, "SQRT", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] sqrt(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 2589 */     return ((double[])(double[])function(instrument, period, side, "SQRT", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] sqrt(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2594 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "SQRT", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double stdDev(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, int shift) throws JFException {
	/* 2598 */     return ((Double)function(instrument, period, side, "STDDEV", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDev) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] stdDev(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, long from, long to) throws JFException {
	/* 2602 */     return ((double[])(double[])function(instrument, period, side, "STDDEV", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDev) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] stdDev(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2606 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "STDDEV", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDev) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] stoch(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int slowKPeriod, IIndicators.MaType slowKMaType, int slowDPeriod, IIndicators.MaType slowDMaType, int shift) throws JFException {
	/* 2610 */     Object[] ret = function(instrument, period, side, "STOCH", null, new Object[] { Integer.valueOf(fastKPeriod), Integer.valueOf(slowKPeriod), Integer.valueOf(slowKMaType.ordinal()), Integer.valueOf(slowDPeriod), Integer.valueOf(slowDMaType.ordinal()) }, shift);
	/* 2611 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] stoch(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int slowKPeriod, IIndicators.MaType slowKMaType, int slowDPeriod, IIndicators.MaType slowDMaType, long from, long to) throws JFException {
	/* 2615 */     Object[] ret = function(instrument, period, side, "STOCH", null, new Object[] { Integer.valueOf(fastKPeriod), Integer.valueOf(slowKPeriod), Integer.valueOf(slowKMaType.ordinal()), Integer.valueOf(slowDPeriod), Integer.valueOf(slowDMaType.ordinal()) }, from, to);
	/* 2616 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] stoch(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int slowKPeriod, IIndicators.MaType slowKMaType, int slowDPeriod, IIndicators.MaType slowDMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2620 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "STOCH", null, new Object[] { Integer.valueOf(fastKPeriod), Integer.valueOf(slowKPeriod), Integer.valueOf(slowKMaType.ordinal()), Integer.valueOf(slowDPeriod), Integer.valueOf(slowDMaType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2621 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] stochF(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int fastDPeriod, IIndicators.MaType fastDMaType, int shift) throws JFException {
	/* 2625 */     Object[] ret = function(instrument, period, side, "STOCHF", null, new Object[] { Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal()) }, shift);
	/* 2626 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] stochF(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int fastDPeriod, IIndicators.MaType fastDMaType, long from, long to) throws JFException {
	/* 2630 */     Object[] ret = function(instrument, period, side, "STOCHF", null, new Object[] { Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal()) }, from, to);
	/* 2631 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] stochF(Instrument instrument, Period period, OfferSide side, int fastKPeriod, int fastDPeriod, IIndicators.MaType fastDMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2635 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "STOCHF", null, new Object[] { Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2636 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[] stochRsi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int fastKPeriod, int fastDPeriod, IIndicators.MaType fastDMaType, int shift) throws JFException {
	/* 2640 */     Object[] ret = function(instrument, period, side, "STOCHRSI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal()) }, shift);
	/* 2641 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] stochRsi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int fastKPeriod, int fastDPeriod, IIndicators.MaType fastDMaType, long from, long to) throws JFException {
	/* 2645 */     Object[] ret = function(instrument, period, side, "STOCHRSI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal()) }, from, to);
	/* 2646 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] stochRsi(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int fastKPeriod, int fastDPeriod, IIndicators.MaType fastDMaType, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2650 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "STOCHRSI", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Integer.valueOf(fastKPeriod), Integer.valueOf(fastDPeriod), Integer.valueOf(fastDMaType.ordinal()) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2651 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double sub(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, int shift) throws JFException {
	/* 2655 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "SUB", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] sub(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, long from, long to) throws JFException {
	/* 2659 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "SUB", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] sub(Instrument instrument, Period period, OfferSide side1, IIndicators.AppliedPrice appliedPrice1, OfferSide side2, IIndicators.AppliedPrice appliedPrice2, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2663 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side1, side2 }, "SUB", new IIndicators.AppliedPrice[] { appliedPrice1, appliedPrice2 }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double sum(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2667 */     return ((Double)function(instrument, period, side, "SUM", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] sum(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2671 */     return ((double[])(double[])function(instrument, period, side, "SUM", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] sum(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2675 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "SUM", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] supportResistance(Instrument instrument, Period period, OfferSide side, int shift) throws JFException
	/*      */   {
	/* 2680 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "S&R", null, null, shift);
	/* 2681 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] supportResistance(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException
	/*      */   {
	/* 2686 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "S&R", null, null, from, to);
	/* 2687 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] supportResistance(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2692 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "S&R", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2693 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double t3(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double factor, int shift) throws JFException
	/*      */   {
	/* 2698 */     return ((Double)function(instrument, period, side, "T3", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(factor) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] t3(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double factor, long from, long to) throws JFException
	/*      */   {
	/* 2703 */     return ((double[])(double[])function(instrument, period, side, "T3", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(factor) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] t3(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double factor, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2708 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "T3", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(factor) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double tan(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2712 */     return ((Double)function(instrument, period, side, "TAN", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] tan(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 2717 */     return ((double[])(double[])function(instrument, period, side, "TAN", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] tan(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2722 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TAN", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double tanh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2726 */     return ((Double)function(instrument, period, side, "TANH", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] tanh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, long from, long to) throws JFException
	/*      */   {
	/* 2731 */     return ((double[])(double[])function(instrument, period, side, "TANH", new IIndicators.AppliedPrice[] { appliedPrice }, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] tanh(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2736 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TANH", new IIndicators.AppliedPrice[] { appliedPrice }, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] tbp(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2740 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "THRUSTBAR", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift);
	/* 2741 */     return new double[] { ((Integer)ret[0]).intValue(), ((Integer)ret[1]).intValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[] tbop(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int shift) throws JFException {
	/* 2745 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "THRUSTOUTSIDEBAR", new IIndicators.AppliedPrice[] { appliedPrice }, null, shift);
	/* 2746 */     return new double[] { ((Integer)ret[0]).intValue(), ((Integer)ret[1]).intValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[] td_i(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2750 */     Object[] ret = function(instrument, period, side, "TD_I", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 2751 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] td_i(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2755 */     Object[] ret = function(instrument, period, side, "TD_I", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2756 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] td_i(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2760 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "TD_I", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2761 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2] };
	/*      */   }
	/*      */ 
	/*      */   public int[] td_s(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2765 */     Object[] ret = function(instrument, period, side, "TD_S", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 2766 */     return new int[] { ((Integer)ret[0]).intValue(), ((Integer)ret[1]).intValue() };
	/*      */   }
	/*      */ 
	/*      */   public int[][] td_s(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2770 */     Object[] ret = function(instrument, period, side, "TD_S", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2771 */     return new int[][] { (int[])(int[])ret[0], (int[])(int[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public int[][] td_s(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2775 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "TD_S", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2776 */     return new int[][] { (int[])(int[])ret[0], (int[])(int[])ret[1] };
	/*      */   }
	/*      */ 
	/*      */   public double tema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2780 */     return ((Double)function(instrument, period, side, "TEMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] tema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2784 */     return ((double[])(double[])function(instrument, period, side, "TEMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] tema(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2788 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TEMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double trange(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 2792 */     return ((Double)function(instrument, period, side, "TRANGE", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] trange(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 2796 */     return ((double[])(double[])function(instrument, period, side, "TRANGE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] trange(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2800 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TRANGE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] trendEnv(Instrument instrument, Period period, OfferSide side, int timePeriod, double deviation, int shift) throws JFException {
	/* 2804 */     Object[] res = function(instrument, period, side, "TrendEnvelopes", null, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, shift);
	/* 2805 */     return new double[] { ((Double)res[0]).doubleValue(), ((Double)res[1]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] trendEnv(Instrument instrument, Period period, OfferSide side, int timePeriod, double deviation, long from, long to) throws JFException {
	/* 2809 */     Object[] res = function(instrument, period, side, "TrendEnvelopes", null, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, from, to);
	/* 2810 */     return new double[][] { (double[])(double[])res[0], (double[])(double[])res[1] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] trendEnv(Instrument instrument, Period period, OfferSide side, int timePeriod, double deviation, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2814 */     Object[] res = calculateIndicator(instrument, period, new OfferSide[] { side }, "TrendEnvelopes", null, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(deviation) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2815 */     return new double[][] { (double[])(double[])res[0], (double[])(double[])res[1] };
	/*      */   }
	/*      */ 
	/*      */   public double trima(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException
	/*      */   {
	/* 2820 */     return ((Double)function(instrument, period, side, "TRIMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] trima(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException
	/*      */   {
	/* 2825 */     return ((double[])(double[])function(instrument, period, side, "TRIMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] trima(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2830 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TRIMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double trix(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2834 */     return ((Double)function(instrument, period, side, "TRIX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] trix(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2838 */     return ((double[])(double[])function(instrument, period, side, "TRIX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] trix(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2842 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TRIX", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double tsf(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2846 */     return ((Double)function(instrument, period, side, "TSF", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] tsf(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2850 */     return ((double[])(double[])function(instrument, period, side, "TSF", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] tsf(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2854 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TSF", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double tvs(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2858 */     return ((Double)function(instrument, period, side, "TVS", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] tvs(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2862 */     return ((double[])(double[])function(instrument, period, side, "TVS", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] tvs(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2866 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TVS", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double typPrice(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 2870 */     return ((Double)function(instrument, period, side, "TYPPRICE", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] typPrice(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 2874 */     return ((double[])(double[])function(instrument, period, side, "TYPPRICE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] typPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2878 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "TYPPRICE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double ultOsc(Instrument instrument, Period period, OfferSide side, int timePeriod1, int timePeriod2, int timePeriod3, int shift) throws JFException {
	/* 2882 */     return ((Double)function(instrument, period, side, "ULTOSC", null, new Object[] { Integer.valueOf(timePeriod1), Integer.valueOf(timePeriod2), Integer.valueOf(timePeriod3) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] ultOsc(Instrument instrument, Period period, OfferSide side, int timePeriod1, int timePeriod2, int timePeriod3, long from, long to) throws JFException {
	/* 2886 */     return ((double[])(double[])function(instrument, period, side, "ULTOSC", null, new Object[] { Integer.valueOf(timePeriod1), Integer.valueOf(timePeriod2), Integer.valueOf(timePeriod3) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] ultOsc(Instrument instrument, Period period, OfferSide side, int timePeriod1, int timePeriod2, int timePeriod3, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2890 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ULTOSC", null, new Object[] { Integer.valueOf(timePeriod1), Integer.valueOf(timePeriod2), Integer.valueOf(timePeriod3) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double var(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, int shift) throws JFException {
	/* 2894 */     return ((Double)function(instrument, period, side, "VAR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDev) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] var(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, long from, long to) throws JFException {
	/* 2898 */     return ((double[])(double[])function(instrument, period, side, "VAR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDev) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] var(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, double nbDev, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2902 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "VAR", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod), Double.valueOf(nbDev) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double volume(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 2906 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "Volume", null, null, shift);
	/* 2907 */     return ((Double)ret[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] volume(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 2911 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "Volume", null, null, from, to);
	/* 2912 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] volume(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2916 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "Volume", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2917 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double volumeWAP(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2921 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "VolumeWAP", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 2922 */     return ((Double)ret[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] volumeWAP(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2926 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "VolumeWAP", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2927 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] volumeWAP(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2931 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "VolumeWAP", new IIndicators.AppliedPrice[] { appliedPrice, appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2932 */     return ((double[])(double[])ret[0]);
	/*      */   }
	/*      */ 
	/*      */   public double waddahAttar(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 2936 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side }, "WADDAHAT", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] waddahAttar(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException
	/*      */   {
	/* 2941 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "WADDAHAT", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] waddahAttar(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2946 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "WADDAHAT", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double wclPrice(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
	/* 2950 */     return ((Double)function(instrument, period, side, "WCLPRICE", null, null, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] wclPrice(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException {
	/* 2954 */     return ((double[])(double[])function(instrument, period, side, "WCLPRICE", null, null, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] wclPrice(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2958 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "WCLPRICE", null, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double willr(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2962 */     return ((Double)function(instrument, period, side, "WILLR", null, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] willr(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2966 */     return ((double[])(double[])function(instrument, period, side, "WILLR", null, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] willr(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2970 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "WILLR", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double wma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, int shift) throws JFException {
	/* 2974 */     return ((Double)function(instrument, period, side, "WMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] wma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, long from, long to) throws JFException {
	/* 2978 */     return ((double[])(double[])function(instrument, period, side, "WMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] wma(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException {
	/* 2982 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "WMA", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] woodPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, int shift) throws JFException {
	/* 2986 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "WOODPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, shift);
	/* 2987 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue(), ((Double)ret[3]).doubleValue(), ((Double)ret[4]).doubleValue(), ((Double)ret[5]).doubleValue(), ((Double)ret[6]).doubleValue() };
	/*      */   }
	/*      */ 
	/*      */   public double[][] woodPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, long from, long to) throws JFException {
	/* 2991 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "WOODPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, from, to);
	/* 2992 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double[][] woodPivot(Instrument instrument, Period period, OfferSide side, int timePeriod, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 2997 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side, side }, "WOODPIVOT", null, new Object[] { Integer.valueOf(timePeriod) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/* 2998 */     return new double[][] { (double[])(double[])ret[0], (double[])(double[])ret[1], (double[])(double[])ret[2], (double[])(double[])ret[3], (double[])(double[])ret[4], (double[])(double[])ret[5], (double[])(double[])ret[6] };
	/*      */   }
	/*      */ 
	/*      */   public double zigzag(Instrument instrument, Period period, OfferSide side, int extDepth, int extDeviation, int extBackstep, int shift) throws JFException {
	/* 3002 */     return ((Double)calculateIndicator(instrument, period, new OfferSide[] { side }, "ZIGZAG", null, new Object[] { Integer.valueOf(extDepth), Integer.valueOf(extDeviation), Integer.valueOf(extBackstep) }, shift)[0]).doubleValue();
	/*      */   }
	/*      */ 
	/*      */   public double[] zigzag(Instrument instrument, Period period, OfferSide side, int extDepth, int extDeviation, int extBackstep, long from, long to) throws JFException
	/*      */   {
	/* 3007 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ZIGZAG", null, new Object[] { Integer.valueOf(extDepth), Integer.valueOf(extDeviation), Integer.valueOf(extBackstep) }, from, to)[0]);
	/*      */   }
	/*      */ 
	/*      */   public double[] zigzag(Instrument instrument, Period period, OfferSide side, int extDepth, int extDeviation, int extBackstep, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
	/*      */   {
	/* 3012 */     return ((double[])(double[])calculateIndicator(instrument, period, new OfferSide[] { side }, "ZIGZAG", null, new Object[] { Integer.valueOf(extDepth), Integer.valueOf(extDeviation), Integer.valueOf(extBackstep) }, filter, numberOfCandlesBefore, time, numberOfCandlesAfter)[0]);
	/*      */   }
	/*      */ 
	/*      */   protected IndicatorHolder getCachedIndicator(String name)
	/*      */     throws JFException
	/*      */   {
	/* 3018 */     IndicatorHolder indicatorHolder = (IndicatorHolder)this.cachedIndicators.remove(name);
	/* 3019 */     if (indicatorHolder == null) {
	/* 3020 */       indicatorHolder = getCustomIndicatorHolder(name);
	/* 3021 */       if (indicatorHolder == null) {
	/* 3022 */         indicatorHolder = IndicatorsProvider.getInstance().getIndicatorHolder(name);
	/*      */       }
	/*      */     }
	/* 3025 */     return indicatorHolder;
	/*      */   }
	/*      */ 
	/*      */   private IIndicator getCustomIndicator(String customIndicatorName) throws JFException {
	/* 3029 */     if (this.customIndicators.containsKey(customIndicatorName)) {
	/* 3030 */       IIndicator custom = null;
	/*      */       try {
	/* 3032 */         custom = (IIndicator)((Class)this.customIndicators.get(customIndicatorName)).newInstance();
	/*      */       } catch (Throwable th) {
	/* 3034 */         throw new JFException("Error while creating custom indicator [" + customIndicatorName + "] instance");
	/*      */       }
	/*      */       try {
	/* 3037 */         custom.onStart(IndicatorHelper.createIndicatorContext());
	/* 3038 */         return custom;
	/*      */       } catch (Throwable th) {
	/* 3040 */         throw new JFException("Exception in onStart method");
	/*      */       }
	/*      */     }
	/* 3043 */     return null;
	/*      */   }
	/*      */ 
	/*      */   private IndicatorHolder getCustomIndicatorHolder(String customIndicatorName) throws JFException {
	/* 3047 */     IIndicator indicator = getCustomIndicator(customIndicatorName);
	/* 3048 */     if (indicator != null) {
	/* 3049 */       return new IndicatorHolder(indicator, IndicatorHelper.createIndicatorContext());
	/*      */     }
	/*      */ 
	/* 3055 */     return null;
	/*      */   }
	/*      */ 
	/*      */   protected void cacheIndicator(String name, IndicatorHolder indicator) {
	/* 3059 */     this.cachedIndicators.put(name, indicator);
	/*      */   }
	/*      */ 
	/*      */   private Object[] function(Instrument instrument, Period period, OfferSide side, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, int shift) throws JFException
	/*      */   {
	/* 3064 */     return calculateIndicator(instrument, period, new OfferSide[] { side }, functionName, inputTypes, optParams, shift);
	/*      */   }
	/*      */ 
	/*      */   public Object[] calculateIndicator(Instrument instrument, Period period, OfferSide[] side, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, int shift) throws JFException
	/*      */   {
	/*      */     try {
	/* 3070 */       IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
	/*      */       try {
	/* 3072 */         checkNotTick(period);
	/* 3073 */         checkSide(period, side);
	/* 3074 */         checkShiftPositive(shift);
	/* 3075 */         if (indicatorHolder == null) {
	/* 3076 */           throw new JFException("Indicator with name " + functionName + " was not found");
	/*      */         }
	/* 3078 */         int[] lookSides = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
	/* 3079 */         Object[] arrayOfObject = calculateIndicator(instrument, period, side, inputTypes, shift, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
	/*      */ 
	/* 3083 */         return arrayOfObject;
	/*      */       }
	/*      */       finally
	/*      */       {
	/* 3081 */         if (indicatorHolder != null) {
	/* 3082 */           indicatorHolder.getIndicatorContext().setChartInfo(null, null, null);
	/* 3083 */           cacheIndicator(functionName, indicatorHolder);
	/*      */         }
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 3087 */       throw e;
	/*      */     } catch (RuntimeException e) {
	/* 3089 */       throw e;
	/*      */     } catch (Exception e) {
	/* 3091 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   protected int[] calculateLookbackLookforward(IIndicator indicator, Object[] optParams) throws JFException {
	/* 3096 */     setOptParams(indicator, optParams);
	/*      */     int indicatorLookback;
	/*      */     int lookback;
	/*      */     try {
	/* 3100 */       lookback = indicatorLookback = indicator.getLookback();
	/*      */     } catch (Throwable t) {
	/* 3102 */       LOGGER.error(t.getMessage(), t);
	/* 3103 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3104 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3105 */       throw new JFException(t);
	/*      */     }
	/*      */     int lookforward;
	/*      */     try {
	/* 3109 */       lookforward = indicator.getLookforward();
	/*      */     } catch (AbstractMethodError e) {
	/* 3111 */       lookforward = 0;
	/*      */     } catch (Throwable t) {
	/* 3113 */       LOGGER.error(t.getMessage(), t);
	/* 3114 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3115 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3116 */       throw new JFException(t);
	/*      */     }
	/* 3118 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3119 */     if (indicatorInfo.isUnstablePeriod())
	/*      */     {
	/* 3121 */       lookback += ((lookback * 4 < 100) ? 100 : lookback * 4);
	/*      */     }
	/* 3123 */     return new int[] { indicatorLookback, lookback, lookforward };
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(Instrument instrument, Period period, OfferSide[] side, IIndicators.AppliedPrice[] inputTypes, int shift, IndicatorHolder indicatorHolder, int indicatorLookback, int lookback, int lookforward) throws JFException, DataCacheException {
	/* 3127 */     int inputLength = -2147483648;
	/* 3128 */     IIndicator indicator = indicatorHolder.getIndicator();
	/* 3129 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3130 */     IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
	/* 3131 */     indicatorContext.setChartInfo(instrument, period, (indicatorInfo.getNumberOfInputs() > 0) ? side[0] : OfferSide.BID);
	/* 3132 */     int i = 0; for (int j = indicatorInfo.getNumberOfInputs(); i < j; ++i) {
	/* 3133 */       InputParameterInfo info = indicator.getInputParameterInfo(i);
	/* 3134 */       Instrument currentInstrument = (info.getInstrument() == null) ? instrument : info.getInstrument();
	/* 3135 */       Period currentPeriod = (info.getPeriod() == null) ? period : info.getPeriod();
	/* 3136 */       OfferSide currentSide = (info.getOfferSide() == null) ? side[i] : info.getOfferSide();
	/*      */       Object inputData;
	/* 3138 */       if ((info.getInstrument() == null) && (info.getPeriod() == null)) {
	/* 3139 */         inputData = getInputData(currentInstrument, currentPeriod, currentSide, info.getType(), (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null, shift, lookback, lookforward);
	/*      */       }
	/*      */       else {
	/* 3142 */         if (period.getInterval() > currentPeriod.getInterval()) {
	/* 3143 */           throw new JFException("Indicator [" + indicatorInfo.getName() + "] cannot be run with periods longer than " + currentPeriod);
	/*      */         }
	/* 3145 */         int currentShift = calculateShift(instrument, currentPeriod, period, shift);
	/*      */ 
	/* 3147 */         int currentLookforward = (currentShift >= lookforward) ? lookforward : (currentShift < 0) ? 0 : currentShift;
	/* 3148 */         inputData = getInputData(currentInstrument, currentPeriod, currentSide, info.getType(), (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null, currentShift, lookback, currentLookforward);
	/*      */       }
	/*      */ 
	/* 3151 */       if (inputData == null)
	/*      */       {
	/* 3153 */         Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3154 */         int k = 0; for (int l = indicatorInfo.getNumberOfOutputs(); k < l; ++k) {
	/* 3155 */           OutputParameterInfo outInfo = indicator.getOutputParameterInfo(k);
	/* 3156 */           switch (outInfo.getType())
	/*      */           {
	/*      */           case DOUBLE:
	/* 3158 */             ret[k] = Double.valueOf((0.0D / 0.0D));
	/* 3159 */             break;
	/*      */           case INT:
	/* 3161 */             ret[k] = Integer.valueOf(-2147483648);
	/* 3162 */             break;
	/*      */           case OBJECT:
	/* 3164 */             ret[k] = null;
	/*      */           }
	/*      */         }
	/*      */ 
	/* 3168 */         return ret;
	/*      */       }
	/* 3170 */       if ((info.getInstrument() == null) && (info.getPeriod() == null)) {
	/* 3171 */         if (info.getType() == InputParameterInfo.Type.PRICE)
	/* 3172 */           inputLength = ((double[][])(double[][])inputData)[0].length;
	/* 3173 */         else if (info.getType() == InputParameterInfo.Type.DOUBLE)
	/* 3174 */           inputLength = ((double[])(double[])inputData).length;
	/* 3175 */         else if (info.getType() == InputParameterInfo.Type.BAR)
	/* 3176 */           inputLength = ((IBar[])(IBar[])inputData).length;
	/*      */       }
	/*      */       try
	/*      */       {
	/* 3180 */         indicator.setInputParameter(i, inputData);
	/*      */       } catch (Throwable t) {
	/* 3182 */         LOGGER.error(t.getMessage(), t);
	/* 3183 */         String error = StrategyWrapper.representError(indicator, t);
	/* 3184 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3185 */         throw new JFException(t);
	/*      */       }
	/*      */     }
	/* 3188 */     if (inputLength == -2147483648) {
	/* 3189 */       if (indicatorInfo.getNumberOfInputs() == 0) {
	/* 3190 */         inputLength = 0;
	/*      */       } else {
	/* 3192 */         Object inputData = getInputData(instrument, period, OfferSide.BID, InputParameterInfo.Type.BAR, null, shift, lookback, lookforward);
	/* 3193 */         inputLength = ((IBar[])(IBar[])inputData).length;
	/*      */       }
	/*      */     }
	/* 3196 */     return calculateIndicator(indicator, indicatorLookback, lookback, lookforward, inputLength);
	/*      */   }
	/*      */ 
	/*      */   protected int calculateShift(Instrument instrument, Period currentPeriod, Period period, int shift) throws JFException {
	/* 3200 */     if (period == currentPeriod) {
	/* 3201 */       return shift;
	/*      */     }
	/* 3203 */     if (shift == 0) {
	/* 3204 */       return 0;
	/*      */     }
	/* 3206 */     long currentBarStartTime = this.history.getStartTimeOfCurrentBar(instrument, period);
	/* 3207 */     long requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, DataCacheUtils.getPreviousCandleStartFast(period, currentBarStartTime), shift);
	/* 3208 */     currentBarStartTime = this.history.getStartTimeOfCurrentBar(instrument, currentPeriod);
	/* 3209 */     requestedBarStartTime = DataCacheUtils.getCandleStartFast(currentPeriod, requestedBarStartTime);
	/* 3210 */     if (requestedBarStartTime <= currentBarStartTime) {
	/* 3211 */       return (DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, requestedBarStartTime, currentBarStartTime) - 1);
	/*      */     }
	/* 3213 */     return (-(DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentBarStartTime, requestedBarStartTime) - 1));
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(IIndicator indicator, int indicatorLookback, int lookback, int lookforward, int inputLength)
	/*      */     throws JFException
	/*      */   {
	/* 3219 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/*      */ 
	/* 3221 */     if (inputLength <= indicatorLookback + lookforward) {
	/* 3222 */       LOGGER.warn("There is no enough data to calculate value");
	/* 3223 */       Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3224 */       int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3225 */         OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3226 */         switch (info.getType())
	/*      */         {
	/*      */         case DOUBLE:
	/* 3228 */           ret[i] = Double.valueOf((0.0D / 0.0D));
	/* 3229 */           break;
	/*      */         case INT:
	/* 3231 */           ret[i] = Integer.valueOf(-2147483648);
	/* 3232 */           break;
	/*      */         case OBJECT:
	/* 3234 */           ret[i] = null;
	/*      */         }
	/*      */       }
	/*      */ 
	/* 3238 */       return ret;
	/*      */     }
	/*      */ 
	/* 3241 */     Object[] outputs = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3242 */     int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3243 */       OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3244 */       switch (info.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 3246 */         double[] doubles = new double[inputLength - (indicatorLookback + lookforward)];
	/* 3247 */         outputs[i] = doubles;
	/*      */         try {
	/* 3249 */           indicator.setOutputParameter(i, doubles);
	/*      */         } catch (Throwable t) {
	/* 3251 */           LOGGER.error(t.getMessage(), t);
	/* 3252 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3253 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3254 */           throw new JFException(t);
	/*      */         }
	/*      */       case INT:
	/* 3258 */         int[] ints = new int[inputLength - (indicatorLookback + lookforward)];
	/* 3259 */         outputs[i] = ints;
	/*      */         try {
	/* 3261 */           indicator.setOutputParameter(i, ints);
	/*      */         } catch (Throwable t) {
	/* 3263 */           LOGGER.error(t.getMessage(), t);
	/* 3264 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3265 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3266 */           throw new JFException(t);
	/*      */         }
	/*      */       case OBJECT:
	/* 3270 */         Object[] objects = new Object[inputLength - (indicatorLookback + lookforward)];
	/* 3271 */         outputs[i] = objects;
	/*      */         try {
	/* 3273 */           indicator.setOutputParameter(i, objects);
	/*      */         } catch (Throwable t) {
	/* 3275 */           LOGGER.error(t.getMessage(), t);
	/* 3276 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3277 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3278 */           throw new JFException(t);
	/*      */         }
	/*      */       }
	/*      */     }
	/*      */     IndicatorResult result;
	/*      */     try
	/*      */     {
	/* 3285 */       result = indicator.calculate(0, inputLength - 1);
	/*      */     } catch (TaLibException e) {
	/* 3287 */       Throwable t = e.getCause();
	/* 3288 */       LOGGER.error(t.getMessage(), t);
	/* 3289 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3290 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3291 */       throw new JFException(t);
	/*      */     } catch (Throwable t) {
	/* 3293 */       LOGGER.error(t.getMessage(), t);
	/* 3294 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3295 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3296 */       throw new JFException(t);
	/*      */     }
	/* 3298 */     if ((result.getLastValueIndex() == -2147483648) && (result.getNumberOfElements() != 0)) {
	/* 3299 */       if (lookforward != 0) {
	/* 3300 */         String error = "calculate() method of indicator [" + indicator.getIndicatorInfo().getName() + "] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero";
	/*      */ 
	/* 3302 */         LOGGER.error(error);
	/* 3303 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/* 3304 */         throw new JFException(error);
	/*      */       }
	/* 3306 */       result.setLastValueIndex(inputLength - 1);
	/*      */     }
	/*      */ 
	/* 3309 */     if ((result.getLastValueIndex() + 1 - result.getFirstValueIndex() < inputLength - lookback - lookforward) || (result.getNumberOfElements() < inputLength - lookback - lookforward)) {
	/* 3310 */       String error = "calculate() method of indicator [" + indicator.getIndicatorInfo().getName() + "] returned less values than expected";
	/* 3311 */       LOGGER.error(error);
	/* 3312 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/* 3313 */       throw new JFException(error);
	/*      */     }
	/* 3315 */     Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3316 */     i = 0; 
					for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3317 */       OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3318 */       if (result.getNumberOfElements() == 0) {
	/* 3319 */         throw new JFException("Indicator didn't return any value");
	/*      */       }
	/* 3321 */       switch (info.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 3323 */         double[] doubles = (double[])(double[])outputs[i];
	/* 3324 */         ret[i] = Double.valueOf(doubles[(result.getNumberOfElements() - 1)]);
	/* 3325 */         break;
	/*      */       case INT:
	/* 3327 */         int[] ints = (int[])(int[])outputs[i];
	/* 3328 */         ret[i] = Integer.valueOf(ints[(result.getNumberOfElements() - 1)]);
	/* 3329 */         break;
	/*      */       case OBJECT:
	/* 3331 */         Object[] objects = (Object[])(Object[])outputs[i];
	/* 3332 */         ret[i] = objects[(result.getNumberOfElements() - 1)];
	/*      */       }
	/*      */     }
	/*      */ 
	/* 3336 */     return ret;
	/*      */   }
	/*      */ 
	/*      */   private Object[] function(Instrument instrument, Period period, OfferSide side, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, long from, long to) throws JFException
	/*      */   {
	/* 3341 */     return calculateIndicator(instrument, period, new OfferSide[] { side }, functionName, inputTypes, optParams, from, to);
	/*      */   }
	/*      */ 
	/*      */   public Object[] calculateIndicator(Instrument instrument, Period period, OfferSide[] side, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, long from, long to) throws JFException
	/*      */   {
	/*      */     try {
	/* 3347 */       checkSide(period, side);
	/* 3348 */       checkIntervalValid(period, from, to);
	/* 3349 */       IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
	/* 3350 */       if (indicatorHolder == null)
	/* 3351 */         throw new JFException("Indicator with name " + functionName + " was not found");
	/*      */       try
	/*      */       {
	/* 3354 */         setOptParams(indicatorHolder.getIndicator(), optParams);
	/* 3355 */         int[] lookSides = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
	/* 3356 */         Object[] arrayOfObject = calculateIndicator(instrument, period, side, inputTypes, from, to, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
	/*      */ 
	/* 3359 */         return arrayOfObject;
	/*      */       }
	/*      */       finally
	/*      */       {
	/* 3358 */         indicatorHolder.getIndicatorContext().setChartInfo(null, null, null);
	/* 3359 */         cacheIndicator(functionName, indicatorHolder);
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 3362 */       throw e;
	/*      */     } catch (RuntimeException e) {
	/* 3364 */       throw e;
	/*      */     } catch (Exception e) {
	/* 3366 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(Instrument instrument, Period period, OfferSide[] side, IIndicators.AppliedPrice[] inputTypes, long from, long to, IndicatorHolder indicatorHolder, int indicatorLookback, int lookback, int lookforward)
	/*      */     throws JFException, DataCacheException
	/*      */   {
	/* 3373 */     IIndicator indicator = indicatorHolder.getIndicator();
	/* 3374 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3375 */     IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
	/* 3376 */     indicatorContext.setChartInfo(instrument, period, (indicatorInfo.getNumberOfInputs() > 0) ? side[0] : OfferSide.BID);
	/* 3377 */     int length = -2147483648;
	/* 3378 */     int i = 0; for (int j = indicatorInfo.getNumberOfInputs(); i < j; ++i) {
	/* 3379 */       InputParameterInfo info = indicator.getInputParameterInfo(i);
	/* 3380 */       OfferSide currentSide = (info.getOfferSide() == null) ? side[i] : info.getOfferSide();
	/* 3381 */       Period currentPeriod = (info.getPeriod() == null) ? period : info.getPeriod();
	/* 3382 */       Instrument currentInstrument = (info.getInstrument() == null) ? instrument : info.getInstrument();
	/* 3383 */       if (period.getInterval() > currentPeriod.getInterval()) {
	/* 3384 */         throw new JFException("Indicator [" + indicatorInfo.getName() + "] cannot be run with periods longer than " + currentPeriod);
	/*      */       }
	/* 3386 */       int flats = 0;
	/* 3387 */       long currentTo = to;
	/* 3388 */       boolean addCurrentCandle = false;
	/* 3389 */       if (instrument != currentInstrument) {
	/* 3390 */         long lastCandleTime = DataCacheUtils.getPreviousCandleStartFast(period, this.history.getStartTimeOfCurrentBar(instrument, period));
	/* 3391 */         if (currentTo > lastCandleTime) {
	/* 3392 */           currentTo = lastCandleTime;
	/*      */         }
	/* 3394 */         currentTo = DataCacheUtils.getCandleStartFast(currentPeriod, currentTo);
	/* 3395 */         long lastFormedCandleTime = DataCacheUtils.getPreviousCandleStartFast(currentPeriod, this.history.getStartTimeOfCurrentBar(currentInstrument, currentPeriod));
	/* 3396 */         if (currentTo > lastFormedCandleTime) {
	/* 3397 */           addCurrentCandle = true;
	/*      */         }
	/* 3399 */         lastCandleTime = this.history.getStartTimeOfCurrentBar(currentInstrument, currentPeriod);
	/* 3400 */         if (currentTo > lastCandleTime) {
	/* 3401 */           flats = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, lastCandleTime, currentTo) - 1;
	/*      */         }
	/* 3403 */         if (currentTo > lastFormedCandleTime) {
	/* 3404 */           currentTo = lastFormedCandleTime;
	/*      */         }
	/*      */       }
	/* 3407 */       Object inputData = getInputData(currentInstrument, currentPeriod, currentSide, info.getType(), (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null, from, currentTo, lookback, lookforward, addCurrentCandle, flats);
	/*      */ 
	/* 3410 */       if ((info.getInstrument() == null) && (info.getPeriod() == null)) {
	/* 3411 */         if (info.getType() == InputParameterInfo.Type.PRICE)
	/* 3412 */           length = ((double[][])(double[][])inputData)[0].length;
	/* 3413 */         else if (info.getType() == InputParameterInfo.Type.DOUBLE)
	/* 3414 */           length = ((double[])(double[])inputData).length;
	/* 3415 */         else if (info.getType() == InputParameterInfo.Type.BAR)
	/* 3416 */           length = ((IBar[])(IBar[])inputData).length;
	/*      */       }
	/*      */       try
	/*      */       {
	/* 3420 */         indicator.setInputParameter(i, inputData);
	/*      */       } catch (Throwable t) {
	/* 3422 */         LOGGER.error(t.getMessage(), t);
	/* 3423 */         String error = StrategyWrapper.representError(indicator, t);
	/* 3424 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3425 */         throw new JFException(t);
	/*      */       }
	/*      */     }
	/* 3428 */     if (length == -2147483648) {
	/* 3429 */       if (indicatorInfo.getNumberOfInputs() == 0) {
	/* 3430 */         length = 0;
	/*      */       } else {
	/* 3432 */         Object inputData = getInputData(instrument, period, OfferSide.BID, InputParameterInfo.Type.BAR, null, from, to, lookback, lookforward, false, 0);
	/*      */ 
	/* 3434 */         length = ((IBar[])(IBar[])inputData).length;
	/*      */       }
	/*      */     }
	/* 3437 */     return calculateIndicator(period, from, to, indicator, indicatorLookback, lookback, lookforward, length);
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(Period period, long from, long to, IIndicator indicator, int indicatorLookback, int lookback, int lookforward, int inputLength) throws JFException {
	/* 3441 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/*      */ 
	/* 3443 */     if (inputLength <= indicatorLookback + lookforward) {
	/* 3444 */       LOGGER.warn("There is no enough data to calculate values");
	/* 3445 */       Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3446 */       int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3447 */         OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3448 */         switch (info.getType())
	/*      */         {
	/*      */         case DOUBLE:
	/* 3450 */           ret[i] = new double[0];
	/* 3451 */           break;
	/*      */         case INT:
	/* 3453 */           ret[i] = new int[0];
	/* 3454 */           break;
	/*      */         case OBJECT:
	/* 3456 */           ret[i] = new Object[0];
	/*      */         }
	/*      */       }
	/*      */ 
	/* 3460 */       return ret;
	/*      */     }
	/*      */ 
	/* 3463 */     Object[] outputs = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3464 */     int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3465 */       OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3466 */       switch (info.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 3468 */         double[] doubles = new double[inputLength - (indicatorLookback + lookforward)];
	/* 3469 */         outputs[i] = doubles;
	/*      */         try {
	/* 3471 */           indicator.setOutputParameter(i, doubles);
	/*      */         } catch (Throwable t) {
	/* 3473 */           LOGGER.error(t.getMessage(), t);
	/* 3474 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3475 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3476 */           throw new JFException(t);
	/*      */         }
	/*      */       case INT:
	/* 3480 */         int[] ints = new int[inputLength - (indicatorLookback + lookforward)];
	/* 3481 */         outputs[i] = ints;
	/*      */         try {
	/* 3483 */           indicator.setOutputParameter(i, ints);
	/*      */         } catch (Throwable t) {
	/* 3485 */           LOGGER.error(t.getMessage(), t);
	/* 3486 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3487 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3488 */           throw new JFException(t);
	/*      */         }
	/*      */       case OBJECT:
	/* 3492 */         Object[] objects = new Object[inputLength - (indicatorLookback + lookforward)];
	/* 3493 */         outputs[i] = objects;
	/*      */         try {
	/* 3495 */           indicator.setOutputParameter(i, objects);
	/*      */         } catch (Throwable t) {
	/* 3497 */           LOGGER.error(t.getMessage(), t);
	/* 3498 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3499 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3500 */           throw new JFException(t);
	/*      */         }
	/*      */       }
	/*      */     }
	/*      */     IndicatorResult result;
	/*      */     try
	/*      */     {
	/* 3507 */       result = indicator.calculate(0, inputLength - 1);
	/*      */     } catch (TaLibException e) {
	/* 3509 */       Throwable t = e.getCause();
	/* 3510 */       LOGGER.error(t.getMessage(), t);
	/* 3511 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3512 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3513 */       throw new JFException(t);
	/*      */     } catch (Throwable t) {
	/* 3515 */       LOGGER.error(t.getMessage(), t);
	/* 3516 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3517 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3518 */       throw new JFException(t);
	/*      */     }
	/* 3520 */     if ((result.getLastValueIndex() == -2147483648) && (result.getNumberOfElements() != 0)) {
	/* 3521 */       if (lookforward != 0) {
	/* 3522 */         String error = "calculate() method of indicator [" + indicator.getIndicatorInfo().getName() + "] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero";
	/*      */ 
	/* 3524 */         LOGGER.error(error);
	/* 3525 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/* 3526 */         throw new JFException(error);
	/*      */       }
	/* 3528 */       result.setLastValueIndex(inputLength - 1);
	/*      */     }
	/*      */ 
	/* 3531 */     if ((result.getLastValueIndex() + 1 - result.getFirstValueIndex() < inputLength - lookback - lookforward) || (result.getNumberOfElements() < inputLength - lookback - lookforward)) {
	/* 3532 */       String error = "calculate() method of indicator [" + indicator.getIndicatorInfo().getName() + "] returned less values than expected";
	/* 3533 */       LOGGER.error(error);
	/* 3534 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/* 3535 */       throw new JFException(error);
	/*      */     }
	/* 3537 */     Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3538 */     i = 0; 
					for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3539 */       OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/*      */       int outCount;
	/* 3542 */       if (period != null) {
	/* 3543 */         outCount = (DataCacheUtils.getCandlesCountBetweenFast((period == Period.TICK) ? Period.ONE_SEC : period, from, to) > result.getNumberOfElements()) ? result.getNumberOfElements() : DataCacheUtils.getCandlesCountBetweenFast((period == Period.TICK) ? Period.ONE_SEC : period, from, to);
	/*      */       }
	/*      */       else
	/*      */       {
	/* 3547 */         outCount = inputLength - lookback - lookforward;
	/*      */       }
	/*      */ 
	/* 3550 */       switch (info.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 3552 */         double[] doubles = (double[])(double[])outputs[i];
	/* 3553 */         double[] retDoubles = new double[outCount];
	/* 3554 */         System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
	/* 3555 */         ret[i] = retDoubles;
	/* 3556 */         break;
	/*      */       case INT:
	/* 3558 */         int[] ints = (int[])(int[])outputs[i];
	/* 3559 */         int[] retInts = new int[outCount];
	/* 3560 */         System.arraycopy(ints, result.getNumberOfElements() - outCount, retInts, 0, outCount);
	/* 3561 */         ret[i] = retInts;
	/* 3562 */         break;
	/*      */       case OBJECT:
	/* 3564 */         Object[] objects = (Object[])(Object[])outputs[i];
	/* 3565 */         Object[] retObjs = new Object[outCount];
	/* 3566 */         System.arraycopy(objects, result.getNumberOfElements() - outCount, retObjs, 0, outCount);
	/* 3567 */         ret[i] = retObjs;
	/*      */       }
	/*      */     }
	/*      */ 
	/* 3571 */     return ret;
	/*      */   }
	/*      */ 
	/*      */   public Object[] calculateIndicator(Instrument instrument, Period period, OfferSide[] side, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
	/*      */     throws JFException
	/*      */   {
	/* 3587 */     RateDataIndicatorCalculationResultWraper wraper = calculateIndicatorReturnSourceData(instrument, period, side, functionName, inputTypes, optParams, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
	/*      */ 
	/* 3599 */     return wraper.getIndicatorCalculationResult();
	/*      */   }
	/*      */ 
	/*      */   public static int calculateLookback(IIndicator indicator) {
	/* 3603 */     return calculateLookback(indicator, indicator.getLookback());
	/*      */   }
	/*      */ 
	/*      */   private static int calculateLookback(IIndicator indicator, int initialLookback) {
	/* 3607 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3608 */     if (indicatorInfo.isUnstablePeriod())
	/*      */     {
	/* 3610 */       initialLookback += ((initialLookback * 4 < 100) ? 100 : initialLookback * 4);
	/*      */     }
	/* 3612 */     return initialLookback;
	/*      */   }
	/*      */ 
	/*      */   public RateDataIndicatorCalculationResultWraper calculateIndicatorReturnSourceData(Instrument instrument, Period period, OfferSide[] side, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter)
	/*      */     throws JFException
	/*      */   {
	/* 3644 */     RateDataIndicatorCalculationResultWraper calculationResultWraper = new RateDataIndicatorCalculationResultWraper();
	/*      */     try
	/*      */     {
	/* 3647 */       checkNotTick(period);
	/* 3648 */       checkSide(period, side);
	/* 3649 */       checkIntervalValid(numberOfCandlesBefore, numberOfCandlesAfter);
	/* 3650 */       IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
	/*      */       try {
	/* 3652 */         if (indicatorHolder == null) {
	/* 3653 */           throw new JFException("Indicator with name " + functionName + " was not found");
	/*      */         }
	/* 3655 */         IIndicator indicator = indicatorHolder.getIndicator();
	/* 3656 */         setOptParams(indicator, optParams);
	/*      */         int indicatorLookback;
	/*      */         int lookback;
	/*      */         try {
	/* 3660 */           lookback = indicatorLookback = indicator.getLookback();
	/*      */         } catch (Throwable t) {
	/* 3662 */           LOGGER.error(t.getMessage(), t);
	/* 3663 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3664 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3665 */           throw new JFException(t);
	/*      */         }
	/*      */         int lookforward;
	/*      */         try {
	/* 3669 */           lookforward = indicator.getLookforward();
	/*      */         } catch (AbstractMethodError e) {
	/* 3671 */           lookforward = 0;
	/*      */         } catch (Throwable t) {
	/* 3673 */           LOGGER.error(t.getMessage(), t);
	/* 3674 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3675 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3676 */           throw new JFException(t);
	/*      */         }
	/* 3678 */         IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3679 */         if (indicatorInfo.isUnstablePeriod())
	/*      */         {
	/* 3681 */           lookback = calculateLookback(indicator, lookback);
	/*      */         }
	/* 3683 */         IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
	/* 3684 */         indicatorContext.setChartInfo(instrument, period, (indicatorInfo.getNumberOfInputs() > 0) ? side[0] : OfferSide.BID);
	/* 3685 */         int length = -2147483648;
	/* 3686 */         int i = 0; for (int j = indicatorInfo.getNumberOfInputs(); i < j; ++i) {
	/* 3687 */           InputParameterInfo info = indicator.getInputParameterInfo(i);
	/*      */           Object inputData;
	/*      */             List sourceBars;
	/* 3690 */           if ((info.getInstrument() == null) && (info.getPeriod() == null)) {
	/* 3691 */             InputParameterInfo.Type inputType = info.getType();
	/* 3692 */             IIndicators.AppliedPrice appliedPrice = (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null;
	/*      */ 
	/* 3694 */             sourceBars = getInputBars(instrument, period, side[i], inputType, appliedPrice, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, lookback, lookforward, 0);
	/* 3695 */             inputData = barsToReal(sourceBars, inputType, appliedPrice);
	/*      */           } else {
	/* 3697 */             OfferSide currentSide = (info.getOfferSide() == null) ? side[i] : info.getOfferSide();
	/* 3698 */             Instrument currentInstrument = (info.getInstrument() == null) ? instrument : info.getInstrument();
	/* 3699 */             Period currentPeriod = (info.getPeriod() == null) ? period : info.getPeriod();
	/* 3700 */             if (period.getInterval() > currentPeriod.getInterval())
	/* 3701 */               throw new JFException("Indicator [" + indicatorInfo.getName() + "] cannot be run with periods longer than " + currentPeriod);
	/* 3703 */             if (currentPeriod != period)
	/*      */             {
	/* 3705 */               IBar[] bars = (IBar[])(IBar[])getInputData(instrument, period, side[i], InputParameterInfo.Type.BAR, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, 0, 0, 0);
	/* 3706 */               if ((bars != null) && (bars.length > 0)) {
	/* 3707 */                 long currentFrom = DataCacheUtils.getCandleStartFast(currentPeriod, bars[0].getTime());
	/* 3708 */                 long currentTo = DataCacheUtils.getCandleStartFast(currentPeriod, bars[(bars.length - 1)].getTime());
	/* 3709 */                 int before = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentFrom, currentTo);
	/* 3710 */                 long currentBarTime = this.history.getStartTimeOfCurrentBar(currentInstrument, currentPeriod);
	/* 3711 */                 int flats = 0;
	/* 3712 */                 if (currentTo > currentBarTime) {
	/* 3713 */                   flats = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentBarTime, currentTo) - 1;
	/* 3714 */                   currentTo = currentBarTime;
	/*      */                 }
	/*      */ 
	/* 3717 */                 InputParameterInfo.Type inputType = info.getType();
	/* 3718 */                 IIndicators.AppliedPrice appliedPrice = (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null;
	/*      */ 
	/* 3720 */                 sourceBars = getInputBars(currentInstrument, currentPeriod, currentSide, info.getType(), appliedPrice, filter, before, currentTo, 0, (lookback - flats < 0) ? 0 : lookback - flats, lookforward, flats);
	/*      */ 
	/* 3723 */                 inputData = barsToReal(sourceBars, inputType, appliedPrice);
	/*      */               } else {
	/* 3725 */                 inputData = bars;
	/* 3726 */                 sourceBars = Arrays.asList(bars);
	/*      */               }
	/*      */             } else {
	/* 3729 */               long currentBarTime = this.history.getStartTimeOfCurrentBar(currentInstrument, period);
	/* 3730 */               int flats = 0;
	/* 3731 */               long currentTo = time;
	/* 3732 */               if (currentTo > currentBarTime) {
	/* 3733 */                 flats = DataCacheUtils.getCandlesCountBetweenFast(currentPeriod, currentBarTime, currentTo) - 1;
	/* 3734 */                 currentTo = currentBarTime;
	/*      */               }
	/*      */ 
	/* 3737 */               InputParameterInfo.Type inputType = info.getType();
	/* 3738 */               IIndicators.AppliedPrice appliedPrice = (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null;
	/*      */ 
	/* 3740 */               sourceBars = getInputBars(currentInstrument, currentPeriod, currentSide, inputType, appliedPrice, filter, numberOfCandlesBefore, currentTo, numberOfCandlesAfter, (lookback - flats < 0) ? 0 : lookback - flats, (flats > 0) ? 0 : lookforward, flats);
	/*      */ 
	/* 3742 */               inputData = barsToReal(sourceBars, inputType, appliedPrice);
	/*      */             }
	/*      */           }
	/* 3745 */           if ((info.getInstrument() == null) && (info.getPeriod() == null)) {
	/* 3746 */             if (info.getType() == InputParameterInfo.Type.PRICE)
	/* 3747 */               length = ((double[][])(double[][])inputData)[0].length;
	/* 3748 */             else if (info.getType() == InputParameterInfo.Type.DOUBLE)
	/* 3749 */               length = ((double[])(double[])inputData).length;
	/* 3750 */             else if (info.getType() == InputParameterInfo.Type.BAR)
	/* 3751 */               length = ((IBar[])(IBar[])inputData).length;
	/*      */           }
	/*      */           try
	/*      */           {
	/* 3755 */             indicator.setInputParameter(i, inputData);
	/* 3756 */             calculationResultWraper.setSourceData(sourceBars);
	/*      */           } catch (Throwable t) {
	/* 3758 */             LOGGER.error(t.getMessage(), t);
	/* 3759 */             String error = StrategyWrapper.representError(indicator, t);
	/* 3760 */             NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3761 */             throw new JFException(t);
	/*      */           }
	/*      */         }
	/* 3764 */         if (length == -2147483648) {
	/* 3765 */           if (indicatorInfo.getNumberOfInputs() == 0) {
	/* 3766 */             length = 0;
	/*      */           } else {
	/* 3768 */             Object inputData = getInputData(instrument, period, OfferSide.BID, InputParameterInfo.Type.BAR, null, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, lookback, lookforward, 0);
	/*      */ 
	/* 3770 */             length = ((IBar[])(IBar[])inputData).length;
	/*      */           }
	/*      */         }
	/*      */ 
	/* 3774 */         Object[] ret = calculateIndicator(numberOfCandlesBefore, time, numberOfCandlesAfter, indicator, indicatorLookback, indicatorLookback, lookforward, length);
	/*      */ 
	/* 3776 */         calculationResultWraper.setIndicatorCalculationResult(ret);
	/*      */ 
	/* 3779 */         return calculationResultWraper; } finally { cacheIndicator(functionName, indicatorHolder);
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 3782 */       throw e;
	/*      */     } catch (RuntimeException e) {
	/* 3784 */       throw e;
	/*      */     } catch (Exception e) {
	/* 3786 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(int numberOfCandlesBefore, long time, int numberOfCandlesAfter, IIndicator indicator, int indicatorLookback, int lookback, int lookforward, int inputLength)
	/*      */     throws JFException
	/*      */   {
	/* 3800 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3801 */     if (inputLength < numberOfCandlesBefore + numberOfCandlesAfter + indicatorLookback + lookforward) {
	/* 3802 */       LOGGER.warn("There is no enough data to calculate values");
	/* 3803 */       Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3804 */       int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3805 */         OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3806 */         switch (info.getType())
	/*      */         {
	/*      */         case DOUBLE:
	/* 3808 */           ret[i] = new double[0];
	/* 3809 */           break;
	/*      */         case INT:
	/* 3811 */           ret[i] = new int[0];
	/* 3812 */           break;
	/*      */         case OBJECT:
	/* 3814 */           ret[i] = new Object[0];
	/*      */         }
	/*      */ 
	/*      */       }
	/*      */ 
	/* 3819 */       return ret;
	/*      */     }
	/* 3821 */     Object[] outputs = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3822 */     int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3823 */       OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/* 3824 */       switch (info.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 3826 */         double[] doubles = new double[inputLength - (indicatorLookback + lookforward)];
	/* 3827 */         outputs[i] = doubles;
	/*      */         try {
	/* 3829 */           indicator.setOutputParameter(i, doubles);
	/*      */         } catch (Throwable t) {
	/* 3831 */           LOGGER.error(t.getMessage(), t);
	/* 3832 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3833 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3834 */           throw new JFException(t);
	/*      */         }
	/*      */       case INT:
	/* 3838 */         int[] ints = new int[inputLength - (indicatorLookback + lookforward)];
	/* 3839 */         outputs[i] = ints;
	/*      */         try {
	/* 3841 */           indicator.setOutputParameter(i, ints);
	/*      */         } catch (Throwable t) {
	/* 3843 */           LOGGER.error(t.getMessage(), t);
	/* 3844 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3845 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3846 */           throw new JFException(t);
	/*      */         }
	/*      */       case OBJECT:
	/* 3850 */         Object[] objects = new Object[inputLength - (indicatorLookback + lookforward)];
	/* 3851 */         outputs[i] = objects;
	/*      */         try {
	/* 3853 */           indicator.setOutputParameter(i, objects);
	/*      */         } catch (Throwable t) {
	/* 3855 */           LOGGER.error(t.getMessage(), t);
	/* 3856 */           String error = StrategyWrapper.representError(indicator, t);
	/* 3857 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3858 */           throw new JFException(t);
	/*      */         }
	/*      */       }
	/*      */     }
	/*      */     IndicatorResult result;
	/*      */     try
	/*      */     {
	/* 3865 */       result = indicator.calculate(0, inputLength - 1);
	/*      */     } catch (TaLibException e) {
	/* 3867 */       Throwable t = e.getCause();
	/* 3868 */       LOGGER.error(t.getMessage(), t);
	/* 3869 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3870 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3871 */       throw new JFException(t);
	/*      */     } catch (Throwable t) {
	/* 3873 */       LOGGER.error(t.getMessage(), t);
	/* 3874 */       String error = StrategyWrapper.representError(indicator, t);
	/* 3875 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3876 */       throw new JFException(t);
	/*      */     }
	/* 3878 */     if ((result.getLastValueIndex() == -2147483648) && (result.getNumberOfElements() != 0)) {
	/* 3879 */       if (lookforward != 0) {
	/* 3880 */         String error = "calculate() method of indicator [" + indicator.getIndicatorInfo().getName() + "] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero";
	/*      */ 
	/* 3882 */         LOGGER.error(error);
	/* 3883 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/* 3884 */         throw new JFException(error);
	/*      */       }
	/* 3886 */       result.setLastValueIndex(inputLength - 1);
	/*      */     }
	/*      */ 
	/* 3889 */     if ((result.getLastValueIndex() + 1 - result.getFirstValueIndex() < inputLength - lookback - lookforward) || (result.getNumberOfElements() < inputLength - lookback - lookforward)) {
	/* 3890 */       String error = "calculate() method of indicator [" + indicator.getIndicatorInfo().getName() + "] returned less values than expected";
	/* 3891 */       LOGGER.error(error);
	/* 3892 */       NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/* 3893 */       throw new JFException(error);
	/*      */     }
	/* 3895 */     Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 3896 */     i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 3897 */       OutputParameterInfo info = indicator.getOutputParameterInfo(i);
	/*      */       int outCount;
	/* 3898 */       switch (info.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 3900 */         double[] doubles = (double[])(double[])outputs[i];
	/* 3901 */         outCount = (numberOfCandlesBefore + numberOfCandlesAfter > result.getNumberOfElements()) ? result.getNumberOfElements() : numberOfCandlesBefore + numberOfCandlesAfter;
	/*      */ 
	/* 3903 */         double[] retDoubles = new double[outCount];
	/* 3904 */         System.arraycopy(doubles, result.getNumberOfElements() - outCount, retDoubles, 0, outCount);
	/* 3905 */         ret[i] = retDoubles;
	/* 3906 */         break;
	/*      */       case INT:
	/* 3908 */         int[] ints = (int[])(int[])outputs[i];
	/* 3909 */         outCount = (numberOfCandlesBefore + numberOfCandlesAfter > result.getNumberOfElements()) ? result.getNumberOfElements() : numberOfCandlesBefore + numberOfCandlesAfter;
	/*      */ 
	/* 3911 */         int[] retInts = new int[outCount];
	/* 3912 */         System.arraycopy(ints, result.getNumberOfElements() - outCount, retInts, 0, outCount);
	/* 3913 */         ret[i] = retInts;
	/* 3914 */         break;
	/*      */       case OBJECT:
	/* 3916 */         Object[] objects = (Object[])(Object[])outputs[i];
	/* 3917 */         outCount = (numberOfCandlesBefore + numberOfCandlesAfter > result.getNumberOfElements()) ? result.getNumberOfElements() : numberOfCandlesBefore + numberOfCandlesAfter;
	/*      */ 
	/* 3919 */         Object[] retObjs = new Object[outCount];
	/* 3920 */         System.arraycopy(objects, result.getNumberOfElements() - outCount, retObjs, 0, outCount);
	/* 3921 */         ret[i] = retObjs;
	/*      */       }
	/*      */ 
	/*      */     }
	/*      */ 
	/* 3926 */     return ret;
	/*      */   }
	/*      */ 
	/*      */   protected void setOptParams(IIndicator indicator, Object[] optParams) throws JFException {
	/* 3930 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 3931 */     int i = 0; for (int j = indicatorInfo.getNumberOfOptionalInputs(); i < j; ++i)
	/*      */       try {
	/* 3933 */         indicator.setOptInputParameter(i, optParams[i]);
	/*      */       } catch (Throwable t) {
	/* 3935 */         LOGGER.error(t.getMessage(), t);
	/* 3936 */         String error = StrategyWrapper.representError(indicator, t);
	/* 3937 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 3938 */         throw new JFException(t);
	/*      */       }
	/*      */   }
	/*      */ 
	/*      */   Object getInputData(Instrument instrument, Period period, OfferSide side, InputParameterInfo.Type inputType, IIndicators.AppliedPrice appliedPrice, int shift, int lookback, int lookforward)
	/*      */     throws DataCacheException, JFException
	/*      */   {
	/* 3945 */     if ((lookback == 0) && (lookforward == 0))
	/*      */     {
	/*      */       IBar bar;
	/* 3948 */       if (shift < 0) {
	/* 3949 */         bar = this.history.getCurrentBar(instrument, period, side);
	/* 3950 */         if (bar == null) {
	/* 3951 */           return null;
	/*      */         }
	/* 3953 */         double price = bar.getClose();
	/* 3954 */         bar = new CandleData(DataCacheUtils.getTimeForNCandlesForwardFast(period, bar.getTime(), -shift + 1), price, price, price, price, 0.0D);
	/*      */       } else {
	/* 3956 */         bar = this.history.getBar(instrument, period, side, shift);
	/* 3957 */         if (bar == null) {
	/* 3958 */           return null;
	/*      */         }
	/*      */       }
	/* 3961 */       List bars = new ArrayList(1);
	/* 3962 */       bars.add(bar);
	/* 3963 */       return barsToReal(bars, inputType, appliedPrice); }
	/* 3964 */     if (lookback == 0) {
	/* 3965 */       int flats = (shift < 0) ? -shift : 0;
	/* 3966 */       shift = (shift < 0) ? 0 : shift;
	/* 3967 */       IBar currentBar = null;
	/* 3968 */       long currentBarStartTime = this.history.getStartTimeOfCurrentBar(instrument, period);
	/* 3969 */       long requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, DataCacheUtils.getPreviousCandleStart(period, currentBarStartTime), shift);
	/* 3970 */       int count = DataCacheUtils.getCandlesCountBetweenFast(period, requestedBarStartTime, currentBarStartTime) - 1;
	/*      */       long lookforwardStartTime;
	/* 3972 */       if (count > lookforward) {
	/* 3973 */         lookforwardStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, count + 1);
	/* 3974 */       } else if (count == lookforward) {
	/* 3975 */         lookforwardStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, count);
	/* 3976 */         currentBar = this.history.getCurrentBar(instrument, period, side);
	/*      */       }
	/*      */       else {
	/* 3979 */         return null;
	/*      */       }
	/* 3981 */       List bars = this.history.getBars(instrument, period, side, requestedBarStartTime, lookforwardStartTime);
	/* 3982 */       if (currentBar != null) {
	/* 3983 */         bars.add(currentBar);
	/*      */       }
	/* 3985 */       if ((flats > 0) && (!(bars.isEmpty())))
	/*      */       {
	/* 3988 */         IBar lastCandle = (IBar)bars.get(bars.size() - 1);
	/* 3989 */         double price = lastCandle.getClose();
	/* 3990 */         int i = 0; for (long time = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < flats; time = DataCacheUtils.getNextCandleStartFast(period, time)) {
	/* 3991 */           bars.add(new CandleData(time, price, price, price, price, 0.0D));
	/*      */ 
	/* 3990 */           ++i;
	/*      */         }
	/*      */       }
	/*      */ 
	/* 3994 */       return barsToReal(bars, inputType, appliedPrice);
	/*      */     }
	/* 3996 */     int flats = (shift < 0) ? -shift : 0;
	/* 3997 */     shift = (shift < 0) ? 0 : shift;
	/* 3998 */     IBar currentBar = null;
	/* 3999 */     if (shift == 0) {
	/* 4000 */       if (lookforward > 0)
	/*      */       {
	/* 4002 */         return null;
	/*      */       }
	/* 4004 */       shift = 1;
	/* 4005 */       --lookback;
	/* 4006 */       currentBar = this.history.getCurrentBar(instrument, period, side);
	/* 4007 */       if (currentBar == null) {
	/* 4008 */         return null;
	/*      */       }
	/*      */     }
	/* 4011 */     long currentBarStartTime = this.history.getStartTimeOfCurrentBar(instrument, period);
	/* 4012 */     long requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, DataCacheUtils.getPreviousCandleStart(period, currentBarStartTime), shift);
	/* 4013 */     long lookbackStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, requestedBarStartTime, lookback + 1);
	/* 4014 */     if (lookforward > 0) {
	/* 4015 */       int count = DataCacheUtils.getCandlesCountBetweenFast(period, requestedBarStartTime, currentBarStartTime) - 1;
	/* 4016 */       if (count > lookforward) {
	/* 4017 */         requestedBarStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, lookforward + 1);
	/* 4018 */       } else if (count == lookforward) {
	/* 4019 */         requestedBarStartTime = DataCacheUtils.getTimeForNCandlesForwardFast(period, requestedBarStartTime, count);
	/* 4020 */         currentBar = this.history.getCurrentBar(instrument, period, side);
	/*      */       }
	/*      */       else {
	/* 4023 */         return null;
	/*      */       }
	/*      */     }
	/* 4026 */     List bars = this.history.getBars(instrument, period, side, lookbackStartTime, requestedBarStartTime);
	/* 4027 */     if (currentBar != null) {
	/* 4028 */       bars.add(currentBar);
	/*      */     }
	/* 4030 */     if ((flats > 0) && (!(bars.isEmpty())))
	/*      */     {
	/* 4033 */       IBar lastCandle = (IBar)bars.get(bars.size() - 1);
	/* 4034 */       double price = lastCandle.getClose();
	/* 4035 */       int i = 0; for (long time = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < flats; time = DataCacheUtils.getNextCandleStartFast(period, time)) {
	/* 4036 */         bars.add(new CandleData(time, price, price, price, price, 0.0D));
	/*      */ 
	/* 4035 */         ++i;
	/*      */       }
	/*      */     }
	/*      */ 
	/* 4039 */     return barsToReal(bars, inputType, appliedPrice);
	/*      */   }
	/*      */ 
	/*      */   Object getInputData(Instrument instrument, Period period, OfferSide side, InputParameterInfo.Type inputType, IIndicators.AppliedPrice appliedPrice, long from, long to, int lookback, int lookforward, boolean addCurrentCandle, int addFlatsAtTheEnd)
	/*      */     throws JFException, DataCacheException
	/*      */   {
	/* 4047 */     if (period == Period.TICK) {
	/* 4048 */       from = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, from);
	/* 4049 */       from -= lookback * 1000;
	/* 4050 */       long candleTo = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, to);
	/* 4051 */       long timeOfLastTick = this.history.getTimeOfLastTick(instrument);
	/* 4052 */       if (lookforward > 0) {
	/* 4053 */         long lastCandle = DataCacheUtils.getCandleStartFast(Period.ONE_SEC, timeOfLastTick);
	/* 4054 */         int count = DataCacheUtils.getCandlesCountBetweenFast(Period.ONE_SEC, candleTo, lastCandle) - 1;
	/* 4055 */         count = (count > lookforward) ? lookforward : count;
	/* 4056 */         to = DataCacheUtils.getTimeForNCandlesForwardFast(Period.ONE_SEC, candleTo, count + 1);
	/* 4057 */         candleTo = to;
	/* 4058 */         if (candleTo + 999L <= timeOfLastTick)
	/* 4059 */           to = candleTo + 999L;
	/* 4060 */         else if (candleTo <= timeOfLastTick)
	/* 4061 */           to = timeOfLastTick;
	/*      */       }
	/* 4063 */       else if (candleTo + 999L <= timeOfLastTick) {
	/* 4064 */         to = candleTo + 999L;
	/*      */       }
	/* 4066 */       List ticks = this.history.getTicks(instrument, from, to);
	/* 4067 */       List bars = new ArrayList((int)((to - from) / 1000L + 1L));
	/* 4068 */       if ((ticks.isEmpty()) || (DataCacheUtils.getCandleStartFast(Period.ONE_SEC, ((ITick)ticks.get(0)).getTime()) != from)) {
	/* 4069 */         ITick tickBefore = this.history.getLastTickBefore(instrument, from - 1L);
	/* 4070 */         double price = (side == OfferSide.ASK) ? tickBefore.getAsk() : tickBefore.getBid();
	/* 4071 */         long timeOfLastFlatCandle = (ticks.isEmpty()) ? DataCacheUtils.getCandleStartFast(Period.ONE_SEC, to) : DataCacheUtils.getPreviousCandleStartFast(Period.ONE_SEC, DataCacheUtils.getCandleStartFast(Period.ONE_SEC, ((ITick)ticks.get(0)).getTime()));
	/*      */ 
	/* 4073 */         for (long time = from; time <= timeOfLastFlatCandle; time += 1000L) {
	/* 4074 */           bars.add(new CandleData(time, price, price, price, price, 0.0D));
	/*      */         }
	/*      */       }
	/* 4077 */       createCandlesFromTicks(ticks, bars, side);
	/* 4078 */       return barsToReal(bars, inputType, appliedPrice);
	/*      */     }
	/* 4080 */     from = DataCacheUtils.getCandleStartFast(period, from);
	/* 4081 */     long lookbackStartTime = DataCacheUtils.getTimeForNCandlesBackFast(period, from, lookback + 1);
	/* 4082 */     if (lookforward > 0) {
	/* 4083 */       to = DataCacheUtils.getCandleStartFast(period, to);
	/* 4084 */       long lastCandle = DataCacheUtils.getPreviousCandleStartFast(period, this.history.getStartTimeOfCurrentBar(instrument, period));
	/* 4085 */       int count = DataCacheUtils.getCandlesCountBetweenFast(period, to, lastCandle) - 1;
	/* 4086 */       count = (count > lookforward) ? lookforward : count;
	/* 4087 */       to = DataCacheUtils.getTimeForNCandlesForwardFast(period, to, count + 1);
	/*      */     }
	/* 4089 */     List bars = this.history.getBars(instrument, period, side, lookbackStartTime, to);
	/* 4090 */     if (addCurrentCandle) {
	/* 4091 */       IBar currentBar = this.history.getCurrentBar(instrument, period, side);
	/* 4092 */       bars.add(currentBar);
	/*      */     }
	/* 4094 */     if ((addFlatsAtTheEnd > 0) && (!(bars.isEmpty())))
	/*      */     {
	/* 4097 */       IBar lastCandle = (IBar)bars.get(bars.size() - 1);
	/* 4098 */       double price = lastCandle.getClose();
	/* 4099 */       int i = 0; for (long lastTime = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < addFlatsAtTheEnd; lastTime = DataCacheUtils.getNextCandleStartFast(period, lastTime)) {
	/* 4100 */         bars.add(new CandleData(lastTime, price, price, price, price, 0.0D));
	/*      */ 
	/* 4099 */         ++i;
	/*      */       }
	/*      */     }
	/*      */ 
	/* 4103 */     return barsToReal(bars, inputType, appliedPrice);
	/*      */   }
	/*      */ 
	/*      */   Object getInputData(Instrument instrument, Period period, OfferSide side, InputParameterInfo.Type inputType, IIndicators.AppliedPrice appliedPrice, Filter filter, int before, long time, int after, int lookback, int lookforward, int addFlatsAtTheEnd)
	/*      */     throws JFException, DataCacheException
	/*      */   {
	/* 4111 */     List bars = getInputBars(instrument, period, side, inputType, appliedPrice, filter, before, time, after, lookback, lookforward, addFlatsAtTheEnd);
	/* 4112 */     return barsToReal(bars, inputType, appliedPrice);
	/*      */   }
	/*      */ 
	/*      */   private List<IBar> getInputBars(Instrument instrument, Period period, OfferSide side, InputParameterInfo.Type inputType, IIndicators.AppliedPrice appliedPrice, Filter filter, int before, long time, int after, int lookback, int lookforward, int addFlatsAtTheEnd)
	/*      */     throws JFException, DataCacheException
	/*      */   {
	/* 4130 */     time = DataCacheUtils.getCandleStartFast(period, time);
	/* 4131 */     before += lookback;
	/* 4132 */     after += lookforward;
	/* 4133 */     List bars = this.history.getBars(instrument, period, side, filter, before, time, after);
	/* 4134 */     if ((addFlatsAtTheEnd > 0) && (!(bars.isEmpty())))
	/*      */     {
	/* 4137 */       IBar lastCandle = (IBar)bars.get(bars.size() - 1);
	/* 4138 */       double price = lastCandle.getClose();
	/* 4139 */       int i = 0; for (long lastTime = DataCacheUtils.getNextCandleStartFast(period, lastCandle.getTime()); i < addFlatsAtTheEnd; lastTime = DataCacheUtils.getNextCandleStartFast(period, lastTime)) {
	/* 4140 */         bars.add(new CandleData(lastTime, price, price, price, price, 0.0D));
	/*      */ 
	/* 4139 */         ++i;
	/*      */       }
	/*      */     }
	/*      */ 
	/* 4143 */     return bars;
	/*      */   }
	/*      */ 
	/*      */   protected void checkSide(Period period, OfferSide[] side) throws JFException {
	/* 4147 */     if (period == Period.TICK) {
	/* 4148 */       return;
	/*      */     }
	/* 4150 */     checkSide(side);
	/*      */   }
	/*      */ 
	/*      */   protected void checkSide(OfferSide[] side) throws JFException {
	/* 4154 */     if (side == null) {
	/* 4155 */       throw new JFException("Side parameter cannot be null");
	/*      */     }
	/* 4157 */     for (OfferSide offerSide : side)
	/* 4158 */       if (offerSide == null)
	/* 4159 */         throw new JFException("Side parameter cannot be null");
	/*      */   }
	/*      */ 
	/*      */   private void checkSide(Period period, OfferSide side)
	/*      */     throws JFException
	/*      */   {
	/* 4166 */     if (period == Period.TICK) {
	/* 4167 */       return;
	/*      */     }
	/* 4169 */     if (side == null)
	/* 4170 */       throw new JFException("Side parameter cannot be null");
	/*      */   }
	/*      */ 
	/*      */   protected void checkIntervalValid(Period period, long from, long to) throws JFException
	/*      */   {
	/* 4175 */     if (!(this.history.isIntervalValid(period, from, to))) {
	/* 4176 */       SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
	/* 4177 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	/* 4178 */       throw new JFException("Interval from [" + dateFormat.format(new Date(from)) + "] to [" + dateFormat.format(new Date(to)) + "] GMT is not valid");
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   private void checkIntervalValid(int before, int after) throws JFException
	/*      */   {
	/* 4184 */     if ((before <= 0) && (after <= 0))
	/* 4185 */       throw new IllegalArgumentException("Negative or zero number of candles requested");
	/*      */   }
	/*      */ 
	/*      */   protected void checkNotTick(Period period) throws JFException
	/*      */   {
	/* 4190 */     if (period == Period.TICK)
	/* 4191 */       throw new JFException("Functions with shift parameter doesn't support ticks");
	/*      */   }
	/*      */ 
	/*      */   protected void checkShiftPositive(int shift) throws JFException
	/*      */   {
	/* 4196 */     if (shift < 0)
	/* 4197 */       throw new JFException("Shift parameter must not be negative");
	/*      */   }
	/*      */ 
	/*      */   protected <B extends IBar> Object barsToReal(List<B> bars, InputParameterInfo.Type inputType, IIndicators.AppliedPrice appliedPrice) throws JFException
	/*      */   {
	/* 4202 */     int i = 0;
	/* 4203 */     switch (inputType)
	/*      */     {
	/*      */     case PRICE:
	/* 4205 */       double[] retDouble = new double[bars.size()];
	/* 4206 */       for (IBar bar : bars) {
	/* 4207 */         retDouble[i] = barToReal(bar, appliedPrice);
	/* 4208 */         ++i;
	/*      */       }
	/* 4210 */       return retDouble;
	/*      */     case DOUBLE:
	/* 4212 */       double[][] retPrice = new double[5][bars.size()];
	/* 4213 */       for (IBar bar : bars) {
	/* 4214 */         retPrice[0][i] = barToReal(bar, IIndicators.AppliedPrice.OPEN);
	/* 4215 */         retPrice[1][i] = barToReal(bar, IIndicators.AppliedPrice.CLOSE);
	/* 4216 */         retPrice[2][i] = barToReal(bar, IIndicators.AppliedPrice.HIGH);
	/* 4217 */         retPrice[3][i] = barToReal(bar, IIndicators.AppliedPrice.LOW);
	/* 4218 */         retPrice[4][i] = barToReal(bar, IIndicators.AppliedPrice.VOLUME);
	/* 4219 */         ++i;
	/*      */       }
	/* 4221 */       return retPrice;
	/*      */     case BAR:
	/* 4223 */       return bars.toArray(new IBar[bars.size()]);
	/*      */     }
	/* 4225 */     throw new RuntimeException("Unexpected input type [" + inputType + "]");
	/*      */   }
	/*      */ 
	/*      */   private double barToReal(IBar bar, IIndicators.AppliedPrice appliedPrice) throws JFException
	/*      */   {
	/* 4230 */     switch (appliedPrice)
	/*      */     {
	/*      */     case CLOSE:
	/* 4232 */       return bar.getClose();
	/*      */     case HIGH:
	/* 4234 */       return bar.getHigh();
	/*      */     case LOW:
	/* 4236 */       return bar.getLow();
	/*      */     case MEDIAN_PRICE:
	/* 4238 */       return ((bar.getHigh() + bar.getLow()) / 2.0D);
	/*      */     case OPEN:
	/* 4240 */       return bar.getOpen();
	/*      */     case TYPICAL_PRICE:
	/* 4242 */       return ((bar.getHigh() + bar.getLow() + bar.getClose()) / 3.0D);
	/*      */     case WEIGHTED_CLOSE:
	/* 4244 */       return ((bar.getHigh() + bar.getLow() + bar.getClose() + bar.getClose()) / 4.0D);
	/*      */     case TIMESTAMP:
	/* 4246 */       return bar.getTime();
	/*      */     case VOLUME:
	/* 4248 */       return bar.getVolume();
	/*      */     }
	/* 4250 */     throw new JFException("Parameter should not be ASK, BID, ASK_VOLUME or BID_VOLUME for bars");
	/*      */   }
	/*      */ 
	/*      */   private void createCandlesFromTicks(List<ITick> timeData, List<IBar> tickCandles, OfferSide side)
	/*      */   {
	/* 4255 */     if (timeData.isEmpty()) {
	/* 4256 */       return;
	/*      */     }
	/* 4258 */     IntraPeriodCandleData[][] generatedCandles = new IntraPeriodCandleData[Period.values().length][];
	/* 4259 */     generatedCandles[Period.ONE_SEC.ordinal()] = new IntraPeriodCandleData[0];
	/* 4260 */     List<Object[]> formedCandles = new ArrayList<Object[]>();
	/* 4261 */     for (ITick tickData : timeData) {
	/* 4262 */       IntraperiodCandlesGenerator.addTickToCandles(tickData.getTime(), tickData.getAsk(), tickData.getBid(), tickData.getAskVolume(), tickData.getBidVolume(), null, generatedCandles, formedCandles, null);
	/*      */     }
	/*      */ 
	/* 4275 */     long nextCandleTime = DataCacheUtils.getNextCandleStartFast(Period.ONE_SEC, DataCacheUtils.getCandleStartFast(Period.ONE_SEC, ((ITick)timeData.get(timeData.size() - 1)).getTime()));
	/* 4276 */     IntraperiodCandlesGenerator.addTickToCandles(nextCandleTime, 0.0D, 0.0D, 0.0D, 0.0D, null, generatedCandles, formedCandles, null);
	/*      */ 
	/* 4288 */     for (Object[] formedCandlesData : formedCandles)
	/*      */     {
	/*      */       IntraPeriodCandleData candle;
	/* 4290 */       if (side == OfferSide.ASK)
	/* 4291 */         candle = (IntraPeriodCandleData)formedCandlesData[1];
	/*      */       else {
	/* 4293 */         candle = (IntraPeriodCandleData)formedCandlesData[2];
	/*      */       }
	/* 4295 */       IBar bar = new CandleData(candle.time, candle.open, candle.close, candle.low, candle.high, candle.vol);
	/* 4296 */       tickCandles.add(bar);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   public void registerCustomIndicator(final File compiledCustomIndcatorFile) throws JFException
	/*      */   {
	/*      */     try {
	/* 4303 */       AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
		@Override
		public File run() throws Exception {
			/* 4305 */           registerCustomIndicatorSecured(compiledCustomIndcatorFile);
			/* 4306 */           return null;
			/*      */         }} );
	/*      */     } catch (PrivilegedActionException e) {
	/* 4309 */       Exception ex = e.getException();
	/* 4310 */       if (ex instanceof JFException)
	/* 4311 */         throw ((JFException)ex);
	/* 4312 */       if (ex instanceof RuntimeException) {
	/* 4313 */         throw ((RuntimeException)ex);
	/*      */       }
	/* 4315 */       LOGGER.error(ex.getMessage(), ex);
	/* 4316 */       throw new JFException(ex);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   public void registerCustomIndicator(final Class<? extends IIndicator> indicatorClass) throws JFException
	/*      */   {
	/*      */     try
	/*      */     {
	/* 4324 */       AccessController.doPrivileged(new PrivilegedExceptionAction<Class<? extends IIndicator>>() {
	/*      */         public Class<? extends IIndicator> run() throws Exception {
	/* 4326 */           registerCustomIndicatorSecured(indicatorClass);
	/* 4327 */           return null;
	/*      */         }
	/*      */       });
	/*      */     }
	/*      */     catch (PrivilegedActionException e) {
	/* 4332 */       Exception ex = e.getException();
	/* 4333 */       if (ex instanceof JFException)
	/* 4334 */         throw ((JFException)ex);
	/* 4335 */       if (ex instanceof RuntimeException) {
	/* 4336 */         throw ((RuntimeException)ex);
	/*      */       }
	/* 4338 */       LOGGER.error(ex.getMessage(), ex);
	/* 4339 */       throw new JFException(ex);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   private void registerCustomIndicatorSecured(File compiledCustomIndicatorFile) throws JFException
	/*      */   {
	/* 4345 */     if (!(compiledCustomIndicatorFile.exists())) {
	/* 4346 */       throw new JFException("Indicator file doesn't exists");
	/*      */     }
	/* 4348 */     IndicatorsProvider indicatorsProvider = IndicatorsProvider.getInstance();
	/* 4349 */     CustIndicatorWrapper indicatorWrapper = new CustIndicatorWrapper();
	/* 4350 */     indicatorWrapper.setBinaryFile(compiledCustomIndicatorFile);
	/* 4351 */     if (indicatorsProvider.enableIndicator(indicatorWrapper, NotificationUtilsProvider.getNotificationUtils()) == null)
	/* 4352 */       throw new JFException("Cannot register indicator");
	/*      */   }
	/*      */ 
	/*      */   private void registerCustomIndicatorSecured(Class<? extends IIndicator> indicatorClass) throws JFException
	/*      */   {
	/* 4357 */     if (indicatorClass == null) {
	/* 4358 */       throw new JFException("Indicator class is null");
	/*      */     }
	/*      */ 
	/* 4361 */     IIndicator indicator = null;
	/*      */     try {
	/* 4363 */       indicator = (IIndicator)indicatorClass.newInstance();
	/*      */     } catch (Exception ex) {
	/* 4365 */       throw new JFException("Cannot create indicator [" + indicatorClass + "] instance.");
	/*      */     }
	/*      */     try {
	/* 4368 */       indicator.onStart(IndicatorHelper.createIndicatorContext());
	/*      */     }
	/*      */     catch (Throwable th) {
	/* 4371 */       throw new JFException("Exception in onStart method");
	/*      */     }
	/* 4373 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 4374 */     if (indicatorInfo == null) {
	/* 4375 */       throw new JFException("Indicator info is null");
	/*      */     }
	/*      */ 
	/* 4378 */     String indicatorName = indicatorInfo.getName();
	/* 4379 */     if ((indicatorName != null) && (!(indicatorName.trim().isEmpty()))) {
	/* 4380 */       this.customIndicators.put(indicatorName, indicatorClass);
	/* 4381 */       cacheIndicator(indicatorName, new IndicatorHolder(indicator, IndicatorHelper.createIndicatorContext()));
	/*      */     }
	/*      */     else
	/*      */     {
	/* 4386 */       throw new JFException("Indicator name is empty");
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   private Period noFilterPeriod(Period period)
	/*      */   {
	/* 4395 */     if (period == null) {
	/* 4396 */       return null;
	/*      */     }
	/* 4398 */     if ((period == Period.DAILY_SKIP_SUNDAY) || (period == Period.DAILY_SUNDAY_IN_MONDAY))
	/*      */     {
	/* 4400 */       return Period.DAILY;
	/*      */     }
	/* 4402 */     return period;
	/*      */   }
	/*      */ 
	/*      */   private Period dailyFilterPeriod(Period period)
	/*      */   {
	/* 4408 */     if ((period == Period.DAILY) || (period == Period.DAILY_SKIP_SUNDAY) || (period == Period.DAILY_SUNDAY_IN_MONDAY))
	/*      */     {
	/* 4410 */       return period;
	/*      */     }
	/* 4412 */     return null;
	/*      */   }
	/*      */ 
	/*      */   private Object getIndicatorInputData(InputParameterInfo inputParameterInfo, IIndicators.AppliedPrice appliedPrice, CandleData[] timeData)
	/*      */   {
	/* 4417 */     int dataSize = timeData.length;
	/*      */     int k;
	/* 4418 */     switch (inputParameterInfo.getType())
	/*      */     {
	/*      */     case PRICE:
	/* 4420 */       double[] open = new double[dataSize];
	/* 4421 */       double[] high = new double[dataSize];
	/* 4422 */       double[] low = new double[dataSize];
	/* 4423 */       double[] close = new double[dataSize];
	/* 4424 */       double[] volume = new double[dataSize];
	/* 4425 */       k = 0;
	/* 4426 */       for (CandleData candle : timeData) {
	/* 4427 */         open[k] = candle.open;
	/* 4428 */         high[k] = candle.high;
	/* 4429 */         low[k] = candle.low;
	/* 4430 */         close[k] = candle.close;
	/* 4431 */         volume[k] = candle.vol;
	/* 4432 */         ++k;
	/*      */       }
	/* 4434 */       return new double[][] { open, close, high, low, volume };
	/*      */     case DOUBLE:
	/* 4436 */       double[] data = new double[dataSize];
	/* 4437 */       switch (appliedPrice.ordinal())
	/*      */       {
	/*      */       case 1:
	/* 4439 */         k = 0;
	/* 4440 */         for (CandleData candle : timeData) {
	/* 4441 */           data[k] = candle.close;
	/* 4442 */           ++k;
	/*      */         }
	/* 4444 */         break;
	/*      */       case 2:
	/* 4446 */         k = 0;
	/* 4447 */         for (CandleData candle : timeData) {
	/* 4448 */           data[k] = candle.high;
	/* 4449 */           ++k;
	/*      */         }
	/* 4451 */         break;
	/*      */       case 3:
	/* 4453 */         k = 0;
	/* 4454 */         for (CandleData candle : timeData) {
	/* 4455 */           data[k] = candle.low;
	/* 4456 */           ++k;
	/*      */         }
	/* 4458 */         break;
	/*      */       case 5:
	/* 4460 */         k = 0;
	/* 4461 */         for (CandleData candle : timeData) {
	/* 4462 */           data[k] = candle.open;
	/* 4463 */           ++k;
	/*      */         }
	/* 4465 */         break;
	/*      */       case 4:
	/* 4467 */         k = 0;
	/* 4468 */         for (CandleData candle : timeData) {
	/* 4469 */           data[k] = ((candle.high + candle.low) / 2.0D);
	/* 4470 */           ++k;
	/*      */         }
	/* 4472 */         break;
	/*      */       case 6:
	/* 4474 */         k = 0;
	/* 4475 */         for (CandleData candle : timeData) {
	/* 4476 */           data[k] = ((candle.high + candle.low + candle.close) / 3.0D);
	/* 4477 */           ++k;
	/*      */         }
	/* 4479 */         break;
	/*      */       case 7:
	/* 4481 */         k = 0;
	/* 4482 */         for (CandleData candle : timeData) {
	/* 4483 */           data[k] = ((candle.high + candle.low + candle.close + candle.close) / 4.0D);
	/* 4484 */           ++k;
	/*      */         }
	/* 4486 */         break;
	/*      */       case 8:
	/* 4488 */         k = 0;
	/* 4489 */         for (CandleData candle : timeData) {
	/* 4490 */           data[k] = candle.time;
	/* 4491 */           ++k;
	/*      */         }
	/* 4493 */         break;
	/*      */       case 9:
	/* 4495 */         k = 0;
	/* 4496 */         for (CandleData candle : timeData) {
	/* 4497 */           data[k] = candle.vol;
	/* 4498 */           ++k;
	/*      */         }
	/*      */       }
	/*      */ 
	/* 4502 */       return data;
	/*      */     case BAR:
	/* 4504 */       return timeData;
	/*      */     }
	/* 4507 */     return null;
	/*      */   }
	/*      */ 
	/*      */   protected final CandleData[] putDataInListFromToIndexes(int from, int to, CandleData[] buffer)
	/*      */   {
	/* 4512 */     CandleData[] data = new CandleData[to - from + 1];
	/* 4513 */     System.arraycopy(buffer, from, data, 0, to - from + 1);
	/* 4514 */     return data;
	/*      */   }
	/*      */ 
	/*      */   private double[][][] calculateIndicatorInputs(Instrument instrument, Period period, OfferSide side, Collection<AbstractDataProvider.IndicatorData> formulasToRecalculate, CandleData[] timeDataAsk, CandleData[] timeDataBid, double[][][] doubleInputs, boolean isTicksDataType)
	/*      */   {
	/* 4521 */     double[][][] priceInput = (double[][][])null;
	/* 4522 */     for (AbstractDataProvider.IndicatorData formulaData : formulasToRecalculate) {
	/* 4523 */       if (formulaData.disabledIndicator) {
	/*      */         continue;
	/*      */       }
	/* 4526 */       IIndicator indicator = formulaData.indicatorWrapper.getIndicator();
	/* 4527 */       OfferSide[] tickOfferSides = formulaData.indicatorWrapper.getOfferSidesForTicks();
	/* 4528 */       IIndicators.AppliedPrice[] appliedPrices = formulaData.indicatorWrapper.getAppliedPricesForCandles();
	/* 4529 */       if ((timeDataAsk == null) && (timeDataBid == null))
	/*      */       {
	/*      */         continue;
	/*      */       }
	/*      */ 
	/* 4537 */       if (indicator instanceof ConnectorIndicator) {
	/* 4538 */         ((ConnectorIndicator)indicator).setCurrentInstrument(instrument);
	/* 4539 */         ((ConnectorIndicator)indicator).setCurrentPeriod(period);
	/*      */       }
	/*      */ 
	/* 4542 */       int i = 0; for (int j = indicator.getIndicatorInfo().getNumberOfInputs(); i < j; ++i) {
	/* 4543 */         InputParameterInfo inputParameterInfo = indicator.getInputParameterInfo(i);
	/* 4544 */         if ((inputParameterInfo.getOfferSide() != null) || (inputParameterInfo.getPeriod() != null) || (inputParameterInfo.getInstrument() != null)) { if (inputParameterInfo.getOfferSide() != null);
	/* 4544 */           if ((inputParameterInfo.getOfferSide() != ((isTicksDataType) ? tickOfferSides[i] : side)) || ((inputParameterInfo.getPeriod() != null) && (((noFilterPeriod(inputParameterInfo.getPeriod()) != period) || (dailyFilterPeriod(inputParameterInfo.getPeriod()) != dailyFilterPeriod(this.dailyFilterPeriod))))) || ((inputParameterInfo.getInstrument() != null) && (inputParameterInfo.getInstrument() != instrument)))
	/*      */           {
	/*      */             continue;
	/*      */           }
	/*      */ 
	/*      */         }
	/*      */ 
	/* 4551 */         switch (inputParameterInfo.getType())
	/*      */         {
	/*      */         case PRICE:
	/* 4553 */           if ((priceInput == null) || ((isTicksDataType) && (priceInput[tickOfferSides[i].ordinal()] == null))) {
	/* 4554 */             if (priceInput == null) {
	/* 4555 */               priceInput = new double[2][][];
	/*      */             }
	/* 4557 */             priceInput[((!(isTicksDataType)) ? 0 : tickOfferSides[i].ordinal())] = ((double[][])(double[][])getIndicatorInputData(inputParameterInfo, appliedPrices[i], ((!(isTicksDataType)) || (tickOfferSides[i] == OfferSide.ASK)) ? timeDataAsk : timeDataBid)); } break;
	/*      */         case DOUBLE:
	/* 4563 */           if (doubleInputs[tickOfferSides[i].ordinal()][appliedPrices[i].ordinal()] == null) {
	/* 4564 */             doubleInputs[tickOfferSides[i].ordinal()][appliedPrices[i].ordinal()] = ((double[])(double[])getIndicatorInputData(inputParameterInfo, appliedPrices[i], ((!(isTicksDataType)) || (tickOfferSides[i] == OfferSide.ASK)) ? timeDataAsk : timeDataBid));
	/*      */           }
	/*      */ 
	/*      */         }
	/*      */ 
	/*      */       }
	/*      */ 
	/*      */     }
	/*      */ 
	/* 4573 */     return priceInput;
	/*      */   }
	/*      */ 
	/*      */   protected void copyToIndicatorOutput(int from, int to, int recalculateStart, Object outputData, int firstValueIndex, int numberOfElements, Object inputData, OutputParameterInfo.Type type, int lastIndex, int bufferLength)
	/*      */   {
	/*      */     int toSkip;
	/* 4582 */     if (recalculateStart <= from)
	/* 4583 */       toSkip = from - recalculateStart;
	/*      */     else {
	/* 4585 */       toSkip = bufferLength - recalculateStart + from;
	/*      */     }
	/*      */ 
	/* 4590 */     int valuesNA = firstValueIndex - toSkip;
	/*      */     int outputBufferCounter;
	/*      */     int inputBufferCounter;
	/* 4593 */     if (valuesNA > 0) {
	/* 4594 */       inputBufferCounter = 0;
	/* 4595 */       if (lastIndex - from + 1 >= valuesNA)
	/* 4596 */         outputBufferCounter = from + valuesNA;
	/*      */       else
	/* 4598 */         throw new RuntimeException("Cannot happen");
	/* 4600 */       switch (type)
	/*      */       {
	/*      */       case INT:
	/* 4602 */         int[] outputDataInt = (int[])(int[])outputData;
	/* 4603 */         Arrays.fill(outputDataInt, from, outputBufferCounter, -2147483648);
	/*      */ 
	/* 4605 */         break;
	/*      */       case DOUBLE:
	/* 4607 */         double[] outputDataDouble = (double[])(double[])outputData;
	/* 4608 */         Arrays.fill(outputDataDouble, from, outputBufferCounter, (0.0D / 0.0D));
	/* 4609 */         break;
	/*      */       case OBJECT:
	/* 4611 */         Object[] outputDataObject = (Object[])(Object[])outputData;
	/* 4612 */         Arrays.fill(outputDataObject, from, outputBufferCounter, null);
	/*      */       }
	/*      */     }
	/*      */     else {
	/* 4616 */       outputBufferCounter = from;
	/* 4617 */       inputBufferCounter = toSkip - firstValueIndex;
	/* 4618 */       numberOfElements -= inputBufferCounter;
	/*      */     }
	/*      */ 
	/* 4622 */     if (lastIndex - outputBufferCounter + 1 >= numberOfElements)
	/* 4623 */       System.arraycopy(inputData, inputBufferCounter, outputData, outputBufferCounter, numberOfElements);
	/*      */     else {
	/* 4625 */       throw new RuntimeException("Cannot happen");
	/*      */     }
	/*      */ 
	/* 4630 */     if (to - (outputBufferCounter + numberOfElements) + 1 > 0)
	/* 4631 */       switch (type)
	/*      */       {
	/*      */       case INT:
	/* 4633 */         int[] outputDataInt = (int[])(int[])outputData;
	/* 4634 */         Arrays.fill(outputDataInt, outputBufferCounter + numberOfElements, to + 1, -2147483648);
	/*      */ 
	/* 4636 */         break;
	/*      */       case DOUBLE:
	/* 4638 */         double[] outputDataDouble = (double[])(double[])outputData;
	/* 4639 */         Arrays.fill(outputDataDouble, outputBufferCounter + numberOfElements, to + 1, (0.0D / 0.0D));
	/*      */ 
	/* 4641 */         break;
	/*      */       case OBJECT:
	/* 4643 */         Object[] outputDataObject = (Object[])(Object[])outputData;
	/* 4644 */         Arrays.fill(outputDataObject, outputBufferCounter + numberOfElements, to + 1, null);
	/*      */       }
	/*      */   }
	/*      */ 
	/*      */   private boolean populateIndicatorInputFromDataProvider(Period period, AbstractDataProvider.IndicatorData indicatorData, int inputIndex, long from, long to, int finalLookback, int finalLookforward, int maxNumberOfCandles, int bufferSizeMultiplier)
	/*      */   {
	/* 4655 */     IIndicator indicator = indicatorData.indicatorWrapper.getIndicator();
	/* 4656 */     assert (indicatorData.inputDataProviders[inputIndex] != null) : "Input data provider is null";
	/* 4657 */     Period inputPeriod = (indicatorData.inputPeriods[inputIndex] == null) ? period : indicatorData.inputPeriods[inputIndex];
	/* 4658 */     Period inputCandlePeriod = (inputPeriod == Period.TICK) ? Period.ONE_SEC : inputPeriod;
	/*      */ 
	/* 4660 */     long latestTo = to = DataCacheUtils.getCandleStartFast(inputCandlePeriod, to);
	/*      */ 
	/* 4662 */     from = DataCacheUtils.getCandleStartFast(inputCandlePeriod, from);
	/*      */ 
	/* 4664 */     long latestDataTime = indicatorData.inputDataProviders[inputIndex].getLastLoadedDataTime();
	/*      */ 
	/* 4666 */     if (latestDataTime == -9223372036854775808L) {
	/* 4667 */       LOGGER.debug("WARN: Indicator data provider doesn't have any data");
	/* 4668 */       return false;
	/*      */     }
	/* 4670 */     if (to > latestDataTime) {
	/* 4671 */       to = DataCacheUtils.getCandleStartFast(inputCandlePeriod, latestDataTime);
	/*      */     }
	/* 4673 */     if (from > to) {
	/* 4674 */       return false;
	/*      */     }
	/* 4676 */     int candlesBefore = DataCacheUtils.getCandlesCountBetweenFast(inputCandlePeriod, from, to) + finalLookback;
	/* 4677 */     if (candlesBefore == 0) {
	/* 4678 */       LOGGER.debug("WARN: Nothing to request from indicator data provider");
	/* 4679 */       return false;
	/*      */     }
	/* 4681 */     if (candlesBefore + finalLookforward > maxNumberOfCandles * bufferSizeMultiplier) {
	/* 4682 */       candlesBefore = maxNumberOfCandles * bufferSizeMultiplier - finalLookforward;
	/*      */     }
	/*      */ 
	/* 4685 */     IDataSequence dataSequence = indicatorData.inputDataProviders[inputIndex].getDataSequence(candlesBefore, to, finalLookforward);
	/*      */     CandleData[] data;
	/* 4687 */     if (inputPeriod == Period.TICK) {
	/* 4688 */       TickDataSequence tickSequence = (TickDataSequence)dataSequence;
	/* 4689 */       OfferSide offerSide = indicator.getInputParameterInfo(inputIndex).getOfferSide();
	/* 4690 */       OfferSide[] tickOfferSides = indicatorData.indicatorWrapper.getOfferSidesForTicks();
	/* 4691 */       if (((offerSide != null) && (offerSide == OfferSide.ASK)) || ((offerSide == null) && (tickOfferSides[inputIndex] == OfferSide.ASK)))
	/* 4692 */         data = tickSequence.getOneSecCandlesAsk();
	/*      */       else {
	/* 4694 */         data = tickSequence.getOneSecCandlesBid();
	/*      */       }
	/* 4696 */       if ((tickSequence.getOneSecExtraBefore() > 0) || (tickSequence.getOneSecExtraAfter() > 0)) {
	/* 4697 */         CandleData[] newData = new CandleData[data.length - tickSequence.getOneSecExtraBefore() - tickSequence.getOneSecExtraAfter()];
	/* 4698 */         System.arraycopy(data, tickSequence.getOneSecExtraBefore(), newData, 0, newData.length);
	/* 4699 */         data = newData;
	/*      */       }
	/*      */     } else {
	/* 4702 */       CandleDataSequence candleSequence = (CandleDataSequence)dataSequence;
	/* 4703 */       data = (CandleData[])candleSequence.getData();
	/* 4704 */       if ((dataSequence.getExtraBefore() > 0) || (dataSequence.getExtraAfter() > 0)) {
	/* 4705 */         CandleData[] newData = new CandleData[data.length - dataSequence.getExtraBefore() - dataSequence.getExtraAfter()];
	/* 4706 */         System.arraycopy(data, dataSequence.getExtraBefore(), newData, 0, newData.length);
	/* 4707 */         data = newData;
	/*      */       }
	/*      */     }
	/* 4710 */     if (data.length == 0) {
	/* 4711 */       indicator.setInputParameter(inputIndex, getIndicatorInputData(indicatorData.indicatorWrapper.getIndicator().getInputParameterInfo(inputIndex), indicatorData.indicatorWrapper.getAppliedPricesForCandles()[inputIndex], new CandleData[0]));
	/*      */ 
	/* 4713 */       return true;
	/*      */     }
	/* 4715 */     int flats = 0;
	/* 4716 */     if (latestTo > to)
	/* 4717 */       flats = DataCacheUtils.getCandlesCountBetweenFast(inputCandlePeriod, to, latestTo) - 1;
	/*      */     CandleData[] correctData;
	/* 4720 */     if (flats > 0) {
	/* 4721 */       correctData = new CandleData[data.length + flats];
	/* 4722 */       System.arraycopy(data, 0, correctData, 0, data.length);
	/*      */ 
	/* 4725 */       CandleData lastCandle = correctData[(data.length - 1)];
	/* 4726 */       int i = 1; long time = DataCacheUtils.getNextCandleStartFast(inputCandlePeriod, lastCandle.time);
	/* 4727 */       for (; i <= flats; time = DataCacheUtils.getNextCandleStartFast(inputCandlePeriod, time)) {
	/* 4728 */         correctData[(data.length - 1 + i)] = new CandleData(time, lastCandle.close, lastCandle.close, lastCandle.close, lastCandle.close, 0.0D);
	/*      */ 
	/* 4727 */         ++i;
	/*      */       }
	/*      */     }
	/*      */     else {
	/* 4731 */       correctData = data;
	/*      */     }
	/*      */ 
	/* 4734 */     indicator.setInputParameter(inputIndex, getIndicatorInputData(indicatorData.indicatorWrapper.getIndicator().getInputParameterInfo(inputIndex), indicatorData.indicatorWrapper.getAppliedPricesForCandles()[inputIndex], correctData));
	/*      */ 
	/* 4736 */     return true;
	/*      */   }
	/*      */ 
	/*      */   public void calculateIndicators(Instrument instrument, Period period, OfferSide side, int from, int to, Collection<AbstractDataProvider.IndicatorData> formulasToRecalculate, int lastIndex, CandleData[] bufferAsk, CandleData[] bufferBid, boolean isTicksDataType, int maxNumberOfCandles, int bufferSizeMultiplier, Data firstData)
	/*      */   {
	/* 4745 */     if ((firstData == null) || (lastIndex == -1))
	/*      */     {
	/* 4747 */       return;
	/*      */     }
	/*      */ 
	/* 4751 */     int finalLookback = 0;
	/* 4752 */     int finalLookforward = 0;
	/* 4753 */     boolean needAsk = false;
	/* 4754 */     boolean needBid = false;
	/*      */ 
	/* 4757 */     for (AbstractDataProvider.IndicatorData formulaData : formulasToRecalculate) {
	/* 4758 */       if (formulaData.disabledIndicator)
	/*      */       {
	/*      */         continue;
	/*      */       }
	/*      */ 
	/* 4763 */       IIndicator indicator = formulaData.indicatorWrapper.getIndicator();
	/* 4764 */       int lookback = formulaData.lookback;
	/* 4765 */       int lookforward = formulaData.lookforward;
	/* 4766 */       if (indicator.getIndicatorInfo().isUnstablePeriod())
	/*      */       {
	/* 4769 */         lookback += ((lookback * 4 < 100) ? 100 : lookback * 4);
	/*      */       }
	/* 4771 */       if (finalLookback < lookback) {
	/* 4772 */         finalLookback = lookback;
	/*      */       }
	/* 4774 */       if (finalLookforward < lookforward) {
	/* 4775 */         finalLookforward = lookforward;
	/*      */       }
	/*      */ 
	/* 4778 */       if ((isTicksDataType) && (((!(needBid)) || (!(needAsk))))) {
	/* 4779 */         int i = 0; for (int j = indicator.getIndicatorInfo().getNumberOfInputs(); i < j; ++i) {
	/* 4780 */           side = formulaData.indicatorWrapper.getOfferSidesForTicks()[i];
	/* 4781 */           if (side == OfferSide.ASK)
	/* 4782 */             needAsk = true;
	/*      */           else {
	/* 4784 */             needBid = true;
	/*      */           }
	/*      */         }
	/*      */       }
	/*      */     }
	/*      */ 
	/* 4790 */     if (to != lastIndex)
	/*      */     {
	/* 4793 */       to = (lastIndex - to > finalLookback) ? to + finalLookback : lastIndex;
	/*      */     }
	/* 4795 */     if ((finalLookforward != 0) && (from != 0))
	/*      */     {
	/* 4798 */       from = (from > finalLookforward) ? from - finalLookforward : 0;
	/*      */     }
	/*      */ 
	/* 4801 */     int recalculateStart = (from > finalLookback) ? from - finalLookback : 0;
	/* 4802 */     int recalculateEnd = (lastIndex - to > finalLookforward) ? to + finalLookforward : lastIndex;
	/*      */ 
	/* 4804 */     CandleData[] timeDataAsk = null;
	/* 4805 */     CandleData[] timeDataBid = null;
	/* 4806 */     if (isTicksDataType) {
	/* 4807 */       if (needAsk) {
	/* 4808 */         timeDataAsk = putDataInListFromToIndexes(recalculateStart, recalculateEnd, bufferAsk);
	/*      */       }
	/* 4810 */       if (needBid)
	/* 4811 */         timeDataBid = putDataInListFromToIndexes(recalculateStart, recalculateEnd, bufferBid);
	/*      */     }
	/*      */     else {
	/* 4814 */       timeDataAsk = putDataInListFromToIndexes(recalculateStart, recalculateEnd, bufferAsk);
	/*      */     }
	/*      */     long lastInputTime;
	/* 4818 */     if (timeDataAsk != null) {
	/* 4819 */       lastInputTime = timeDataAsk[(timeDataAsk.length - 1)].time;
	/*      */     }
	/*      */     else {
	/* 4822 */       lastInputTime = -1L;
	/*      */     }
	/* 4824 */     if (timeDataBid != null) {
	/* 4825 */       long time = timeDataBid[(timeDataBid.length - 1)].time;
	/* 4826 */       if (time > lastInputTime) {
	/* 4827 */         lastInputTime = time;
	/*      */       }
	/*      */     }
	/*      */ 
	/* 4831 */     double[][][] doubleInputs = new double[OfferSide.values().length][IIndicators.AppliedPrice.values().length];
	/* 4832 */     double[][][] priceInput = calculateIndicatorInputs(instrument, period, side, formulasToRecalculate, timeDataAsk, timeDataBid, doubleInputs, isTicksDataType);
	/*      */ 
	/* 4835 */     label477: for (AbstractDataProvider.IndicatorData formulaData : formulasToRecalculate) {
	/* 4836 */       if (formulaData.disabledIndicator) {
	/*      */         continue;
	/*      */       }
	/*      */ 
	/* 4840 */       IIndicator indicator = formulaData.indicatorWrapper.getIndicator();
	/* 4841 */       IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 4842 */       OfferSide[] tickOfferSides = formulaData.indicatorWrapper.getOfferSidesForTicks();
	/* 4843 */       IIndicators.AppliedPrice[] appliedPrices = formulaData.indicatorWrapper.getAppliedPricesForCandles();
	/*      */ 
	/* 4845 */       if ((timeDataAsk == null) && (timeDataBid == null)) {
	/*      */         continue;
	/*      */       }
	/*      */ 
	/* 4849 */       IndicatorContext indicatorContext = formulaData.indicatorWrapper.getIndicatorHolder().getIndicatorContext();
	/* 4850 */       indicatorContext.setChartInfo(instrument, period, (indicatorInfo.getNumberOfInputs() > 0) ? side : (isTicksDataType) ? tickOfferSides[0] : OfferSide.BID);
	/*      */       try {
	/*      */         int dataSize;
	/*      */         Object[] outArrays;
	/*      */         IndicatorResult result;
	/*      */         while (true) { dataSize = (timeDataAsk == null) ? timeDataBid.length : timeDataAsk.length;
	/*      */ 
	/* 4857 */           int i = 0; for (int j = indicatorInfo.getNumberOfInputs(); i < j; ++i) {
	/* 4858 */             InputParameterInfo inputParameterInfo = indicator.getInputParameterInfo(i);
	/* 4859 */             if ((inputParameterInfo.getOfferSide() != null) || (inputParameterInfo.getPeriod() != null) || (inputParameterInfo.getInstrument() != null)) { if (inputParameterInfo.getOfferSide() != null);
	/* 4859 */               if ((inputParameterInfo.getOfferSide() != ((isTicksDataType) ? tickOfferSides[i] : side)) || ((inputParameterInfo.getPeriod() != null) && (((noFilterPeriod(inputParameterInfo.getPeriod()) != period) || (dailyFilterPeriod(inputParameterInfo.getPeriod()) != dailyFilterPeriod(this.dailyFilterPeriod))))) || ((inputParameterInfo.getInstrument() != null) && (inputParameterInfo.getInstrument() != instrument)))
	/*      */               {
	/*      */                 break label1137;
	/*      */               }
	/*      */ 
	/*      */             }
	/*      */ 
	/* 4867 */             switch (inputParameterInfo.getType()) {
	/*      */             case PRICE:
	/*      */               try {
	/* 4870 */                 indicator.setInputParameter(i, (!(isTicksDataType)) ? priceInput[0] : priceInput[tickOfferSides[i].ordinal()]);
	/*      */               }
	/*      */               catch (Throwable t) {
	/* 4873 */                 LOGGER.error(t.getMessage(), t);
	/* 4874 */                 String error = StrategyWrapper.representError(indicator, t);
	/* 4875 */                 NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */               }
	/*      */ 
	/* 5152 */               indicatorContext.setChartInfo(null, null, null); break;
	/*      */             case DOUBLE:
	/*      */               try {
	/* 4881 */                 indicator.setInputParameter(i, (!(isTicksDataType)) ? doubleInputs[0][appliedPrices[i].ordinal()] : doubleInputs[tickOfferSides[i].ordinal()][appliedPrices[i].ordinal()]);
	/*      */               }
	/*      */               catch (Throwable t) {
	/* 4884 */                 LOGGER.error(t.getMessage(), t);
	/* 4885 */                 String error = StrategyWrapper.representError(indicator, t);
	/* 4886 */                 NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */               }
	/*      */ 
	/* 5152 */               indicatorContext.setChartInfo(null, null, null); break;
	/*      */             case BAR:
	/*      */               try {
	/* 4892 */                 indicator.setInputParameter(i, ((!(isTicksDataType)) || (tickOfferSides[i] == OfferSide.ASK)) ? timeDataAsk : timeDataBid);
	/*      */               } catch (Throwable t) {
	/* 4894 */                 LOGGER.error(t.getMessage(), t);
	/* 4895 */                 String error = StrategyWrapper.representError(indicator, t);
	/* 4896 */                 NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */               }
	/*      */ 
	/* 5152 */               indicatorContext.setChartInfo(null, null, null); break;
	/*      */             }
	/* 4899 */             continue;
	/*      */ 
	/* 4902 */             if (populateIndicatorInputFromDataProvider(period, formulaData, i, bufferAsk[from].time, bufferAsk[to].time, finalLookback, finalLookforward, maxNumberOfCandles, bufferSizeMultiplier)) {
	/*      */               continue;
	/*      */             }
	/* 4905 */             int k = 0; for (int n = indicatorInfo.getNumberOfOutputs(); k < n; ++k) {
	/* 4906 */               OutputParameterInfo outputParameterInfo = indicator.getOutputParameterInfo(k);
	/* 4907 */               switch (outputParameterInfo.getType())
	/*      */               {
	/*      */               case INT:
	/* 4909 */                 Arrays.fill(formulaData.outputDataInt[k], from, to + 1, -2147483648);
	/* 4910 */                 break;
	/*      */               case DOUBLE:
	/* 4912 */                 Arrays.fill(formulaData.outputDataDouble[k], from, to + 1, (0.0D / 0.0D));
	/* 4913 */                 break;
	/*      */               case OBJECT:
	/* 4915 */                 Arrays.fill(formulaData.outputDataObject[k], from, to + 1, null);
	/*      */               }
	/*      */ 
	/*      */             }
	/*      */ 
	/* 5152 */             indicatorContext.setChartInfo(null, null, null); break label477:
	/*      */           }
	/* 4925 */           outArrays = new Object[indicatorInfo.getNumberOfOutputs()];
	/*      */ 
	/* 4927 */           if (dataSize <= formulaData.lookback + formulaData.lookforward) {
	/* 4928 */             result = new IndicatorResult(0, 0, 0); break label2648:
	/*      */           }
	/*      */ 
	/* 4931 */           i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 4932 */             OutputParameterInfo outputParameterInfo = indicator.getOutputParameterInfo(i);
	/* 4933 */             switch (outputParameterInfo.getType())
	/*      */             {
	/*      */             case INT:
	/* 4935 */               int[] arrayInt = new int[dataSize - (formulaData.lookback + formulaData.lookforward)];
	/*      */               try {
	/* 4937 */                 indicator.setOutputParameter(i, arrayInt);
	/*      */               } catch (Throwable t) {
	/* 4939 */                 LOGGER.error(t.getMessage(), t);
	/* 4940 */                 String error = StrategyWrapper.representError(indicator, t);
	/* 4941 */                 NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */               }
	/*      */ 
	/* 5152 */               indicatorContext.setChartInfo(null, null, null); break label477:
	/*      */ 
	/* 4944 */               outArrays[i] = arrayInt;
	/* 4945 */               break;
	/*      */             case DOUBLE:
	/* 4947 */               double[] arrayDouble = new double[dataSize - (formulaData.lookback + formulaData.lookforward)];
	/*      */               try {
	/* 4949 */                 indicator.setOutputParameter(i, arrayDouble);
	/*      */               } catch (Throwable t) {
	/* 4951 */                 LOGGER.error(t.getMessage(), t);
	/* 4952 */                 String error = StrategyWrapper.representError(indicator, t);
	/* 4953 */                 NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */               }
	/*      */ 
	/* 5152 */               indicatorContext.setChartInfo(null, null, null); break label477:
	/*      */ 
	/* 4956 */               outArrays[i] = arrayDouble;
	/* 4957 */               break;
	/*      */             case OBJECT:
	/* 4959 */               Object[] arrayObject = new Object[dataSize - (formulaData.lookback + formulaData.lookforward)];
	/*      */               try {
	/* 4961 */                 indicator.setOutputParameter(i, arrayObject);
	/*      */               } catch (Throwable t) {
	/* 4963 */                 LOGGER.error(t.getMessage(), t);
	/* 4964 */                 String error = StrategyWrapper.representError(indicator, t);
	/* 4965 */                 NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */               }
	/*      */ 
	/* 5152 */               indicatorContext.setChartInfo(null, null, null); break label477:
	/*      */ 
	/* 4968 */               outArrays[i] = arrayObject;
	/*      */             }
	/*      */           }
	/*      */ 
	/*      */           try
	/*      */           {
	/* 4974 */             result = indicator.calculate(0, dataSize - 1);
	/*      */           } catch (TaLibException e) {
	/* 4976 */             Throwable t = e.getCause();
	/* 4977 */             LOGGER.error(t.getMessage(), t);
	/* 4978 */             String error = StrategyWrapper.representError(indicator, t);
	/* 4979 */             NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */ 
	/* 5152 */             indicatorContext.setChartInfo(null, null, null); break label477:
	/*      */           }
	/*      */           catch (Throwable t)
	/*      */           {
	/* 4982 */             LOGGER.error(t.getMessage(), t);
	/* 4983 */             String error = StrategyWrapper.representError(indicator, t);
	/* 4984 */             NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/*      */           }
	/*      */ 
	/*      */         }
	/*      */ 
	/* 5152 */         indicatorContext.setChartInfo(null, null, null); break label477:
	/*      */ 
	/* 4988 */         if (result.getNumberOfElements() < dataSize - formulaData.lookback - formulaData.lookforward) {
	/* 4989 */           String error = "calculate() method of indicator [" + indicatorInfo.getName() + "] returned less values than expected. Requested from-to [0]-[" + (dataSize - 1) + "], input array size [" + dataSize + "], returned first calculated index [" + result.getFirstValueIndex() + "], number of calculated values [" + result.getNumberOfElements() + "], lookback [" + formulaData.lookback + "], lookforward [" + formulaData.lookforward + "], expected number of elements is [" + (dataSize - formulaData.lookback - formulaData.lookforward) + "]";
	/*      */ 
	/* 5006 */           LOGGER.error(error);
	/* 5007 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/*      */ 
	/* 5152 */           indicatorContext.setChartInfo(null, null, null);
	/*      */         }
	/* 5010 */         if (result.getFirstValueIndex() + result.getNumberOfElements() > dataSize) {
	/* 5011 */           String error = "calculate() method of indicator [" + indicatorInfo.getName() + "] returned incorrect values. Requested from-to [0]-[" + (dataSize - 1) + "], input array size [" + dataSize + "], returned first calculated index [" + result.getFirstValueIndex() + "], number of calculated values [" + result.getNumberOfElements() + "], lookback [" + formulaData.lookback + "], lookforward [" + formulaData.lookforward + "], first index + number of elements cannot be > input array size";
	/*      */ 
	/* 5026 */           LOGGER.error(error);
	/* 5027 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/*      */ 
	/* 5152 */           indicatorContext.setChartInfo(null, null, null);
	/*      */         }
	/* 5030 */         if ((result.getLastValueIndex() == -2147483648) && (result.getNumberOfElements() != 0)) {
	/* 5031 */           if (formulaData.lookforward != 0) {
	/* 5032 */             String error = "calculate() method of indicator [" + indicatorInfo.getName() + "] returned result without lastValueIndex set. This is only allowed when lookforward is equals to zero";
	/*      */ 
	/* 5035 */             LOGGER.error(error);
	/* 5036 */             NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/*      */ 
	/* 5152 */             indicatorContext.setChartInfo(null, null, null);
	/*      */           }
	/* 5039 */           result.setLastValueIndex(dataSize - 1);
	/*      */         }
	/*      */ 
	/* 5042 */         if (result.getLastValueIndex() + 1 - result.getFirstValueIndex() < dataSize - formulaData.lookback - formulaData.lookforward)
	/*      */         {
	/* 5045 */           String error = "calculate() method of indicator [" + indicatorInfo.getName() + "] returned incorrect first value and last value indexes. Requested from-to [0]-[" + (dataSize - 1) + "], input array size [" + dataSize + "], returned first calculated index [" + result.getFirstValueIndex() + "], number of calculated values [" + result.getNumberOfElements() + "], last calculated index (set to max index by default) [" + result.getLastValueIndex() + "], lookback [" + formulaData.lookback + "], lookforward [" + formulaData.lookforward + "]";
	/*      */ 
	/* 5059 */           LOGGER.error(error);
	/* 5060 */           NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, true);
	/*      */ 
	/* 5152 */           indicatorContext.setChartInfo(null, null, null);
	/*      */         }
	/* 5067 */         int firstValueIndex = result.getFirstValueIndex();
	/* 5068 */         int numberOfElements = result.getNumberOfElements();
	/*      */ 
	/* 5072 */         if (indicator instanceof ConnectorIndicator) {
	/* 5073 */           firstValueIndex = (result.getFirstValueIndex() < 0) ? 0 : result.getFirstValueIndex();
	/* 5074 */           numberOfElements = (result.getNumberOfElements() < result.getLastValueIndex() - firstValueIndex) ? result.getNumberOfElements() : result.getLastValueIndex() - firstValueIndex;
	/*      */         }
	/*      */ 
	/* 5080 */         if ((lastInputTime > 0L) && (formulaData.lastTime != lastInputTime)) {
	/* 5081 */           formulaData.lastTime = lastInputTime;
	/* 5082 */           formulaData.lastValues = new Object[indicatorInfo.getNumberOfOutputs()];
	/*      */         }
	/*      */ 
	/* 5086 */         int i = 0; for (int j = indicatorInfo.getNumberOfOutputs(); i < j; ++i) {
	/* 5087 */           OutputParameterInfo outputParameterInfo = indicator.getOutputParameterInfo(i);
	/* 5088 */           switch (outputParameterInfo.getType())
	/*      */           {
	/*      */           case INT:
	/* 5090 */             if (numberOfElements == 0)
	/* 5091 */               Arrays.fill(formulaData.outputDataInt[i], from, to + 1, -2147483648);
	/*      */             else {
	/* 5093 */               copyToIndicatorOutput(from, to, recalculateStart, formulaData.outputDataInt[i], firstValueIndex, numberOfElements, outArrays[i], outputParameterInfo.getType(), lastIndex, bufferAsk.length);
	/*      */             }
	/*      */ 
	/* 5106 */             if (formulaData.lastTime == lastInputTime)
	/* 5107 */               formulaData.lastValues[i] = Integer.valueOf(formulaData.outputDataInt[i][lastIndex]); break;
	/*      */           case DOUBLE:
	/* 5111 */             if (numberOfElements == 0)
	/* 5112 */               Arrays.fill(formulaData.outputDataDouble[i], from, to + 1, (0.0D / 0.0D));
	/*      */             else {
	/* 5114 */               copyToIndicatorOutput(from, to, recalculateStart, formulaData.outputDataDouble[i], firstValueIndex, numberOfElements, outArrays[i], outputParameterInfo.getType(), lastIndex, bufferAsk.length);
	/*      */             }
	/*      */ 
	/* 5127 */             if (formulaData.lastTime == lastInputTime)
	/* 5128 */               formulaData.lastValues[i] = Double.valueOf(formulaData.outputDataDouble[i][lastIndex]); break;
	/*      */           case OBJECT:
	/* 5132 */             if (numberOfElements == 0)
	/* 5133 */               Arrays.fill(formulaData.outputDataObject[i], from, to + 1, null);
	/*      */             else {
	/* 5135 */               copyToIndicatorOutput(from, to, recalculateStart, formulaData.outputDataObject[i], firstValueIndex, numberOfElements, outArrays[i], outputParameterInfo.getType(), lastIndex, bufferAsk.length);
	/*      */             }
	/*      */ 
	/* 5145 */             if (formulaData.lastTime == lastInputTime)
	/* 5146 */               formulaData.lastValues[i] = formulaData.outputDataObject[i][lastIndex];
	/*      */           }
	/*      */         }
	/*      */       }
	/*      */       finally
	/*      */       {
	/* 5152 */         indicatorContext.setChartInfo(null, null, null);
	/*      */       }
	/*      */     }
	/* 5155 */     label2648: label1137: return;
	/*      */   }
	/*      */ 
	/*      */   public Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide[] offerSides, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, long from, long to)
	/*      */     throws JFException
	/*      */   {
	/* 5167 */     if (feedDescriptor == null) {
	/* 5168 */       throw new JFException("FeedDescriptor is null");
	/*      */     }
	/* 5170 */     if (feedDescriptor.getInstrument() == null) {
	/* 5171 */       throw new JFException("Instrument is null");
	/*      */     }
	/*      */ 
	/* 5174 */     if ((DataType.TICKS.equals(feedDescriptor.getDataType())) || (DataType.TIME_PERIOD_AGGREGATION.equals(feedDescriptor.getDataType())))
	/*      */     {
	/* 5178 */       return calculateIndicator(feedDescriptor.getInstrument(), feedDescriptor.getPeriod(), offerSides, functionName, inputTypes, optParams, from, to);
	/*      */     }
	/*      */     try
	/*      */     {
	/* 5182 */       checkSide(offerSides);
	/* 5183 */       this.history.validateTimeInterval(feedDescriptor.getInstrument(), from, to);
	/* 5184 */       IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
	/* 5185 */       if (indicatorHolder == null)
	/* 5186 */         throw new JFException("Indicator with name " + functionName + " was not found");
	/*      */       try
	/*      */       {
	/* 5189 */         setOptParams(indicatorHolder.getIndicator(), optParams);
	/* 5190 */         int[] lookSides = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
	/*      */ 
	/* 5192 */         Object[] arrayOfObject = calculateIndicator(feedDescriptor, offerSides, inputTypes, from, to, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
	/*      */ 
	/* 5195 */         return arrayOfObject;
	/*      */       }
	/*      */       finally
	/*      */       {
	/* 5194 */         indicatorHolder.getIndicatorContext().setFeedDescriptor(null);
	/* 5195 */         cacheIndicator(functionName, indicatorHolder);
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 5198 */       throw e;
	/*      */     } catch (RuntimeException e) {
	/* 5200 */       throw e;
	/*      */     } catch (Exception e) {
	/* 5202 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(final IFeedDescriptor feedDescriptor, OfferSide[] offerSides, IIndicators.AppliedPrice[] inputTypes, final long from, final long to, IndicatorHolder indicatorHolder, int indicatorLookback, final int lookBack, final int lookForward)
	/*      */     throws JFException, DataCacheException
	/*      */   {
	/* 5217 */     int inputLength = feedDataToIndicator(new BarsLoadable()
	/*      */     {
	/*      */       public List<IBar> load(IFeedDescriptor fd) throws JFException
	/*      */       {
	/* 5221 */         return loadBarsFromTo(feedDescriptor, from, to, lookBack, lookForward);
	/*      */       }
	/*      */     }
	/*      */     , feedDescriptor, offerSides, inputTypes, indicatorHolder, indicatorLookback, lookBack, lookForward);
	/*      */ 
	/* 5233 */     IIndicator indicator = indicatorHolder.getIndicator();
	/*      */ 
	/* 5235 */     if (inputLength <= 0)
	/*      */     {
	/* 5237 */       return getEmptyIndicatorCalculationResult(indicator);
	/*      */     }
	/*      */ 
	/* 5240 */     return calculateIndicator(null, from, to, indicator, indicatorLookback, lookBack, lookForward, inputLength);
	/*      */   }
	/*      */ 
	/*      */   private List<IBar> loadBarsFromTo(final IFeedDescriptor feedDescriptor, final long from, final long to, final int lookBack, final int lookForward)
	/*      */     throws JFException
	/*      */   {
	/* 5251 */     DataType dataType = feedDescriptor.getDataType();
	/*      */     List converedResult;
	/* 5254 */     if (DataType.PRICE_RANGE_AGGREGATION.equals(dataType)) {
	/* 5255 */       List result = loadBarsFromTo(new BarsLoadable()
	/*      */       {
	/*      */         public List<IRangeBar> load(IFeedDescriptor fd) throws JFException
	/*      */         {
	/* 5259 */           return history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), from, to);
	/*      */         }
	/*      */       }
	/*      */       , new BarsLoadable()
	/*      */       {
	/*      */         public List<IRangeBar> load(IFeedDescriptor fd)
	/*      */           throws JFException
	/*      */         {
	/* 5265 */           return history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), lookBack, DataCacheUtils.getPreviousPriceAggregationBarStart(from), 0);
	/*      */         }
	/*      */       }
	/*      */       , new BarsLoadable()
	/*      */       {
	/*      */         public List<IRangeBar> load(IFeedDescriptor fd)
	/*      */           throws JFException
	/*      */         {
	/* 5271 */           return history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), 0, DataCacheUtils.getNextPriceAggregationBarStart(to), lookForward);
	/*      */         }
	/*      */       }
	/*      */       , lookBack, lookForward);
	/*      */ 
	/* 5277 */       converedResult = new ArrayList(result);
	/*      */     }
	/*      */     else
	/*      */     {
	/* 5279 */       if (DataType.POINT_AND_FIGURE.equals(dataType)) {
	/* 5280 */         List result = loadBarsFromTo(new BarsLoadable()
	/*      */         {
	/*      */           public List<IPointAndFigure> load(IFeedDescriptor fd) throws JFException
	/*      */           {
	/* 5284 */             return history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), from, to);
	/*      */           }
	/*      */         }
	/*      */         , new BarsLoadable()
	/*      */         {
	/*      */           public List<IPointAndFigure> load(IFeedDescriptor fd)
	/*      */             throws JFException
	/*      */           {
	/* 5290 */             return history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), lookBack, DataCacheUtils.getPreviousPriceAggregationBarStart(from), 0);
	/*      */           }
	/*      */         }
	/*      */         , new BarsLoadable()
	/*      */         {
	/*      */           public List<IPointAndFigure> load(IFeedDescriptor fd)
	/*      */             throws JFException
	/*      */           {
	/* 5296 */             return history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), 0, DataCacheUtils.getNextPriceAggregationBarStart(to), lookForward);
	/*      */           }
	/*      */         }
	/*      */         , lookBack, lookForward);
	/*      */ 
	/* 5302 */         converedResult = new ArrayList(result);
	/*      */       }
	/*      */       else
	/*      */       {
	/* 5304 */         if (DataType.TICK_BAR.equals(dataType)) {
	/* 5305 */           List result = loadBarsFromTo(new BarsLoadable()
	/*      */           {
	/*      */             public List<ITickBar> load(IFeedDescriptor fd) throws JFException
	/*      */             {
	/* 5309 */               return history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), from, to);
	/*      */             }
	/*      */           }
	/*      */           , new BarsLoadable()
	/*      */           {
	/*      */             public List<ITickBar> load(IFeedDescriptor fd)
	/*      */               throws JFException
	/*      */             {
	/* 5315 */               return history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), lookBack, DataCacheUtils.getPreviousPriceAggregationBarStart(from), 0);
	/*      */             }
	/*      */           }
	/*      */           , new BarsLoadable()
	/*      */           {
	/*      */             public List<ITickBar> load(IFeedDescriptor fd)
	/*      */               throws JFException
	/*      */             {
	/* 5321 */               return history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), 0, DataCacheUtils.getNextPriceAggregationBarStart(to), lookForward);
	/*      */             }
	/*      */           }
	/*      */           , lookBack, lookForward);
	/*      */ 
	/* 5327 */           converedResult = new ArrayList(result);
	/*      */         }
	/*      */         else {
	/* 5330 */           throw new JFException("Unsupported data type [" + dataType + "]");
	/*      */         }
	/*      */       }
	/*      */     }
	/* 5333 */     return converedResult;
	/*      */   }
	/*      */ 
	/*      */   private <T extends IBar> List<T> loadBarsFromTo(BarsLoadable<T> fromToLoadable, BarsLoadable<T> lookBackLoadable, BarsLoadable<T> lookForwardLoadable, int lookBack, int lookForward)
	/*      */     throws JFException
	/*      */   {
	/* 5347 */     List rangeBars = fromToLoadable.load(null);
	/* 5348 */     List lookBackBars = null;
	/* 5349 */     List lookForwardBars = null;
	/*      */ 
	/* 5351 */     if (lookBack > 0) {
	/* 5352 */       lookBackBars = lookBackLoadable.load(null);
	/*      */     }
	/*      */ 
	/* 5355 */     if (lookForward > 0) {
	/* 5356 */       lookForwardBars = lookForwardLoadable.load(null);
	/*      */     }
	/*      */ 
	/* 5359 */     List result = new ArrayList();
	/*      */ 
	/* 5361 */     if (lookBackBars != null) {
	/* 5362 */       result.addAll(lookBackBars);
	/*      */     }
	/*      */ 
	/* 5365 */     result.addAll(rangeBars);
	/*      */ 
	/* 5367 */     if (lookForwardBars != null) {
	/* 5368 */       result.addAll(lookForwardBars);
	/*      */     }
	/*      */ 
	/* 5371 */     return result;
	/*      */   }
	/*      */ 
	/*      */   public Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide[] offerSides, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, int shift)
	/*      */     throws JFException
	/*      */   {
	/* 5383 */     if (feedDescriptor == null) {
	/* 5384 */       throw new JFException("FeedDescriptor is null");
	/*      */     }
	/*      */ 
	/* 5387 */     if ((DataType.TICKS.equals(feedDescriptor.getDataType())) || (DataType.TIME_PERIOD_AGGREGATION.equals(feedDescriptor.getDataType())))
	/*      */     {
	/* 5391 */       return calculateIndicator(feedDescriptor.getInstrument(), feedDescriptor.getPeriod(), offerSides, functionName, inputTypes, optParams, shift);
	/*      */     }
	/*      */     try
	/*      */     {
	/* 5395 */       IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
	/*      */       try {
	/* 5397 */         checkSide(offerSides);
	/* 5398 */         checkShiftPositive(shift);
	/* 5399 */         if (indicatorHolder == null) {
	/* 5400 */           throw new JFException("Indicator with name " + functionName + " was not found");
	/*      */         }
	/* 5402 */         int[] lookSides = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
	/* 5403 */         Object[] arrayOfObject = calculateIndicator(feedDescriptor, offerSides, inputTypes, shift, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
	/*      */ 
	/* 5407 */         return arrayOfObject;
	/*      */       }
	/*      */       finally
	/*      */       {
	/* 5405 */         if (indicatorHolder != null) {
	/* 5406 */           indicatorHolder.getIndicatorContext().setFeedDescriptor(null);
	/* 5407 */           cacheIndicator(functionName, indicatorHolder);
	/*      */         }
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 5411 */       throw e;
	/*      */     } catch (RuntimeException e) {
	/* 5413 */       throw e;
	/*      */     } catch (Exception e) {
	/* 5415 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   protected Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide[] offerSides, IIndicators.AppliedPrice[] inputTypes, final int shift, IndicatorHolder indicatorHolder, int indicatorLookback, final int lookback, final int lookforward)
	/*      */     throws JFException, DataCacheException
	/*      */   {
	/* 5430 */     int inputLength = feedDataToIndicator(new BarsLoadable()
	/*      */     {
	/*      */       public List<IBar> load(IFeedDescriptor feedDescriptor) throws JFException
	/*      */       {
	/* 5434 */         return loadBarsByShift(feedDescriptor, shift, lookback, lookforward);
	/*      */       }
	/*      */     }
	/*      */     , feedDescriptor, offerSides, inputTypes, indicatorHolder, indicatorLookback, lookback, lookforward);
	/*      */ 
	/* 5446 */     IIndicator indicator = indicatorHolder.getIndicator();
	/*      */ 
	/* 5448 */     if (inputLength <= 0)
	/*      */     {
	/* 5450 */       return getEmptyIndicatorCalculationResult(indicator);
	/*      */     }
	/*      */ 
	/* 5454 */     return calculateIndicator(indicator, indicatorLookback, lookback, lookforward, inputLength);
	/*      */   }
	/*      */ 
	/*      */   private List<IBar> loadBarsByShift(IFeedDescriptor feedDescriptor, int shift, int lookback, int lookforward)
	/*      */     throws JFException
	/*      */   {
	/* 5464 */     if (lookforward > shift) {
	/* 5465 */       return null;
	/*      */     }
	/*      */ 
	/* 5468 */     int finalShift = shift - lookforward;
	/* 5469 */     IBar shiftedBar = null;
	/*      */ 
	/* 5471 */     if (DataType.PRICE_RANGE_AGGREGATION.equals(feedDescriptor.getDataType())) {
	/* 5472 */       shiftedBar = this.history.getRangeBar(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), finalShift);
	/*      */     }
	/* 5474 */     else if (DataType.POINT_AND_FIGURE.equals(feedDescriptor.getDataType())) {
	/* 5475 */       shiftedBar = this.history.getPointAndFigure(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), finalShift);
	/*      */     }
	/* 5477 */     else if (DataType.TICK_BAR.equals(feedDescriptor.getDataType())) {
	/* 5478 */       shiftedBar = this.history.getTickBar(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), finalShift);
	/*      */     }
	/*      */     else {
	/* 5481 */       throw new JFException("Unsupported data type [" + feedDescriptor.getDataType() + "]");
	/*      */     }
	/*      */ 
	/* 5484 */     if (shiftedBar == null) {
	/* 5485 */       return null;
	/*      */     }
	/*      */ 
	/* 5488 */     List result = new ArrayList();
	/*      */ 
	/* 5490 */     if (lookback + lookforward <= 0) {
	/* 5491 */       result.add(shiftedBar);
	/*      */     }
	/*      */     else {
	/* 5494 */       long time = shiftedBar.getTime();
	/* 5495 */       int beforeCount = lookback + lookforward + 1;
	/*      */ 
	/* 5497 */       if (DataType.PRICE_RANGE_AGGREGATION.equals(feedDescriptor.getDataType())) {
	/* 5498 */         List bars = this.history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), beforeCount, time, 0);
	/* 5499 */         result.addAll(bars);
	/*      */       }
	/* 5501 */       else if (DataType.POINT_AND_FIGURE.equals(feedDescriptor.getDataType())) {
	/* 5502 */         List bars = this.history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), beforeCount, time, 0);
	/* 5503 */         result.addAll(bars);
	/*      */       }
	/* 5505 */       else if (DataType.TICK_BAR.equals(feedDescriptor.getDataType())) {
	/* 5506 */         List bars = this.history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), beforeCount, time, 0);
	/* 5507 */         result.addAll(bars);
	/*      */       }
	/*      */     }
	/*      */ 
	/* 5511 */     return ((result.isEmpty()) ? null : result);
	/*      */   }
	/*      */ 
	/*      */   public Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide[] offerSides, String functionName, IIndicators.AppliedPrice[] inputTypes, Object[] optParams, int numberOfBarsBefore, long time, int numberOfBarsAfter)
	/*      */     throws JFException
	/*      */   {
	/* 5526 */     if (feedDescriptor == null) {
	/* 5527 */       throw new JFException("FeedDescriptor is null");
	/*      */     }
	/*      */ 
	/* 5530 */     if (feedDescriptor.getInstrument() == null) {
	/* 5531 */       throw new JFException("Instrument is null");
	/*      */     }
	/*      */ 
	/* 5534 */     if ((DataType.TICKS.equals(feedDescriptor.getDataType())) || (DataType.TIME_PERIOD_AGGREGATION.equals(feedDescriptor.getDataType())))
	/*      */     {
	/* 5538 */       return calculateIndicator(feedDescriptor.getInstrument(), feedDescriptor.getPeriod(), offerSides, functionName, inputTypes, optParams, Filter.NO_FILTER, numberOfBarsBefore, time, numberOfBarsAfter);
	/*      */     }
	/*      */ 
	/*      */     try
	/*      */     {
	/* 5543 */       IndicatorHolder indicatorHolder = getCachedIndicator(functionName);
	/*      */       try {
	/* 5545 */         checkSide(offerSides);
	/* 5546 */         this.history.validateBeforeTimeAfter(feedDescriptor.getInstrument(), numberOfBarsBefore, time, numberOfBarsAfter);
	/* 5547 */         if (indicatorHolder == null) {
	/* 5548 */           throw new JFException("Indicator with name " + functionName + " was not found");
	/*      */         }
	/* 5550 */         int[] lookSides = calculateLookbackLookforward(indicatorHolder.getIndicator(), optParams);
	/* 5551 */         Object[] arrayOfObject = calculateIndicator(feedDescriptor, offerSides, inputTypes, numberOfBarsBefore, time, numberOfBarsAfter, indicatorHolder, lookSides[0], lookSides[1], lookSides[2]);
	/*      */ 
	/* 5555 */         return arrayOfObject;
	/*      */       }
	/*      */       finally
	/*      */       {
	/* 5553 */         if (indicatorHolder != null) {
	/* 5554 */           indicatorHolder.getIndicatorContext().setFeedDescriptor(null);
	/* 5555 */           cacheIndicator(functionName, indicatorHolder);
	/*      */         }
	/*      */       }
	/*      */     } catch (JFException e) {
	/* 5559 */       throw e;
	/*      */     } catch (RuntimeException e) {
	/* 5561 */       throw e;
	/*      */     } catch (Exception e) {
	/* 5563 */       throw new JFException(e);
	/*      */     }
	/*      */   }
	/*      */ 
	/*      */   private Object[] calculateIndicator(IFeedDescriptor feedDescriptor, OfferSide[] offerSides, IIndicators.AppliedPrice[] inputTypes, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter, IndicatorHolder indicatorHolder, int indicatorLookback, final int lookback, final int lookforward)
	/*      */     throws JFException
	/*      */   {
	/* 5580 */     int inputLength = feedDataToIndicator(new BarsLoadable()
	/*      */     {
	/*      */       public List<IBar> load(IFeedDescriptor feedDescriptor) throws JFException
	/*      */       {
	/* 5584 */         return loadBarsBeforeTimeAfter(feedDescriptor, numberOfBarsBefore, time, numberOfBarsAfter, lookback, lookforward);
	/*      */       }
	/*      */     }
	/*      */     , feedDescriptor, offerSides, inputTypes, indicatorHolder, indicatorLookback, lookback, lookforward);
	/*      */ 
	/* 5596 */     IIndicator indicator = indicatorHolder.getIndicator();
	/*      */ 
	/* 5598 */     if (inputLength <= 0)
	/*      */     {
	/* 5600 */       return getEmptyIndicatorCalculationResult(indicator);
	/*      */     }
	/*      */ 
	/* 5603 */     return calculateIndicator(numberOfBarsBefore, time, numberOfBarsAfter, indicator, indicatorLookback, lookback, lookforward, inputLength);
	/*      */   }
	/*      */ 
	/*      */   private List<IBar> loadBarsBeforeTimeAfter(IFeedDescriptor feedDescriptor, int numberOfBarsBefore, long time, int numberOfBarsAfter, int lookback, int lookforward)
	/*      */     throws JFException
	/*      */   {
	/* 5615 */     List data = new ArrayList();
	/*      */ 
	/* 5617 */     int beforeCount = numberOfBarsBefore + lookback;
	/* 5618 */     int afterCount = numberOfBarsAfter + lookforward;
	/*      */ 
	/* 5620 */     if (DataType.PRICE_RANGE_AGGREGATION.equals(feedDescriptor.getDataType())) {
	/* 5621 */       List bars = this.history.getRangeBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), beforeCount, time, afterCount);
	/* 5622 */       data.addAll(bars);
	/*      */     }
	/* 5624 */     else if (DataType.POINT_AND_FIGURE.equals(feedDescriptor.getDataType())) {
	/* 5625 */       List bars = this.history.getPointAndFigures(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getPriceRange(), feedDescriptor.getReversalAmount(), beforeCount, time, afterCount);
	/* 5626 */       data.addAll(bars);
	/*      */     }
	/* 5628 */     else if (DataType.TICK_BAR.equals(feedDescriptor.getDataType())) {
	/* 5629 */       List bars = this.history.getTickBars(feedDescriptor.getInstrument(), feedDescriptor.getOfferSide(), feedDescriptor.getTickBarSize(), beforeCount, time, afterCount);
	/* 5630 */       data.addAll(bars);
	/*      */     }
	/*      */     else {
	/* 5633 */       throw new JFException("Unsupported data type [" + feedDescriptor.getDataType() + "]");
	/*      */     }
	/*      */ 
	/* 5636 */     List result = (data.size() < beforeCount + afterCount) ? null : data;
	/* 5637 */     return result;
	/*      */   }
	/*      */ 
	/*      */   private Object[] getEmptyIndicatorCalculationResult(IIndicator indicator) {
	/* 5641 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/*      */ 
	/* 5643 */     Object[] ret = new Object[indicatorInfo.getNumberOfOutputs()];
	/* 5644 */     int k = 0; for (int l = indicatorInfo.getNumberOfOutputs(); k < l; ++k) {
	/* 5645 */       OutputParameterInfo outInfo = indicator.getOutputParameterInfo(k);
	/* 5646 */       switch (outInfo.getType())
	/*      */       {
	/*      */       case DOUBLE:
	/* 5648 */         ret[k] = Double.valueOf((0.0D / 0.0D));
	/* 5649 */         break;
	/*      */       case INT:
	/* 5651 */         ret[k] = Integer.valueOf(-2147483648);
	/* 5652 */         break;
	/*      */       case OBJECT:
	/* 5654 */         ret[k] = null;
	/*      */       }
	/*      */     }
	/*      */ 
	/* 5658 */     return ret;
	/*      */   }
	/*      */ 
	/*      */   private int feedDataToIndicator(BarsLoadable<IBar> loadable, IFeedDescriptor feedDescriptor, OfferSide[] offerSides, IIndicators.AppliedPrice[] inputTypes, IndicatorHolder indicatorHolder, int indicatorLookback, int lookback, int lookforward)
	/*      */     throws JFException
	/*      */   {
	/* 5672 */     if (feedDescriptor == null) {
	/* 5673 */       throw new JFException("FeedDescriptor is null");
	/*      */     }
	/*      */ 
	/* 5676 */     int inputLength = -2147483648;
	/* 5677 */     IIndicator indicator = indicatorHolder.getIndicator();
	/* 5678 */     IndicatorInfo indicatorInfo = indicator.getIndicatorInfo();
	/* 5679 */     IndicatorContext indicatorContext = indicatorHolder.getIndicatorContext();
	/*      */ 
	/* 5681 */     OfferSide offerSide = (indicatorInfo.getNumberOfInputs() > 0) ? offerSides[0] : OfferSide.BID;
	/* 5682 */     feedDescriptor.setOfferSide(offerSide);
	/* 5683 */     indicatorContext.setFeedDescriptor(feedDescriptor);
	/*      */ 
	/* 5685 */     int i = 0; for (int j = indicatorInfo.getNumberOfInputs(); i < j; ++i) {
	/* 5686 */       InputParameterInfo info = indicator.getInputParameterInfo(i);
	/*      */ 
	/* 5688 */       if ((info.getOfferSide() != null) || (info.getInstrument() != null) || (info.getPeriod() != null)) {
	/* 5689 */         throw new JFException("Indicator [" + indicatorInfo.getName() + "] cannot be calculated for data type " + feedDescriptor.getDataType());
	/*      */       }
	/*      */ 
	/* 5692 */       OfferSide currentOfferSide = offerSides[i];
	/*      */ 
	/* 5694 */       IFeedDescriptor fd = new FeedDescriptor(feedDescriptor);
	/* 5695 */       fd.setOfferSide(currentOfferSide);
	/*      */ 
	/* 5697 */       List bars = loadable.load(fd);
	/* 5698 */       Object inputData = null;
	/*      */ 
	/* 5700 */       if ((bars == null) || (bars.isEmpty())) {
	/* 5701 */         return 0;
	/*      */       }
	/*      */ 
	/* 5704 */       inputLength = bars.size();
	/* 5705 */       inputData = barsToReal(bars, info.getType(), (info.getType() == InputParameterInfo.Type.DOUBLE) ? inputTypes[i] : null);
	/*      */       try
	/*      */       {
	/* 5709 */         indicator.setInputParameter(i, inputData);
	/*      */       } catch (Throwable t) {
	/* 5711 */         LOGGER.error(t.getMessage(), t);
	/* 5712 */         String error = StrategyWrapper.representError(indicator, t);
	/* 5713 */         NotificationUtilsProvider.getNotificationUtils().postErrorMessage("Error in indicator: " + error, t, true);
	/* 5714 */         throw new JFException(t);
	/*      */       }
	/*      */     }
	/*      */ 
	/* 5718 */     return inputLength;
	/*      */   }
	/*      */ 
	/*      */   abstract interface BarsLoadable<T>
	/*      */   {
	/*      */     public abstract List<T> load(IFeedDescriptor paramIFeedDescriptor)
	/*      */       throws JFException;
	/*      */   }
	/*      */ }

	/* Location:           C:\Projects\jforex\libs\greed-common-162.jar
	 * Qualified Name:     com.dukascopy.api.impl.Indicators
	 * Java Class Version: 6 (50.0)
	 * JD-Core Version:    0.5.3
	 */	