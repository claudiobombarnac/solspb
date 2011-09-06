/*      */ package solspb.jforex;
/*      */ 
/*      */ import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import artist.api.ContextLoader;
import artist.api.IContext;
import artist.api.IDataQueue;
import artist.api.beans.Quote;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.PriceRange;
import com.dukascopy.api.ReversalAmount;
import com.dukascopy.api.TickBarSize;
import com.dukascopy.api.impl.connect.AuthorizationClient;
import com.dukascopy.charts.data.datacache.CacheDataUpdatedListener;
import com.dukascopy.charts.data.datacache.CandleData;
import com.dukascopy.charts.data.datacache.CurvesDataLoader;
import com.dukascopy.charts.data.datacache.DataCacheException;
import com.dukascopy.charts.data.datacache.DataCacheUtils;
import com.dukascopy.charts.data.datacache.IAuthenticator;
import com.dukascopy.charts.data.datacache.ICurvesProtocolHandler;
import com.dukascopy.charts.data.datacache.IFeedDataProvider;
import com.dukascopy.charts.data.datacache.IFeedInfo;
import com.dukascopy.charts.data.datacache.InstrumentSubscriptionListener;
import com.dukascopy.charts.data.datacache.IntraPeriodCandleData;
import com.dukascopy.charts.data.datacache.IntraperiodCandlesGenerator;
import com.dukascopy.charts.data.datacache.IsDataCachedAction;
import com.dukascopy.charts.data.datacache.LiveCandleListener;
import com.dukascopy.charts.data.datacache.LiveFeedListener;
import com.dukascopy.charts.data.datacache.LoadDataAction;
import com.dukascopy.charts.data.datacache.LoadInProgressCandleDataAction;
import com.dukascopy.charts.data.datacache.LoadLastAvailableDataAction;
import com.dukascopy.charts.data.datacache.LoadNumberOfCandlesAction;
import com.dukascopy.charts.data.datacache.LoadNumberOfLastAvailableDataAction;
import com.dukascopy.charts.data.datacache.LoadOrdersAction;
import com.dukascopy.charts.data.datacache.LoadProgressingAction;
import com.dukascopy.charts.data.datacache.LoadingProgressAdapter;
import com.dukascopy.charts.data.datacache.LoadingProgressListener;
import com.dukascopy.charts.data.datacache.LocalCacheManager;
import com.dukascopy.charts.data.datacache.OrderHistoricalData;
import com.dukascopy.charts.data.datacache.OrdersListener;
import com.dukascopy.charts.data.datacache.TickData;
import com.dukascopy.charts.data.datacache.TickListener;
import com.dukascopy.charts.data.datacache.UpdateCacheDataAction;
import com.dukascopy.charts.data.datacache.intraperiod.LastPointAndFigureLiveFeedListener;
import com.dukascopy.charts.data.datacache.intraperiod.LastPriceRangeLiveFeedListener;
import com.dukascopy.charts.data.datacache.intraperiod.LastTickBarLiveFeedListener;
import com.dukascopy.charts.data.datacache.listener.SaveCandlesLiveFeedListener;
import com.dukascopy.charts.data.datacache.listener.SaveCandlesLoadingProgressListener;
import com.dukascopy.charts.data.datacache.pnf.IPointAndFigureLiveFeedListener;
import com.dukascopy.charts.data.datacache.pnf.LoadLatestPointAndFigureAction;
import com.dukascopy.charts.data.datacache.pnf.LoadNumberOfPointAndFigureAction;
import com.dukascopy.charts.data.datacache.pnf.LoadPointAndFigureTimeIntervalAction;
import com.dukascopy.charts.data.datacache.pnf.PointAndFigureData;
import com.dukascopy.charts.data.datacache.pnf.PointAndFigureLiveFeedAdapter;
import com.dukascopy.charts.data.datacache.rangebar.IPriceRangeLiveFeedListener;
import com.dukascopy.charts.data.datacache.rangebar.LoadLatestPriceRangeAction;
import com.dukascopy.charts.data.datacache.rangebar.LoadNumberOfPriceRangeAction;
import com.dukascopy.charts.data.datacache.rangebar.LoadPriceRangeTimeIntervalAction;
import com.dukascopy.charts.data.datacache.rangebar.PriceRangeData;
import com.dukascopy.charts.data.datacache.rangebar.PriceRangeLiveFeedAdapter;
import com.dukascopy.charts.data.datacache.thread.BackgroundFeedLoadingThread;
import com.dukascopy.charts.data.datacache.tickbar.ITickBarLiveFeedListener;
import com.dukascopy.charts.data.datacache.tickbar.LoadLatestTickBarAction;
import com.dukascopy.charts.data.datacache.tickbar.LoadNumberOfTickBarAction;
import com.dukascopy.charts.data.datacache.tickbar.LoadTickBarTimeIntervalAction;
import com.dukascopy.charts.data.datacache.tickbar.TickBarData;
import com.dukascopy.charts.data.datacache.tickbar.TickBarLiveFeedAdapter;
import com.dukascopy.charts.data.datacache.wrapper.Weekend;
import com.dukascopy.charts.data.orders.IOrdersProvider;
import com.dukascopy.charts.data.orders.OrdersProvider;
import com.dukascopy.dds2.greed.agent.strategy.StratUtils;
import com.dukascopy.dds2.greed.util.IOrderUtils;
import com.dukascopy.transport.common.msg.request.CurrencyMarket;
import com.dukascopy.transport.common.msg.request.CurrencyOffer;
import com.dukascopy.transport.util.Hex;
/*      */ 
/*      */ public class FeedDataProvider extends com.dukascopy.charts.data.datacache.FeedDataProvider
/*      */   implements IFeedDataProvider, TickListener, OrdersListener
/*      */ {
/*      */   private static final Logger LOGGER;
/*      */   public static final int WEEKENDS_CACHE_SIZE = 40;
/*      */   private static FeedDataProvider feedDataProvider;
/*      */   private final ExecutorService actionsExecutorService;
/*   94 */   private final List<Runnable> currentlyRunningTasks = new ArrayList();
/*   95 */   private final List<LiveFeedListener>[] tickListeners = new List[Instrument.values().length];
/*   96 */   private final Map<Instrument, Map<Period, Map<com.dukascopy.api.OfferSide, List<LiveFeedListener>>>> periodListenersMap = new HashMap();
/*   97 */   private final List<LiveFeedListener>[][] allPeriodListeners = new List[Instrument.values().length][2];
/*   98 */   private final List<LiveCandleListener> allCandlePeriodListener = Collections.synchronizedList(new ArrayList());
/*   99 */   private final Map<Instrument, Map<Period, Map<com.dukascopy.api.OfferSide, List<LiveFeedListener>>>> inProgressCandleListenersMap = new HashMap();
/*  100 */   private final List<CacheDataUpdatedListener>[] cacheDataChangeListeners = new List[Instrument.values().length];
/*  101 */   protected final double[] lastAsks = new double[Instrument.values().length];
/*  102 */   protected final double[] lastBids = new double[Instrument.values().length];
/*  103 */   protected final TickData[] lastTicks = new TickData[Instrument.values().length];
/*  104 */   protected final long[] currentTimes = new long[Instrument.values().length];
/*  105 */   protected volatile long currentTime = -9223372036854775808L;
/*  106 */   protected volatile long firstTickLocalTime = -9223372036854775808L;
/*  107 */   protected Deque<Weekend> cachedWeekends = new LinkedList();
/*      */   private final CurvesDataLoader curvesDataLoader;
/*      */   protected LocalCacheManager localCacheManager;
/*      */   private static ICurvesProtocolHandler curvesProtocolHandler;
/*      */   private static String accountId;
/*      */   protected IntraperiodCandlesGenerator intraperiodCandlesGenerator;
/*      */   private final IOrdersProvider ordersProvider;
/*      */   private final IFeedInfo feedInfo;
/*  117 */   private final List<Instrument> subscribedInstruments = new ArrayList();
/*  118 */   private final List<InstrumentSubscriptionListener> instrumentSubscriptionListeners = Collections.synchronizedList(new ArrayList());
/*  119 */   protected CurvesDataLoader.IntraperiodExistsPolicy intraperiodExistsPolicy = CurvesDataLoader.IntraperiodExistsPolicy.DOWNLOAD_CHUNK_IN_BACKGROUND;
/*      */   private static final long[][] timesOfTheFirstCandle;
/*      */   private static final long[][] timesOfTheFirstOurCandle;
/*  124 */   private final int[] networkLatency = new int[15];
/*  125 */   private int latencyIndex = 0;
/*      */   private static String platformTicket;
/*      */   private static String encryptionKey;
/*  130 */   private volatile boolean connected = true;
/*      */   private volatile long disconnectTime;
/*      */   private volatile boolean stopped;
/*  135 */   private final Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, List<IPriceRangeLiveFeedListener>>>> inProgressPriceRangeLiveFeedListenersMap = new HashMap();
/*  136 */   private final Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, Map<ReversalAmount, List<IPointAndFigureLiveFeedListener>>>>> inProgressPointAndFigureListenersMap = new HashMap();
/*  137 */   private final Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<TickBarSize, List<ITickBarLiveFeedListener>>>> inProgressTickBarLiveFeedListenersMap = new HashMap();
/*      */ 
/*  139 */   private final Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, List<IPriceRangeLiveFeedListener>>>> priceRangeNotificationListenersMap = new HashMap();
/*  140 */   private final Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, Map<ReversalAmount, List<IPointAndFigureLiveFeedListener>>>>> pointAndFigureNotificationListenersMap = new HashMap();
/*  141 */   private final Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<TickBarSize, List<ITickBarLiveFeedListener>>>> tickBarNotificationListenersMap = new HashMap();
/*      */   private BackgroundFeedLoadingThread backgroundFeedLoadingThread;
/*      */ 
/*      */   protected FeedDataProvider(String cacheName, boolean disableFlatsGenerationByTimeout, IOrdersProvider ordersProvider, IFeedInfo feedInfo)
/*      */     throws DataCacheException
/*      */   {
			super(cacheName, disableFlatsGenerationByTimeout, ordersProvider, feedInfo);
/*  259 */     ThreadFactory threadFactory = new FeedThreadFactory();
/*      */ 
/*  262 */     this.actionsExecutorService = new FeedExecutor(5, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue(15 + Instrument.values().length * 3, false), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy(), this.currentlyRunningTasks);
/*      */ 
/*  266 */     for (int i = 0; i < this.tickListeners.length; ++i) {
/*  267 */       this.tickListeners[i] = Collections.synchronizedList(new ArrayList());
/*      */     }
/*  269 */     for (int i = 0; i < this.cacheDataChangeListeners.length; ++i) {
/*  270 */       this.cacheDataChangeListeners[i] = Collections.synchronizedList(new ArrayList());
/*      */     }
/*  272 */     for (int i = 0; i < this.currentTimes.length; ++i) {
/*  273 */       this.currentTimes[i] = -9223372036854775808L;
/*      */     }
/*  275 */     for (int i = 0; i < this.lastAsks.length; ++i) {
/*  276 */       this.lastAsks[i] = (0.0D / 0.0D);
/*  277 */       this.lastBids[i] = (0.0D / 0.0D);
/*      */     }
/*      */ 
/*  280 */     this.curvesDataLoader = new CurvesDataLoader(this);
/*  281 */     this.localCacheManager = new LocalCacheManager(cacheName, cacheName.equals("LIVE"));
/*  282 */     this.instrumentSubscriptionListeners.add(this.localCacheManager);
/*  283 */     this.intraperiodCandlesGenerator = new IntraperiodCandlesGenerator(disableFlatsGenerationByTimeout, this);
/*  284 */     this.ordersProvider = ordersProvider;
/*  285 */     this.feedInfo = feedInfo;
/*      */   }
/*      */ 
/*      */   public static FeedDataProvider getDefaultInstance()
/*      */   {
/*  295 */     return feedDataProvider;
/*      */   }
/*      */ 
/*      */   public static void createFeedDataProvider(String cacheName)
/*      */     throws DataCacheException
/*      */   {
/*  305 */     createFeedDataProvider(cacheName, null, null);
/*      */   }
/*      */ 
/*      */   public static void createFeedDataProvider(String cacheName, IFeedInfo feedInfo) throws DataCacheException {
/*  309 */     createFeedDataProvider(cacheName, null, feedInfo);
/*      */   }
/*      */ 
/*      */   public static void createFeedDataProvider(String cacheName, ICurvesProtocolHandler curvesProtocolHandler, IFeedInfo feedInfo) throws DataCacheException {
/*  313 */     if (feedDataProvider == null) {
/*  314 */       OrdersProvider ordersProvider = OrdersProvider.getInstance();
/*  315 */       feedDataProvider = new FeedDataProvider(cacheName, false, ordersProvider, feedInfo);
/*  319 */       curvesProtocolHandler = curvesProtocolHandler;
/*  321 */       for (Instrument instrument : Instrument.values())
/*  322 */         ordersProvider.addOrdersListener(instrument, feedDataProvider);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void connectToHistoryServer(Collection<String> authServerUrls, String userName, String instanceId, String historyServerUrl, String encryptionKey, String version)
/*      */   {
/*  335 */     accountId = userName;
/*  336 */     if ((accountId != null) && (accountId.equals(userName))) {
/*  337 */       this.ordersProvider.clear();
/*      */     }
/*  339 */     if (encryptionKey == null) {
/*  340 */       encryptionKey = encryptionKey;
/*      */     }
/*  342 */     curvesProtocolHandler.connect(new FeedAuthenticator(authServerUrls, version, userName, instanceId), userName, instanceId, historyServerUrl);
/*      */   }
/*      */ 
/*      */   public void connectToHistoryServer(IAuthenticator authenticator, String userName, String instanceId, String historyServerUrl, String encryptionKey)
/*      */   {
/*  353 */     accountId = userName;
/*  354 */     if (encryptionKey == null) {
/*  355 */       encryptionKey = encryptionKey;
/*      */     }
/*  357 */     curvesProtocolHandler.connect(authenticator, userName, instanceId, historyServerUrl);
/*      */   }
/*      */ 
/*      */   public IFeedInfo getFeedInfo() {
/*  361 */     return this.feedInfo;
/*      */   }
/*      */ 
/*      */   public static void setPlatformTicket(String platformTicket) {
/*  365 */     platformTicket = platformTicket;
/*      */   }
/*      */ 
/*      */   public void subscribeToLiveFeed(Instrument instrument, LiveFeedListener listener)
/*      */   {
/*  370 */     LOGGER.trace(new StringBuilder().append("Subscribing one more listener to instrument [").append(instrument).append("]").toString());
/*  371 */     this.tickListeners[instrument.ordinal()].add(listener);
/*      */   }
/*      */ 
/*      */   public void unsubscribeToLiveFeed(Instrument instrument, LiveFeedListener listener)
/*      */   {
/*  377 */     LOGGER.trace(new StringBuilder().append("Unsubscribing listener from instrument [").append(instrument).append("]").toString());
/*  378 */     this.tickListeners[instrument.ordinal()].remove(listener);
/*      */   }
/*      */ 
/*      */   private Map<com.dukascopy.api.OfferSide, List<LiveFeedListener>> getOfferSideMap(Map<Instrument, Map<Period, Map<com.dukascopy.api.OfferSide, List<LiveFeedListener>>>> map, Instrument instrument, Period period)
/*      */   {
/*  386 */     if (!(map.containsKey(instrument))) {
/*  387 */       map.put(instrument, new HashMap());
/*      */     }
/*  389 */     Map periodMap = (Map)map.get(instrument);
/*  390 */     if (!(periodMap.containsKey(period))) {
/*  391 */       periodMap.put(period, new HashMap());
/*      */     }
/*  393 */     return ((Map)periodMap.get(period));
/*      */   }
/*      */ 
/*      */   private List<LiveFeedListener> getPeriodListeners(Map<Instrument, Map<Period, Map<com.dukascopy.api.OfferSide, List<LiveFeedListener>>>> map, Instrument instrument, Period period, com.dukascopy.api.OfferSide offerSide)
/*      */   {
/*  402 */     Map offerSideMap = getOfferSideMap(map, instrument, period);
/*  403 */     return ((List)offerSideMap.get(offerSide));
/*      */   }
/*      */ 
/*      */   private void putPeriodListeners(Map<Instrument, Map<Period, Map<com.dukascopy.api.OfferSide, List<LiveFeedListener>>>> map, Instrument instrument, Period period, com.dukascopy.api.OfferSide offerSide, List<LiveFeedListener> listeners)
/*      */   {
/*  413 */     Map offerSideMap = getOfferSideMap(map, instrument, period);
/*  414 */     offerSideMap.put(offerSide, listeners);
/*      */   }
/*      */ 
/*      */   public void subscribeToPeriodNotifications(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, LiveFeedListener listener)
/*      */   {
/*  419 */     LOGGER.trace(new StringBuilder().append("Subscribing one more listener to instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("]").toString());
/*      */ 
/*  423 */     List listenersList = getPeriodListeners(this.periodListenersMap, instrument, period, side);
/*  424 */     if (listenersList == null) {
/*  425 */       listenersList = Collections.synchronizedList(new ArrayList());
/*  426 */       putPeriodListeners(this.periodListenersMap, instrument, period, side, listenersList);
/*      */     }
/*  428 */     listenersList.add(listener);
/*      */   }
/*      */ 
/*      */   public void unsubscribeToPeriodNotifications(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, LiveFeedListener listener)
/*      */   {
/*  433 */     LOGGER.trace(new StringBuilder().append("Unsubscribing listener from instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("]").toString());
/*  434 */     List listenersList = getPeriodListeners(this.periodListenersMap, instrument, period, side);
/*  435 */     if (listenersList != null)
/*  436 */       listenersList.remove(listener);
/*      */   }
/*      */ 
/*      */   public void subscribeToAllPeriodNotifications(Instrument instrument, com.dukascopy.api.OfferSide side, LiveFeedListener listener)
/*      */   {
/*  442 */     LOGGER.trace(new StringBuilder().append("Subscribing one more listener to instrument [").append(instrument).append("], all periods, side [").append(side).append("]").toString());
/*  443 */     List listenersList = this.allPeriodListeners[instrument.ordinal()][1];
/*  444 */     if (listenersList == null) {
/*  445 */       listenersList = Collections.synchronizedList(new ArrayList());
/*  446 */       this.allPeriodListeners[instrument.ordinal()][((side == com.dukascopy.api.OfferSide.ASK) ? 0 : 1)] = listenersList;
/*      */     }
/*  448 */     listenersList.add(listener);
/*      */   }
/*      */ 
/*      */   public void unsubscribeToAllPeriodNotifications(Instrument instrument, com.dukascopy.api.OfferSide side, LiveFeedListener listener)
/*      */   {
/*  453 */     LOGGER.trace(new StringBuilder().append("Unsubscribing listener from instrument [").append(instrument).append("], all periods, side [").append(side).append("]").toString());
/*  454 */     List listenersList = this.allPeriodListeners[instrument.ordinal()][1];
/*  455 */     if (listenersList != null)
/*  456 */       listenersList.remove(listener);
/*      */   }
/*      */ 
/*      */   public void subscribeToAllCandlePeriods(LiveCandleListener listener)
/*      */   {
/*  462 */     this.allCandlePeriodListener.add(listener);
/*      */   }
/*      */ 
/*      */   public void unsubscribeFromAllCandlePeriods(LiveCandleListener listener)
/*      */   {
/*  467 */     this.allCandlePeriodListener.remove(listener);
/*      */   }
/*      */ 
/*      */   public void addInProgressCandleListener(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, LiveFeedListener listener)
/*      */   {
/*  472 */     LOGGER.trace(new StringBuilder().append("Subscribing one more inProgressCandle listener to instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("]").toString());
/*  473 */     if (listener == null) {
/*  474 */       throw new NullPointerException("Listener for in-progress candle is null");
/*      */     }
/*  476 */     List listenersList = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, side);
/*  477 */     if (listenersList == null) {
/*  478 */       listenersList = Collections.synchronizedList(new ArrayList());
/*  479 */       putPeriodListeners(this.inProgressCandleListenersMap, instrument, period, side, listenersList);
/*      */     }
/*  481 */     listenersList.add(listener);
/*      */ 
/*  483 */     this.intraperiodCandlesGenerator.inProgressCandleListenerAdded(instrument, period, side, listener);
/*      */   }
/*      */ 
/*      */   public void removeInProgressCandleListener(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, LiveFeedListener listener)
/*      */   {
/*  488 */     LOGGER.trace(new StringBuilder().append("Unsubscribing inProgressCandle listener from instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("]").toString());
/*  489 */     List listenersList = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, side);
/*  490 */     if (listenersList != null) {
/*  491 */       listenersList.remove(listener);
/*      */     }
/*  493 */     this.intraperiodCandlesGenerator.inProgressCandleListenerRemoved(instrument, period, side, listener);
/*      */   }
/*      */ 
/*      */   public void setInstrumentNamesSubscribed(Set<String> instrumentNames) {
/*  497 */     Set instruments = new HashSet();
/*  498 */     for (String instrumentStr : instrumentNames) {
/*  499 */       instruments.add(Instrument.fromString(instrumentStr));
/*      */     }
/*  501 */     setInstrumentsSubscribed(instruments);
/*      */   }
/*      */ 
/*      */   public void addInstrumentNamesSubscribed(Set<String> instrumentNames) {
/*  505 */     synchronized (this.subscribedInstruments) {
/*  506 */       Set instruments = new HashSet(this.subscribedInstruments);
/*  507 */       for (String instrumentStr : instrumentNames) {
/*  508 */         Instrument instrument = Instrument.fromString(instrumentStr);
/*  509 */         if (!(instruments.contains(instrument))) {
/*  510 */           instruments.add(instrument);
/*      */         }
/*      */       }
/*  513 */       setInstrumentsSubscribed(instruments);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void setInstrumentNamesSubscribed(List<String> instrumentNames) {
/*  518 */     Set instruments = new HashSet();
/*  519 */     for (String instrumentStr : instrumentNames) {
/*  520 */       instruments.add(Instrument.fromString(instrumentStr));
/*      */     }
/*  522 */     setInstrumentsSubscribed(instruments);
/*      */   }
/*      */ 
/*      */   public void setInstrumentsSubscribed(Set<Instrument> instruments) {
/*  526 */     synchronized (this.subscribedInstruments) {
/*  527 */       List<Instrument> instrumentsAdded = new ArrayList(instruments);
/*  528 */       List<Instrument> instrumentsRemoved = new ArrayList(this.subscribedInstruments);
/*  529 */       instrumentsAdded.removeAll(this.subscribedInstruments);
/*  530 */       instrumentsRemoved.removeAll(instruments);
/*  531 */       this.subscribedInstruments.clear();
/*  532 */       this.subscribedInstruments.addAll(instruments);
/*      */       try {
/*  534 */         for (Instrument instrument : instrumentsAdded) {
/*  535 */           fireInstrumentSubscribed(instrument);
/*  536 */           this.intraperiodCandlesGenerator.addInstrument(instrument);
/*      */         }
/*  538 */         for (Instrument instrument : instrumentsRemoved) {
/*  539 */           this.lastTicks[instrument.ordinal()] = null;
/*  540 */           this.currentTimes[instrument.ordinal()] = -9223372036854775808L;
/*  541 */           fireInstrumentUnsubscribed(instrument);
/*  542 */           unsubscribeListenersFromInstrument(instrument);
/*      */         }
/*      */       } catch (Exception e) {
/*  545 */         LOGGER.error(e.getMessage(), e);
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   private void unsubscribeListenersFromInstrument(Instrument instrument) {
/*  551 */     LOGGER.trace(new StringBuilder().append("Unsubscribing all listeners from instrument [").append(instrument).append("]").toString());
/*  552 */     this.localCacheManager.clearTicksIntraPeriod(instrument);
/*  553 */     this.tickListeners[instrument.ordinal()].clear();
/*  554 */     LOGGER.trace(new StringBuilder().append("Unsubscribing all listeners from instrument [").append(instrument).append("] for all periods and both sides").toString());
/*  555 */     this.intraperiodCandlesGenerator.removeInstrument(instrument);
/*  556 */     for (Period period : Period.values()) {
/*  557 */       putPeriodListeners(this.periodListenersMap, instrument, period, com.dukascopy.api.OfferSide.ASK, null);
/*  558 */       putPeriodListeners(this.periodListenersMap, instrument, period, com.dukascopy.api.OfferSide.BID, null);
/*      */     }
/*  560 */     this.allPeriodListeners[instrument.ordinal()][0] = null;
/*  561 */     this.allPeriodListeners[instrument.ordinal()][1] = null;
/*      */   }
/*      */ 
/*      */   public void fireInstrumentSubscribed(Instrument instrument) {
/*  565 */     InstrumentSubscriptionListener[] listeners = (InstrumentSubscriptionListener[])this.instrumentSubscriptionListeners.toArray(new InstrumentSubscriptionListener[this.instrumentSubscriptionListeners.size()]);
/*      */ 
/*  567 */     for (InstrumentSubscriptionListener instrumentSubscriptionListener : listeners)
/*  568 */       instrumentSubscriptionListener.subscribedToInstrument(instrument);
/*      */   }
/*      */ 
/*      */   public void fireInstrumentUnsubscribed(Instrument instrument)
/*      */   {
/*  573 */     InstrumentSubscriptionListener[] listeners = (InstrumentSubscriptionListener[])this.instrumentSubscriptionListeners.toArray(new InstrumentSubscriptionListener[this.instrumentSubscriptionListeners.size()]);
/*      */ 
/*  575 */     for (InstrumentSubscriptionListener instrumentSubscriptionListener : listeners)
/*  576 */       instrumentSubscriptionListener.unsubscribedFromInstrument(instrument);
/*      */   }
/*      */ 
/*      */   public void addInstrumentSubscriptionListener(InstrumentSubscriptionListener listener)
/*      */   {
/*  582 */     if (listener == null) {
/*  583 */       throw new NullPointerException("listener is null");
/*      */     }
/*  585 */     if (!(this.instrumentSubscriptionListeners.contains(listener)))
/*  586 */       this.instrumentSubscriptionListeners.add(listener);
/*      */   }
/*      */ 
/*      */   public List<Instrument> getInstrumentsCurrentlySubscribed()
/*      */   {
/*  592 */     synchronized (this.subscribedInstruments) {
/*  593 */       return new ArrayList(this.subscribedInstruments);
/*      */     }
/*      */   }
/*      */ 
/*      */   public boolean isSubscribedToInstrument(Instrument instrument)
/*      */   {
/*  599 */     synchronized (this.subscribedInstruments) {
/*  600 */       return this.subscribedInstruments.contains(instrument);
/*      */     }
/*      */   }
/*      */ 
/*      */   public void removeInstrumentSubscriptionListener(InstrumentSubscriptionListener listener)
/*      */   {
/*  606 */     this.instrumentSubscriptionListeners.remove(listener);
/*      */   }
/*      */ 
/*      */   public boolean isDataCached(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to) throws DataCacheException
/*      */   {
/*  611 */     if (LOGGER.isTraceEnabled()) {
/*  612 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  613 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  614 */       LOGGER.trace(new StringBuilder().append("Checking isDataCached for instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/*      */     }
/*  616 */     IsDataCachedAction isDataCachedAction = new IsDataCachedAction(this, instrument, period, side, from, to);
/*  617 */     return isDataCachedAction.call().booleanValue();
/*      */   }
/*      */ 
/*      */   public void loadCandlesData(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  623 */     LoadDataAction loadDataAction = getLoadCandlesDataAction(instrument, period, side, from, to, candleListener, loadingProgress, false, false);
/*  624 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataBefore(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandles, long to, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  630 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, period, side, numberOfCandles, 0, to, filter, candleListener, loadingProgress);
/*      */ 
/*  632 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataAfter(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandles, long from, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  638 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, period, side, 0, numberOfCandles, from, filter, candleListener, loadingProgress);
/*      */ 
/*  640 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataBeforeAfter(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandlesBefore, int numberOfCandlesAfter, long time, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  647 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, period, side, numberOfCandlesBefore, numberOfCandlesAfter, time, filter, candleListener, loadingProgress);
/*      */ 
/*  649 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataBeforeAfterSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandlesBefore, int numberOfCandlesAfter, long time, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  656 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, period, side, numberOfCandlesBefore, numberOfCandlesAfter, time, filter, candleListener, loadingProgress);
/*      */ 
/*  658 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataBeforeSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandles, long to, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  664 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, period, side, numberOfCandles, 0, to, filter, candleListener, loadingProgress);
/*      */ 
/*  666 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataAfterSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandles, long from, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  672 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, period, side, 0, numberOfCandles, from, filter, candleListener, loadingProgress);
/*      */ 
/*  674 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadLastAvailableCandlesDataSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  680 */     LoadLastAvailableDataAction loadDataAction = getLoadLastAvailableCandlesDataAction(instrument, period, side, from, to, candleListener, loadingProgress);
/*  681 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadLastAvailableNumberOfCandlesDataSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandles, long to, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  688 */     LoadNumberOfLastAvailableDataAction loadDataAction = getLoadNumberOfLastAvailableCandlesDataAction(instrument, period, side, numberOfCandles, to, filter, candleListener, loadingProgress);
/*  689 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  695 */     LoadDataAction loadDataAction = getLoadCandlesDataAction(instrument, period, side, from, to, candleListener, loadingProgress, false, false);
/*  696 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress, boolean loadFromChunkStart) throws DataCacheException
/*      */   {
/*  701 */     LoadDataAction loadDataAction = getLoadCandlesDataAction(instrument, period, side, from, to, candleListener, loadingProgress, false, loadFromChunkStart);
/*  702 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataBlockingSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  708 */     LoadDataAction loadDataAction = getLoadCandlesDataAction(instrument, period, side, from, to, candleListener, loadingProgress, true, false);
/*  709 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadInProgressCandleData(Instrument instrument, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  716 */     LoadInProgressCandleDataAction loadDataAction = getLoadInProgressCandleDataAction(instrument, to, candleListener, loadingProgress);
/*  717 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadInProgressCandleDataSynched(Instrument instrument, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  724 */     LoadInProgressCandleDataAction loadDataAction = getLoadInProgressCandleDataAction(instrument, to, candleListener, loadingProgress);
/*  725 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   private LoadNumberOfCandlesAction getLoadNumberOfCandlesAction(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandlesBefore, int numberOfCandlesAfter, long time, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  732 */     if (LOGGER.isTraceEnabled()) {
/*  733 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  734 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  735 */       LOGGER.trace(new StringBuilder().append("Loading [").append(numberOfCandlesBefore).append("] candles before and [").append(numberOfCandlesAfter).append("] after for instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("] time [").append(dateFormat.format(new Date(time))).append("]").append((filter == Filter.ALL_FLATS) ? " filtering all flats" : (filter == Filter.WEEKENDS) ? " filtering weekends" : "").toString());
/*      */ 
/*  738 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  739 */       loadingProgress = new LoadingProgressListener()
/*      */       {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  744 */           this.lastCurrentTime = currentTime;
/*  745 */           FeedDataProvider.LOGGER.trace(information);
/*  746 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e)
/*      */         {
/*  751 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  752 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob()
/*      */         {
/*  757 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/*  761 */     LoadNumberOfCandlesAction loadDataAction = new LoadNumberOfCandlesAction(this, instrument, period, side, numberOfCandlesBefore, numberOfCandlesAfter, time, filter, candleListener, loadingProgress, this.intraperiodExistsPolicy, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/*  764 */     if (this.stopped) {
/*  765 */       loadDataAction.cancel();
/*      */     }
/*  767 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadNumberOfCandlesAction getLoadNumberOfCandlesAction(Instrument instrument, int numberOfSecondsBefore, int numberOfSecondsAfter, long time, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/*  773 */     if (LOGGER.isTraceEnabled()) {
/*  774 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  775 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  776 */       LOGGER.trace(new StringBuilder().append("Loading [").append(numberOfSecondsBefore).append("] seconds before and [").append(numberOfSecondsAfter).append("] after of ticks for instrument [").append(instrument).append("] time [").append(dateFormat.format(new Date(time))).append("]").append((filter == Filter.ALL_FLATS) ? " filtering all flats" : (filter == Filter.WEEKENDS) ? " filtering weekends" : "").toString());
/*      */ 
/*  779 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  780 */       loadingProgress = new LoadingProgressListener()
/*      */       {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  785 */           this.lastCurrentTime = currentTime;
/*  786 */           FeedDataProvider.LOGGER.trace(information);
/*  787 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e)
/*      */         {
/*  792 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  793 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob()
/*      */         {
/*  798 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/*  802 */     LoadNumberOfCandlesAction loadDataAction = new LoadNumberOfCandlesAction(this, instrument, numberOfSecondsBefore, numberOfSecondsAfter, time, filter, candleListener, loadingProgress, this.intraperiodExistsPolicy, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/*  804 */     if (this.stopped) {
/*  805 */       loadDataAction.cancel();
/*      */     }
/*  807 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadDataAction getLoadCandlesDataAction(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress, boolean blocking, boolean loadFromChunkStart) throws DataCacheException
/*      */   {
/*  812 */     if (LOGGER.isTraceEnabled()) {
/*  813 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  814 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  815 */       LOGGER.trace(new StringBuilder().append("Loading candles for instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/*      */ 
/*  817 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  818 */       loadingProgress = new LoadingProgressListener()
/*      */       {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  823 */           this.lastCurrentTime = currentTime;
/*  824 */           FeedDataProvider.LOGGER.trace(information);
/*  825 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e)
/*      */         {
/*  830 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  831 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob()
/*      */         {
/*  836 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/*  840 */     LoadDataAction loadDataAction = new LoadDataAction(this, instrument, period, side, from, to, candleListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null, blocking, this.intraperiodExistsPolicy, loadFromChunkStart);
/*      */ 
/*  843 */     if (this.stopped) {
/*  844 */       loadDataAction.cancel();
/*      */     }
/*  846 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadLastAvailableDataAction getLoadLastAvailableCandlesDataAction(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/*  851 */     if (LOGGER.isTraceEnabled()) {
/*  852 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  853 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  854 */       LOGGER.trace(new StringBuilder().append("Loading last available candles for instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/*      */ 
/*  856 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  857 */       loadingProgress = new LoadingProgressListener()
/*      */       {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  862 */           this.lastCurrentTime = currentTime;
/*  863 */           FeedDataProvider.LOGGER.trace(information);
/*  864 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e)
/*      */         {
/*  869 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  870 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  874 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/*  878 */     LoadLastAvailableDataAction loadDataAction = new LoadLastAvailableDataAction(this, instrument, period, side, from, to, this.intraperiodExistsPolicy, candleListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/*  880 */     if (this.stopped) {
/*  881 */       loadDataAction.cancel();
/*      */     }
/*  883 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadNumberOfLastAvailableDataAction getLoadNumberOfLastAvailableCandlesDataAction(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int numberOfCandles, long to, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/*  888 */     if (LOGGER.isTraceEnabled()) {
/*  889 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  890 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  891 */       LOGGER.trace(new StringBuilder().append("Loading last available candles for instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("] number of candles [").append(numberOfCandles).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/*      */ 
/*  894 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  895 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  899 */           this.lastCurrentTime = currentTime;
/*  900 */           FeedDataProvider.LOGGER.trace(information);
/*  901 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  905 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  906 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  910 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/*  914 */     LoadNumberOfLastAvailableDataAction loadDataAction = new LoadNumberOfLastAvailableDataAction(this, instrument, period, side, numberOfCandles, to, filter, this.intraperiodExistsPolicy, candleListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/*  917 */     if (this.stopped) {
/*  918 */       loadDataAction.cancel();
/*      */     }
/*  920 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadInProgressCandleDataAction getLoadInProgressCandleDataAction(Instrument instrument, long to, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/*  925 */     if (LOGGER.isTraceEnabled()) {
/*  926 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  927 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  928 */       LOGGER.trace(new StringBuilder().append("Loading in-progress candle for instrument [").append(instrument).append("], to time [").append(dateFormat.format(new Date(to))).append("]").toString());
/*  929 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  930 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  934 */           this.lastCurrentTime = currentTime;
/*  935 */           FeedDataProvider.LOGGER.trace(information);
/*  936 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  940 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  941 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  945 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/*  949 */     LoadInProgressCandleDataAction loadDataAction = new LoadInProgressCandleDataAction(this, instrument, to, candleListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/*  951 */     if (this.stopped) {
/*  952 */       loadDataAction.cancel();
/*      */     }
/*  954 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private boolean isAssertionsEnabled() {
/*  958 */     boolean b = false;
/*  959 */     assert (1 != 0);
/*  960 */     return b;
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataInCache(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/*  965 */     LoadDataAction loadDataAction = getLoadCandlesDataInCacheAction(instrument, period, side, from, to, loadingProgress, false);
/*  966 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadCandlesDataInCacheSynched(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/*  971 */     LoadDataAction loadDataAction = getLoadCandlesDataInCacheAction(instrument, period, side, from, to, loadingProgress, false);
/*  972 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   private LoadDataAction getLoadCandlesDataInCacheAction(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long from, long to, LoadingProgressListener loadingProgress, boolean loadFromChunkStart) throws DataCacheException
/*      */   {
/*  977 */     if (LOGGER.isTraceEnabled()) {
/*  978 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/*  979 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/*  980 */       LOGGER.trace(new StringBuilder().append("Loading candles in cache for instrument [").append(instrument).append("], period [").append(period).append("], side [").append(side).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/*  981 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/*  982 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/*  986 */           this.lastCurrentTime = currentTime;
/*  987 */           FeedDataProvider.LOGGER.trace(information);
/*  988 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/*  992 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/*  993 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/*  997 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/* 1001 */     LoadDataAction loadDataAction = new LoadDataAction(this, instrument, period, side, from, to, null, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null, false, CurvesDataLoader.IntraperiodExistsPolicy.FORCE_CHUNK_DOWNLOADING, loadFromChunkStart);
/*      */ 
/* 1004 */     if (this.stopped) {
/* 1005 */       loadDataAction.cancel();
/*      */     }
/* 1007 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   public void loadTicksData(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1012 */     LoadDataAction loadDataAction = getLoadTicksDataAction(instrument, from, to, tickListener, loadingProgress, false, false);
/* 1013 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadTicksDataBefore(Instrument instrument, int numberOfSeconds, long to, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1018 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, numberOfSeconds, 0, to, filter, candleListener, loadingProgress);
/*      */ 
/* 1020 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadTicksDataAfter(Instrument instrument, int numberOfSeconds, long from, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1025 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, 0, numberOfSeconds, from, filter, candleListener, loadingProgress);
/*      */ 
/* 1027 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadTicksDataBeforeAfter(Instrument instrument, int numberOfSecondsBefore, int numberOfSecondsAfter, long time, Filter filter, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1032 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, numberOfSecondsBefore, numberOfSecondsAfter, time, filter, tickListener, loadingProgress);
/*      */ 
/* 1034 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadTicksDataBeforeAfterSynched(Instrument instrument, int numberOfSecondsBefore, int numberOfSecondsAfter, long time, Filter filter, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1039 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, numberOfSecondsBefore, numberOfSecondsAfter, time, filter, tickListener, loadingProgress);
/*      */ 
/* 1041 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadTicksDataBeforeSynched(Instrument instrument, int numberOfSeconds, long to, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1046 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, numberOfSeconds, 0, to, filter, candleListener, loadingProgress);
/*      */ 
/* 1048 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadTicksDataAfterSynched(Instrument instrument, int numberOfSeconds, long from, Filter filter, LiveFeedListener candleListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1053 */     LoadNumberOfCandlesAction loadDataAction = getLoadNumberOfCandlesAction(instrument, 0, numberOfSeconds, from, filter, candleListener, loadingProgress);
/*      */ 
/* 1055 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadLastAvailableTicksDataSynched(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1060 */     LoadLastAvailableDataAction loadDataAction = getLoadLastAvailableTicksDataAction(instrument, from, to, tickListener, loadingProgress);
/* 1061 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadLastAvailableNumberOfTicksDataSynched(Instrument instrument, int numberOfSeconds, long to, Filter filter, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1066 */     LoadNumberOfLastAvailableDataAction loadDataAction = getLoadNumberOfLastAvailableTicksData(instrument, numberOfSeconds, to, filter, tickListener, loadingProgress);
/* 1067 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadTicksDataSynched(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1072 */     LoadDataAction loadDataAction = getLoadTicksDataAction(instrument, from, to, tickListener, loadingProgress, false, false);
/* 1073 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadTicksDataSynched(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress, boolean loadFromChunkStart) throws DataCacheException
/*      */   {
/* 1078 */     LoadDataAction loadDataAction = getLoadTicksDataAction(instrument, from, to, tickListener, loadingProgress, false, loadFromChunkStart);
/* 1079 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadTicksDataBlockingSynched(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1084 */     LoadDataAction loadDataAction = getLoadTicksDataAction(instrument, from, to, tickListener, loadingProgress, true, false);
/* 1085 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   private LoadDataAction getLoadTicksDataAction(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress, boolean blocking, boolean loadFromChunkStart) throws DataCacheException
/*      */   {
/* 1090 */     if (LOGGER.isTraceEnabled()) {
/* 1091 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/* 1092 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/* 1093 */       LOGGER.trace(new StringBuilder().append("Loading ticks for instrument [").append(instrument).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/* 1094 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/* 1095 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/* 1099 */           this.lastCurrentTime = currentTime;
/* 1100 */           FeedDataProvider.LOGGER.trace(information);
/* 1101 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/* 1105 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/* 1106 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/* 1110 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/* 1114 */     LoadDataAction loadDataAction = new LoadDataAction(this, instrument, from, to, tickListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null, blocking, this.intraperiodExistsPolicy, loadFromChunkStart);
/*      */ 
/* 1117 */     if (this.stopped) {
/* 1118 */       loadDataAction.cancel();
/*      */     }
/* 1120 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadLastAvailableDataAction getLoadLastAvailableTicksDataAction(Instrument instrument, long from, long to, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1125 */     if (LOGGER.isTraceEnabled()) {
/* 1126 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/* 1127 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/* 1128 */       LOGGER.trace(new StringBuilder().append("Loading last available ticks for instrument [").append(instrument).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/* 1129 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/* 1130 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/* 1134 */           this.lastCurrentTime = currentTime;
/* 1135 */           FeedDataProvider.LOGGER.trace(information);
/* 1136 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/* 1140 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/* 1141 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/* 1145 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/* 1149 */     LoadLastAvailableDataAction loadDataAction = new LoadLastAvailableDataAction(this, instrument, from, to, this.intraperiodExistsPolicy, tickListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/* 1151 */     if (this.stopped) {
/* 1152 */       loadDataAction.cancel();
/*      */     }
/* 1154 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   private LoadNumberOfLastAvailableDataAction getLoadNumberOfLastAvailableTicksData(Instrument instrument, int numberOfSeconds, long to, Filter filter, LiveFeedListener tickListener, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1159 */     if (LOGGER.isTraceEnabled()) {
/* 1160 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/* 1161 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/* 1162 */       LOGGER.trace(new StringBuilder().append("Loading last available ticks for instrument [").append(instrument).append("] number of seconds [").append(numberOfSeconds).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/*      */ 
/* 1164 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/* 1165 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/* 1169 */           this.lastCurrentTime = currentTime;
/* 1170 */           FeedDataProvider.LOGGER.trace(information);
/* 1171 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/* 1175 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/* 1176 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/* 1180 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/* 1184 */     LoadNumberOfLastAvailableDataAction loadDataAction = new LoadNumberOfLastAvailableDataAction(this, instrument, numberOfSeconds, to, filter, this.intraperiodExistsPolicy, tickListener, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/* 1186 */     if (this.stopped) {
/* 1187 */       loadDataAction.cancel();
/*      */     }
/* 1189 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   public void loadTicksDataInCache(Instrument instrument, long from, long to, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1194 */     LoadDataAction loadDataAction = getLoadTicksDataInCacheAction(instrument, from, to, loadingProgress, false);
/* 1195 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadTicksDataInCacheSynched(Instrument instrument, long from, long to, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1200 */     LoadDataAction loadDataAction = getLoadTicksDataInCacheAction(instrument, from, to, loadingProgress, false);
/* 1201 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   private LoadDataAction getLoadTicksDataInCacheAction(Instrument instrument, long from, long to, LoadingProgressListener loadingProgress, boolean loadFromChunkStart) throws DataCacheException
/*      */   {
/* 1206 */     if (LOGGER.isTraceEnabled()) {
/* 1207 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/* 1208 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/* 1209 */       LOGGER.trace(new StringBuilder().append("Loading ticks in cache for instrument [").append(instrument).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/* 1210 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/* 1211 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/* 1215 */           this.lastCurrentTime = currentTime;
/* 1216 */           FeedDataProvider.LOGGER.trace(information);
/* 1217 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/* 1221 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/* 1222 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/* 1226 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/* 1230 */     LoadDataAction loadDataAction = new LoadDataAction(this, instrument, from, to, null, loadingProgress, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null, false, CurvesDataLoader.IntraperiodExistsPolicy.FORCE_CHUNK_DOWNLOADING, loadFromChunkStart);
/*      */ 
/* 1233 */     if (this.stopped) {
/* 1234 */       loadDataAction.cancel();
/*      */     }
/* 1236 */     return loadDataAction;
/*      */   }
/*      */ 
/*      */   public CandleData getInProgressCandle(Instrument instrument, Period period, com.dukascopy.api.OfferSide side) {
/* 1266 */     return this.intraperiodCandlesGenerator.getInProgressCandle(instrument, period, side);
/*      */   }
/*      */ 
/*      */   public CandleData getInProgressCandleBlocking(Instrument instrument, Period period, com.dukascopy.api.OfferSide side) throws DataCacheException {
/* 1270 */     return this.intraperiodCandlesGenerator.getInProgressCandleBlocking(instrument, period, side);
/*      */   }
/*      */ 
/*      */   public CurvesDataLoader getCurvesDataLoader() {
/* 1274 */     return this.curvesDataLoader;
/*      */   }
/*      */ 
/*      */   public LocalCacheManager getLocalCacheManager() {
/* 1278 */     return this.localCacheManager;
/*      */   }
/*      */ 
/*      */   public static ICurvesProtocolHandler getCurvesProtocolHandler() {
/* 1282 */     return curvesProtocolHandler;
/*      */   }
/*      */ 
/*      */   protected void fireNewTick(Instrument instrument, long time, double ask, double bid, double askVol, double bidVol) {
/* 1286 */     List listeners = this.tickListeners[instrument.ordinal()];
/* 1287 */     LiveFeedListener[] listenersArr = (LiveFeedListener[])listeners.toArray(new LiveFeedListener[listeners.size()]);
/* 1288 */     for (LiveFeedListener liveFeedListener : listenersArr)
/* 1289 */       liveFeedListener.newTick(instrument, time, ask, bid, askVol, bidVol);
/*      */   }
/*      */ 
/*      */   protected void fireInProgressCandleUpdated(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, CandleData candle)
/*      */   {
/* 1294 */     List listenersList = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, side);
/* 1295 */     if (listenersList != null) {
/* 1296 */       LiveFeedListener[] listeners = (LiveFeedListener[])listenersList.toArray(new LiveFeedListener[listenersList.size()]);
/* 1297 */       for (LiveFeedListener liveFeedListener : listeners)
/* 1298 */         liveFeedListener.newCandle(instrument, period, side, candle.time, candle.open, candle.close, candle.low, candle.high, candle.vol);
/*      */     }
/*      */   }
/*      */ 
/*      */   protected void fireCandlesFormed(Instrument instrument, Period period, IntraPeriodCandleData askCandleData, IntraPeriodCandleData bidCandleData)
/*      */   {
/* 1308 */     long nextCandleStart = DataCacheUtils.getNextCandleStartFast(period, (askCandleData != null) ? askCandleData.time : bidCandleData.time);
/* 1309 */     if (this.currentTimes[instrument.ordinal()] < nextCandleStart) {
/* 1310 */       this.currentTimes[instrument.ordinal()] = nextCandleStart;
/*      */     }
/* 1312 */     if (this.currentTime < nextCandleStart) {
/* 1313 */       this.currentTime = nextCandleStart;
/*      */     }
/*      */ 
/* 1316 */     if (askCandleData != null) {
/* 1317 */       List listenersList = getPeriodListeners(this.periodListenersMap, instrument, period, com.dukascopy.api.OfferSide.ASK);
/* 1318 */       if (listenersList != null) {
/* 1319 */         LiveFeedListener[] listeners = (LiveFeedListener[])listenersList.toArray(new LiveFeedListener[listenersList.size()]);
/* 1320 */         for (LiveFeedListener liveFeedListener : listeners) {
/* 1321 */           liveFeedListener.newCandle(instrument, period, com.dukascopy.api.OfferSide.ASK, askCandleData.time, askCandleData.open, askCandleData.close, askCandleData.low, askCandleData.high, askCandleData.vol);
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/* 1326 */     if (bidCandleData != null) {
/* 1327 */       List listenersList = getPeriodListeners(this.periodListenersMap, instrument, period, com.dukascopy.api.OfferSide.BID);
/* 1328 */       if (listenersList != null) {
/* 1329 */         LiveFeedListener[] listeners = (LiveFeedListener[])listenersList.toArray(new LiveFeedListener[listenersList.size()]);
/* 1330 */         for (LiveFeedListener liveFeedListener : listeners) {
/* 1331 */           liveFeedListener.newCandle(instrument, period, com.dukascopy.api.OfferSide.BID, bidCandleData.time, bidCandleData.open, bidCandleData.close, bidCandleData.low, bidCandleData.high, bidCandleData.vol);
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/* 1336 */     if (askCandleData != null) {
/* 1337 */       List listenersList = this.allPeriodListeners[instrument.ordinal()][0];
/* 1338 */       if (listenersList != null) {
/* 1339 */         LiveFeedListener[] listeners = (LiveFeedListener[])listenersList.toArray(new LiveFeedListener[listenersList.size()]);
/* 1340 */         for (LiveFeedListener liveFeedListener : listeners) {
/* 1341 */           liveFeedListener.newCandle(instrument, period, com.dukascopy.api.OfferSide.ASK, askCandleData.time, askCandleData.open, askCandleData.close, askCandleData.low, askCandleData.high, askCandleData.vol);
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/* 1346 */     if (bidCandleData != null) {
/* 1347 */       List listenersList = this.allPeriodListeners[instrument.ordinal()][1];
/* 1348 */       if (listenersList != null) {
/* 1349 */         LiveFeedListener[] listeners = (LiveFeedListener[])listenersList.toArray(new LiveFeedListener[listenersList.size()]);
/* 1350 */         for (LiveFeedListener liveFeedListener : listeners) {
/* 1351 */           liveFeedListener.newCandle(instrument, period, com.dukascopy.api.OfferSide.BID, bidCandleData.time, bidCandleData.open, bidCandleData.close, bidCandleData.low, bidCandleData.high, bidCandleData.vol);
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/* 1356 */     if ((askCandleData != null) && (bidCandleData != null)) {
/* 1357 */       LiveCandleListener[] listeners = (LiveCandleListener[])this.allCandlePeriodListener.toArray(new LiveCandleListener[this.allCandlePeriodListener.size()]);
/* 1358 */       for (LiveCandleListener liveFeedListener : listeners)
/* 1359 */         liveFeedListener.newCandle(instrument, period, askCandleData, bidCandleData);
/*      */     }
/*      */   }
/*      */ 
/*      */   public long getFirstTickLocalTime()
/*      */   {
/* 1365 */     return this.firstTickLocalTime;
/*      */   }
/*      */ 
/*      */   public void tickReceived(CurrencyMarket tick)
/*      */   {
/* 1371 */     Instrument instrument = Instrument.fromString(tick.getInstrument());
/* 1372 */     long time = tick.getCreationTimestamp().longValue();
/*      */ 
/* 1375 */     double askVol = 0.0D;
/* 1376 */     double bidVol = 0.0D;
/*      */     double ask;
/*      */     double bid;
/* 1377 */     synchronized (this) {
/* 1378 */       if (!(tick.isBackup())) {
/* 1379 */         addTickToLatency(time);
/*      */       }
/* 1381 */       if (this.firstTickLocalTime == -9223372036854775808L) {
/* 1382 */         this.firstTickLocalTime = System.currentTimeMillis();
/*      */       }
/* 1384 */       CurrencyOffer currencyOffer = tick.getBestOffer(com.dukascopy.transport.common.model.type.OfferSide.ASK);
/* 1385 */       ask = this.lastAsks[instrument.ordinal()];
/* 1386 */       if (currencyOffer != null) {
/* 1387 */         ask = currencyOffer.getPrice().getValue().doubleValue();
/* 1388 */         this.lastAsks[instrument.ordinal()] = ask;
/* 1389 */         askVol = StratUtils.roundHalfEven(currencyOffer.getAmount().getValue().doubleValue() / 1000000.0D, 2);
/*      */       }
/* 1391 */       currencyOffer = tick.getBestOffer(com.dukascopy.transport.common.model.type.OfferSide.BID);
/* 1392 */       bid = this.lastBids[instrument.ordinal()];
/* 1393 */       if (currencyOffer != null) {
/* 1394 */         bid = currencyOffer.getPrice().getValue().doubleValue();
/* 1395 */         this.lastBids[instrument.ordinal()] = bid;
/* 1396 */         bidVol = StratUtils.roundHalfEven(currencyOffer.getAmount().getValue().doubleValue() / 1000000.0D, 2);
/*      */       }
/* 1398 */       if ((Double.isNaN(ask)) || (Double.isNaN(bid)))
/*      */       {
/* 1400 */         return;
/*      */       }
/*      */ 
/* 1403 */       if ((this.lastTicks[instrument.ordinal()] != null) && (time < this.lastTicks[instrument.ordinal()].time)) {
/* 1404 */         LOGGER.warn("Receved tick has time older than previous tick, ignoring");
/* 1405 */         return;
/*      */       }
/* 1407 */       List<CurrencyOffer> askOffers = tick.getAsks();
/* 1408 */       double[] asks = new double[(askOffers.size() > 0) ? askOffers.size() : 1];
/* 1409 */       double[] askVols = new double[asks.length];
/*      */       int i;
/* 1410 */       if (askOffers.size() > 0) {
/* 1411 */         i = 0;
/* 1412 */         for (CurrencyOffer askOffer : askOffers) {
/* 1413 */           asks[i] = askOffer.getPrice().getValue().doubleValue();
/* 1414 */           askVols[i] = StratUtils.roundHalfEven(askOffer.getAmount().getValue().doubleValue() / 1000000.0D, 2);
/* 1415 */           ++i;
/*      */         }
/*      */       } else {
/* 1418 */         asks[0] = ask;
/* 1419 */         askVols[0] = askVol;
/*      */       }
/* 1421 */       List<CurrencyOffer> bidOffers = tick.getBids();
/* 1422 */       double[] bids = new double[(bidOffers.size() > 0) ? bidOffers.size() : 1];
/* 1423 */       double[] bidVols = new double[bids.length];
/* 1424 */       if (bidOffers.size() > 0) {
/* 1425 */         i = 0;
/* 1426 */         for (CurrencyOffer bidOffer : bidOffers) {
/* 1427 */           bids[i] = bidOffer.getPrice().getValue().doubleValue();
/* 1428 */           bidVols[i] = StratUtils.roundHalfEven(bidOffer.getAmount().getValue().doubleValue() / 1000000.0D, 2);
/* 1429 */           ++i;
/*      */         }
/*      */       } else {
/* 1432 */         bids[0] = bid;
/* 1433 */         bidVols[0] = bidVol;
/*      */       }
/* 1435 */       TickData tickData = new TickData(time, ask, bid, askVol, bidVol, asks, bids, askVols, bidVols);
/* 1436 */       if ((LOGGER.isDebugEnabled()) && (this.lastTicks[instrument.ordinal()] == null)) {
/* 1437 */         LOGGER.debug(new StringBuilder().append("First tick received for [").append(instrument).append("] - [").append(tickData).append("]").toString());
/*      */       }
/* 1439 */       this.lastTicks[instrument.ordinal()] = tickData;
/* 1440 */       this.currentTimes[instrument.ordinal()] = time;
/* 1441 */       this.currentTime = time;
/*      */     }
/*      */ 
/* 1444 */     this.localCacheManager.newTick(instrument, time, ask, bid, askVol, bidVol, false);
/*      */ 
/* 1446 */     this.intraperiodCandlesGenerator.newTick(instrument, time, ask, bid, askVol, bidVol);
/*      */ 
/* 1448 */     fireNewTick(instrument, time, ask, bid, askVol, bidVol);
/*      */   }
/*      */ 
/*      */   public void newOrder(Instrument instrument, OrderHistoricalData orderData)
/*      */   {
/*      */   }
/*      */ 
/*      */   public void orderChange(Instrument instrument, OrderHistoricalData orderData)
/*      */   {
/* 1458 */     if ((((orderData.getMergedToGroupId() != null) || (orderData.isClosed()))) && (orderData.isOpened()) && (accountId != null)) {
/* 1459 */       if (LOGGER.isDebugEnabled())
/* 1460 */         LOGGER.debug(new StringBuilder().append("Saving closed order [").append(orderData).append("]").toString());
/*      */       try
/*      */       {
/* 1463 */         this.localCacheManager.saveOrderData(accountId, instrument, orderData);
/*      */       } catch (DataCacheException e) {
/* 1465 */         LOGGER.error(e.getMessage(), e);
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   public void orderMerge(Instrument instrument, OrderHistoricalData resultingOrderData, List<OrderHistoricalData> mergedOrdersData)
/*      */   {
/* 1472 */     if ((((resultingOrderData.getMergedToGroupId() != null) || (resultingOrderData.isClosed()))) && (resultingOrderData.isOpened()) && (accountId != null)) {
/* 1473 */       if (LOGGER.isDebugEnabled())
/* 1474 */         LOGGER.debug(new StringBuilder().append("Saving closed order [").append(resultingOrderData).append("]").toString());
/*      */       try
/*      */       {
/* 1477 */         this.localCacheManager.saveOrderData(accountId, instrument, resultingOrderData);
/*      */       } catch (DataCacheException e) {
/* 1479 */         LOGGER.error(e.getMessage(), e);
/*      */       }
/*      */     }
/* 1482 */     for (OrderHistoricalData mergedOrder : mergedOrdersData) {
/* 1483 */       if (LOGGER.isDebugEnabled())
/* 1484 */         LOGGER.debug(new StringBuilder().append("Saving merged order [").append(mergedOrder).append("]").toString());
/*      */       try
/*      */       {
/* 1487 */         this.localCacheManager.saveOrderData(accountId, instrument, mergedOrder);
/*      */       } catch (DataCacheException e) {
/* 1489 */         LOGGER.error(e.getMessage(), e);
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   public void ordersInvalidated(Instrument instrument)
/*      */   {
/*      */   }
/*      */ 
/*      */   public synchronized double getLastAsk(Instrument instrument)
/*      */   {
/* 1500 */     return this.lastAsks[instrument.ordinal()];
/*      */   }
/*      */ 
/*      */   public synchronized double getLastBid(Instrument instrument) {
/* 1504 */     return this.lastBids[instrument.ordinal()];
/*      */   }
/*      */ 
/*      */   public synchronized long getCurrentTime(Instrument instrument) {
/* 1508 */     return this.currentTimes[instrument.ordinal()];
/*      */   }
/*      */ 
/*      */   public synchronized long getLastTickTime(Instrument instrument) {
/* 1512 */     TickData tickData = this.lastTicks[instrument.ordinal()];
/* 1513 */     if (tickData == null) {
/* 1514 */       return -9223372036854775808L;
/*      */     }
/* 1516 */     return tickData.time;
/*      */   }
/*      */ 
/*      */   public synchronized long getLastTickTime()
/*      */   {
/* 1521 */     long ret = -9223372036854775808L;
/* 1522 */     for (TickData lastTick : this.lastTicks) {
/* 1523 */       if ((lastTick != null) && (lastTick.time > ret)) {
/* 1524 */         ret = lastTick.time;
/*      */       }
/*      */     }
/* 1527 */     return ret;
/*      */   }
/*      */ 
///*      */   public synchronized TickData getLastTick(Instrument instrument) {
///* 1531 */     return this.lastTicks[instrument.ordinal()];
///*      */   }
/*      */ 
/*      */   public void setCurrentTime(long currentTime)
/*      */   {
/* 1538 */     this.currentTime = currentTime;
/*      */   }
/*      */ 
/*      */   public long getCurrentTime() {
/* 1542 */     return this.currentTime;
/*      */   }
/*      */ 
/*      */   public long getTimeOfFirstCandle(Instrument instrument, Period period) {
/* 1546 */     Period basicPeriod = Period.getBasicPeriodForCustom(period);
				try {
/* 1547 */     		return timesOfTheFirstCandle[instrument.ordinal()][basicPeriod.ordinal()];
				}
				catch (Exception e) {
					return timesOfTheFirstCandle[0][0];
				}
/*      */   }
/*      */ 
/*      */   public static long getTimeOfFirstCandleStatic(Instrument instrument, Period period) {
/* 1551 */     return timesOfTheFirstCandle[instrument.ordinal()][period.ordinal()];
/*      */   }
/*      */ 
/*      */   public static long getTimeOfFirstOurCandle(Instrument instrument, Period period) {
/* 1555 */     return timesOfTheFirstOurCandle[instrument.ordinal()][period.ordinal()];
/*      */   }
/*      */ 
/*      */   public void runTask(Runnable task) {
/* 1559 */     this.actionsExecutorService.submit(task);
/*      */   }
/*      */ 
/*      */   protected void finalize() throws Throwable
/*      */   {
/* 1564 */     super.finalize();
/* 1565 */     if (!(this.stopped))
/* 1566 */       close();
/*      */   }
/*      */ 
/*      */   public void close()
/*      */   {
/* 1571 */     this.stopped = true;
/* 1572 */     this.actionsExecutorService.shutdown();
/* 1573 */     synchronized (this.currentlyRunningTasks) {
/* 1574 */       for (Runnable currentlyRunningTask : this.currentlyRunningTasks) {
/* 1575 */         if (currentlyRunningTask instanceof LoadProgressingAction)
/* 1576 */           ((LoadProgressingAction)currentlyRunningTask).cancel();
/*      */       }
/*      */     }
/*      */     try
/*      */     {
/* 1581 */       if (!(this.actionsExecutorService.awaitTermination(15L, TimeUnit.SECONDS)))
/* 1582 */         this.actionsExecutorService.shutdownNow();
/*      */     }
/*      */     catch (InterruptedException e) {
/* 1585 */       LOGGER.error(e.getMessage(), e);
/* 1586 */       this.actionsExecutorService.shutdownNow();
/*      */     }
/*      */ 
/* 1589 */     this.intraperiodCandlesGenerator.stop();
///*      */     try {
///* 1591 */       this.localCacheManager.closeHandles();
///* 1592 */       if ((this.localCacheManager.cacheLock != null) && (this.localCacheManager.cacheLock.isValid())) {
///* 1593 */         this.localCacheManager.cacheLock.release();
///* 1594 */         this.localCacheManager.cacheLock.channel().close();
///*      */       }
/* 1596 */       this.localCacheManager = null;
///*      */     } catch (IOException e) {
///* 1598 */       LOGGER.error(e.getMessage(), e);
///*      */     }
/* 1600 */     this.instrumentSubscriptionListeners.clear();
/* 1601 */     for (Instrument instrument : Instrument.values()) {
/* 1602 */       for (Period period : Period.values()) {
/* 1603 */         List askInProgressCandleListeners = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, com.dukascopy.api.OfferSide.ASK);
/* 1604 */         List bidInProgressCandleListeners = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, com.dukascopy.api.OfferSide.BID);
/*      */ 
/* 1606 */         if (askInProgressCandleListeners != null) {
/* 1607 */           askInProgressCandleListeners.clear();
/*      */         }
/* 1609 */         if (bidInProgressCandleListeners != null) {
/* 1610 */           bidInProgressCandleListeners.clear();
/*      */         }
/*      */       }
/*      */     }
/* 1614 */     this.allCandlePeriodListener.clear();
/* 1615 */     for (int i = 0; i < Instrument.values().length; ++i) {
/* 1616 */       for (int j = 0; j < Period.values().length; ++j) {
/* 1617 */         Instrument instrument = Instrument.values()[i];
/* 1618 */         Period period = Period.values()[j];
/* 1619 */         List askPeriodListeners = getPeriodListeners(this.periodListenersMap, instrument, period, com.dukascopy.api.OfferSide.ASK);
/* 1620 */         List bidPeriodListeners = getPeriodListeners(this.periodListenersMap, instrument, period, com.dukascopy.api.OfferSide.BID);
/* 1621 */         if (askPeriodListeners != null) {
/* 1622 */           askPeriodListeners.clear();
/*      */         }
/* 1624 */         if (bidPeriodListeners != null) {
/* 1625 */           bidPeriodListeners.clear();
/*      */         }
/*      */       }
/* 1628 */       if (this.allPeriodListeners[i][0] != null) {
/* 1629 */         this.allPeriodListeners[i][0].clear();
/*      */       }
/* 1631 */       if (this.allPeriodListeners[i][1] != null) {
/* 1632 */         this.allPeriodListeners[i][1].clear();
/*      */       }
/*      */     }
/* 1635 */     for (List tickListener : this.tickListeners) {
/* 1636 */       tickListener.clear();
/*      */     }
/* 1638 */     for (List cacheDataChangeListener : this.cacheDataChangeListeners) {
/* 1639 */       cacheDataChangeListener.clear();
/*      */     }
/* 1641 */     this.ordersProvider.close();
/* 1642 */     if (this == getDefaultInstance()) {
/* 1643 */       feedDataProvider = null;
/* 1644 */       curvesProtocolHandler.close();
/* 1645 */       curvesProtocolHandler = null;
/*      */     }
/*      */ 
/* 1648 */     if (this.backgroundFeedLoadingThread != null)
/* 1649 */       this.backgroundFeedLoadingThread.setFinished(true);
/*      */   }
/*      */ 
/*      */   protected LoadOrdersAction getLoadOrdersAction(Instrument instrument, long from, long to, OrdersListener ordersListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/* 1655 */     if (LOGGER.isTraceEnabled()) {
/* 1656 */       DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss SSS");
/* 1657 */       dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
/* 1658 */       LOGGER.trace(new StringBuilder().append("Loading orders instrument [").append(instrument).append("] from [").append(dateFormat.format(new Date(from))).append("] to [").append(dateFormat.format(new Date(to))).append("]").toString());
/* 1659 */       final LoadingProgressListener originalLoadingProgressListener = loadingProgress;
/* 1660 */       loadingProgress = new LoadingProgressListener() {
/*      */         private long lastCurrentTime;
/*      */ 
/*      */         public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
/* 1664 */           this.lastCurrentTime = currentTime;
/* 1665 */           FeedDataProvider.LOGGER.trace(information);
/* 1666 */           originalLoadingProgressListener.dataLoaded(startTime, endTime, currentTime, information);
/*      */         }
/*      */ 
/*      */         public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime, Exception e) {
/* 1670 */           FeedDataProvider.LOGGER.trace(new StringBuilder().append("Loading fineshed with ").append((allDataLoaded) ? "OK" : "ERROR").append(" status").toString());
/* 1671 */           originalLoadingProgressListener.loadingFinished(allDataLoaded, startTime, endTime, (allDataLoaded) ? currentTime : this.lastCurrentTime, e);
/*      */         }
/*      */ 
/*      */         public boolean stopJob() {
/* 1675 */           return originalLoadingProgressListener.stopJob();
/*      */         }
/*      */       };
/*      */     }
/* 1679 */     LoadOrdersAction loadOrdersAction = new LoadOrdersAction(this, accountId, instrument, from, to, ordersListener, loadingProgress, this.intraperiodExistsPolicy, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/*      */ 
/* 1681 */     if (this.stopped) {
/* 1682 */       loadOrdersAction.cancel();
/*      */     }
/* 1684 */     return loadOrdersAction;
/*      */   }
/*      */ 
/*      */   public void loadOrdersHistoricalData(Instrument instrument, long from, long to, OrdersListener ordersListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/* 1690 */     LoadOrdersAction loadDataAction = getLoadOrdersAction(instrument, from, to, ordersListener, loadingProgress);
/* 1691 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadOrdersHistoricalDataSynched(Instrument instrument, long from, long to, OrdersListener ordersListener, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/* 1697 */     LoadOrdersAction loadDataAction = getLoadOrdersAction(instrument, from, to, ordersListener, loadingProgress);
/* 1698 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   public void loadOrdersHistoricalDataInCache(Instrument instrument, long from, long to, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1703 */     LoadOrdersAction loadDataAction = getLoadOrdersAction(instrument, from, to, null, loadingProgress);
/* 1704 */     this.actionsExecutorService.submit(loadDataAction);
/*      */   }
/*      */ 
/*      */   public void loadOrdersHistoricalDataInCacheSynched(Instrument instrument, long from, long to, LoadingProgressListener loadingProgress) throws DataCacheException
/*      */   {
/* 1709 */     LoadOrdersAction loadDataAction = getLoadOrdersAction(instrument, from, to, null, loadingProgress);
/* 1710 */     loadDataAction.run();
/*      */   }
/*      */ 
/*      */   protected static String getEncryptionKey() {
/* 1714 */     if (encryptionKey == null) {
/* 1715 */       LOGGER.warn("Encryption key was not found, creation temporary key");
/*      */       try {
/* 1717 */         KeyGenerator kgen = KeyGenerator.getInstance("AES");
/* 1718 */         kgen.init(128);
/*      */ 
/* 1721 */         SecretKey skey = kgen.generateKey();
/* 1722 */         byte[] raw = skey.getEncoded();
/* 1723 */         encryptionKey = Hex.encodeHexString(raw);
/*      */       } catch (NoSuchAlgorithmException e) {
/* 1725 */         LOGGER.error(e.getMessage(), e);
/*      */       }
/*      */     }
/* 1728 */     return encryptionKey;
/*      */   }
/*      */ 
/*      */   public void subscribeToOrdersNotifications(Instrument instrument, OrdersListener listener)
/*      */   {
/* 1733 */     LOGGER.trace(new StringBuilder().append("Subscribing one more listener for orders notifications to instrument [").append(instrument).append("]").toString());
/* 1734 */     this.ordersProvider.addOrdersListener(instrument, listener);
/*      */   }
/*      */ 
/*      */   public void unsubscribeToOrdersNotifications(Instrument instrument, OrdersListener listener)
/*      */   {
/* 1739 */     LOGGER.trace(new StringBuilder().append("Unsubscribing orders notifications listener from instrument [").append(instrument).append("]").toString());
/* 1740 */     this.ordersProvider.removeOrdersListener(listener);
/*      */   }
/*      */ 
/*      */   public IOrdersProvider getOrdersProvider() {
/* 1744 */     return this.ordersProvider;
/*      */   }
/*      */ 
/*      */   public IOrderUtils getOrderUtils()
/*      */   {
/* 1749 */     return this.ordersProvider.getOrderUtils();
/*      */   }
/*      */ 
/*      */   public synchronized void disconnected() {
/* 1753 */     if (this.connected) {
/* 1754 */       this.connected = false;
/* 1755 */       long lastTickTime = getLastTickTime();
/* 1756 */       if (lastTickTime == -9223372036854775808L) {
/* 1757 */         lastTickTime = System.currentTimeMillis();
/*      */       }
/* 1759 */       this.disconnectTime = (lastTickTime - 300000L);
/* 1760 */       for (int i = 0; i < this.lastAsks.length; ++i) {
/* 1761 */         this.lastAsks[i] = (0.0D / 0.0D);
/* 1762 */         this.lastBids[i] = (0.0D / 0.0D);
/*      */       }
/* 1764 */       for (int i = 0; i < this.lastTicks.length; ++i) {
/* 1765 */         this.lastTicks[i] = null;
/*      */       }
/* 1767 */       this.localCacheManager.resetLastOrderUpdateTimes();
/*      */     }
/*      */   }
/*      */ 
/*      */   public synchronized void connected() {
/* 1772 */     if (!(this.connected)) {
/* 1773 */       UpdateCacheDataAction action = new UpdateCacheDataAction(this, this.disconnectTime, (isAssertionsEnabled()) ? Thread.currentThread().getStackTrace() : null);
/* 1774 */       this.actionsExecutorService.submit(action);
/* 1775 */       this.connected = true;
/*      */     }
/*      */   }
/*      */ 
/*      */   public void addCacheDataUpdatedListener(Instrument instrument, CacheDataUpdatedListener listener)
/*      */   {
				if (cacheDataChangeListeners != null)
/* 1781 */     		this.cacheDataChangeListeners[instrument.ordinal()].add(listener);
/*      */   }
/*      */ 
/*      */   public void removeCacheDataUpdatedListener(Instrument instrument, CacheDataUpdatedListener listener)
/*      */   {
	if (cacheDataChangeListeners != null)
	/* 1786 */     this.cacheDataChangeListeners[instrument.ordinal()].remove(listener);
/*      */   }
/*      */ 
/*      */   protected void fireCacheDataChanged(Instrument instrument, long disconnectedTime, long connectedTime) {
/* 1790 */     CacheDataUpdatedListener[] listeners = (CacheDataUpdatedListener[])this.cacheDataChangeListeners[instrument.ordinal()].toArray(new CacheDataUpdatedListener[this.cacheDataChangeListeners[instrument.ordinal()].size()]);
/* 1791 */     for (CacheDataUpdatedListener listener : listeners)
/* 1792 */       listener.cacheUpdated(instrument, disconnectedTime, connectedTime);
/*      */   }
/*      */ 
/*      */   public boolean isPeriodSubscribedInProgressCandle(Instrument instrument, Period period)
/*      */   {
/* 1797 */     List askListenersList = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, com.dukascopy.api.OfferSide.ASK);
/* 1798 */     List bidListenersList = getPeriodListeners(this.inProgressCandleListenersMap, instrument, period, com.dukascopy.api.OfferSide.BID);
/*      */ 
/* 1803 */     return (((askListenersList != null) && (!(askListenersList.isEmpty()))) || ((bidListenersList != null) && (!(bidListenersList.isEmpty()))));
/*      */   }
/*      */ 
/*      */   public void loadPriceRangeData(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int numberOfPriceRangesBefore, long time, int numberOfPriceRangesAfter, PriceRange priceRange, IPriceRangeLiveFeedListener priceRangeLiveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 1820 */     LoadNumberOfPriceRangeAction loadPriceRangesAction = createLoadPriceRangeAction(instrument, offerSide, numberOfPriceRangesBefore, time, numberOfPriceRangesAfter, priceRange, priceRangeLiveFeedListener, loadingProgressListener);
/*      */ 
/* 1830 */     this.actionsExecutorService.submit(loadPriceRangesAction);
/*      */   }
/*      */ 
/*      */   public void loadPriceRangeDataSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int numberOfPriceRangesBefore, long time, int numberOfPriceRangesAfter, PriceRange priceRange, IPriceRangeLiveFeedListener priceRangeLiveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 1844 */     LoadNumberOfPriceRangeAction loadPriceRangesAction = createLoadPriceRangeAction(instrument, offerSide, numberOfPriceRangesBefore, time, numberOfPriceRangesAfter, priceRange, priceRangeLiveFeedListener, loadingProgressListener);
/*      */ 
/* 1854 */     loadPriceRangesAction.run();
/*      */   }
/*      */ 
/*      */   public List<PriceRangeData> loadPriceRangeData(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int numberOfPriceRangesBefore, long time, int numberOfPriceRangesAfter, PriceRange priceRange)
/*      */   {
/* 1866 */     final List result = new ArrayList();
/* 1867 */     loadPriceRangeDataSynched(instrument, offerSide, numberOfPriceRangesBefore, time, numberOfPriceRangesAfter, priceRange, new PriceRangeLiveFeedAdapter()
/*      */     {
/*      */       public void newPriceDatas(PriceRangeData[] priceRanges, int loadedNumberBefore, int loadedNumberAfter)
/*      */       {
/* 1877 */         result.addAll(Arrays.asList(priceRanges));
/*      */       }
/*      */     }
/*      */     , new LoadingProgressAdapter()
/*      */     {
/*      */     });
/* 1882 */     return result;
/*      */   }
/*      */ 
/*      */   public PriceRangeData loadLastPriceRangeDataSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange)
/*      */   {
/* 1893 */     LastPriceRangeLiveFeedListener liveFeedListener = new LastPriceRangeLiveFeedListener();
/* 1894 */     LoadingProgressListener loadingProgressListener = new LoadingProgressAdapter()
/*      */     {
/*      */     };
/* 1896 */     LoadLatestPriceRangeAction action = new LoadLatestPriceRangeAction(this, instrument, offerSide, priceRange, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 1905 */     action.run();
/* 1906 */     return liveFeedListener.getLastData();
/*      */   }
/*      */ 
/*      */   private LoadNumberOfPriceRangeAction createLoadPriceRangeAction(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int numberOfPriceRangesBefore, long time, int numberOfPriceRangesAfter, PriceRange priceRange, IPriceRangeLiveFeedListener priceRangeLiveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 1921 */     LoadNumberOfPriceRangeAction loadPriceRangesAction = new LoadNumberOfPriceRangeAction(instrument, offerSide, numberOfPriceRangesBefore, time, numberOfPriceRangesAfter, priceRange, this, priceRangeLiveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 1933 */     return loadPriceRangesAction;
/*      */   }
/*      */ 
/*      */   public void loadPointAndFigureData(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int beforeTimeCandlesCount, long time, int afterTimeCandlesCount, PriceRange boxSize, ReversalAmount reversalAmount, IPointAndFigureLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 1949 */     Runnable action = createLoadPointAndFigureAction(this, instrument, offerSide, time, beforeTimeCandlesCount, afterTimeCandlesCount, boxSize, reversalAmount, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 1963 */     this.actionsExecutorService.submit(action);
/*      */   }
/*      */ 
/*      */   public void loadPointAndFigureDataSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, long time, int beforeTimeCandlesCount, int afterTimeCandlesCount, PriceRange boxSize, ReversalAmount reversalAmount, IPointAndFigureLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 1980 */     Runnable action = createLoadPointAndFigureAction(this, instrument, offerSide, time, beforeTimeCandlesCount, afterTimeCandlesCount, boxSize, reversalAmount, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 1994 */     action.run();
/*      */   }
/*      */ 
/*      */   public List<PointAndFigureData> loadPointAndFigureData(Instrument instrument, com.dukascopy.api.OfferSide offerSide, long time, int beforeTimeCandlesCount, int afterTimeCandlesCount, PriceRange boxSize, ReversalAmount reversalAmount)
/*      */   {
/* 2007 */     final List result = new ArrayList();
/*      */ 
/* 2009 */     loadPointAndFigureDataSynched(instrument, offerSide, time, beforeTimeCandlesCount, afterTimeCandlesCount, boxSize, reversalAmount, new PointAndFigureLiveFeedAdapter()
/*      */     {
/*      */       public void newPriceDatas(PointAndFigureData[] pointAndFigures, int loadedNumberBefore, int loadedNumberAfter)
/*      */       {
/* 2020 */         result.addAll(Arrays.asList(pointAndFigures));
/*      */       }
/*      */     }
/*      */     , new LoadingProgressAdapter()
/*      */     {
/*      */     });
/* 2026 */     return result;
/*      */   }
/*      */ 
/*      */   private Runnable createLoadPointAndFigureAction(FeedDataProvider feedDataProvider, Instrument instrument, com.dukascopy.api.OfferSide offerSide, long time, int beforeTimeCandlesCount, int afterTimeCandlesCount, PriceRange boxSize, ReversalAmount reversalAmount, IPointAndFigureLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener, CurvesDataLoader.IntraperiodExistsPolicy intraperiodExistsPolicy)
/*      */   {
/* 2044 */     Runnable action = new LoadNumberOfPointAndFigureAction(feedDataProvider, instrument, offerSide, beforeTimeCandlesCount, time, afterTimeCandlesCount, boxSize, reversalAmount, liveFeedListener, loadingProgressListener, intraperiodExistsPolicy);
/*      */ 
/* 2058 */     return action;
/*      */   }
/*      */ 
/*      */   public PointAndFigureData loadLastPointAndFigureDataSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange boxSize, ReversalAmount reversalAmount)
/*      */   {
/* 2068 */     LastPointAndFigureLiveFeedListener liveFeedListener = new LastPointAndFigureLiveFeedListener();
/* 2069 */     LoadingProgressListener loadingProgressListener = new LoadingProgressAdapter()
/*      */     {
/*      */     };
/* 2071 */     LoadLatestPointAndFigureAction action = new LoadLatestPointAndFigureAction(this, instrument, offerSide, boxSize, reversalAmount, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2082 */     action.run();
/* 2083 */     return liveFeedListener.getLastData();
/*      */   }
/*      */ 
/*      */   public void loadTickBarData(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int beforeTimeBarsCount, long time, int afterTimeBarsCount, TickBarSize tickBarSize, ITickBarLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2098 */     Runnable action = createLoadTickBarAction(this, instrument, offerSide, beforeTimeBarsCount, time, afterTimeBarsCount, tickBarSize, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2110 */     this.actionsExecutorService.submit(action);
/*      */   }
/*      */ 
/*      */   public void loadTickBarDataSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int beforeTimeBarsCount, long time, int afterTimeBarsCount, TickBarSize tickBarSize, ITickBarLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2125 */     Runnable action = createLoadTickBarAction(this, instrument, offerSide, beforeTimeBarsCount, time, afterTimeBarsCount, tickBarSize, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2137 */     action.run();
/*      */   }
/*      */ 
/*      */   public List<TickBarData> loadTickBarData(Instrument instrument, com.dukascopy.api.OfferSide offerSide, int beforeTimeBarsCount, long time, int afterTimeBarsCount, TickBarSize tickBarSize)
/*      */   {
/* 2149 */     final List result = new ArrayList();
/* 2150 */     loadTickBarDataSynched(instrument, offerSide, beforeTimeBarsCount, time, afterTimeBarsCount, tickBarSize, new TickBarLiveFeedAdapter()
/*      */     {
/*      */       public void newPriceDatas(TickBarData[] tickBars, int loadedNumberBefore, int loadedNumberAfter)
/*      */       {
/* 2160 */         result.addAll(Arrays.asList(tickBars));
/*      */       }
/*      */     }
/*      */     , new LoadingProgressAdapter()
/*      */     {
/*      */     });
/* 2165 */     return result;
/*      */   }
/*      */ 
/*      */   public TickBarData loadLastTickBarDataSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize)
/*      */   {
/* 2175 */     LastTickBarLiveFeedListener liveFeedListener = new LastTickBarLiveFeedListener();
/* 2176 */     LoadingProgressListener loadingProgressListener = new LoadingProgressAdapter()
/*      */     {
/*      */     };
/* 2178 */     LoadLatestTickBarAction action = new LoadLatestTickBarAction(this, instrument, offerSide, tickBarSize, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2187 */     action.run();
/* 2188 */     return liveFeedListener.getLastData();
/*      */   }
/*      */ 
/*      */   private Runnable createLoadTickBarAction(FeedDataProvider feedDataProvider, Instrument instrument, com.dukascopy.api.OfferSide offerSide, int beforeTimeBarsCount, long time, int afterTimeBarsCount, TickBarSize tickBarSize, ITickBarLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener, CurvesDataLoader.IntraperiodExistsPolicy intraperiodExistsPolicy)
/*      */   {
/* 2205 */     return new LoadNumberOfTickBarAction(instrument, offerSide, beforeTimeBarsCount, time, afterTimeBarsCount, tickBarSize, feedDataProvider, liveFeedListener, loadingProgressListener, intraperiodExistsPolicy);
/*      */   }
/*      */ 
/*      */   private void addTickToLatency(long time)
/*      */   {
/* 2220 */     int latency = (int)(System.currentTimeMillis() - time);
/* 2221 */     this.networkLatency[this.latencyIndex] = latency;
/* 2222 */     this.latencyIndex += 1;
/* 2223 */     if (this.latencyIndex >= this.networkLatency.length)
/* 2224 */       this.latencyIndex = 0;
/*      */   }
/*      */ 
/*      */   public synchronized int getLatency()
/*      */   {
/* 2229 */     int latencySum = 0;
/* 2230 */     for (int aNetworkLatency : this.networkLatency) {
/* 2231 */       latencySum += aNetworkLatency;
/*      */     }
/* 2233 */     return (latencySum / this.networkLatency.length);
/*      */   }
/*      */ 
/*      */   public long getEstimatedServerTime() {
/* 2237 */     return (System.currentTimeMillis() - getLatency());
/*      */   }
/*      */ 
/*      */   public void fireInProgressTickBarFormed(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, TickBarData bar)
/*      */   {
/* 2327 */     List<ITickBarLiveFeedListener> listeners = getTickBarLiveFeedListeners(this.tickBarNotificationListenersMap, instrument, offerSide, tickBarSize);
/* 2328 */     for (ITickBarLiveFeedListener listener : listeners)
/* 2329 */       listener.newPriceData(bar);
/*      */   }
/*      */ 
/*      */   public void fireInProgressTickBarUpdated(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, TickBarData bar)
/*      */   {
/* 2339 */     List<ITickBarLiveFeedListener> listeners = getTickBarLiveFeedListeners(this.inProgressTickBarLiveFeedListenersMap, instrument, offerSide, tickBarSize);
/* 2340 */     for (ITickBarLiveFeedListener listener : listeners)
/* 2341 */       listener.newPriceData(bar);
/*      */   }
/*      */ 
/*      */   public void fireInProgressPointAndFigureFormed(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, PointAndFigureData bar)
/*      */   {
/* 2352 */     List<IPointAndFigureLiveFeedListener> listeners = getPointAndFigureLiveFeedListeners(this.pointAndFigureNotificationListenersMap, instrument, offerSide, priceRange, reversalAmount);
/* 2353 */     for (IPointAndFigureLiveFeedListener listener : listeners)
/* 2354 */       listener.newPriceData(bar);
/*      */   }
/*      */ 
/*      */   public void fireInProgressPointAndFigureUpdated(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, PointAndFigureData bar)
/*      */   {
/* 2365 */     List<IPointAndFigureLiveFeedListener> listeners = getPointAndFigureLiveFeedListeners(this.inProgressPointAndFigureListenersMap, instrument, offerSide, priceRange, reversalAmount);
/* 2366 */     for (IPointAndFigureLiveFeedListener listener : listeners)
/* 2367 */       listener.newPriceData(bar);
/*      */   }
/*      */ 
/*      */   public void fireInProgressPriceRangeUpdated(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, PriceRangeData bar)
/*      */   {
/* 2377 */     List<IPriceRangeLiveFeedListener> listeners = getPriceRangeLiveFeedListeners(this.inProgressPriceRangeLiveFeedListenersMap, instrument, offerSide, priceRange);
/* 2378 */     for (IPriceRangeLiveFeedListener listener : listeners)
/* 2379 */       listener.newPriceData(bar);
/*      */   }
/*      */ 
/*      */   public void fireInProgressPriceRangeFormed(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, PriceRangeData bar)
/*      */   {
/* 2389 */     List<IPriceRangeLiveFeedListener> listeners = getPriceRangeLiveFeedListeners(this.priceRangeNotificationListenersMap, instrument, offerSide, priceRange);
/* 2390 */     for (IPriceRangeLiveFeedListener listener : listeners)
/* 2391 */       listener.newPriceData(bar);
/*      */   }
/*      */ 
/*      */   public void addInProgressPriceRangeListener(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, IPriceRangeLiveFeedListener listener)
/*      */   {
/* 2402 */     List listeners = getPriceRangeLiveFeedListeners(this.inProgressPriceRangeLiveFeedListenersMap, instrument, offerSide, priceRange);
/* 2403 */     listeners.add(listener);
/* 2404 */     this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().startToFillInProgressPriceRange(instrument, offerSide, priceRange);
/*      */   }
/*      */ 
/*      */   public void removeInProgressPriceRangeListener(IPriceRangeLiveFeedListener listener)
/*      */   {
/* 2409 */     removePriceRangeLiveFeedListener(this.inProgressPriceRangeLiveFeedListenersMap, listener);
/*      */   }
/*      */ 
/*      */   public void addPriceRangeNotificationListener(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, IPriceRangeLiveFeedListener listener)
/*      */   {
/* 2419 */     List listeners = getPriceRangeLiveFeedListeners(this.priceRangeNotificationListenersMap, instrument, offerSide, priceRange);
/* 2420 */     listeners.add(listener);
/* 2421 */     this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().startToFillInProgressPriceRange(instrument, offerSide, priceRange);
/*      */   }
/*      */ 
/*      */   public void removePriceRangeNotificationListener(IPriceRangeLiveFeedListener listener)
/*      */   {
/* 2426 */     removePriceRangeLiveFeedListener(this.priceRangeNotificationListenersMap, listener);
/*      */   }
/*      */ 
/*      */   public void addInProgressPointAndFigureListener(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, IPointAndFigureLiveFeedListener listener)
/*      */   {
/* 2438 */     List listeners = getPointAndFigureLiveFeedListeners(this.inProgressPointAndFigureListenersMap, instrument, offerSide, priceRange, reversalAmount);
/* 2439 */     listeners.add(listener);
/* 2440 */     this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().startToFillInProgressPointAndFigure(instrument, offerSide, priceRange, reversalAmount);
/*      */   }
/*      */ 
/*      */   public void removeInProgressPointAndFigureListener(IPointAndFigureLiveFeedListener listener)
/*      */   {
/* 2445 */     removePointAndFigureLiveFeedListener(this.inProgressPointAndFigureListenersMap, listener);
/*      */   }
/*      */ 
/*      */   public void addPointAndFigureNotificationListener(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, IPointAndFigureLiveFeedListener listener)
/*      */   {
/* 2456 */     List listeners = getPointAndFigureLiveFeedListeners(this.pointAndFigureNotificationListenersMap, instrument, offerSide, priceRange, reversalAmount);
/* 2457 */     listeners.add(listener);
/* 2458 */     this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().startToFillInProgressPointAndFigure(instrument, offerSide, priceRange, reversalAmount);
/*      */   }
/*      */ 
/*      */   public void removePointAndFigureNotificationListener(IPointAndFigureLiveFeedListener listener)
/*      */   {
/* 2463 */     removePointAndFigureLiveFeedListener(this.pointAndFigureNotificationListenersMap, listener);
/*      */   }
/*      */ 
/*      */   public void addInProgressTickBarListener(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, ITickBarLiveFeedListener listener)
/*      */   {
/* 2474 */     List listeners = getTickBarLiveFeedListeners(this.inProgressTickBarLiveFeedListenersMap, instrument, offerSide, tickBarSize);
/* 2475 */     listeners.add(listener);
/* 2476 */     this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().startToFillInProgressTickBar(instrument, offerSide, tickBarSize);
/*      */   }
/*      */ 
/*      */   public void removeInProgressTickBarListener(ITickBarLiveFeedListener listener)
/*      */   {
/* 2481 */     removeTickBarLiveFeedListener(this.inProgressTickBarLiveFeedListenersMap, listener);
/*      */   }
/*      */ 
/*      */   public void addTickBarNotificationListener(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, ITickBarLiveFeedListener listener)
/*      */   {
/* 2491 */     List listeners = getTickBarLiveFeedListeners(this.tickBarNotificationListenersMap, instrument, offerSide, tickBarSize);
/* 2492 */     listeners.add(listener);
/* 2493 */     this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().startToFillInProgressTickBar(instrument, offerSide, tickBarSize);
/*      */   }
/*      */ 
/*      */   public void removeTickBarNotificationListener(ITickBarLiveFeedListener listener)
/*      */   {
/* 2498 */     removeTickBarLiveFeedListener(this.tickBarNotificationListenersMap, listener);
/*      */   }
/*      */ 
/*      */   private List<IPriceRangeLiveFeedListener> getPriceRangeLiveFeedListeners(Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, List<IPriceRangeLiveFeedListener>>>> map, Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange)
/*      */   {
/* 2508 */     Map instrumentMap = (Map)map.get(instrument);
/* 2509 */     if (instrumentMap == null) {
/* 2510 */       instrumentMap = new HashMap();
/* 2511 */       map.put(instrument, instrumentMap);
/*      */     }
/* 2513 */     Map offerSideMap = (Map)instrumentMap.get(offerSide);
/* 2514 */     if (offerSideMap == null) {
/* 2515 */       offerSideMap = new HashMap();
/* 2516 */       instrumentMap.put(offerSide, offerSideMap);
/*      */     }
/* 2518 */     List list = (List)offerSideMap.get(priceRange);
/* 2519 */     if (list == null) {
/* 2520 */       list = new ArrayList();
/* 2521 */       offerSideMap.put(priceRange, list);
/*      */     }
/* 2523 */     return list;
/*      */   }
/*      */ 
/*      */   private void removePriceRangeLiveFeedListener(Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, List<IPriceRangeLiveFeedListener>>>> map, IPriceRangeLiveFeedListener listener)
/*      */   {
/* 2530 */     if (listener == null) {
/* 2531 */       return;
/*      */     }
/*      */ 
/* 2534 */     for (Iterator i$ = map.keySet().iterator(); i$.hasNext(); ) {Instrument instrument = (Instrument)i$.next();
/* 2535 */       Map instrumentMap = (Map)map.get(instrument);
/* 2536 */       if (instrumentMap == null) {
/*      */         continue;
/*      */       }
/* 2539 */       for (i$ = instrumentMap.keySet().iterator(); i$.hasNext(); ) {OfferSide offerSide = (com.dukascopy.api.OfferSide)i$.next();
/* 2540 */         Map<PriceRange, ?> offerSideMap = (Map)instrumentMap.get(offerSide);
/* 2541 */         if (offerSideMap == null) {
/*      */           continue;
/*      */         }
/* 2544 */         for (PriceRange priceRange : offerSideMap.keySet()) {
/* 2545 */           List list = (List)offerSideMap.get(priceRange);
/* 2546 */           if (list == null) {
/*      */             continue;
/*      */           }
/* 2549 */           if (list.contains(listener)) {
/* 2550 */             list.remove(listener);
/*      */ 
/* 2553 */             if (list.isEmpty())
/*      */             {
/* 2557 */               this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().stopToFillInProgressPriceRange(instrument, offerSide, priceRange);
/*      */             }
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*      */     Instrument instrument;
/*      */     Map instrumentMap;
/*      */     Iterator i$;
/*      */     com.dukascopy.api.OfferSide offerSide;
/*      */     Map offerSideMap;
/*      */   }
/*      */ 
/*      */   private List<IPointAndFigureLiveFeedListener> getPointAndFigureLiveFeedListeners(Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, Map<ReversalAmount, List<IPointAndFigureLiveFeedListener>>>>> map, Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount)
/*      */   {
/* 2572 */     Map instrumentMap = (Map)map.get(instrument);
/* 2573 */     if (instrumentMap == null) {
/* 2574 */       instrumentMap = new HashMap();
/* 2575 */       map.put(instrument, instrumentMap);
/*      */     }
/* 2577 */     Map offerSideMap = (Map)instrumentMap.get(offerSide);
/* 2578 */     if (offerSideMap == null) {
/* 2579 */       offerSideMap = new HashMap();
/* 2580 */       instrumentMap.put(offerSide, offerSideMap);
/*      */     }
/* 2582 */     Map priceRangeMap = (Map)offerSideMap.get(priceRange);
/* 2583 */     if (priceRangeMap == null) {
/* 2584 */       priceRangeMap = new HashMap();
/* 2585 */       offerSideMap.put(priceRange, priceRangeMap);
/*      */     }
/* 2587 */     List list = (List)priceRangeMap.get(reversalAmount);
/* 2588 */     if (list == null) {
/* 2589 */       list = new ArrayList();
/* 2590 */       priceRangeMap.put(reversalAmount, list);
/*      */     }
/* 2592 */     return list;
/*      */   }
/*      */ 
/*      */   private void removePointAndFigureLiveFeedListener(Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<PriceRange, Map<ReversalAmount, List<IPointAndFigureLiveFeedListener>>>>> map, IPointAndFigureLiveFeedListener listener)
/*      */   {
/* 2599 */     if (listener == null) {
/* 2600 */       return;
/*      */     }
/*      */ 
/* 2603 */     for (Iterator i$ = map.keySet().iterator(); i$.hasNext(); ) { Instrument instrument = (Instrument)i$.next();
/* 2604 */       Map instrumentMap = (Map)map.get(instrument);
/* 2605 */       if (instrumentMap == null) {
/*      */         continue;
/*      */       }
/* 2608 */       for (i$ = instrumentMap.keySet().iterator(); i$.hasNext(); ) { OfferSide offerSide = (com.dukascopy.api.OfferSide)i$.next();
/* 2609 */         Map offerSideMap = (Map)instrumentMap.get(offerSide);
/* 2610 */         if (offerSideMap == null) {
/*      */           continue;
/*      */         }
/* 2613 */         for (i$ = offerSideMap.keySet().iterator(); i$.hasNext(); ) { PriceRange priceRange = (PriceRange)i$.next();
/* 2614 */           Map<ReversalAmount,?> priceRangeMap = (Map)offerSideMap.get(priceRange);
/* 2615 */           if (priceRangeMap == null) {
/*      */             continue;
/*      */           }
/* 2618 */           for (ReversalAmount reversalAmount : priceRangeMap.keySet()) {
/* 2619 */             List list = (List)priceRangeMap.get(reversalAmount);
/* 2620 */             if (list == null) {
/*      */               continue;
/*      */             }
/* 2623 */             if (list.contains(listener)) {
/* 2624 */               list.remove(listener);
/*      */ 
/* 2627 */               if (list.isEmpty())
/*      */               {
/* 2631 */                 this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().stopToFillInProgressPointAndFigure(instrument, offerSide, priceRange, reversalAmount); } } }
/*      */         }
/*      */       }
/*      */     }
/*      */     Instrument instrument;
/*      */     Map instrumentMap;
/*      */     Iterator i$;
/*      */     com.dukascopy.api.OfferSide offerSide;
/*      */     Map offerSideMap;
/*      */     PriceRange priceRange;
/*      */     Map priceRangeMap;
/*      */   }
/*      */ 
/*      */   private List<ITickBarLiveFeedListener> getTickBarLiveFeedListeners(Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<TickBarSize, List<ITickBarLiveFeedListener>>>> map, Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize) {
/* 2646 */     Map instrumentMap = (Map)map.get(instrument);
/* 2647 */     if (instrumentMap == null) {
/* 2648 */       instrumentMap = new HashMap();
/* 2649 */       map.put(instrument, instrumentMap);
/*      */     }
/* 2651 */     Map offerSideMap = (Map)instrumentMap.get(offerSide);
/* 2652 */     if (offerSideMap == null) {
/* 2653 */       offerSideMap = new HashMap();
/* 2654 */       instrumentMap.put(offerSide, offerSideMap);
/*      */     }
/* 2656 */     List list = (List)offerSideMap.get(tickBarSize);
/* 2657 */     if (list == null) {
/* 2658 */       list = new ArrayList();
/* 2659 */       offerSideMap.put(tickBarSize, list);
/*      */     }
/* 2661 */     return list;
/*      */   }
/*      */ 
/*      */   private void removeTickBarLiveFeedListener(Map<Instrument, Map<com.dukascopy.api.OfferSide, Map<TickBarSize, List<ITickBarLiveFeedListener>>>> map, ITickBarLiveFeedListener listener)
/*      */   {
/* 2668 */     if (listener == null) {
/* 2669 */       return;
/*      */     }
/*      */ 
/* 2672 */     for (Iterator i$ = map.keySet().iterator(); i$.hasNext(); ) {Instrument instrument = (Instrument)i$.next();
/* 2673 */       Map instrumentMap = (Map)map.get(instrument);
/* 2674 */       if (instrumentMap == null) {
/*      */         continue;
/*      */       }
/* 2677 */       for (i$ = instrumentMap.keySet().iterator(); i$.hasNext(); ) {OfferSide offerSide = (com.dukascopy.api.OfferSide)i$.next();
/* 2678 */         Map<TickBarSize, ?> offerSideMap = (Map)instrumentMap.get(offerSide);
/* 2679 */         if (offerSideMap == null) {
/*      */           continue;
/*      */         }
/* 2682 */         for (TickBarSize tickBarSize : offerSideMap.keySet()) {
/* 2683 */           List list = (List)offerSideMap.get(tickBarSize);
/* 2684 */           if (list == null) {
/*      */             continue;
/*      */           }
/* 2687 */           if (list.contains(listener)) {
/* 2688 */             list.remove(listener);
/*      */ 
/* 2691 */             if (list.isEmpty())
/*      */             {
/* 2695 */               this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().stopToFillInProgressTickBar(instrument, offerSide, tickBarSize); } } } }
/*      */     }
/*      */     Instrument instrument;
/*      */     Map instrumentMap;
/*      */     Iterator i$;
/*      */     com.dukascopy.api.OfferSide offerSide;
/*      */     Map offerSideMap;
/*      */   }
/*      */ 
/*      */   public PointAndFigureData getInProgressPointAndFigure(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount) {
/* 2705 */     return this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().getInProgressPointAndFigure(instrument, offerSide, priceRange, reversalAmount);
/*      */   }
/*      */ 
/*      */   public PointAndFigureData getOrLoadInProgressPointAndFigure(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount)
/*      */   {
/* 2710 */     PointAndFigureData bar = getInProgressPointAndFigure(instrument, offerSide, priceRange, reversalAmount);
/* 2711 */     if (bar == null) {
/* 2712 */       bar = loadLastPointAndFigureDataSynched(instrument, offerSide, priceRange, reversalAmount);
/*      */     }
/* 2714 */     return bar;
/*      */   }
/*      */ 
/*      */   public PriceRangeData getInProgressPriceRange(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange)
/*      */   {
/* 2719 */     return this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().getInProgressPriceRange(instrument, offerSide, priceRange);
/*      */   }
/*      */ 
/*      */   public PriceRangeData getOrLoadInProgressPriceRange(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange)
/*      */   {
/* 2724 */     PriceRangeData bar = getInProgressPriceRange(instrument, offerSide, priceRange);
/* 2725 */     if (bar == null) {
/* 2726 */       bar = loadLastPriceRangeDataSynched(instrument, offerSide, priceRange);
/*      */     }
/* 2728 */     return bar;
/*      */   }
/*      */ 
/*      */   public TickBarData getInProgressTickBar(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize)
/*      */   {
/* 2733 */     return this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().getInProgressTickBar(instrument, offerSide, tickBarSize);
/*      */   }
/*      */ 
/*      */   public TickBarData getOrLoadInProgressTickBar(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize)
/*      */   {
/* 2738 */     TickBarData bar = getInProgressTickBar(instrument, offerSide, tickBarSize);
/* 2739 */     if (bar == null) {
/* 2740 */       bar = loadLastTickBarDataSynched(instrument, offerSide, tickBarSize);
/*      */     }
/* 2742 */     return bar;
/*      */   }
/*      */ 
/*      */   public long getLatestKnownTimeOrCurrentGMTTime(Instrument instrument) {
/* 2746 */     if (instrument == null) {
/* 2747 */       throw new NullPointerException("Params are not correct!");
/*      */     }
/*      */ 
/* 2750 */     long time = getLastTickTime(instrument);
/* 2751 */     if (-9223372036854775808L == time) {
/* 2752 */       time = Calendar.getInstance(TimeZone.getTimeZone("GMT 0")).getTimeInMillis();
/*      */     }
/* 2754 */     return time;
/*      */   }
/*      */ 
/*      */   public boolean isInProgressPriceRangeLoadingNow(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange)
/*      */   {
/* 2763 */     return this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().isInProgressPriceRangeLoadingNow(instrument, offerSide, priceRange);
/*      */   }
/*      */ 
/*      */   public boolean isInProgressTickBarLoadingNow(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize)
/*      */   {
/* 2772 */     return this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().isInProgressTickBarLoadingNow(instrument, offerSide, tickBarSize);
/*      */   }
/*      */ 
/*      */   public boolean isInProgressPointAndFigureLoadingNow(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount)
/*      */   {
/* 2782 */     return this.intraperiodCandlesGenerator.getIntraperiodBarsGenerator().isInProgressPointAndFigureLoadingNow(instrument, offerSide, priceRange, reversalAmount);
/*      */   }
/*      */ 
/*      */   public void loadPriceRangeTimeIntervalSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, long fromTime, long toTime, IPriceRangeLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2795 */     Runnable action = new LoadPriceRangeTimeIntervalAction(instrument, offerSide, priceRange, fromTime, toTime, this, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2806 */     action.run();
/*      */   }
/*      */ 
/*      */   public void loadPriceRangeTimeInterval(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, long fromTime, long toTime, IPriceRangeLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2819 */     Runnable action = new LoadPriceRangeTimeIntervalAction(instrument, offerSide, priceRange, fromTime, toTime, this, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2830 */     this.actionsExecutorService.submit(action);
/*      */   }
/*      */ 
/*      */   public void loadPointAndFigureTimeIntervalSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, long fromTime, long toTime, IPointAndFigureLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2845 */     Runnable action = new LoadPointAndFigureTimeIntervalAction(instrument, offerSide, priceRange, reversalAmount, fromTime, toTime, this, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2857 */     action.run();
/*      */   }
/*      */ 
/*      */   public void loadPointAndFigureTimeInterval(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, long fromTime, long toTime, IPointAndFigureLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2870 */     Runnable action = new LoadPointAndFigureTimeIntervalAction(instrument, offerSide, priceRange, reversalAmount, fromTime, toTime, this, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2883 */     this.actionsExecutorService.submit(action);
/*      */   }
/*      */ 
/*      */   public void loadTickBarTimeIntervalSynched(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, long fromTime, long toTime, ITickBarLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2897 */     Runnable action = new LoadTickBarTimeIntervalAction(instrument, offerSide, tickBarSize, fromTime, toTime, this, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2908 */     action.run();
/*      */   }
/*      */ 
/*      */   public void loadTickBarTimeInterval(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, long fromTime, long toTime, ITickBarLiveFeedListener liveFeedListener, LoadingProgressListener loadingProgressListener)
/*      */   {
/* 2921 */     Runnable action = new LoadTickBarTimeIntervalAction(instrument, offerSide, tickBarSize, fromTime, toTime, this, liveFeedListener, loadingProgressListener, this.intraperiodExistsPolicy);
/*      */ 
/* 2932 */     this.actionsExecutorService.submit(action);
/*      */   }
/*      */ 
/*      */   public List<TickData> loadTicksBefore(Instrument instrument, int itemsCount, long toDateMs)
/*      */     throws Exception
/*      */   {
/* 2939 */     return null;
/*      */   }
/*      */ 
/*      */   public List<TickData> loadTicksAfter(Instrument instrument, long fromDateMs, int itemsCount)
/*      */     throws Exception
/*      */   {
/* 2945 */     return null;
/*      */   }
/*      */ 
/*      */   public List<CandleData> loadCandlesBefore(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, int itemsCount, long toDateMs)
/*      */     throws Exception
/*      */   {
/* 2952 */     return null;
/*      */   }
/*      */ 
/*      */   public List<CandleData> loadCandlesAfter(Instrument instrument, Period period, com.dukascopy.api.OfferSide side, long fromDateMs, int itemsCount)
/*      */     throws Exception
/*      */   {
/* 2959 */     return null;
/*      */   }
/*      */ 
/*      */   public void startInBackgroundFeedPreloadingToLocalCache()
/*      */   {
/* 2964 */     if (this.backgroundFeedLoadingThread == null) {
/* 2965 */       this.backgroundFeedLoadingThread = new BackgroundFeedLoadingThread(this);
/* 2966 */       this.backgroundFeedLoadingThread.start();
/*      */     }
/*      */   }
/*      */ 
/*      */   public List<Weekend> calculateWeekends(Period period, int expectedNumberOfCandles, long expectedFrom, long expectedTo, LoadingProgressListener loadingProgress)
/*      */     throws DataCacheException
/*      */   {
/* 2977 */     Instrument instrument = Instrument.values()[0];
/* 2978 */     List weekends = getApproximateWeekends(expectedFrom, expectedTo);
/*      */     SaveCandlesLoadingProgressListener c10MinProgressListener;
/*      */     SaveCandlesLiveFeedListener c10MinLiveFeedListener;
/* 2980 */     if (period.getInterval() < Period.FOUR_HOURS.getInterval()) {
/* 2981 */       List<Weekend> weekendsCopy = new ArrayList(weekends);
/*      */       Iterator iterator;
/*      */       Iterator cachedWeekendIterator;
/* 2984 */       synchronized (this.cachedWeekends) {
/* 2985 */         for (iterator = weekendsCopy.iterator(); iterator.hasNext(); ) {
/* 2986 */           Weekend weekend = (Weekend)iterator.next();
/* 2987 */           for (cachedWeekendIterator = this.cachedWeekends.iterator(); cachedWeekendIterator.hasNext(); ) {
/* 2988 */             Weekend cachedWeekend = (Weekend)cachedWeekendIterator.next();
/* 2989 */             if ((cachedWeekend.getStart() <= weekend.getStart()) && (cachedWeekend.getEnd() >= weekend.getEnd())) {
/* 2990 */               weekend.setStart(cachedWeekend.getStart());
/* 2991 */               weekend.setEnd(cachedWeekend.getEnd());
/* 2992 */               iterator.remove();
/*      */ 
/* 2995 */               cachedWeekendIterator.remove();
/* 2996 */               this.cachedWeekends.addFirst(cachedWeekend);
/* 2997 */               break;
/*      */             }
/*      */           }
/*      */ 
/*      */         }
/*      */ 
/*      */       }
/*      */ 
/* 3005 */       c10MinProgressListener = new SaveCandlesLoadingProgressListener(loadingProgress);
/* 3006 */       c10MinLiveFeedListener = new SaveCandlesLiveFeedListener();
/* 3007 */       for (Weekend weekend : weekendsCopy) {
/* 3008 */         if (loadingProgress.stopJob()) {
/* 3009 */           loadingProgress.loadingFinished(false, 0L, expectedNumberOfCandles, expectedNumberOfCandles, null);
/* 3010 */           return null;
/*      */         }
/* 3012 */         c10MinLiveFeedListener.getSavedCandles().clear();
/* 3013 */         LoadDataAction loadDataAction = new LoadDataAction(this, instrument, Period.TEN_MINS, com.dukascopy.api.OfferSide.BID, weekend.getStart() - 3000000L, weekend.getStart() - 600000L, c10MinLiveFeedListener, c10MinProgressListener, null, false, this.intraperiodExistsPolicy, false);
/*      */ 
/* 3027 */         loadDataAction.run();
/* 3028 */         if (!(c10MinProgressListener.isLoadedSuccessfully())) {
/* 3029 */           loadingProgress.loadingFinished(false, 0L, expectedNumberOfCandles, expectedNumberOfCandles, c10MinProgressListener.getException());
/*      */ 
/* 3036 */           return null;
/*      */         }
/*      */         int i = 1;
/* 3039 */         boolean cache = true;
/* 3040 */         if (c10MinLiveFeedListener.getSavedCandles().size() < 5) {
/* 3041 */           cache = false;
/* 3042 */           i = 0;
/*      */         } else {
/* 3044 */           for (CandleData candle : c10MinLiveFeedListener.getSavedCandles()) {
/* 3045 */             if ((candle.open != candle.close) || (candle.close != candle.low) || (candle.low != candle.high) || (candle.vol != 0.0D)) {
/* 3046 */               i = 0;
/* 3047 */               break;
/*      */             }
/*      */           }
/*      */         }
/* 3051 */         if (i != 0) {
/* 3052 */           weekend.setStart(weekend.getStart() - 3600000L);
/*      */         }
/*      */ 
/* 3055 */         if (loadingProgress.stopJob()) {
/* 3056 */           loadingProgress.loadingFinished(false, 0L, expectedNumberOfCandles, expectedNumberOfCandles, null);
/* 3057 */           return null;
/*      */         }
/* 3059 */         c10MinLiveFeedListener.getSavedCandles().clear();
/* 3060 */         loadDataAction = new LoadDataAction(this, instrument, Period.TEN_MINS, com.dukascopy.api.OfferSide.BID, weekend.getEnd(), weekend.getEnd() + 2400000L, c10MinLiveFeedListener, c10MinProgressListener, null, false, this.intraperiodExistsPolicy, false);
/*      */ 
/* 3074 */         loadDataAction.run();
/* 3075 */         if (!(c10MinProgressListener.isLoadedSuccessfully())) {
/* 3076 */           loadingProgress.loadingFinished(false, 0L, expectedNumberOfCandles, expectedNumberOfCandles, c10MinProgressListener.getException());
/*      */ 
/* 3083 */           return null;
/*      */         }
/* 3085 */         i = 1;
/* 3086 */         if (c10MinLiveFeedListener.getSavedCandles().size() < 5) {
/* 3087 */           cache = false;
/* 3088 */           i = 0;
/*      */         } else {
/* 3090 */           for (CandleData candle : c10MinLiveFeedListener.getSavedCandles()) {
/* 3091 */             if ((candle.open != candle.close) || (candle.close != candle.low) || (candle.low != candle.high) || (candle.vol != 0.0D)) {
/* 3092 */               i = 0;
/* 3093 */               break;
/*      */             }
/*      */           }
/*      */         }
/* 3097 */         if (i != 0) {
/* 3098 */           weekend.setEnd(weekend.getEnd() + 3600000L);
/*      */         }
/*      */ 
/* 3102 */         if (cache) {
/* 3103 */           synchronized (this.cachedWeekends) {
/* 3104 */             this.cachedWeekends.addFirst(new Weekend(weekend.getStart(), weekend.getEnd()));
/* 3105 */             while (this.cachedWeekends.size() > 40) {
/* 3106 */               this.cachedWeekends.removeLast();
/*      */             }
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/* 3113 */     Period calcPeriod = period;
/* 3114 */     if (period == Period.TICK) {
/* 3115 */       calcPeriod = Period.ONE_SEC;
/*      */     }
/* 3117 */     for (Iterator iterator = weekends.iterator(); iterator.hasNext(); ) {
/* 3118 */       Weekend weekendTimes = (Weekend)iterator.next();
/*      */ 
/* 3121 */       long weekendStartCandle = DataCacheUtils.getCandleStartFast(calcPeriod, weekendTimes.getStart());
/* 3122 */       if (weekendStartCandle < weekendTimes.getStart())
/*      */       {
/* 3125 */         weekendStartCandle = DataCacheUtils.getNextCandleStartFast(calcPeriod, weekendStartCandle);
/*      */       }
/*      */       long weekendEndCandle;
/* 3129 */       if ((period == Period.DAILY_SKIP_SUNDAY) || (period == Period.DAILY_SUNDAY_IN_MONDAY))
/*      */       {
/* 3131 */         weekendEndCandle = DataCacheUtils.getCandleStartFast(calcPeriod, weekendTimes.getEnd());
/*      */       }
/*      */       else weekendEndCandle = DataCacheUtils.getPreviousCandleStartFast(calcPeriod, DataCacheUtils.getCandleStartFast(calcPeriod, weekendTimes.getEnd()));
/*      */ 
/* 3137 */       if (weekendStartCandle > weekendEndCandle)
/*      */       {
/* 3139 */         iterator.remove();
/*      */       }
/*      */ 
/* 3142 */       if (DataCacheUtils.getNextCandleStartFast(calcPeriod, weekendStartCandle) > weekendTimes.getEnd())
/*      */       {
/* 3144 */         iterator.remove();
/*      */       }
/*      */ 
/* 3147 */       weekendTimes.setStart(weekendStartCandle);
/* 3148 */       weekendTimes.setEnd(weekendEndCandle);
/*      */     }
/* 3150 */     return weekends;
/*      */   }
/*      */ 
/*      */   public List<Weekend> getApproximateWeekends(long expectedFrom, long expectedTo)
/*      */   {
/* 3163 */     List result = new ArrayList();
/*      */ 
/* 3165 */     Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
/* 3166 */     cal.setFirstDayOfWeek(2);
/* 3167 */     cal.setTimeInMillis(expectedFrom);
/* 3168 */     cal.set(11, 22);
/* 3169 */     cal.set(12, 0);
/* 3170 */     cal.set(13, 0);
/* 3171 */     cal.set(14, 0);
/* 3172 */     cal.set(7, 1);
/* 3173 */     if (cal.getTimeInMillis() < expectedFrom) {
/* 3174 */       cal.add(4, 1);
/*      */     }
/*      */ 
/* 3177 */     cal.set(7, 6);
/* 3178 */     cal.set(11, 21);
/* 3179 */     while (cal.getTimeInMillis() <= expectedTo) {
/* 3180 */       cal.set(11, 22);
/* 3181 */       Weekend weekend = new Weekend();
/* 3182 */       weekend.setStart(cal.getTimeInMillis());
/* 3183 */       cal.set(7, 1);
/* 3184 */       cal.set(11, 21);
/* 3185 */       weekend.setEnd(cal.getTimeInMillis());
/* 3186 */       result.add(weekend);
/* 3187 */       cal.set(7, 6);
/* 3188 */       cal.add(4, 1);
/*      */     }
/*      */ 
/* 3191 */     return result;
/*      */   }
/*      */ 
/*      */   public List<PointAndFigureData> loadPointAndFigureTimeInterval(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, ReversalAmount reversalAmount, long fromTime, long toTime)
/*      */   {
/* 3203 */     final List result = new ArrayList();
/*      */ 
/* 3205 */     loadPointAndFigureTimeIntervalSynched(instrument, offerSide, priceRange, reversalAmount, fromTime, toTime, new IPointAndFigureLiveFeedListener()
/*      */     {
/*      */       public void newPriceDatas(PointAndFigureData[] pointAndFigures, int loadedNumberBefore, int loadedNumberAfter)
/*      */       {
/* 3215 */         result.addAll(Arrays.asList(pointAndFigures));
/*      */       }
/*      */ 
/*      */       public void newPriceData(PointAndFigureData pointAndFigure)
/*      */       {
/*      */       }
/*      */     }
/*      */     , new LoadingProgressAdapter()
/*      */     {
/*      */     });
/* 3224 */     return result;
/*      */   }
/*      */ 
/*      */   public List<PriceRangeData> loadPriceRangeTimeInterval(Instrument instrument, com.dukascopy.api.OfferSide offerSide, PriceRange priceRange, long fromTime, long toTime)
/*      */   {
/* 3235 */     final List result = new ArrayList();
/*      */ 
/* 3237 */     feedDataProvider.loadPriceRangeTimeIntervalSynched(instrument, offerSide, priceRange, fromTime, toTime, new IPriceRangeLiveFeedListener()
/*      */     {
/*      */       public void newPriceDatas(PriceRangeData[] priceRanges, int loadedNumberBefore, int loadedNumberAfter)
/*      */       {
/* 3246 */         result.addAll(Arrays.asList(priceRanges));
/*      */       }
/*      */ 
/*      */       public void newPriceData(PriceRangeData priceRange)
/*      */       {
/*      */       }
/*      */     }
/*      */     , new LoadingProgressAdapter()
/*      */     {
/*      */     });
/* 3255 */     return result;
/*      */   }
/*      */ 
/*      */   public List<TickBarData> loadTickBarTimeInterval(Instrument instrument, com.dukascopy.api.OfferSide offerSide, TickBarSize tickBarSize, long fromTime, long toTime)
/*      */   {
/* 3266 */     final List result = new ArrayList();
/*      */ 
/* 3268 */     loadTickBarTimeIntervalSynched(instrument, offerSide, tickBarSize, fromTime, toTime, new ITickBarLiveFeedListener()
/*      */     {
/*      */       public void newPriceDatas(TickBarData[] tickBars, int loadedNumberBefore, int loadedNumberAfter)
/*      */       {
/* 3277 */         result.addAll(Arrays.asList(tickBars));
/*      */       }
/*      */ 
/*      */       public void newPriceData(TickBarData tickBar)
/*      */       {
/*      */       }
/*      */     }
/*      */     , new LoadingProgressAdapter()
/*      */     {
/*      */     });
/* 3287 */     return result;
/*      */   }
/*      */ 
/*      */   static
/*      */   {
/*   89 */     LOGGER = LoggerFactory.getLogger(FeedDataProvider.class);
/*      */ 
/*  147 */     timesOfTheFirstCandle = new long[][] { { 1175270475663L, 9223372036854775807L, 9223372036854775807L, 1070112970000L, 9223372036854775807L, 9223372036854775807L, 1070226900000L, 1113609600000L, 1070110800000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1070470800000L, 1113609600000L, 1078704000000L, 1069632000000L, 1067644800000L, 9223372036854775807L }, { 1229962562419L, 9223372036854775807L, 9223372036854775807L, 1165808700000L, 9223372036854775807L, 9223372036854775807L, 1165808700000L, 1165808700000L, 1165808400000L, 1165808700000L, 9223372036854775807L, 1165807800000L, 1165806000000L, 1165795200000L, 1165795200000L, 1165795200000L, 1164931200000L, 9223372036854775807L }, { 1175270476387L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270476026L, 9223372036854775807L, 9223372036854775807L, 1113609600000L, 9223372036854775807L, 9223372036854775807L, 1121787600000L, 1113609600000L, 1113609600000L, 1113609600000L, 9223372036854775807L, 1113609600000L, 1121889600000L, 1113609600000L, 1122249600000L, 1113177600000L, 1112313600000L, 9223372036854775807L }, { 1175270475792L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270479411L, 9223372036854775807L, 9223372036854775807L, 1128664410000L, 9223372036854775807L, 9223372036854775807L, 1128664440000L, 1128664200000L, 1128664800000L, 1128663900000L, 9223372036854775807L, 1128664800000L, 1128664800000L, 1128657600000L, 1137456000000L, 1129507200000L, 1130803200000L, 9223372036854775807L }, { 1222169529237L, 9223372036854775807L, 9223372036854775807L, 1113609600000L, 9223372036854775807L, 9223372036854775807L, 1113771600000L, 1113609600000L, 1113609600000L, 1113609600000L, 9223372036854775807L, 1113609600000L, 1113969600000L, 1113609600000L, 1122249600000L, 1113177600000L, 1112313600000L, 9223372036854775807L }, { 1175270475961L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1175270477607L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1175270476372L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270479725L, 9223372036854775807L, 9223372036854775807L, 1113609600000L, 9223372036854775807L, 9223372036854775807L, 1133253600000L, 1113609600000L, 1113609600000L, 1113609600000L, 9223372036854775807L, 1113609600000L, 1133409600000L, 1113609600000L, 1156809600000L, 1113177600000L, 1112313600000L, 9223372036854775807L }, { 1175270491333L, 9223372036854775807L, 9223372036854775807L, 1101686400000L, 9223372036854775807L, 9223372036854775807L, 1100628000000L, 1113609600000L, 1100628000000L, 1100628000000L, 9223372036854775807L, 1113609600000L, 1100628000000L, 1113609600000L, 1100649600000L, 1101081600000L, 1088640000000L, 9223372036854775807L }, { 1175270475688L, 9223372036854775807L, 9223372036854775807L, 1059323190000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1058548200000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1052366400000L, 1113609600000L, 887068800000L, 516240000000L, 517968000000L, 9223372036854775807L }, { 1175270475889L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060938900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1061366400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270475843L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270475684L, 9223372036854775807L, 9223372036854775807L, 1059323190000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1058548200000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1052366400000L, 1113609600000L, 912988800000L, 841017600000L, 873072000000L, 9223372036854775807L }, { 1175270513207L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1128664200000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1128663000000L, 1060286400000L, 1128657600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270476382L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1060286400000L, 1113609600000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1175270475764L, 9223372036854775807L, 9223372036854775807L, 1059323190000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1058548200000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1052366400000L, 1113609600000L, 912988800000L, 837993600000L, 873072000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1175270475901L, 9223372036854775807L, 9223372036854775807L, 1059323190000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1113609600000L, 1058548200000L, 1098662400000L, 9223372036854775807L, 1113609600000L, 1052366400000L, 1113609600000L, 912988800000L, 830736000000L, 873072000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1222639495329L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1220541300000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1219071600000L, 1060286400000L, 1219060800000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1222644631734L, 9223372036854775807L, 9223372036854775807L, 1059927990000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1220541300000L, 1059927600000L, 1098662400000L, 9223372036854775807L, 1219071600000L, 1060286400000L, 1219060800000L, 1059868800000L, 1059350400000L, 1059696000000L, 9223372036854775807L }, { 1222644407937L, 9223372036854775807L, 9223372036854775807L, 1101686400000L, 9223372036854775807L, 9223372036854775807L, 1100658000000L, 1220541300000L, 1100628000000L, 1100628000000L, 9223372036854775807L, 1219071600000L, 1100628000000L, 1219060800000L, 1100649600000L, 1101081600000L, 1088640000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1266319146054L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319365518L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319643809L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319689690L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319707873L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319739866L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319767040L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319790394L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319808274L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319851540L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1289491801742L, 9223372036854775807L, 9223372036854775807L, 1059331520000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1244905200000L, 1058548200000L, 1098662400000L, 9223372036854775807L, 1244905200000L, 1052366400000L, 1244894400000L, 895708800000L, 872467200000L, 873072000000L, 9223372036854775807L }, { 1289491789179L, 9223372036854775807L, 9223372036854775807L, 1059331520000L, 9223372036854775807L, 9223372036854775807L, 1060356900000L, 1244905200000L, 1058548200000L, 1098662400000L, 9223372036854775807L, 1244905200000L, 1052366400000L, 1244894400000L, 895708800000L, 871862400000L, 873072000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L } };
/*      */ 
/*  199 */     timesOfTheFirstOurCandle = new long[][] { { 1175270475663L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1121790000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1122465600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1229962562419L, 9223372036854775807L, 9223372036854775807L, 1165808700000L, 9223372036854775807L, 9223372036854775807L, 1165808700000L, 1165808700000L, 1165808400000L, 1165808700000L, 9223372036854775807L, 1165807800000L, 1165806000000L, 1165795200000L, 1165795200000L, 1165795200000L, 1164931200000L, 9223372036854775807L }, { 1175270476387L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113786000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270476026L, 9223372036854775807L, 9223372036854775807L, 1113609600000L, 9223372036854775807L, 9223372036854775807L, 1121787600000L, 1113609600000L, 1113609600000L, 1113609600000L, 9223372036854775807L, 1113609600000L, 1121889600000L, 1113609600000L, 1122249600000L, 1113177600000L, 1112313600000L, 9223372036854775807L }, { 1175270475792L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1121790000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1122465600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270479411L, 9223372036854775807L, 9223372036854775807L, 1128664410000L, 9223372036854775807L, 9223372036854775807L, 1128664440000L, 1128664200000L, 1128664800000L, 1128663900000L, 9223372036854775807L, 1128664800000L, 1128664800000L, 1128657600000L, 1137456000000L, 1129507200000L, 1130803200000L, 9223372036854775807L }, { 1222169529237L, 9223372036854775807L, 9223372036854775807L, 1113609600000L, 9223372036854775807L, 9223372036854775807L, 1113771600000L, 1113609600000L, 1113609600000L, 1113609600000L, 9223372036854775807L, 1113609600000L, 1113969600000L, 1113609600000L, 1122249600000L, 1113177600000L, 1112313600000L, 9223372036854775807L }, { 1175270475961L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1175270477607L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1175270476372L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270479725L, 9223372036854775807L, 9223372036854775807L, 1113609600000L, 9223372036854775807L, 9223372036854775807L, 1133253600000L, 1113609600000L, 1113609600000L, 1113609600000L, 9223372036854775807L, 1113609600000L, 1133409600000L, 1113609600000L, 1156809600000L, 1113177600000L, 1112313600000L, 9223372036854775807L }, { 1175270491333L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1175502000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1170345600000L, 1113609600000L, 1231113600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270475688L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270475889L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1129638000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1129665600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270475843L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1121790000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1122465600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270475684L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270513207L, 9223372036854775807L, 9223372036854775807L, 1128729600000L, 9223372036854775807L, 9223372036854775807L, 1128993600000L, 1128664200000L, 1130803200000L, 1130803200000L, 9223372036854775807L, 1128663000000L, 1130803200000L, 1128657600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270476382L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113786000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 1175270475764L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1175270475901L, 9223372036854775807L, 9223372036854775807L, 1113696000000L, 9223372036854775807L, 9223372036854775807L, 1113780000000L, 1113609600000L, 1114905600000L, 1114905600000L, 9223372036854775807L, 1113609600000L, 1115265600000L, 1113609600000L, 1144713600000L, 1135555200000L, 1136073600000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1222639495329L, 9223372036854775807L, 9223372036854775807L, 1222646400000L, 9223372036854775807L, 9223372036854775807L, 1222646400000L, 1222646400000L, 1220227200000L, 1222819200000L, 9223372036854775807L, 1219071600000L, 1225267200000L, 1219060800000L, 1230768000000L, 1230508800000L, 1230768000000L, 9223372036854775807L }, { 1222644631734L, 9223372036854775807L, 9223372036854775807L, 1222646400000L, 9223372036854775807L, 9223372036854775807L, 1222646400000L, 1222646400000L, 1220227200000L, 1222819200000L, 9223372036854775807L, 1219071600000L, 1225267200000L, 1219060800000L, 1230768000000L, 1230508800000L, 1230768000000L, 9223372036854775807L }, { 1222644407937L, 9223372036854775807L, 9223372036854775807L, 1222646400000L, 9223372036854775807L, 9223372036854775807L, 1222652400000L, 1222646400000L, 1220227200000L, 1222819200000L, 9223372036854775807L, 1219071600000L, 1225267200000L, 1219060800000L, 1144713600000L, 1230508800000L, 1230768000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 1266319146054L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319365518L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319643809L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319689690L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319707873L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319739866L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319767040L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319790394L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319808274L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136241600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1266319851540L, 9223372036854775807L, 9223372036854775807L, 1136073600000L, 9223372036854775807L, 9223372036854775807L, 1136247600000L, 1136073600000L, 1136073600000L, 1136073600000L, 9223372036854775807L, 1136073600000L, 1136433600000L, 1136073600000L, 1144713600000L, 1135555200000L, 1125532800000L, 9223372036854775807L }, { 1289491801742L, 9223372036854775807L, 9223372036854775807L, 1289520000000L, 9223372036854775807L, 9223372036854775807L, 1289520000000L, 1289520000000L, 1291161600000L, 1291161600000L, 9223372036854775807L, 1291161600000L, 1291161600000L, 1293840000000L, 1302480000000L, 1293408000000L, 1293840000000L, 9223372036854775807L }, { 1289491789179L, 9223372036854775807L, 9223372036854775807L, 1289520000000L, 9223372036854775807L, 9223372036854775807L, 1289526000000L, 1289520000000L, 1291161600000L, 1291161600000L, 9223372036854775807L, 1291161600000L, 1298361600000L, 1293840000000L, 1302480000000L, 1293408000000L, 1293840000000L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L }, { 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L, 9223372036854775807L } };
/*      */   }
/*      */ 
/*      */   private static class FeedAuthenticator
/*      */     implements IAuthenticator
/*      */   {
/*      */     private final Collection<String> authServerUrls;
/*      */     private final String version;
/*      */     private final String userName;
/*      */     private final String instanceId;
/*      */ 
/*      */     private FeedAuthenticator(Collection<String> authServerUrls, String version, String userName, String instanceId)
/*      */     {
/* 2301 */       this.authServerUrls = authServerUrls;
/* 2302 */       this.version = version;
/* 2303 */       this.userName = userName;
/* 2304 */       this.instanceId = instanceId;
/*      */     }
/*      */ 
/*      */     public String authenticate()
/*      */     {
/* 2309 */       AuthorizationClient auClient = AuthorizationClient.getInstance(this.authServerUrls, this.version);
/*      */       try {
/* 2311 */         return auClient.getFeedUrlAndTicket(this.userName, FeedDataProvider.platformTicket, this.instanceId);
/*      */       } catch (IOException e) {
/* 2313 */         FeedDataProvider.LOGGER.error(e.getMessage(), e);
/*      */       } catch (NoSuchAlgorithmException e) {
/* 2315 */         FeedDataProvider.LOGGER.error(e.getMessage(), e);
/*      */       }
/* 2317 */       return null;
/*      */     }
/*      */   }
/*      */ 
/*      */   public static class FeedExecutor extends ThreadPoolExecutor
/*      */   {
/*      */     private final List<Runnable> currentlyRunningTasks;
/*      */ 
/*      */     public FeedExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler, List<Runnable> currentlyRunningTasks)
/*      */     {
/* 2268 */       super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
/* 2269 */       this.currentlyRunningTasks = currentlyRunningTasks;
/*      */     }
/*      */ 
/*      */     protected void beforeExecute(Thread t, Runnable r)
/*      */     {
/* 2274 */       synchronized (this.currentlyRunningTasks) {
/* 2275 */         this.currentlyRunningTasks.add(r);
/*      */       }
/* 2277 */       super.beforeExecute(t, r);
/*      */     }
/*      */ 
/*      */     protected void afterExecute(Runnable r, Throwable t)
/*      */     {
/* 2282 */       synchronized (this.currentlyRunningTasks) {
/* 2283 */         this.currentlyRunningTasks.remove(r);
/*      */       }
/* 2285 */       super.afterExecute(r, t);
/*      */     }
/*      */   }
/*      */ 
/*      */   public static class FeedThreadFactory
/*      */     implements ThreadFactory
/*      */   {
/*      */     final AtomicInteger threadNumber;
/*      */ 
/*      */     public FeedThreadFactory()
/*      */     {
/* 2241 */       this.threadNumber = new AtomicInteger(1); }
/*      */ 
/*      */     public Thread newThread(Runnable r) {
/* 2244 */       Thread thread = new Thread(r, "FeedDataProvider_ActionsThread_" + this.threadNumber.getAndIncrement());
/* 2245 */       if (!(thread.isDaemon())) {
/* 2246 */         thread.setDaemon(true);
/*      */       }
/* 2248 */       if (thread.getPriority() != 5) {
/* 2249 */         thread.setPriority(5);
/*      */       }
/* 2251 */       return thread;
/*      */     }
/*      */   }
/*      */
private static IContext context = ContextLoader.getInstance();
private Hashtable<Instrument, Quote> lastQuotes = new Hashtable<Instrument, Quote>();
public TickData getLastTick(Instrument instrument) {
	IDataQueue q = context.getQueue(Quote.class);
	if (q.size() != 0) {
		Quote quote = (Quote)q.pop();
		lastQuotes.put(instrument, quote);
		return new TickData(System.currentTimeMillis(), quote.getHi(), quote.getLow(), quote.getVol(), quote.getVol());
	}
	else
		return null;
}

public IBar getLastCandle(Instrument instrument, Period period, OfferSide side) {
	Quote lastQuote = lastQuotes.get(instrument);
	if (lastQuote == null) {
		IDataQueue q = context.getQueue(Quote.class);
		if (q.size() != 0) {
			Quote quote = (Quote)q.pop();
			lastQuotes.put(instrument, quote);
			lastQuote = quote;
		}
		else
			return null;
	}
	return new CandleData(lastQuote.getDate().getTime(), lastQuote.getOpen(), lastQuote.getClose(), lastQuote.getLow(), lastQuote.getHi(), lastQuote.getVol());
} }

/* Location:           C:\Projects\jforex\libs\greed-common-162.jar
 * Qualified Name:     com.dukascopy.charts.data.datacache.FeedDataProvider
 * Java Class Version: 6 (50.0)
 * JD-Core Version:    0.5.3
 */