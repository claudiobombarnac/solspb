/*      */ package solspb.jforex;
/*      */ 
/*      */ import com.dukascopy.api.Filter;
/*      */ import com.dukascopy.api.IBar;
/*      */ import com.dukascopy.api.IHistory;
/*      */ import com.dukascopy.api.IOrder;
/*      */ import com.dukascopy.api.ITick;
/*      */ import com.dukascopy.api.Instrument;
/*      */ import com.dukascopy.api.JFException;
/*      */ import com.dukascopy.api.LoadingDataListener;
/*      */ import com.dukascopy.api.LoadingOrdersListener;
/*      */ import com.dukascopy.api.OfferSide;
/*      */ import com.dukascopy.api.Period;
/*      */ import com.dukascopy.api.PriceRange;
/*      */ import com.dukascopy.api.ReversalAmount;
/*      */ import com.dukascopy.api.TickBarSize;
/*      */ import com.dukascopy.api.feed.IPointAndFigure;
/*      */ import com.dukascopy.api.feed.IPointAndFigureFeedListener;
/*      */ import com.dukascopy.api.feed.IRangeBar;
/*      */ import com.dukascopy.api.feed.IRangeBarFeedListener;
/*      */ import com.dukascopy.api.feed.ITickBar;
/*      */ import com.dukascopy.api.feed.ITickBarFeedListener;
import com.dukascopy.api.impl.HistoryOrder;
/*      */ import com.dukascopy.api.impl.util.HistoryUtils;
/*      */ import com.dukascopy.charts.data.datacache.CandleData;
/*      */ import com.dukascopy.charts.data.datacache.DataCacheException;
/*      */ import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.FeedDataProvider;
/*      */ import com.dukascopy.charts.data.datacache.LiveFeedListener;
/*      */ import com.dukascopy.charts.data.datacache.LoadingProgressAdapter;
/*      */ import com.dukascopy.charts.data.datacache.OrderHistoricalData;
/*      */ import com.dukascopy.charts.data.datacache.OrdersListener;
/*      */ import com.dukascopy.charts.data.datacache.TickData;
/*      */ import com.dukascopy.charts.data.datacache.pnf.PointAndFigureData;
/*      */ import com.dukascopy.charts.data.datacache.pnf.PointAndFigureLiveFeedAdapter;
/*      */ import com.dukascopy.charts.data.datacache.rangebar.PriceRangeData;
/*      */ import com.dukascopy.charts.data.datacache.rangebar.PriceRangeLiveFeedAdapter;
/*      */ import com.dukascopy.charts.data.datacache.tickbar.TickBarData;
/*      */ import com.dukascopy.charts.data.datacache.tickbar.TickBarLiveFeedAdapter;
/*      */ import com.dukascopy.charts.data.orders.IOrdersProvider;
/*      */ import java.math.BigDecimal;
/*      */ import java.security.AccessController;
/*      */ import java.security.PrivilegedActionException;
/*      */ import java.security.PrivilegedExceptionAction;
/*      */ import java.text.SimpleDateFormat;
/*      */ import java.util.ArrayList;
/*      */ import java.util.Collection;
/*      */ import java.util.Currency;
/*      */ import java.util.Date;
/*      */ import java.util.List;
/*      */ import java.util.ListIterator;
/*      */ import java.util.Map;
/*      */ import java.util.TimeZone;
/*      */ import java.util.concurrent.atomic.AtomicBoolean;
/*      */ import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*      */ 
/*      */ public class History
/*      */   implements IHistory
/*      */ {
/*   63 */   private static final Logger LOGGER = LoggerFactory.getLogger(History.class);
/*      */ 
/*   65 */   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*      */ 
/*   71 */   protected static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1000000L);
/*      */   protected AtomicBoolean ordersHistoryRequestSent;
/*      */   protected IOrdersProvider ordersProvider;
/*      */   protected Currency accountCurrency;
/*      */   protected FeedDataProvider feedDataProvider;
/*      */ 
/*      */   public History(IOrdersProvider ordersProvider, Currency accountCurrency)
/*      */   {
/*   67 */     DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
/*      */ 
/*   73 */     this.ordersHistoryRequestSent = new AtomicBoolean();
/*      */ 
/*   76 */     this.feedDataProvider = FeedDataProvider.getDefaultInstance();
/*      */ 
/*   79 */     this.ordersProvider = ordersProvider;
/*   80 */     this.accountCurrency = accountCurrency;
/*      */   }
/*      */ 
/*      */   protected History(int noNeedToInitializeAnythingForTests)
/*      */   {
/*   67 */     DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
/*      */ 
/*   73 */     this.ordersHistoryRequestSent = new AtomicBoolean();
/*      */ 
/*   76 */     this.feedDataProvider = FeedDataProvider.getDefaultInstance();
/*      */   }
/*      */ 
/*      */   public long getTimeOfLastTick(Instrument instrument)
/*      */     throws JFException
/*      */   {
/*   87 */     if (!(isInstrumentSubscribed(instrument))) {
/*   88 */       throw new JFException("Instrument [" + instrument + "] not opened");
/*      */     }
/*   90 */     long time = this.feedDataProvider.getLastTickTime(instrument);
/*   91 */     return ((time == -9223372036854775808L) ? -1L : time);
/*      */   }
/*      */ 
/*      */   public ITick getLastTick(Instrument instrument) throws JFException {
/*   95 */     if (!(isInstrumentSubscribed(instrument))) {
/*   96 */       throw new JFException("Instrument [" + instrument + "] not opened");
/*      */     }
/*   98 */     TickData tick = this.feedDataProvider.getLastTick(instrument);
/*   99 */     if (tick != null) {
/*  100 */       return new TickData(tick.time, tick.ask, tick.bid, tick.askVol, tick.bidVol, null, null, null, null);
/*      */     }
/*  102 */     return null;
/*      */   }
/*      */ 
/*      */   protected long getCurrentTime(Instrument instrument)
/*      */   {
/*  107 */     return this.feedDataProvider.getCurrentTime(instrument);
/*      */   }
/*      */ 
/*      */   protected long getCurrentTimeBlocking(Instrument instrument) {
/*  111 */     long timeout = 120000L;
/*  112 */     long currentTime = -9223372036854775808L;
/*  113 */     long start = System.currentTimeMillis();
/*      */ 
/*  115 */     while (start + timeout > System.currentTimeMillis()) {
/*  116 */       currentTime = getCurrentTime(instrument);
/*  117 */       if (currentTime != -9223372036854775808L) {
/*      */         break;
/*      */       }
/*      */       try
/*      */       {
/*  122 */         Thread.sleep(500L);
/*      */       }
/*      */       catch (InterruptedException e)
/*      */       {
/*      */       }
/*      */     }
/*  128 */     return currentTime;
/*      */   }
/*      */ 
/*      */   public long getStartTimeOfCurrentBar(Instrument instrument, Period period) throws JFException {
/*  132 */     if (!(isInstrumentSubscribed(instrument))) {
/*  133 */       throw new JFException("Instrument [" + instrument + "] not opened");
/*      */     }
/*  135 */     long timeOfCurrentCandle = getCurrentTime(instrument);
/*  136 */     return ((timeOfCurrentCandle == -9223372036854775808L) ? -1L : DataCacheUtils.getCandleStartFast(period, timeOfCurrentCandle));
/*      */   }
/*      */ 
/*      */   public IBar getBar(Instrument instrument, Period period, OfferSide side, int shift) throws JFException {
/*  140 */     if (!(isInstrumentSubscribed(instrument)))
/*  141 */       throw new JFException("Instrument [" + instrument + "] not opened");
/*      */     try
/*      */     {
/*  144 */       if (shift < 0) {
/*  145 */         throw new JFException("Parameter 'shift' is < 0");
/*      */       }
/*  147 */       if (shift == 0) {
/*  148 */         return getCurrentBar(instrument, period, side);
/*      */       }
/*      */ 
/*  151 */       return getHistoryBarBlocking(instrument, period, side, shift);
/*      */     } catch (DataCacheException e) {
/*  153 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
///*      */   public IBar getCurrentBar(Instrument instrument, Period period, OfferSide side) throws DataCacheException {
//			   return this.feedDataProvider.getLastCandle(instrument, period, side);
//}

/*      */   public IBar getCurrentBar(Instrument instrument, Period period, OfferSide side) throws DataCacheException {
///*  158 */     CandleData candle = this.feedDataProvider.getInProgressCandleBlocking(instrument, period, side);
/*  158 */     CandleData candle = this.feedDataProvider.getInProgressCandle(instrument, period, side);
/*  159 */     if (candle == null) {
/*  160 */       return null;
/*      */     }
/*  162 */     return new CandleData(candle.time, candle.open, candle.close, candle.low, candle.high, candle.vol);
/*      */   }
/*      */ 
/*      */   public IBar getHistoryBarBlocking(Instrument instrument, Period period, OfferSide side, int shift) throws JFException
/*      */   {
/*      */     try {
/*  168 */       if (shift <= 0) {
/*  169 */         throw new JFException("Parameter 'shift' must be > 0");
/*      */       }
/*      */ 
/*  172 */       long timeOfCurrentCandle = getCurrentTimeBlocking(instrument);
/*  173 */       if (timeOfCurrentCandle == -9223372036854775808L) {
/*  174 */         return null;
/*      */       }
/*  176 */       long currentBarStartTime = DataCacheUtils.getCandleStart(period, timeOfCurrentCandle);
/*  177 */       long requestedBarStartTime = DataCacheUtils.getTimeForNCandlesBack(period, DataCacheUtils.getPreviousCandleStart(period, currentBarStartTime), shift);
/*      */ 
/*  181 */       List bars = getBars(instrument, period, side, requestedBarStartTime, requestedBarStartTime);
/*  182 */       if (bars.isEmpty()) {
/*  183 */         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  184 */         dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  185 */         throw new JFException("Could not load bar for instrument [" + instrument + "], period [" + period + "], side [" + side + "], start time [" + dateFormat.format(new Date(requestedBarStartTime)) + "], current bar start time [" + dateFormat.format(new Date(currentBarStartTime)) + "]");
/*      */       }
/*  187 */       return ((IBar)bars.get(0));
/*      */     }
/*      */     catch (DataCacheException e) {
/*  190 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readTicks(final Instrument instrument, final long from, final long to, final LoadingDataListener tickListener, final com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException {
/*  195 */     if (!(isIntervalValid(Period.TICK, from, to))) {
/*  196 */       throw new JFException("Interval from [" + DATE_FORMAT.format(new Date(from)) + "] to [" + DATE_FORMAT.format(new Date(to)) + "] GMT is not valid");
/*      */     }
/*  198 */     if (isInstrumentSubscribed(instrument)) {
/*  199 */       long timeOfCurrentCandle = getCurrentTime(instrument);
/*  200 */       if ((timeOfCurrentCandle != -9223372036854775808L) && (to > timeOfCurrentCandle))
/*  201 */         throw new JFException("\"to\" parameter can't be greater than time of the last tick for this instrument");
/*      */     }
/*      */     try
/*      */     {
/*  205 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Object run() throws Exception {
/*  207 */           readTicksSecured(instrument, from, to, tickListener, loadingProgress);
/*  208 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  212 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   private void readTicksSecured(Instrument instrument, long from, long to, LoadingDataListener tickListener, com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException {
/*      */     try {
/*  218 */       this.feedDataProvider.loadTicksData(instrument, from, to, new LiveFeedListenerWrapper(tickListener), new LoadingProgressListenerWrapper(loadingProgress, false));
/*      */     } catch (DataCacheException e) {
/*  220 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readBars(final Instrument instrument, final Period period, final OfferSide side, final long from, final long to, final LoadingDataListener barListener, final com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException {
/*  225 */     if (!(isIntervalValid(period, from, to))) {
/*  226 */       throw new JFException("Interval from [" + DATE_FORMAT.format(new Date(from)) + "] to [" + DATE_FORMAT.format(new Date(to)) + "] GMT is not valid for period [" + period + "]");
/*      */     }
/*  228 */     if (isInstrumentSubscribed(instrument)) {
/*  229 */       long timeOfLastCandle = getCurrentTime(instrument);
/*  230 */       if (timeOfLastCandle != -9223372036854775808L) {
/*  231 */         timeOfLastCandle = DataCacheUtils.getCandleStartFast(period, timeOfLastCandle);
/*  232 */         if (to >= timeOfLastCandle)
/*  233 */           throw new JFException("\"to\" parameter can't be greater than time of the last formed bar for this instrument");
/*      */       }
/*      */     }
/*      */     try
/*      */     {
/*  238 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Object run() throws Exception {
/*  240 */           readBarsSecured(instrument, period, side, from, to, barListener, loadingProgress);
/*  241 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  245 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   private void readBarsSecured(Instrument instrument, Period period, OfferSide side, long from, long to, LoadingDataListener barListener, com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException {
/*      */     try {
/*  251 */       this.feedDataProvider.loadCandlesData(instrument, period, side, from, to, new LiveFeedListenerWrapper(barListener), new LoadingProgressListenerWrapper(loadingProgress, false));
/*      */     } catch (DataCacheException e) {
/*  253 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readBars(final Instrument instrument, final Period period, final OfferSide side, final Filter filter, final int numberOfCandlesBefore, final long time, final int numberOfCandlesAfter, final LoadingDataListener barListener, final com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException {
/*  258 */     if (!(isIntervalValid(period, numberOfCandlesBefore, time, numberOfCandlesAfter))) {
/*  259 */       throw new JFException("Number of bars to load = 0 or time is not correct time for the period specified");
/*      */     }
/*  261 */     if (isInstrumentSubscribed(instrument)) {
/*  262 */       long timeOfLastCandle = getCurrentTime(instrument);
/*  263 */       if (timeOfLastCandle != -9223372036854775808L) {
/*  264 */         timeOfLastCandle = DataCacheUtils.getCandleStartFast(period, timeOfLastCandle);
/*  265 */         if (time > timeOfLastCandle)
/*  266 */           throw new JFException("\"to\" parameter can't be greater than time of the last formed bar for this instrument");
/*      */       }
/*      */     }
/*      */     try
/*      */     {
/*  271 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Object run() throws Exception {
/*  273 */           readBarsSecured(instrument, period, side, filter, numberOfCandlesBefore, time, numberOfCandlesAfter, barListener, loadingProgress);
/*  274 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  278 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   private void readBarsSecured(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter, LoadingDataListener barListener, com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException {
/*      */     try {
/*  284 */       this.feedDataProvider.loadCandlesDataBeforeAfter(instrument, period, side, numberOfCandlesBefore, numberOfCandlesAfter, time, filter, new LiveFeedListenerWrapper(barListener), new LoadingProgressListenerWrapper(loadingProgress, false));
/*      */     } catch (DataCacheException e) {
/*  286 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public List<ITick> getTicks(final Instrument instrument, final long from, final long to) throws JFException {
/*  291 */     if (!(isIntervalValid(Period.TICK, from, to))) {
/*  292 */       throw new JFException("Interval from [" + DATE_FORMAT.format(new Date(from)) + "] to [" + DATE_FORMAT.format(new Date(to)) + "] GMT is not valid");
/*      */     }
/*  294 */     if (isInstrumentSubscribed(instrument)) {
/*  295 */       long timeOfCurrentCandle = getCurrentTime(instrument);
/*  296 */       if ((timeOfCurrentCandle != -9223372036854775808L) && 
/*  297 */         (to > timeOfCurrentCandle)) {
/*  298 */         throw new JFException("\"to\" parameter can't be greater than time of the last tick for this instrument");
/*      */       }
/*      */     }
/*      */     try
/*      */     {
/*  303 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<ITick> run() throws Exception {
/*  305 */           return History.this.getTicksSecured(instrument, from, to);
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  309 */       HistoryUtils.throwJFException(e); }
/*  310 */     return null;
/*      */   }
/*      */ 
/*      */   protected List<ITick> getTicksSecured(Instrument instrument, long from, long to) throws JFException
/*      */   {
/*      */     try {
/*  316 */       final List ticks = new ArrayList();
/*  317 */       final int[] result = { 0 };
/*  318 */       final Exception[] exceptions = new Exception[1];
/*  319 */       com.dukascopy.charts.data.datacache.LoadingProgressListener loadingProgressListener = new com.dukascopy.charts.data.datacache.LoadingProgressListener() {
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  324 */           result[0] = ((allDataLoaded) ? 1 : 2);
/*  325 */           exceptions[0] = e;
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  329 */           return false;
/*      */         }
/*      */       };
/*  332 */       this.feedDataProvider.loadTicksDataSynched(instrument, from, to, new LiveFeedListener() {
/*      */         public void newTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol) {
/*  334 */           ticks.add(new TickData(time, ask, bid, askVol, bidVol, null, null, null, null));
/*      */         }
/*      */ 
/*      */         public void newCandle(Instrument instrument, Period period, OfferSide side, long time, double open, double close, double low, double high, double vol)
/*      */         {
/*      */         }
/*      */       }
/*      */       , loadingProgressListener);
/*      */ 
/*  341 */       if (result[0] == 2) {
/*  342 */         throw new JFException("Error while loading ticks", exceptions[0]);
/*      */       }
/*  344 */       return ticks;
/*      */     }
/*      */     catch (DataCacheException e) {
/*  347 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public ITick getLastTickBefore(final Instrument instrument, final long to) throws JFException {
/*  352 */     if (isInstrumentSubscribed(instrument)) {
/*  353 */       long timeOfCurrentCandle = getCurrentTime(instrument);
/*  354 */       if ((timeOfCurrentCandle != -9223372036854775808L) && (to > timeOfCurrentCandle))
/*  355 */         throw new JFException("\"to\" parameter can't be greater than time of the last tick for this instrument");
/*      */     }
/*      */     try
/*      */     {
/*  359 */       return ((ITick)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public ITick run() throws Exception {
/*  361 */           return getLastTickBeforeSecured(instrument, to);
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  365 */       HistoryUtils.throwJFException(e); }
/*  366 */     return null;
/*      */   }
/*      */ 
/*      */   protected ITick getLastTickBeforeSecured(Instrument instrument, long to) throws JFException
/*      */   {
/*      */     try {
/*  372 */       long interval = 15000L;
/*  373 */       long from = to - interval;
/*  374 */       final TickData[] tick = new TickData[1];
/*  375 */       int daysAdded = 0;
/*  376 */       while ((tick[0] == null) && (daysAdded < 5)) {
/*  377 */         final int[] result = { 0 };
/*  378 */         final Exception[] exceptions = new Exception[1];
/*  379 */         com.dukascopy.charts.data.datacache.LoadingProgressListener loadingProgressListener = new com.dukascopy.charts.data.datacache.LoadingProgressListener() {
/*      */           public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*      */           }
/*      */ 
/*      */           public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  384 */             result[0] = ((allDataLoaded) ? 1 : 2);
/*  385 */             exceptions[0] = e;
/*      */           }
/*      */ 
/*      */           public boolean stopJob() {
/*  389 */             return false;
/*      */           }
/*      */         };
/*  392 */         this.feedDataProvider.loadTicksDataSynched(instrument, from, to, new LiveFeedListener() {
/*      */           public void newTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol) {
/*  394 */             if (tick[0] == null) {
/*  395 */               tick[0] = new TickData(time, ask, bid, askVol, bidVol, null, null, null, null);
/*      */             } else {
/*  397 */               tick[0].time = time;
/*  398 */               tick[0].ask = ask;
/*  399 */               tick[0].bid = bid;
/*  400 */               tick[0].askVol = askVol;
/*  401 */               tick[0].bidVol = bidVol;
/*      */             }
/*      */           }
/*      */ 
/*      */           public void newCandle(Instrument instrument, Period period, OfferSide side, long time, double open, double close, double low, double high, double vol)
/*      */           {
/*      */           }
/*      */         }
/*      */         , loadingProgressListener);
/*      */ 
/*  409 */         if (result[0] == 2) {
/*  410 */           throw new JFException("Error while loading ticks", exceptions[0]);
/*      */         }
/*  412 */         if (from < FeedDataProvider.getTimeOfFirstOurCandle(instrument, Period.TICK)) {
/*  413 */           return null;
/*      */         }
/*  415 */         if (interval * 2L > 86400000L) {
/*  416 */           interval += 86400000L;
/*  417 */           ++daysAdded;
/*      */         } else {
/*  419 */           interval *= 2L;
/*      */         }
/*  421 */         from = to - interval;
/*      */       }
/*  423 */       return tick[0];
/*      */     } catch (DataCacheException e) {
/*  425 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public List<IBar> getBars(final Instrument instrument, final Period period, final OfferSide side, final long from, final long to) throws JFException {
/*  430 */     if (!(isIntervalValid(period, from, to))) {
/*  431 */       throw new JFException("Interval from [" + DATE_FORMAT.format(new Date(from)) + "] to [" + DATE_FORMAT.format(new Date(to)) + "] GMT is not valid for period [" + period + "]");
/*      */     }
/*  433 */     if (isInstrumentSubscribed(instrument)) {
/*  434 */       long timeOfLastCandle = getCurrentTime(instrument);
/*  435 */       if (timeOfLastCandle != -9223372036854775808L) {
/*  436 */         timeOfLastCandle = DataCacheUtils.getCandleStartFast(period, timeOfLastCandle);
/*  437 */         if (to > timeOfLastCandle)
/*  438 */           throw new JFException("\"to\" parameter can't be greater than the time of the last formed bar for this instrument");
/*      */       }
/*      */     }
/*      */     try
/*      */     {
/*  443 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<IBar> run() throws Exception {
/*  445 */           return getBarsSecured(instrument, period, side, from, to);
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  449 */       HistoryUtils.throwJFException(e); }
/*  450 */     return null;
/*      */   }
/*      */ 
/*      */   protected List<IBar> getBarsSecured(Instrument instrument, Period period, OfferSide side, long from, long to) throws JFException
/*      */   {
/*      */     try {
/*  456 */       final List bars = new ArrayList();
/*  457 */       final int[] result = { 0 };
/*  458 */       final Exception[] exceptions = new Exception[1];
/*  459 */       IBar currentBar = getCurrentBar(instrument, period, side);
/*  460 */       com.dukascopy.charts.data.datacache.LoadingProgressListener loadingProgressListener = new com.dukascopy.charts.data.datacache.LoadingProgressListener() {
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  465 */           result[0] = ((allDataLoaded) ? 1 : 2);
/*  466 */           exceptions[0] = e;
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  470 */           return false;
/*      */         }
/*      */       };
/*  473 */       this.feedDataProvider.loadCandlesDataSynched(instrument, period, side, from, to, new LiveFeedListener() {
/*      */         public void newTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol) {
/*      */         }
/*      */ 
/*      */         public void newCandle(Instrument instrument, Period period, OfferSide side, long time, double open, double close, double low, double high, double vol) {
/*  478 */           bars.add(new CandleData(time, open, close, low, high, vol));
/*      */         }
/*      */       }
/*      */       , loadingProgressListener);
/*      */ 
/*  482 */       if (result[0] == 2) {
/*  483 */         throw new JFException("Error while loading bars", exceptions[0]);
/*      */       }
/*  485 */       if ((currentBar != null) && 
/*  487 */         (to == currentBar.getTime()) && ((
/*  487 */         (bars.isEmpty()) || (((IBar)bars.get(bars.size() - 1)).getTime() <= currentBar.getTime())))) {
/*  488 */         if ((bars.size() > 0) && (((IBar)bars.get(bars.size() - 1)).getTime() == currentBar.getTime()))
/*      */         {
/*  490 */           bars.remove(bars.size() - 1);
/*      */         }
/*  492 */         bars.add(currentBar);
/*      */       }
/*      */ 
/*  495 */       return bars;
/*      */     }
/*      */     catch (DataCacheException e) {
/*  498 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public List<IBar> getBars(final Instrument instrument, final Period period, final OfferSide side, final Filter filter, final int numberOfCandlesBefore, final long time, final int numberOfCandlesAfter) throws JFException {
/*  503 */     if (!(isIntervalValid(period, numberOfCandlesBefore, time, numberOfCandlesAfter))) {
/*  504 */       throw new JFException("Number of bars to load = 0 or time is not correct time for the period specified");
/*      */     }
/*  506 */     if (isInstrumentSubscribed(instrument)) {
/*  507 */       long timeOfLastCandle = getCurrentTime(instrument);
/*  508 */       if (timeOfLastCandle != -9223372036854775808L) {
/*  509 */         timeOfLastCandle = DataCacheUtils.getCandleStartFast(period, timeOfLastCandle);
/*  510 */         if (time > timeOfLastCandle)
/*  511 */           throw new JFException("\"to\" parameter can't be greater than time of the last formed bar for this instrument");
/*      */       }
/*      */     }
/*      */     try
/*      */     {
/*  516 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<IBar> run() throws Exception {
/*  518 */           return getBarsSecured(instrument, period, side, filter, numberOfCandlesBefore, time, numberOfCandlesAfter);
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  522 */       Exception ex = e.getException();
/*  523 */       if (ex instanceof JFException)
/*  524 */         throw ((JFException)ex);
/*  525 */       if (ex instanceof RuntimeException) {
/*  526 */         throw ((RuntimeException)ex);
/*      */       }
/*  528 */       LOGGER.error(ex.getMessage(), ex);
/*  529 */       throw new JFException(ex);
/*      */     }
/*      */   }
/*      */ 
/*      */   protected List<IBar> getBarsSecured(Instrument instrument, Period period, OfferSide side, Filter filter, int numberOfCandlesBefore, long time, int numberOfCandlesAfter) throws JFException
/*      */   {
/*      */     try
/*      */     {
/*  537 */       IBar currentBar = getCurrentBar(instrument, period, side);
/*  538 */       final List bars = new ArrayList();
/*  539 */       final int[] result = { 0 };
/*  540 */       final Exception[] exceptions = new Exception[1];
/*  541 */       com.dukascopy.charts.data.datacache.LoadingProgressListener loadingProgressListener = new com.dukascopy.charts.data.datacache.LoadingProgressListener() {
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  546 */           result[0] = ((allDataLoaded) ? 1 : 2);
/*  547 */           exceptions[0] = e;
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  551 */           return false;
/*      */         }
/*      */       };
/*  554 */       this.feedDataProvider.loadCandlesDataBeforeAfterSynched(instrument, period, side, numberOfCandlesBefore, numberOfCandlesAfter, time, filter, new LiveFeedListener() {
/*      */         public void newTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol) {
/*      */         }
/*      */ 
/*      */         public void newCandle(Instrument instrument, Period period, OfferSide side, long time, double open, double close, double low, double high, double vol) {
/*  559 */           bars.add(new CandleData(time, open, close, low, high, vol));
/*      */         }
/*      */       }
/*      */       , loadingProgressListener);
/*      */ 
/*  563 */       if ((currentBar != null) && (!(bars.isEmpty())))
/*      */       {
/*      */         ListIterator iterator;
/*  564 */         if (((IBar)bars.get(bars.size() - 1)).getTime() > currentBar.getTime()) {
/*  565 */           for (iterator = bars.listIterator(bars.size()); iterator.hasPrevious(); ) {
/*  566 */             IBar bar = (IBar)iterator.previous();
/*  567 */             if (bar.getTime() <= currentBar.getTime()) break;
/*  568 */             iterator.remove();
/*      */           }
/*      */ 
/*      */         }
/*      */ 
/*  574 */         if (((IBar)bars.get(bars.size() - 1)).getTime() == currentBar.getTime())
/*      */         {
/*  576 */           bars.remove(bars.size() - 1);
/*  577 */           bars.add(currentBar);
/*      */         }
/*      */       }
/*  580 */       if (bars.size() < numberOfCandlesBefore + numberOfCandlesAfter) {
/*  581 */         if (currentBar != null)
/*      */         {
/*  583 */           if ((time == currentBar.getTime()) && (numberOfCandlesBefore != 0) && (((bars.isEmpty()) || (((IBar)bars.get(bars.size() - 1)).getTime() < currentBar.getTime()))))
/*  584 */             bars.add(currentBar);
/*  585 */           else if ((time == currentBar.getTime()) && (numberOfCandlesBefore == 0) && (numberOfCandlesAfter > 0) && (bars.isEmpty()))
/*  586 */             bars.add(currentBar);
/*  587 */           else if ((time < currentBar.getTime()) && (numberOfCandlesAfter > 0) && (((bars.isEmpty()) || (((IBar)bars.get(bars.size() - 1)).getTime() < currentBar.getTime()))) && 
/*  589 */             (((IBar)bars.get(0)).getTime() != this.feedDataProvider.getTimeOfFirstCandle(instrument, period))) {
/*  590 */             bars.add(currentBar);
/*      */           }
/*      */         }
/*      */       }
/*  594 */       else if ((currentBar != null) && (time == currentBar.getTime())) {
/*  595 */         IBar lastBar = (IBar)bars.get(bars.size() - 1);
/*  596 */         if (lastBar.getTime() != currentBar.getTime())
/*      */         {
/*  598 */           bars.remove(0);
/*  599 */           bars.add(currentBar);
/*      */         }
/*      */       }
/*  602 */       if (result[0] == 2) {
/*  603 */         throw new JFException("Error while loading bars", exceptions[0]);
/*      */       }
/*  605 */       return bars;
/*      */     }
/*      */     catch (DataCacheException e) {
/*  608 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readOrdersHistory(final Instrument instrument, final long from, final long to, final LoadingOrdersListener ordersListener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/*  615 */     if (from > to) {
/*  616 */       throw new JFException("Interval from [" + DATE_FORMAT.format(new Date(from)) + "] to [" + DATE_FORMAT.format(new Date(to)) + "] GMT is not valid");
/*      */     }
/*  618 */     if (this.ordersHistoryRequestSent.compareAndSet(false, true))
/*      */       try {
/*      */         try {
/*  621 */           AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */             public Object run() throws Exception {
/*  623 */               readOrdersHistorySecured(instrument, from, to, ordersListener, loadingProgress);
/*  624 */               return null;
/*      */             } } );
/*      */         }
/*      */         catch (PrivilegedActionException e) {
/*  628 */           Exception ex = e.getException();
/*  629 */           if (ex instanceof JFException)
/*  630 */             throw ((JFException)ex);
/*  631 */           if (ex instanceof RuntimeException) {
/*  632 */             throw ((RuntimeException)ex);
/*      */           }
/*  634 */           LOGGER.error(ex.getMessage(), ex);
/*  635 */           throw new JFException(ex);
/*      */         }
/*      */       }
/*      */       finally {
/*  639 */         this.ordersHistoryRequestSent.set(false);
/*      */       }
/*      */     else
/*  642 */       throw new JFException("Only one request for orders history can be sent at one time");
/*      */   }
/*      */ 
/*      */   private void readOrdersHistorySecured(Instrument instrument, long from, long to, LoadingOrdersListener ordersListener, com.dukascopy.api.LoadingProgressListener loadingProgress) throws JFException
/*      */   {
/*      */     try {
/*  648 */       this.feedDataProvider.loadOrdersHistoricalData(instrument, from, to, new LoadingOrdersListenerWrapper(ordersListener, from, to), new LoadingProgressListenerWrapper(loadingProgress, true));
/*      */     } catch (DataCacheException e) {
/*  650 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public List<IOrder> getOrdersHistory(final Instrument instrument, final long from, final long to) throws JFException
/*      */   {
/*  656 */     if (from > to) {
/*  657 */       throw new JFException("Interval from [" + DATE_FORMAT.format(new Date(from)) + "] to [" + DATE_FORMAT.format(new Date(to)) + "] GMT is not valid");
/*      */     }
/*  659 */     if (this.ordersHistoryRequestSent.compareAndSet(false, true)) {
/*      */       try
/*      */       {
/*  662 */         List localList = (List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */           public List<IOrder> run() throws Exception {
/*  664 */             return History.this.getOrdersHistorySecured(instrument, from, to);
/*      */           }
/*      */         });
/*  679 */         return localList;
/*      */       }
/*      */       catch (PrivilegedActionException e)
/*      */       {
/*  668 */         Exception ex = e.getException();
/*  669 */         if (ex instanceof JFException)
/*  670 */           throw ((JFException)ex);
/*  671 */         if (ex instanceof RuntimeException) {
/*  672 */           throw ((RuntimeException)ex);
/*      */         }
/*      */ 
/*  675 */         throw new JFException(ex);
/*      */       }
/*      */       finally
/*      */       {
/*  679 */         this.ordersHistoryRequestSent.set(false);
/*      */       }
/*      */     }
/*  682 */     throw new JFException("Only one request for orders history can be sent at one time");
/*      */   }
/*      */ 
/*      */   protected List<IOrder> getOrdersHistorySecured(Instrument instrument, final long from, final long to) throws JFException
/*      */   {
/*      */     try {
/*  688 */       final List orders = new ArrayList();
/*  689 */       final int[] result = { 0 };
/*  690 */       final Exception[] exceptions = new Exception[1];
/*  691 */       com.dukascopy.charts.data.datacache.LoadingProgressListener loadingProgressListener = new com.dukascopy.charts.data.datacache.LoadingProgressListener() {
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  696 */           result[0] = ((allDataLoaded) ? 1 : 2);
/*  697 */           exceptions[0] = e;
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  701 */           return false;
/*      */         }
/*      */       };
/*  704 */       this.feedDataProvider.loadOrdersHistoricalDataSynched(instrument, from, to, new OrdersListener()
/*      */       {
/*      */         public void newOrder(Instrument instrument, OrderHistoricalData orderData) {
/*  707 */           if (!(orderData.isClosed())) {
/*  708 */             return;
/*      */           }
/*  710 */           HistoryOrder order = processOrders(instrument, orderData, from, to);
/*  711 */           if (order != null)
/*  712 */             orders.add(order);
/*      */         }
/*      */ 
/*      */         public void orderChange(Instrument instrument, OrderHistoricalData orderData)
/*      */         {
/*      */         }
/*      */ 
/*      */         public void orderMerge(Instrument instrument, OrderHistoricalData resultingOrderData, List<OrderHistoricalData> mergedOrdersData)
/*      */         {
/*      */         }
/*      */ 
/*      */         public void ordersInvalidated(Instrument instrument)
/*      */         {
/*      */         }
/*      */       }
/*      */       , loadingProgressListener);
/*      */ 
/*  729 */       if (result[0] == 2) {
/*  730 */         throw new JFException("Error while loading orders history", exceptions[0]);
/*      */       }
/*  732 */       return orders;
/*      */     }
/*      */     catch (DataCacheException e) {
/*  735 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   protected HistoryOrder processOrders(Instrument instrument, OrderHistoricalData orderData, long from, long to) {
/*  740 */     long openTime = orderData.getEntryOrder().getFillTime();
/*  741 */     OrderHistoricalData.CloseData[] closeDatas = (OrderHistoricalData.CloseData[])orderData.getCloseDataMap().values().toArray(new OrderHistoricalData.CloseData[orderData.getCloseDataMap().size()]);
/*  742 */     BigDecimal closePriceMulAmount = BigDecimal.ZERO;
/*  743 */     BigDecimal sumClosedAmounts = BigDecimal.ZERO;
/*  744 */     long closeTime = (orderData.getMergedToTime() == -9223372036854775808L) ? orderData.getHistoryEnd() : orderData.getMergedToTime();
/*  745 */     for (OrderHistoricalData.CloseData closeData : closeDatas) {
/*  746 */       closeTime = closeData.getCloseTime();
/*  747 */       closePriceMulAmount = closePriceMulAmount.add(closeData.getClosePrice().multiply(closeData.getAmount()));
/*  748 */       sumClosedAmounts = sumClosedAmounts.add(closeData.getAmount());
/*      */     }
/*  750 */     if ((to >= openTime) && (from <= closeTime)) {
/*  751 */       double closePrice = 0.0D;
/*  752 */       if (sumClosedAmounts.compareTo(BigDecimal.ZERO) != 0) {
/*  753 */         closePrice = closePriceMulAmount.divide(sumClosedAmounts, 7, 6).doubleValue();
/*      */       }
/*  755 */       return createHistoryOrder(instrument, orderData, closeTime, closePrice);
/*      */     }
/*  757 */     return null;
/*      */   }
/*      */ 
/*      */   protected HistoryOrder createHistoryOrder(Instrument instrument, OrderHistoricalData orderData, long closeTime, double closePrice) {
/*  761 */     OrderHistoricalData.OpenData entryOrder = orderData.getEntryOrder();
/*  762 */     return new HistoryOrder(instrument, entryOrder.getLabel(), orderData.getOrderGroupId(), entryOrder.getFillTime(), closeTime, entryOrder.getSide(), entryOrder.getAmount().divide(ONE_MILLION).doubleValue(), entryOrder.getOpenPrice().doubleValue(), closePrice, entryOrder.getComment(), this.accountCurrency, orderData.getCommission().doubleValue());
/*      */   }
/*      */ 
/*      */   public boolean isIntervalValid(Period period, long from, long to)
/*      */   {
/*  768 */     boolean ret = from <= to;
/*  769 */     if ((ret) && (period != Period.TICK)) {
/*      */       try {
/*  771 */         ret = (ret) && (DataCacheUtils.isIntervalValid(period, from, to));
/*      */       } catch (DataCacheException e) {
/*  773 */         LOGGER.debug(e.getMessage(), e);
/*  774 */         ret = false;
/*      */       }
/*      */     }
/*  777 */     return ret;
/*      */   }
/*      */ 
/*      */   protected boolean isIntervalValid(Period period, int before, long time, int after) throws JFException {
/*  781 */     return ((period != Period.TICK) && (DataCacheUtils.getCandleStartFast(period, time) == time) && (((before > 0) || (after > 0))));
/*      */   }
/*      */ 
/*      */   public long getBarStart(Period period, long time) throws JFException {
/*      */     try {
/*  786 */       return DataCacheUtils.getCandleStart(period, time);
/*      */     } catch (DataCacheException e) {
/*  788 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public long getNextBarStart(Period period, long barTime) throws JFException {
/*      */     try {
/*  794 */       return DataCacheUtils.getNextCandleStart(period, barTime);
/*      */     } catch (DataCacheException e) {
/*  796 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public long getPreviousBarStart(Period period, long barTime) throws JFException {
/*      */     try {
/*  802 */       return DataCacheUtils.getPreviousCandleStart(period, barTime);
/*      */     } catch (DataCacheException e) {
/*  804 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public long getTimeForNBarsBack(Period period, long to, int numberOfBars) throws JFException {
/*      */     try {
/*  810 */       return DataCacheUtils.getTimeForNCandlesBack(period, to, numberOfBars);
/*      */     } catch (DataCacheException e) {
/*  812 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public long getTimeForNBarsForward(Period period, long from, int numberOfBars) throws JFException {
/*      */     try {
/*  818 */       return DataCacheUtils.getTimeForNCandlesForward(period, from, numberOfBars);
/*      */     } catch (DataCacheException e) {
/*  820 */       throw new JFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public boolean isInstrumentSubscribed(Instrument instrument) {
/*  825 */     return this.feedDataProvider.isSubscribedToInstrument(instrument);
/*      */   }
/*      */ 
/*      */   public double getEquity()
/*      */   {
/*  830 */     return this.ordersProvider.recalculateEquity();
/*      */   }
/*      */ 
/*      */   public List<IPointAndFigure> getPointAndFigures(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final ReversalAmount reversalAmount, long from, long to)
/*      */     throws JFException
/*      */   {
/*  922 */     HistoryUtils.validatePointAndFigureParams(instrument, offerSide, priceRange, reversalAmount);
/*  923 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */ 
/*  925 */     HistoryUtils.checkInProgressPointAndFigureIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange, reversalAmount);
/*      */ 
/*  927 */     final long correctedFrom = HistoryUtils.correctRequestTime(from, priceRange, reversalAmount);
/*  928 */     final long correctedTo = HistoryUtils.correctRequestTime(to, priceRange, reversalAmount);
/*      */     try
/*      */     {
/*  931 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<IPointAndFigure> run() throws Exception {
/*  933 */           List data = History.this.feedDataProvider.loadPointAndFigureTimeInterval(instrument, offerSide, priceRange, reversalAmount, correctedFrom, correctedTo);
/*  934 */           List result = HistoryUtils.convert(data);
/*  935 */           return result;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  939 */       HistoryUtils.throwJFException(e); }
/*  940 */     return null;
/*      */   }
/*      */ 
/*      */   public List<ITickBar> getTickBars(final Instrument instrument, final OfferSide offerSide, final TickBarSize tickBarSize, final long from, final long to)
/*      */     throws JFException
/*      */   {
/*  954 */     HistoryUtils.validateTickBarParams(instrument, offerSide, tickBarSize);
/*  955 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */ 
/*  957 */     HistoryUtils.checkInProgressTickBarIsLoaded(this.feedDataProvider, instrument, offerSide, tickBarSize);
/*      */     try
/*      */     {
/*  960 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<ITickBar> run() throws Exception {
/*  962 */           List data = feedDataProvider.loadTickBarTimeInterval(instrument, offerSide, tickBarSize, from, to);
/*  963 */           List result = HistoryUtils.convert(data);
/*  964 */           return result;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/*  968 */       HistoryUtils.throwJFException(e); }
/*  969 */     return null;
/*      */   }
/*      */ 
/*      */   public List<IRangeBar> getRangeBars(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, long from, long to)
/*      */     throws JFException
/*      */   {
/*  982 */     HistoryUtils.validateRangeBarParams(instrument, offerSide, priceRange);
/*  983 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */ 
/*  985 */     HistoryUtils.checkInProgressRangeBarIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange);
/*      */ 
/*  987 */     final long correctedFrom = HistoryUtils.correctRequestTime(from, priceRange);
/*  988 */     final long correctedTo = HistoryUtils.correctRequestTime(to, priceRange);
/*      */     try
/*      */     {
/*  991 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<IRangeBar> run() throws Exception {
/*  993 */           List data = History.this.feedDataProvider.loadPriceRangeTimeInterval(instrument, offerSide, priceRange, correctedFrom, correctedTo);
/*  994 */           List result = HistoryUtils.convert(data);
/*      */ 
/*  996 */           return result;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1000 */       HistoryUtils.throwJFException(e); }
/* 1001 */     return null;
/*      */   }
/*      */ 
/*      */   public List<IPointAndFigure> getPointAndFigures(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final ReversalAmount reversalAmount, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter)
/*      */     throws JFException
/*      */   {
/* 1016 */     HistoryUtils.validatePointAndFigureParams(instrument, offerSide, priceRange, reversalAmount);
/* 1017 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */ 
/* 1019 */     HistoryUtils.checkInProgressPointAndFigureIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange, reversalAmount);
/*      */     try
/*      */     {
/* 1022 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<IPointAndFigure> run() throws Exception {
/* 1024 */           List data = History.this.feedDataProvider.loadPointAndFigureData(instrument, offerSide, time, numberOfBarsBefore, numberOfBarsAfter, priceRange, reversalAmount);
/*      */ 
/* 1034 */           List result = HistoryUtils.convert(data);
/* 1035 */           return result;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1039 */       HistoryUtils.throwJFException(e); }
/* 1040 */     return null;
/*      */   }
/*      */ 
/*      */   public List<IRangeBar> getRangeBars(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter)
/*      */     throws JFException
/*      */   {
/* 1055 */     HistoryUtils.validateRangeBarParams(instrument, offerSide, priceRange);
/* 1056 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */ 
/* 1058 */     HistoryUtils.checkInProgressRangeBarIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange);
/*      */     try
/*      */     {
/* 1061 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<IRangeBar> run() throws Exception {
/* 1063 */           List data = History.this.feedDataProvider.loadPriceRangeData(instrument, offerSide, numberOfBarsBefore, time, numberOfBarsAfter, priceRange);
/*      */ 
/* 1072 */           List result = HistoryUtils.convert(data);
/* 1073 */           return result;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1077 */       HistoryUtils.throwJFException(e); }
/* 1078 */     return null;
/*      */   }
/*      */ 
/*      */   public List<ITickBar> getTickBars(final Instrument instrument, final OfferSide offerSide, final TickBarSize tickBarSize, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter)
/*      */     throws JFException
/*      */   {
/* 1093 */     HistoryUtils.validateTickBarParams(instrument, offerSide, tickBarSize);
/* 1094 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */ 
/* 1096 */     HistoryUtils.checkInProgressTickBarIsLoaded(this.feedDataProvider, instrument, offerSide, tickBarSize);
/*      */     try
/*      */     {
/* 1099 */       return ((List)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public List<ITickBar> run() throws Exception {
/* 1101 */           List data = History.this.feedDataProvider.loadTickBarData(instrument, offerSide, numberOfBarsBefore, time, numberOfBarsAfter, tickBarSize);
/*      */ 
/* 1110 */           List result = HistoryUtils.convert(data);
/* 1111 */           return result;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1115 */       HistoryUtils.throwJFException(e); }
/* 1116 */     return null;
/*      */   }
/*      */ 
/*      */   public void readPointAndFigures(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final ReversalAmount reversalAmount, long from, long to, final IPointAndFigureFeedListener listener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/* 1132 */     HistoryUtils.validatePointAndFigureParams(instrument, offerSide, priceRange, reversalAmount, listener, loadingProgress);
/* 1133 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */ 
/* 1135 */     HistoryUtils.checkInProgressPointAndFigureIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange, reversalAmount);
/*      */ 
/* 1137 */     final long correctedFrom = HistoryUtils.correctRequestTime(from, priceRange, reversalAmount);
/* 1138 */     final long correctedTo = HistoryUtils.correctRequestTime(to, priceRange, reversalAmount);
/*      */     try
/*      */     {
/* 1141 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Void run() throws Exception {
/* 1143 */           feedDataProvider.loadPointAndFigureTimeInterval(instrument, offerSide, priceRange, reversalAmount, correctedFrom, correctedTo, new PointAndFigureLiveFeedAdapter()
/*      */           {
/*      */             public void newPriceData(PointAndFigureData pointAndFigure)
/*      */             {
/* 1153 */               listener.onBar(instrument, offerSide, priceRange, reversalAmount, pointAndFigure);
/*      */             }
/*      */           }
/*      */           , new LoadingProgressListenerWrapper(loadingProgress, false));
/*      */ 
/* 1158 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1162 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readPointAndFigures(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final ReversalAmount reversalAmount, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter, final IPointAndFigureFeedListener listener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/* 1179 */     HistoryUtils.validatePointAndFigureParams(instrument, offerSide, priceRange, reversalAmount, listener, loadingProgress);
/* 1180 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */ 
/* 1182 */     HistoryUtils.checkInProgressPointAndFigureIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange, reversalAmount);
/*      */     try
/*      */     {
/* 1185 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Void run() throws Exception {
/* 1187 */           History.this.feedDataProvider.loadPointAndFigureData(instrument, offerSide, numberOfBarsBefore, time, numberOfBarsAfter, priceRange, reversalAmount, new PointAndFigureLiveFeedAdapter()
/*      */           {
/*      */             public void newPriceData(PointAndFigureData pointAndFigure)
/*      */             {
/* 1198 */               listener.onBar(instrument, offerSide, priceRange, reversalAmount, pointAndFigure);
/*      */             }
/*      */           }
/*      */           , new History.LoadingProgressListenerWrapper(loadingProgress, false));
/*      */ 
/* 1203 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1207 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readTickBars(final Instrument instrument, final OfferSide offerSide, final TickBarSize tickBarSize, final long from, final long to, final ITickBarFeedListener listener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/* 1223 */     HistoryUtils.validateTickBarParams(instrument, offerSide, tickBarSize, listener, loadingProgress);
/* 1224 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */ 
/* 1226 */     HistoryUtils.checkInProgressTickBarIsLoaded(this.feedDataProvider, instrument, offerSide, tickBarSize);
/*      */     try
/*      */     {
/* 1229 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Void run() throws Exception {
/* 1231 */           History.this.feedDataProvider.loadTickBarTimeInterval(instrument, offerSide, tickBarSize, from, to, new TickBarLiveFeedAdapter()
/*      */           {
/*      */             public void newPriceData(TickBarData tickBar)
/*      */             {
/* 1240 */               listener.onBar(instrument, offerSide, tickBarSize, tickBar);
/*      */             }
/*      */           }
/*      */           , new History.LoadingProgressListenerWrapper(loadingProgress, false));
/*      */ 
/* 1245 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1249 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readTickBars(final Instrument instrument, final OfferSide offerSide, final TickBarSize tickBarSize, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter, final ITickBarFeedListener listener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/* 1265 */     HistoryUtils.validateTickBarParams(instrument, offerSide, tickBarSize, listener, loadingProgress);
/* 1266 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */ 
/* 1268 */     HistoryUtils.checkInProgressTickBarIsLoaded(this.feedDataProvider, instrument, offerSide, tickBarSize);
/*      */     try
/*      */     {
/* 1271 */       AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public Void run() throws Exception {
/* 1273 */           History.this.feedDataProvider.loadTickBarData(instrument, offerSide, numberOfBarsBefore, time, numberOfBarsAfter, tickBarSize, new TickBarLiveFeedAdapter()
/*      */           {
/*      */             public void newPriceData(TickBarData bar)
/*      */             {
/* 1283 */               listener.onBar(instrument, offerSide, tickBarSize, bar);
/*      */             }
/*      */           }
/*      */           , new History.LoadingProgressListenerWrapper(loadingProgress, false));
/*      */ 
/* 1288 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1292 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readRangeBars(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final long from, final long to, final IRangeBarFeedListener listener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/* 1307 */     HistoryUtils.validateRangeBarParams(instrument, offerSide, priceRange, listener, loadingProgress);
/* 1308 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */ 
/* 1310 */     HistoryUtils.checkInProgressRangeBarIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange);
/*      */ 
/* 1312 */     final long correctedFrom = HistoryUtils.correctRequestTime(from, priceRange);
/* 1313 */     final long correctedTo = HistoryUtils.correctRequestTime(to, priceRange);
/*      */     try
/*      */     {
/* 1316 */       AccessController.doPrivileged(new PrivilegedExceptionAction()
/*      */       {
/*      */         public Void run() throws Exception {
/* 1319 */           History.this.feedDataProvider.loadPriceRangeTimeInterval(instrument, offerSide, priceRange, correctedFrom, correctedTo, new PriceRangeLiveFeedAdapter()
/*      */           {
/*      */             public void newPriceData(PriceRangeData bar)
/*      */             {
/* 1328 */               listener.onBar(instrument, offerSide, priceRange, bar);
/*      */             }
/*      */           }
/*      */           , new History.LoadingProgressListenerWrapper(loadingProgress, false));
/*      */ 
/* 1334 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1338 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void readRangeBars(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final int numberOfBarsBefore, final long time, final int numberOfBarsAfter, final IRangeBarFeedListener listener, final com.dukascopy.api.LoadingProgressListener loadingProgress)
/*      */     throws JFException
/*      */   {
/* 1355 */     HistoryUtils.validateRangeBarParams(instrument, offerSide, priceRange, listener, loadingProgress);
/* 1356 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */ 
/* 1358 */     HistoryUtils.checkInProgressRangeBarIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange);
/*      */     try
/*      */     {
/* 1361 */       AccessController.doPrivileged(new PrivilegedExceptionAction()
/*      */       {
/*      */         public Void run() throws Exception {
/* 1364 */           History.this.feedDataProvider.loadPriceRangeData(instrument, offerSide, numberOfBarsBefore, time, numberOfBarsAfter, priceRange, new PriceRangeLiveFeedAdapter()
/*      */           {
/*      */             public void newPriceData(PriceRangeData bar)
/*      */             {
/* 1374 */               listener.onBar(instrument, offerSide, priceRange, bar);
/*      */             }
/*      */           }
/*      */           , new History.LoadingProgressListenerWrapper(loadingProgress, false));
/*      */ 
/* 1380 */           return null;
/*      */         } } );
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1384 */       HistoryUtils.throwJFException(e);
/*      */     }
/*      */   }
/*      */ 
/*      */   private ITick getTickSecured(final Instrument instrument, final int shift) throws JFException
/*      */   {
/* 1390 */     ITick lastTick = this.feedDataProvider.getLastTick(instrument);
/* 1391 */     if (lastTick == null) {
/* 1392 */       return null;
/*      */     }
/* 1394 */     if (shift == 0) {
/* 1395 */       return lastTick;
/*      */     }
/*      */     try
/*      */     {
/* 1399 */       HistoryUtils.Loadable loadable = new HistoryUtils.Loadable()
/*      */       {
/*      */         public List<ITick> load(long from, long to) throws Exception {
/* 1402 */           final List bars = new ArrayList(3600);
/* 1403 */           History.this.feedDataProvider.loadTicksDataSynched(instrument, from, to, new LiveFeedListener()
/*      */           {
/*      */             public void newTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol)
/*      */             {
/* 1410 */               bars.add(new TickData(time, ask, bid, askVol, bidVol));
/*      */             }
/*      */ 
/*      */             public void newCandle(Instrument instrument, Period period, OfferSide side, long time, double open, double close, double low, double high, double vol)
/*      */             {
/*      */             }
/*      */           }
/*      */           , new LoadingProgressAdapter()
/*      */           {
/*      */           });
/* 1418 */           return bars;
/*      */         }
/*      */ 
/*      */         public long correctTime(long time) {
/* 1422 */           return time;
/*      */         }
/*      */ 
/*      */         public long getStep() {
/* 1426 */           return 3600000L;
/*      */         }
/*      */       };
/* 1430 */       return ((ITick)HistoryUtils.getByShift(loadable, lastTick.getTime(), shift));
/*      */     }
/*      */     catch (Throwable t)
/*      */     {
/* 1436 */       throw new JFException(t);
/*      */     }
/*      */   }
/*      */ 
/*      */   public ITick getTick(final Instrument instrument, final int shift)
/*      */     throws JFException
/*      */   {
/* 1443 */     if (instrument == null) {
/* 1444 */       throw new JFException("Instrument is null");
/*      */     }
/*      */ 
/* 1447 */     HistoryUtils.validateShift(shift);
/*      */     try
/*      */     {
/* 1450 */       return ((ITick)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public ITick run() throws Exception {
/* 1452 */           ITick tick = History.this.getTickSecured(instrument, shift);
/* 1453 */           return tick;
/*      */         } } ));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1457 */       HistoryUtils.throwJFException(e); }
/* 1458 */     return null;
/*      */   }
/*      */ 
/*      */   public IPointAndFigure getPointAndFigure(final Instrument instrument, final OfferSide offerSide, final PriceRange boxSize, final ReversalAmount reversalAmount, final int shift)
/*      */     throws JFException
/*      */   {
/* 1472 */     HistoryUtils.validatePointAndFigureParams(instrument, offerSide, boxSize, reversalAmount);
/* 1473 */     HistoryUtils.validateShift(shift);
/*      */ 
/* 1475 */     HistoryUtils.checkInProgressPointAndFigureIsLoaded(this.feedDataProvider, instrument, offerSide, boxSize, reversalAmount);
/*      */     try
/*      */     {
/* 1478 */       return ((IPointAndFigure)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public IPointAndFigure run() throws Exception {
/* 1480 */           IPointAndFigure inProgressBar = History.this.feedDataProvider.getInProgressPointAndFigure(instrument, offerSide, boxSize, reversalAmount);
/* 1481 */           if (inProgressBar == null) {
/* 1482 */             return null;
/*      */           }
/* 1484 */           if (shift == 0) {
/* 1485 */             return inProgressBar;
/*      */           }
/*      */ 
/* 1488 */           HistoryUtils.Loadable loadable = new HistoryUtils.Loadable()
/*      */           {
/*      */             public List<PointAndFigureData> load(long from, long to) throws Exception {
/* 1491 */               List result = History.this.feedDataProvider.loadPointAndFigureTimeInterval(instrument, offerSide, boxSize, reversalAmount, from, to);
/*      */ 
/* 1499 */               return result;
/*      */             }
/*      */ 
/*      */             public long correctTime(long time) {
/* 1503 */               return HistoryUtils.correctRequestTime(time, boxSize, reversalAmount);
/*      */             }
/*      */ 
/*      */             public long getStep() {
/* 1507 */               return 86400000L;
/*      */             }
/*      */           };
/* 1510 */           return ((IPointAndFigure)HistoryUtils.getByShift(loadable, inProgressBar.getTime(), shift));
/*      */         }
/*      */       }));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1515 */       HistoryUtils.throwJFException(e); }
/* 1516 */     return null;
/*      */   }
/*      */ 
/*      */   public ITickBar getTickBar(final Instrument instrument, final OfferSide offerSide, final TickBarSize tickBarSize, final int shift)
/*      */     throws JFException
/*      */   {
/* 1529 */     HistoryUtils.validateTickBarParams(instrument, offerSide, tickBarSize);
/* 1530 */     HistoryUtils.validateShift(shift);
/*      */ 
/* 1532 */     HistoryUtils.checkInProgressTickBarIsLoaded(this.feedDataProvider, instrument, offerSide, tickBarSize);
/*      */     try
/*      */     {
/* 1535 */       return ((ITickBar)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public ITickBar run() throws Exception {
/* 1537 */           ITickBar inProgressBar = History.this.feedDataProvider.getInProgressTickBar(instrument, offerSide, tickBarSize);
/* 1538 */           if (inProgressBar == null) {
/* 1539 */             return null;
/*      */           }
/* 1541 */           if (shift == 0) {
/* 1542 */             return inProgressBar;
/*      */           }
/*      */ 
/* 1545 */           HistoryUtils.Loadable loadable = new HistoryUtils.Loadable()
/*      */           {
/*      */             public List<TickBarData> load(long from, long to) throws Exception {
/* 1548 */               List result = History.this.feedDataProvider.loadTickBarTimeInterval(instrument, offerSide, tickBarSize, from, to);
/*      */ 
/* 1555 */               return result;
/*      */             }
/*      */ 
/*      */             public long correctTime(long time) {
/* 1559 */               return time;
/*      */             }
/*      */ 
/*      */             public long getStep() {
/* 1563 */               return (tickBarSize.getSize() * 60 * 60 * 1000);
/*      */             }
/*      */           };
/* 1566 */           return ((ITickBar)HistoryUtils.getByShift(loadable, inProgressBar.getTime(), shift));
/*      */         }
/*      */       }));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1571 */       HistoryUtils.throwJFException(e); }
/* 1572 */     return null;
/*      */   }
/*      */ 
/*      */   public IRangeBar getRangeBar(final Instrument instrument, final OfferSide offerSide, final PriceRange priceRange, final int shift)
/*      */     throws JFException
/*      */   {
/* 1583 */     HistoryUtils.validateRangeBarParams(instrument, offerSide, priceRange);
/* 1584 */     HistoryUtils.validateShift(shift);
/*      */ 
/* 1586 */     HistoryUtils.checkInProgressRangeBarIsLoaded(this.feedDataProvider, instrument, offerSide, priceRange);
/*      */     try
/*      */     {
/* 1589 */       return ((IRangeBar)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*      */         public IRangeBar run() throws Exception {
/* 1591 */           IRangeBar inProgressBar = History.this.feedDataProvider.getInProgressPriceRange(instrument, offerSide, priceRange);
/* 1592 */           if (inProgressBar == null) {
/* 1593 */             return null;
/*      */           }
/* 1595 */           if (shift == 0) {
/* 1596 */             return inProgressBar;
/*      */           }
/*      */ 
/* 1599 */           HistoryUtils.Loadable loadable = new HistoryUtils.Loadable()
/*      */           {
/*      */             public List<PriceRangeData> load(long from, long to) throws Exception {
/* 1602 */               List result = History.this.feedDataProvider.loadPriceRangeTimeInterval(instrument, offerSide, priceRange, from, to);
/*      */ 
/* 1609 */               return result;
/*      */             }
/*      */ 
/*      */             public long correctTime(long time) {
/* 1613 */               return HistoryUtils.correctRequestTime(time, priceRange);
/*      */             }
/*      */ 
/*      */             public long getStep() {
/* 1617 */               return 86400000L;
/*      */             }
/*      */           };
/* 1620 */           return ((IRangeBar)HistoryUtils.getByShift(loadable, inProgressBar.getTime(), shift));
/*      */         }
/*      */       }));
/*      */     }
/*      */     catch (PrivilegedActionException e) {
/* 1625 */       HistoryUtils.throwJFException(e); }
/* 1626 */     return null;
/*      */   }
/*      */ 
/*      */   public void validateTimeInterval(Instrument instrument, long from, long to) throws JFException
/*      */   {
/* 1631 */     HistoryUtils.validateTimeInterval(this.feedDataProvider, instrument, from, to);
/*      */   }
/*      */ 
/*      */   public void validateBeforeTimeAfter(Instrument instrument, int numberOfBarsBefore, long time, int numberOfBarsAfter)
/*      */     throws JFException
/*      */   {
/* 1640 */     HistoryUtils.validateBeforeTimeAfter(this.feedDataProvider, instrument, numberOfBarsBefore, time, numberOfBarsAfter);
/*      */   }
/*      */ 
/*      */   protected class LoadingProgressListenerWrapper
/*      */     implements com.dukascopy.charts.data.datacache.LoadingProgressListener
/*      */   {
/*      */     private com.dukascopy.api.LoadingProgressListener loadingProgressListener;
/*      */     private boolean ordersHistoryRequest;
/*      */ 
/*      */     public LoadingProgressListenerWrapper(com.dukascopy.api.LoadingProgressListener paramLoadingProgressListener, boolean paramBoolean)
/*      */     {
/*  889 */       this.loadingProgressListener = paramLoadingProgressListener;
/*  890 */       this.ordersHistoryRequest = ordersHistoryRequest;
/*      */     }
/*      */ 
/*      */     public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  894 */       this.loadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */     }
/*      */ 
/*      */     public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  898 */       if (e != null) {
/*  899 */         History.LOGGER.error(e.getMessage(), e);
/*      */       }
/*  901 */       this.loadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, currentTime);
/*  902 */       if (this.ordersHistoryRequest)
/*  903 */         History.this.ordersHistoryRequestSent.set(false);
/*      */     }
/*      */ 
/*      */     public boolean stopJob()
/*      */     {
/*  908 */       return this.loadingProgressListener.stopJob();
/*      */     }
/*      */   }
/*      */ 
/*      */   private static class LiveFeedListenerWrapper
/*      */     implements LiveFeedListener
/*      */   {
/*      */     private LoadingDataListener loadingDataListener;
/*      */ 
/*      */     public LiveFeedListenerWrapper(LoadingDataListener loadingDataListener)
/*      */     {
/*  872 */       this.loadingDataListener = loadingDataListener;
/*      */     }
/*      */ 
/*      */     public void newCandle(Instrument instrument, Period period, OfferSide side, long time, double open, double close, double low, double high, double vol) {
/*  876 */       this.loadingDataListener.newBar(instrument, period, side, time, open, close, low, high, vol);
/*      */     }
/*      */ 
/*      */     public void newTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol) {
/*  880 */       this.loadingDataListener.newTick(instrument, time, ask, bid, askVol, bidVol);
/*      */     }
/*      */   }
/*      */ 
/*      */   protected class LoadingOrdersListenerWrapper
/*      */     implements OrdersListener
/*      */   {
/*      */     private LoadingOrdersListener listener;
/*      */     private long from;
/*      */     private long to;
/*      */ 
/*      */     public LoadingOrdersListenerWrapper(LoadingOrdersListener paramLoadingOrdersListener, long paramLong1, long paramLong2)
/*      */     {
/*  839 */       this.listener = paramLoadingOrdersListener;
/*  840 */       this.from = paramLong1;
/*  841 */       this.to = paramLong2;
/*      */     }
/*      */ 
/*      */     public void newOrder(Instrument instrument, OrderHistoricalData orderData)
/*      */     {
/*  846 */       if (!(orderData.isClosed())) {
/*  847 */         return;
/*      */       }
/*  849 */       HistoryOrder order = History.this.processOrders(instrument, orderData, this.from, this.to);
/*  850 */       if (order != null)
/*  851 */         this.listener.newOrder(instrument, order);
/*      */     }
/*      */ 
/*      */     public void orderChange(Instrument instrument, OrderHistoricalData orderData)
/*      */     {
/*      */     }
/*      */ 
/*      */     public void orderMerge(Instrument instrument, OrderHistoricalData resultingOrderData, List<OrderHistoricalData> mergedOrdersData)
/*      */     {
/*      */     }
/*      */ 
/*      */     public void ordersInvalidated(Instrument instrument)
/*      */     {
/*      */     }
/*      */   }
/*      */ }

/* Location:           C:\Projects\jforex\libs\greed-common-162.jar
 * Qualified Name:     com.dukascopy.api.impl.History
 * Java Class Version: 6 (50.0)
 * JD-Core Version:    0.5.3
 */