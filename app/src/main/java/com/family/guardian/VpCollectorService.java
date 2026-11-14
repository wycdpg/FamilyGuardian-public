package com.family.guardian;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IABleConnectStatusListener;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;
import com.veepoo.protocol.listener.data.AbsBloodGlucoseChangeListener;
import com.veepoo.protocol.listener.data.IAutoMeasureSettingDataListener;
import com.veepoo.protocol.listener.data.IBatteryDataListener;
import com.veepoo.protocol.listener.data.IBPDetectDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.IHeartDataListener;
import com.veepoo.protocol.listener.data.IHrvDetectListener;
import com.veepoo.protocol.listener.data.IHRVOriginDataListener;
import com.veepoo.protocol.listener.data.IPressureDetectListener;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.listener.data.IPersonInfoDataListener;
import com.veepoo.protocol.listener.data.IResponseListener;
import com.veepoo.protocol.listener.data.ISleepDataListener;
import com.veepoo.protocol.listener.data.ISocialMsgDataListener;
import com.veepoo.protocol.listener.data.ISpo2hDataListener;
import com.veepoo.protocol.listener.data.ISportDataListener;
import com.veepoo.protocol.model.datas.AutoMeasureData;
import com.veepoo.protocol.model.datas.BatteryData;
import com.veepoo.protocol.model.datas.HRVOriginData;
import com.veepoo.protocol.model.datas.BpData;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.datas.HeartData;
import com.veepoo.protocol.model.datas.PersonInfoData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.HeartWaringData;
import com.veepoo.protocol.model.datas.EcgDetectInfo;
import com.veepoo.protocol.model.datas.EcgDetectState;
import com.veepoo.protocol.model.datas.EcgDetectResult;
import com.veepoo.protocol.model.datas.EcgDiagnosis;
import com.veepoo.protocol.model.datas.MiniCheckupResultData;
import com.veepoo.protocol.model.datas.MiniCheckupBloodComponent;
import com.veepoo.protocol.model.datas.MiniCheckupBodyComponent;
import com.veepoo.protocol.model.datas.MiniCheckupSkinElectricity;
import com.veepoo.protocol.model.datas.MiniCheckupDetailData;
import com.veepoo.protocol.model.datas.BloodComponent;
import com.veepoo.protocol.model.datas.BodyComponent;
import com.veepoo.protocol.model.datas.GsrDetectResult;
import com.veepoo.protocol.listener.data.IBloodComponentDetectListener;
import com.veepoo.protocol.listener.data.IBodyComponentDetectListener;
import com.veepoo.protocol.listener.data.IGsrDetectListener;
import com.veepoo.protocol.model.enums.EBloodComponentDetectState;
import com.veepoo.protocol.model.enums.GsrDetectAck;
import com.veepoo.protocol.model.enums.DepressionRisk;
import com.veepoo.protocol.model.enums.DetectState;
import com.veepoo.protocol.model.enums.EMiniCheckupTestErrorCode;
import com.veepoo.protocol.model.enums.ESocailMsg;
import com.veepoo.protocol.model.settings.HeartWaringSetting;
import com.veepoo.protocol.model.settings.ContentSmsSetting;
import com.veepoo.protocol.listener.IMiniCheckupOptListener;
import com.veepoo.protocol.listener.data.IHeartWaringDataListener;
import com.veepoo.protocol.listener.data.IECGDetectListener;
import com.veepoo.protocol.listener.data.ITemptureDetectDataListener;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.veepoo.protocol.model.datas.Spo2hData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.model.datas.TimeData;
import com.veepoo.protocol.model.datas.TemptureDetectData;
import com.veepoo.protocol.model.enums.EBloodGlucoseRiskLevel;
import com.veepoo.protocol.model.enums.EBloodGlucoseStatus;
import com.veepoo.protocol.model.enums.EAutoMeasureType;
import com.veepoo.protocol.model.enums.EBPDetectModel;
import com.veepoo.protocol.model.enums.PressureDetectState;
import com.veepoo.protocol.model.enums.EBPDetectStatus;
import com.veepoo.protocol.model.enums.EDeviceStatus;
import com.veepoo.protocol.model.enums.EFunctionStatus;
import com.veepoo.protocol.model.enums.EHeartStatus;
import com.veepoo.protocol.model.enums.ESex;
import com.veepoo.protocol.model.enums.ETimeMode;
import com.veepoo.protocol.model.enums.HrvDetectState;
import com.veepoo.protocol.model.settings.DeviceTimeSetting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 前台采集服务：自动连接手环、定时采集、CSV 落盘、通知界面更新 */
public class VpCollectorService extends Service {
    public static final String ACTION_UPDATE = "com.family.guardian.UPDATE";
    public static final String ACTION_RESCAN = "com.family.guardian.RESCAN";
    public static final String ACTION_SETALERT = "com.family.guardian.SETALERT";
    public static final String ACTION_MICRO = "com.family.guardian.MICRO";
    public static final String ACTION_ECG = "com.family.guardian.ECG";
    public static final String ACTION_BC = "com.family.guardian.BC";
    public static final String ACTION_BODY = "com.family.guardian.BODY";
    public static final String ACTION_GSR = "com.family.guardian.GSR";
    public static final String ACTION_ACC = "com.family.guardian.ACC";
    public static final String ACTION_SETFAMILY = "com.family.guardian.SETFAMILY";
    public static final String ACTION_SNAPSHOT = "com.family.guardian.SNAPSHOT";
    public static final String ACTION_SETUSERINFO = "com.family.guardian.SETUSERINFO";
    public static final String ACTION_SETTHRESH = "com.family.guardian.SETTHRESH";
    public static final String ACTION_FALLCONFIRM = "com.family.guardian.FALLCONFIRM";
    public static final String ACTION_FALLHELP = "com.family.guardian.FALLHELP";
    public static final String ACTION_TESTBAND = "com.family.guardian.TESTBAND";
    private static final String TAG = "WOZAI";
    private static final String TARGET_MAC = "00:00:00:00:00:00"; // 使用前改成自己的手环 MAC；也可只靠 H600 名称匹配
    private static final String CHANNEL_ID = "vp_collector";
    private static final long MIN5 = 5 * 60 * 1000L;
    private static final long MIN30 = 30 * 60 * 1000L;
    private static final long MIN60 = 60 * 60 * 1000L;
    public static final String ACTION_TICK = "com.family.guardian.TICK";
    public static final String ACTION_RETRY = "com.family.guardian.RETRY";
    private static final long MIN3 = 3 * 60 * 1000L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock = null; // 采集期间防止CPU睡着
    private final java.util.ArrayList<Integer> ecgWaveBuf = new java.util.ArrayList<Integer>();
    private String mac = null, name = null;
    private boolean flowStarted = false;   // 连接/采集链路是否正忙
    private boolean connected = false;
    private boolean autoReconnect = false;
    private final boolean[] found = {false};

    // 一轮采集内的状态（严格一条命令等回调）
    private boolean roundLight = false;
    private boolean roundBoost = false;      // 醒来后加速采集轮(心率+血氧，跳过血压)
    private long wakeBoostUntil = 0L;        // 醒来后30分钟内进入3分钟节拍
    private String lastSleepUpKey = null;    // 已处理过的"睡醒"段，避免重复触发
    private boolean batteryDone = false, stepDone = false, hrRequested = false, spo2Requested = false;
    private boolean bpRequested = false, hrvRequested = false, sleepRequested = false;
    private boolean hrDone = false, spo2Done = false, bpDone = false, hrvDone = false;
    private boolean personSynced = false;
    private boolean bpSupported = true, hrvSupported = true;
    private boolean sugarSupported = true, stressSupported = true;
    private boolean tempSupported = false;

    // 最近一次采到的值（供 CSV/波形/界面使用）
    private int lastBatt = -1, lastSteps = -1, lastHr = -1, lastSpo2 = -1;
    private int lastBpH = -1, lastBpL = -1, lastHrv = -1;
    private int lastDis = -1, lastKcal = -1;
    private float lastSugar = -1; private int lastStress = -1;
    private long lastSugarAt = 0, lastStressAt = 0;
    private boolean sugarRequested = false, stressRequested = false, sugarDone = false, stressDone = false;
    private float sugarTmp = -1;
    private float lastTemp = -1, tempTmp = -1; private long lastTempAt = 0;
    private boolean tempRequested = false, tempDone = false;
    private int hrvOriginBest = -1; // 本轮HRV历史读到的最新有效值
    private boolean hrvDetectRunning = false;
    private final java.util.ArrayList<Integer> hrvDetectVals = new java.util.ArrayList<>();
    private final java.util.ArrayList<HRVOriginData> hrvOriginList = new java.util.ArrayList<>();
    private int hrvDetectMedian = -1;
    private boolean hrvOriginReadStarted = false;
    private boolean autoHrvChecked = false; // 每次连接只检查一次手环HRV自动测量开关
    private int sleepMin = -1, sleepDeep = -1, sleepLight = -1, sleepWakeN = -1;
    private int sleepRem = -1, sleepAwakeMin = -1;   // 快速眼动/清醒分钟数(从sleepLine解析)
    private String sleepSeg = null, sleepExtFrom = null;     // 分期时间轴"起-止:码;..." 与补救段起点
    private String sleepDown = null, sleepUp = null;
    // 高心率/低血氧手环震动提醒开关 + 微体检/心电图状态
    private boolean alertHrOn = true, alertSpo2On = true;
    private long lastHrAlertAt = 0, lastSpo2AlertAt = 0;
    // 可调整告警阈值；第一次越界手环震动，连续第二次越界发短信给家人
    private int spo2Thresh = 90;
    private int hrHighThresh = 140;
    private int hrLowThresh = 45;
    private int hrHighStage = 0, hrLowStage = 0, spo2Stage = 0;
    private float sugarThresh = 3.9f;
    private float fallGThresh = 2.5f;
    private int sugarStage = 0;
    private long lastSugarAlertAt = 0;
    private boolean ecgRunning = false, microRunning = false, bcRunning = false, bodyRunning = false, gsrRunning = false, manualBusy = false;
    private Boolean heartWaringSupported = null, ecgSupported = null, microSupported = null;
    private Boolean bcSupported = null, bodySupported = null, gsrSupported = null;
    private boolean roundHasSleep = false;
    private long lastBpAt = 0, lastHrvAt = 0, lastSleepAt = 0;
    private SleepData sleepBest = null;
    private String sleepBestDay = null;   // sleepBest 对应的day标记(0=最近一天)
    private Runnable sleepWaiter = null;

    // 当日波形序列 + CSV
    private final JSONArray seq = new JSONArray();
    private String seqDate = "";
    private int tick = 0;
    private boolean autoToastShown = false; // 自动重连只在第一次弹提示
    private int watchDay = 3; // 手环端保存的睡眠天数（SDK 实际拼写为 WathcDay）
    private int bpFailCount = 0; // 血压连续失败次数，连续2次才判定不可用
    private int bpTmpH = -1, bpTmpL = -1; // 血压测量期间的最后有效读数
    private boolean hrvUnsupLatch = false;
    private boolean hrvFallback = false; // HRV当天历史读不到时，再试读前一天的 // 一旦SDK报告HRV不支持，永久关闭HRV采集
    private boolean hrvUnsupSent = false;

    // ===== 云端同步（v1.11）：把最新数值 + 异常提醒上传到云端 =====
    private static final long CLOUD_UPLOAD_INTERVAL = 3 * 60 * 1000L;
    private boolean cloudUploading = false;
    private boolean configPulling = false;

    private void scheduleCloudUpload(long delay) {
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                pullCloudConfig();
                uploadSnapshot(false);
                maybeUploadResearchLogs();
                ui.postDelayed(this, CLOUD_UPLOAD_INTERVAL);
            }
        }, delay);
    }

    // ===== 健康数据上传（v1.14，默认开，老人端设置可关）：CSV+判定日志 每小时同步到 家庭码/logs/ =====
    private long lastLogUploadAt = 0;
    private boolean logUploading = false;

    private void maybeUploadResearchLogs() {
        try {
            if (!CloudSync.logUploadOn(this)) return;
            if (CloudSync.code(this).length() == 0) return;
            long now = System.currentTimeMillis();
            if (now - lastLogUploadAt < 60 * 60 * 1000L) return;
            if (logUploading) return;
            logUploading = true;
            lastLogUploadAt = now;
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
                        java.io.File csv = csvFile();
                        if (csv.exists() && csv.length() > 0) {
                            CloudSync.uploadBytes(VpCollectorService.this, CloudSync.code(VpCollectorService.this) + "/logs/" + day + ".csv", readAllBytes(csv));
                        }
                        java.io.File al = new java.io.File(getExternalFilesDir(null), "alarm.log");
                        if (al.exists() && al.length() > 0) {
                            CloudSync.uploadBytes(VpCollectorService.this, CloudSync.code(VpCollectorService.this) + "/logs/" + day + "_alarm.txt", readAllBytes(al));
                        }
                        Log.w(TAG, "research logs uploaded");
                    } catch (Exception e) {
                        Log.w(TAG, "research log upload err " + e);
                    } finally {
                        logUploading = false;
                    }
                }
            }).start();
        } catch (Exception e) {
            logUploading = false;
        }
    }

    private static byte[] readAllBytes(java.io.File f) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(f);
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = fis.read(buf)) > 0) bo.write(buf, 0, n);
        fis.close();
        return bo.toByteArray();
    }

    /** 远程设置：每轮上传前先拉一次 家庭码/config.json，家人端改的阈值自动生效 */
    private void pullCloudConfig() {
        try {
            if (configPulling) return;
            if (!CloudSync.configured() || CloudSync.code(this).length() == 0) return;
            configPulling = true;
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        String json = CloudSync.downloadConfig(VpCollectorService.this, CloudSync.code(VpCollectorService.this));
                        if (json == null || json.length() == 0) return;
                        JSONObject cfg = new JSONObject(json);
                        int spo2 = clamp(cfg.optInt("spo2", spo2Thresh), 70, 99);
                        int high = clamp(cfg.optInt("hrHigh", hrHighThresh), 80, 220);
                        int low = clamp(cfg.optInt("hrLow", hrLowThresh), 30, 90);
                        float sugar = clampF((float) cfg.optDouble("sugarLow", sugarThresh), 2.0f, 6.0f);
                        float fall = clampF((float) cfg.optDouble("fallG", fallGThresh), 1.5f, 5.0f);
                        if (low >= high) low = high - 10;
                        boolean changed = spo2 != spo2Thresh || high != hrHighThresh || low != hrLowThresh
                                || sugar != sugarThresh || fall != fallGThresh;
                        if (!changed) { Log.w(TAG, "cloud config unchanged"); return; }
                        spo2Thresh = spo2; hrHighThresh = high; hrLowThresh = low;
                        sugarThresh = sugar; fallGThresh = fall;
                        hrHighStage = 0; hrLowStage = 0; spo2Stage = 0; sugarStage = 0;
                        getSharedPreferences("fg_thresh", Context.MODE_PRIVATE).edit()
                                .putInt("spo2", spo2).putInt("hrHigh", high).putInt("hrLow", low)
                                .putFloat("sugarLow", sugar).putFloat("fallG", fall).apply();
                        Log.w(TAG, "cloud config applied spo2=" + spo2 + " hrHigh=" + high + " hrLow=" + low + " sugarLow=" + sugar + " fallG=" + fall);
                        writeAlarmLog("CLOUD CONFIG spo2=" + spo2 + " hrHigh=" + high + " hrLow=" + low + " sugarLow=" + sugar + " fallG=" + fall);
                        sendToast("家人更新了告警阈值，已自动生效");
                    } catch (Exception e) {
                        Log.w(TAG, "config pull err " + e);
                    } finally {
                        configPulling = false;
                    }
                }
            }).start();
        } catch (Exception e) {
            configPulling = false;
            Log.w(TAG, "config pull err " + e);
        }
    }

    private void uploadSnapshot(boolean withAlert) {
        try {
            if (cloudUploading) return;
            if (!CloudSync.configured() || CloudSync.code(this).length() == 0) return;
            cloudUploading = true;
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
                        o.put("ts", System.currentTimeMillis());
                        o.put("connected", connected ? 1 : 0);
                        o.put("hr", lastHr);
                        o.put("spo2", lastSpo2);
                        o.put("bpHigh", lastBpH);
                        o.put("bpLow", lastBpL);
                        o.put("hrv", lastHrv);
                        o.put("steps", lastSteps);
                        o.put("kcal", lastKcal);
                        o.put("dis", lastDis);
                        o.put("batt", lastBatt);
                        if (lastTemp > 0) o.put("temp", Math.round(lastTemp * 10) / 10.0);
                        if (lastSugar > 0) o.put("sugar", Math.round(lastSugar * 10) / 10.0);
                        o.put("stress", lastStress);
                        JSONObject sl = new JSONObject();
                        sl.put("min", sleepMin); sl.put("deep", sleepDeep); sl.put("light", sleepLight); sl.put("rem", sleepRem);
                        o.put("sleep", sl);
                        JSONObject th = new JSONObject();
                        th.put("spo2", spo2Thresh); th.put("hrHigh", hrHighThresh); th.put("hrLow", hrLowThresh); th.put("sugarLow", sugarThresh);
                        th.put("fallG", fallGThresh);
                        o.put("thresh", th);
                        o.put("alerts", CloudSync.getAlerts(VpCollectorService.this));
                        o.put("microReports", getMicroReports());
                        CloudSync.upload(VpCollectorService.this, o);
                        Log.w(TAG, "cloud upload ok" + (withAlert ? " (alert)" : ""));
                    } catch (Exception e) {
                        Log.w(TAG, "cloud upload err " + e);
                    } finally {
                        cloudUploading = false;
                    }
                }
            }).start();
        } catch (Exception e) {
            cloudUploading = false;
            Log.w(TAG, "cloud snapshot err " + e);
        }
    }

    /** 异常发生时：记一条提醒，并立刻上传 */
    private void cloudAlert(String type, String msg, String val) {
        try {
            JSONObject a = new JSONObject();
            a.put("type", type);
            a.put("msg", msg);
            a.put("val", val);
            a.put("ts", System.currentTimeMillis());
            a.put("time", new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
            CloudSync.addAlert(this, a);
            writeAlarmLog("CLOUD ALERT " + type + " " + msg);
            uploadSnapshot(true);
        } catch (Exception e) { Log.w(TAG, "cloud alert err " + e); }
    }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        Log.w(TAG, "service onCreate");
        startForeground(1, buildNotification());
        VPOperateManager.getInstance().init(this);
        VPOperateManager.getInstance().setAutoConnectBTBySdk(false);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(rx, new android.content.IntentFilter(ACTION_RESCAN));
        lbm.registerReceiver(rx3, new android.content.IntentFilter(ACTION_SETALERT));
        lbm.registerReceiver(rxFamily, new android.content.IntentFilter(ACTION_SETFAMILY));
        lbm.registerReceiver(rxThresh, new android.content.IntentFilter(ACTION_SETTHRESH));
        android.content.IntentFilter fbf = new android.content.IntentFilter(ACTION_FALLCONFIRM);
        fbf.addAction(ACTION_FALLHELP);
        lbm.registerReceiver(rxFall, fbf);
        lbm.registerReceiver(rx5, new android.content.IntentFilter(ACTION_SETUSERINFO));
        android.content.IntentFilter mef = new android.content.IntentFilter(ACTION_MICRO);
        mef.addAction(ACTION_ECG);
        mef.addAction(ACTION_BC);
        mef.addAction(ACTION_BODY);
        mef.addAction(ACTION_GSR);
        lbm.registerReceiver(rx4, mef);
        lbm.registerReceiver(rx2, new android.content.IntentFilter(ACTION_SNAPSHOT));
        lbm.registerReceiver(rxAcc, new android.content.IntentFilter(ACTION_ACC));
        registerReceiver(smsResultRx, new android.content.IntentFilter("com.family.guardian.SMS_SENT"));
        // 系统蓝牙开关变化：蓝牙关闭时暂停重连，避免SDK空指针崩溃
        try {
            android.content.IntentFilter btf = new android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(btRx, btf);
        } catch (Exception e) { Log.w(TAG, "bt receiver err " + e); }
        loadFamilyPhones();
        loadThresholds();
        loadSeqFromCsv();
        // 云端同步：启动30秒后传第一次，之后每3分钟传一次
        scheduleCloudUpload(30000L);
        // 每30秒推送一次完整数据（含全部记录），避免某些华为机型快照广播不触发。
        final Runnable recsTick = new Runnable() {
            @Override public void run() {
                try { if (connected) broadcastAll(); } catch (Exception e) { Log.w(TAG, "recsTick err " + e); }
                ui.postDelayed(this, 30000L);
            }
        };
        ui.postDelayed(recsTick, 10000L);
        ui.postDelayed(new Runnable() { @Override public void run() { startConnectFlow(false); } }, 800);
        // 定时心跳：每 5 分钟一跳，逢单数轻轮（只测心率）、逢双数整轮
        scheduleTick(MIN5);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());
        if (intent != null && ACTION_TICK.equals(intent.getAction())) runTick();
        else if (intent != null && ACTION_RETRY.equals(intent.getAction())) { if (!connected && !flowStarted) startConnectFlow(true); }
        else if (intent != null && ACTION_SNAPSHOT.equals(intent.getAction())) broadcastAll();
        if (intent != null && ACTION_TESTBAND.equals(intent.getAction())) {
            if (!connected) { sendToast("手环未连接，不能测试提醒"); }
            else { writeAlarmLog("TEST BAND MSG"); sendBandMsg("测试提醒：看到这条说明手环提醒正常"); }
        }
        return START_STICKY;
    }

    // 用系统级闹钟安排下一次采集：即使手机进入深度省电(Doze)也会尽量准点唤醒
    private void scheduleTick(long delay) {
        long at = System.currentTimeMillis() + delay;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent it = new Intent(this, VpCollectorService.class).setAction(ACTION_TICK);
        PendingIntent pi = PendingIntent.getService(this, 1001, it,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
        Log.w(TAG, "next tick scheduled in " + delay + "ms");
    }

    // 采集期间握住 CPU 唤醒锁，防止测量中途 CPU 睡着导致命令超时
    private void holdWake() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vp:round");
            wakeLock.setReferenceCounted(false);
        }
        try { wakeLock.acquire(15 * 60 * 1000L); } catch (Exception ignored) { }
    }

    private void releaseWake() {
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) { }
    }

    // 醒来后30分钟内用3分钟节拍，其余时间5分钟
    private boolean boostActive() { return System.currentTimeMillis() < wakeBoostUntil; }
    private long nextDelay() { return boostActive() ? MIN3 : MIN5; }

    private void runTick() {
        tick++;
        Log.w(TAG, "tick=" + tick + " connected=" + connected + " busy=" + flowStarted);
        scheduleTick(nextDelay());
        if (connected) {
            // 三轴看门狗：数据流静默超2分钟则重新下发启动指令，防止跌倒监测悄悄失效
            long nowAcc = System.currentTimeMillis();
            if (nowAcc - lastAccSampleAt > 120000L) {
                Log.w(TAG, "acc watchdog: silent " + ((nowAcc - lastAccSampleAt) / 1000) + "s, restart gsensor");
                accRunning = false;
                startFallMonitor();
            }
        }
        if (!connected && !flowStarted) { startConnectFlow(true); }
        else if (connected && !flowStarted && !manualBusy) {
            holdWake();
            if (boostActive()) startBoostRound();
            else if (tick % 2 == 1) startLightRound(); else startFullRound();
        }
    }

    private final android.content.BroadcastReceiver rx = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            if (i.getAction() != null && ACTION_SETALERT.equals(i.getAction())) {
                alertHrOn = i.getBooleanExtra("hr", true);
                alertSpo2On = i.getBooleanExtra("spo2", true);
                Log.w(TAG, "alert toggles hr=" + alertHrOn + " spo2=" + alertSpo2On);
                return;
            }
            if (connected) { sendToast("手环已连接，无需重扫"); return; }
            if (flowStarted) { sendToast("正在扫描/采集中，请稍候"); return; }
            if (!flowStarted) startConnectFlow(false);
        }
    };
    private final android.content.BroadcastReceiver rx2 = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            broadcastAll();
            if (!connected && !flowStarted) {
                Log.w(TAG, "snapshot: not connected, restart flow");
                ui.postDelayed(new Runnable() { @Override public void run() { if (!connected && !flowStarted) startConnectFlow(false); } }, 1000);
            }
        }
    };

    // 设置开关：App里高心率/低血氧震动开关同步过来
    private final android.content.BroadcastReceiver rx3 = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) { }
    };
    // 个人信息同步（来自设置页）
    private final android.content.BroadcastReceiver rx5 = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            try {
                int h = i.getIntExtra("h", 0), w = i.getIntExtra("w", 0), a = i.getIntExtra("age", 0);
                String sex = i.getStringExtra("sex");
                if (h <= 0 || w <= 0 || a <= 0) { sendToast("个人信息不完整，没同步"); return; }
                android.content.SharedPreferences sp = getSharedPreferences("fg_user", Context.MODE_PRIVATE);
                sp.edit().putInt("h", h).putInt("w", w).putInt("age", a).putString("sex", sex == null ? "女" : sex).apply();
                personSynced = false;
                sendToast("个人信息已同步到手环");
                syncPersonForce();
            } catch (Exception e) { Log.w(TAG, "setuser err " + e); }
        }
    };
    // 微体检/心电图启动指令（来自页面按钮）
    private final android.content.BroadcastReceiver rx4 = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            if (i.getAction() == null) return;
            if (ACTION_MICRO.equals(i.getAction())) startMicro();
            else if (ACTION_ECG.equals(i.getAction())) startEcg();
            else if (ACTION_BC.equals(i.getAction())) startBc();
            else if (ACTION_BODY.equals(i.getAction())) startBody();
            else if (ACTION_GSR.equals(i.getAction())) startGsr();
        }
    };
    // 蓝牙开关状态：关闭时停扫不重试；打开时自动恢复重连
    private final android.content.BroadcastReceiver btRx = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            int st = i.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1);
            if (st == android.bluetooth.BluetoothAdapter.STATE_OFF) {
                Log.w(TAG, "bluetooth OFF -> pause reconnect");
                flowStarted = false;
                try { VPOperateManager.getInstance().stopScanDevice(); } catch (Exception ignored) { }
                sendToast("手机蓝牙已关闭，暂停重连；打开蓝牙后会自动继续");
            } else if (st == android.bluetooth.BluetoothAdapter.STATE_ON) {
                Log.w(TAG, "bluetooth ON -> resume reconnect");
                if (!connected && !flowStarted) ui.postDelayed(new Runnable() { @Override public void run() { if (!connected && !flowStarted) startConnectFlow(true); } }, 1200);
            }
        }
    };

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "手环数据采集", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setContentTitle("我在 · 手环采集中")
                .setContentText("断开后会自动重连，无需操作")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true).build();
    }

    // ===== 连接主流程 =====
    private void startConnectFlow(boolean auto) {
        Log.w(TAG, "flow requested auto=" + auto + " started=" + flowStarted + " connected=" + connected);
        if (flowStarted) return;
        if (!ensureReady()) return;
        flowStarted = true;
        batteryDone = stepDone = hrRequested = spo2Requested = bpRequested = hrvRequested = sleepRequested = false;
        roundBoost = false;
        hrDone = spo2Done = bpDone = hrvDone = stressDone = sugarDone = false;
        personSynced = false;
        found[0] = false;
        if (!auto || !autoToastShown) { sendToast(auto ? "正在自动重连手环…" : "正在扫描手环…"); autoToastShown = true; }
        Log.w(TAG, "scan started");
        try {
            VPOperateManager.getInstance().startScanDevice(20, scanResponse);
        } catch (Exception e) {
            // 蓝牙中途被关等情况下SDK可能抛空指针，不能让进程崩溃
            Log.w(TAG, "scan start err: " + e);
            flowStarted = false;
            sendToast("蓝牙扫描启动失败，请确认蓝牙开关已打开");
            if (auto) scheduleRetry();
            return;
        }
        ui.postDelayed(new Runnable() { @Override public void run() {
            if (!found[0] && !connected) {
                Log.w(TAG, "not found in 13s, switch to auto retry");
                flowStarted = false;
                autoReconnect = true;
                sendToast("没扫到手环，3秒后自动重试");
                ui.postDelayed(new Runnable() { @Override public void run() { if (!connected) startConnectFlow(true); } }, 3000);
            }
        }}, 13000);
    }

    private boolean ensureReady() {
        android.bluetooth.BluetoothAdapter ad = ((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (ad == null || !ad.isEnabled()) { sendToast("请先打开手机蓝牙"); return false; }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            sendToast("缺少定位权限，无法扫描蓝牙");
            return false;
        }
        return true;
    }

    private final SearchResponse scanResponse = new SearchResponse() {
        @Override public void onSearchStarted() { }
        @Override public void onSearchStopped() { if (!found[0] && !connected) { Log.w(TAG, "scan stopped, not found"); flowStarted = false; } }
        @Override public void onSearchCanceled() { }
        @Override public void onDeviceFounded(com.inuker.bluetooth.library.search.SearchResult d) {
            String dName = d.getName() == null ? "" : d.getName();
            String dMac = d.getAddress() == null ? "" : d.getAddress();
            Log.w(TAG, "found: " + dName + " / " + dMac);
            if (!found[0] && (dMac.equalsIgnoreCase(TARGET_MAC) || dName.toUpperCase().contains("H600"))) {
                found[0] = true;
                mac = dMac; name = dName.isEmpty() ? "H600" : dName;
                VPOperateManager.getInstance().stopScanDevice();
                connect();
            }
        }
    };

    private void connect() {
        Log.w(TAG, "connecting to " + mac + " name=" + name);
        sendToast("正在连接手环…");
        VPOperateManager.getInstance().registerConnectStatusListener(mac, statusListener);
        VPOperateManager.getInstance().connectDevice(mac, name, new IConnectResponse() {
            @Override public void connectState(int code, com.inuker.bluetooth.library.model.BleGattProfile profile, boolean isOad) {
                if (code != Code.REQUEST_SUCCESS) {
                    Log.w(TAG, "connect failed code=" + code);
                    sendToast("手环连接失败，正在重试");
                    flowStarted = false;
                    if (autoReconnect) scheduleRetry();
                }
            }
        }, new INotifyResponse() {
            @Override public void notifyState(int state) {
                Log.w(TAG, "notifyState=" + state);
                if (state == Code.REQUEST_SUCCESS) {
                    confirmPwd();
                } else {
                    sendToast("手环连接失败（通知通道未就绪）");
                    flowStarted = false;
                    if (autoReconnect) scheduleRetry();
                }
            }
        });
    }

    private final IABleConnectStatusListener statusListener = new IABleConnectStatusListener() {
        @Override public void onConnectStatusChanged(String macAddr, int status) {
            if (status == Constants.STATUS_CONNECTED) {
                connected = true; autoReconnect = false; autoToastShown = false;
                Intent ci = new Intent(ACTION_UPDATE); ci.putExtra("conn", 1); LocalBroadcastManager.getInstance(VpCollectorService.this).sendBroadcast(ci);
                sendToast("手环已连接");
            } else if (status == Constants.STATUS_DISCONNECTED) {
                connected = false; flowStarted = false; autoReconnect = true;
                Intent ci = new Intent(ACTION_UPDATE); ci.putExtra("conn", 0); LocalBroadcastManager.getInstance(VpCollectorService.this).sendBroadcast(ci);
                sendToast("手环已断开，3秒后自动重连");
                scheduleRetry();
            }
        }
    };

    private void scheduleRetry() {
        // 用系统级闹钟安排重连：手机息屏/深度省电时普通定时器会被冻结，闹钟可以穿透
        Intent it = new Intent(this, VpCollectorService.class).setAction(ACTION_RETRY);
        PendingIntent pi = PendingIntent.getService(this, 1002, it,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long at = System.currentTimeMillis() + 5000;
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
        Log.w(TAG, "retry alarm in 5s");
    }

    private void confirmPwd() {
        VPOperateManager.getInstance().confirmDevicePwd(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) sendToast("密码校验指令发送失败"); }
        }, new IPwdDataListener() {
            @Override public void onPwdDataChange(PwdData pwd) { Log.w(TAG, "pwd: " + pwd); }
            @Override public void onConnectionConfirmTimeout() { sendToast("密码校验超时"); flowStarted = false; if (autoReconnect) scheduleRetry(); }
        }, new IDeviceFuctionDataListener() {
            @Override public void onFunctionSupportDataChange(FunctionDeviceSupportData fs) {
                Log.w(TAG, "function support: " + fs);
                try {
                    bpSupported = fs.getBp() == EFunctionStatus.SUPPORT;
                    hrvSupported = fs.getHrvFunction() == EFunctionStatus.SUPPORT;
                    sugarSupported = fs.getBloodGlucose() == EFunctionStatus.SUPPORT;
                    stressSupported = fs.getStress() == EFunctionStatus.SUPPORT;
                    heartWaringSupported = fs.getHeartWaring() == EFunctionStatus.SUPPORT;
                    ecgSupported = fs.getEcg() == EFunctionStatus.SUPPORT;
                    microSupported = fs.getMiniCheckup() == EFunctionStatus.SUPPORT;
                    bcSupported = fs.getBloodComponent() == EFunctionStatus.SUPPORT;
                    bodySupported = fs.getBodyComponent() == EFunctionStatus.SUPPORT;
                    gsrSupported = fs.getGSR() == EFunctionStatus.SUPPORT;
                    tempSupported = fs.getTemperatureFunction() == EFunctionStatus.SUPPORT;
                } catch (Exception e) { Log.w(TAG, "support read err: " + e); }
                try { int wd = fs.getWathcDay(); if (wd > 0) watchDay = wd; } catch (Exception e) { Log.w(TAG, "watchDay err: " + e); }
                Log.w(TAG, "watchDay=" + watchDay);
                Log.w(TAG, "support flags: bp=" + bpSupported + " hrv=" + hrvSupported + " sugar=" + sugarSupported + " stress=" + stressSupported + " temp=" + tempSupported);
                syncTime();
                setupHeartWarning();
                startFallMonitor();
            }
            @Override public void onDeviceFunctionPackage1Report(DeviceFunctionPackage1 d) { }
            @Override public void onDeviceFunctionPackage2Report(DeviceFunctionPackage2 d) { }
            @Override public void onDeviceFunctionPackage3Report(DeviceFunctionPackage3 d) { }
            @Override public void onDeviceFunctionPackage4Report(DeviceFunctionPackage4 d) { }
            @Override public void onDeviceFunctionPackage5Report(DeviceFunctionPackage5 d) { }
        }, new ISocialMsgDataListener() {
            @Override public void onSocialMsgSupportDataChange(FunctionSocailMsgData data) { }
            @Override public void onSocialMsgSupportDataChange2(FunctionSocailMsgData data) { }
        }, "0000", false);
    }

    // ===== 校时：把手机时间同步给手环 =====
    private void syncTime() {
        try {
            java.util.Calendar c = java.util.Calendar.getInstance();
            DeviceTimeSetting t = new DeviceTimeSetting(
                    c.get(java.util.Calendar.YEAR),
                    c.get(java.util.Calendar.MONTH) + 1,
                    c.get(java.util.Calendar.DAY_OF_MONTH),
                    c.get(java.util.Calendar.HOUR_OF_DAY),
                    c.get(java.util.Calendar.MINUTE),
                    c.get(java.util.Calendar.SECOND),
                    ETimeMode.MODE_24);
            Log.w(TAG, "sync time " + t.getYear() + "-" + t.getMonth() + "-" + t.getDay() + " " + t.getHour() + ":" + t.getMinute() + ":" + t.getSecond());
            VPOperateManager.getInstance().settingTime(new IBleWriteResponse() {
                @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) Log.w(TAG, "time cmd fail " + code); }
            }, new IResponseListener() {
                @Override public void response(int state) { Log.w(TAG, "time synced state=" + state); syncPerson(); }
            }, t);
        } catch (Exception e) {
            Log.w(TAG, "syncTime err: " + e);
            syncPerson();
        }
    }

    private void syncPerson() {
        if (personSynced) return;
        personSynced = true;
        syncPersonForce();
    }

    private void syncPersonForce() {
        android.content.SharedPreferences sp = getSharedPreferences("fg_user", Context.MODE_PRIVATE);
        int ph = sp.getInt("h", 160), pw = sp.getInt("w", 55), pa = sp.getInt("age", 70);
        String psex = sp.getString("sex", "女");
        PersonInfoData info = new PersonInfoData("男".equals(psex) ? ESex.MAN : ESex.WOMEN, ph, pw, pa, 8000);
        VPOperateManager.getInstance().syncPersonInfo(new IBleWriteResponse() {
            @Override public void onResponse(int code) { }
        }, new IPersonInfoDataListener() {
            @Override public void OnPersoninfoDataChange(com.veepoo.protocol.model.enums.EOprateStauts st) {
                Log.w(TAG, "person synced: " + st);
                flowStarted = false; // 连接链路结束，解除忙碌，开始采集
                startFullRound();
            }
        }, info);
    }

    // ===== 采集轮次 =====
    private void startFullRound() {
        if (!connected || flowStarted) return;
        flowStarted = true;
        roundLight = false;
        roundHasSleep = false;
        batteryDone = stepDone = hrRequested = spo2Requested = bpRequested = hrvRequested = sleepRequested = false;
        roundBoost = false;
        hrDone = spo2Done = bpDone = hrvDone = stressDone = sugarDone = false;
        Log.w(TAG, "full round start");
        readBattery();
    }

    private void startLightRound() {
        if (!connected || flowStarted) return;
        flowStarted = true;
        roundLight = true;
        stepDone = false;
        hrRequested = false; hrDone = false;
        Log.w(TAG, "light round start");
        readStep();
    }

    // 醒来后加速轮：心率+血氧，3分钟一次；血压仍按30分钟节奏，不扰人
    private void startBoostRound() {
        if (!connected || flowStarted) return;
        flowStarted = true;
        roundLight = false;
        roundBoost = true;
        roundHasSleep = false;
        batteryDone = stepDone = hrRequested = spo2Requested = bpRequested = hrvRequested = sleepRequested = false;
        hrDone = spo2Done = bpDone = hrvDone = stressDone = sugarDone = false;
        Log.w(TAG, "boost round start");
        readBattery();
    }

    private void readBattery() {
        if (batteryDone) return;
        batteryDone = true;
        VPOperateManager.getInstance().readBattery(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "battery cmd fail"); readStep(); } }
        }, new IBatteryDataListener() {
            @Override public void onDataChange(BatteryData d) {
                int v = d.isPercent() ? d.getBatteryPercent() : d.getBatteryLevel();
                Log.w(TAG, "battery=" + v);
                lastBatt = v;
                sendMetric("batt", v);
                readStep();
            }
        });
    }

    private void readStep() {
        if (stepDone) return;
        stepDone = true;
        if (accRunning) {
            // 三轴实时监测优先，不能中断；步数由 GSensor 回调或上一次读数延续。
            if (lastSteps >= 0) sendMetric("steps", lastSteps);
            if (lastDis >= 0) sendMetric("dis", lastDis);
            if (lastKcal >= 0) sendMetric("kcal", lastKcal);
            startHeart();
            return;
        }
        long now = System.currentTimeMillis();
        if (lastStepReadAt > 0 && now - lastStepReadAt < MIN10) {
            Log.w(TAG, "step read skipped, next in " + (MIN10 - (now - lastStepReadAt)) + "ms");
            startHeart();
            return;
        }
        lastStepReadAt = now;
        VPOperateManager.getInstance().readSportStep(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "step cmd fail"); startHeart(); } }
        }, new ISportDataListener() {
            @Override public void onSportDataChange(SportData d) {
                Log.w(TAG, "steps=" + d.getStep() + " dis=" + d.getDis() + " kcal=" + d.getKcal());
                lastSteps = d.getStep();
                try { lastDis = (int) d.getDis(); lastKcal = (int) d.getKcal(); } catch (Exception e) { }
                sendMetric("steps", lastSteps);
                sendMetric("dis", lastDis);
                sendMetric("kcal", lastKcal);
                startHeart();
            }
        });
    }

    private void startHeart() {
        if (hrRequested) return;
        hrRequested = true;
        hrDone = false;
        VPOperateManager.getInstance().startDetectHeart(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "hr cmd fail"); startSpo2(); } }
        }, new IHeartDataListener() {
            @Override public void onDataChange(HeartData d) {
                if (d.getHeartStatus() == EHeartStatus.STATE_HEART_NORMAL && d.getData() > 0 && !hrDone) {
                    hrDone = true;
                    int rawHr = d.getData();
                    boolean motionSuspect = isMoving() && lastHr > 0 && Math.abs(rawHr - lastHr) > 30;
                    if (motionSuspect) {
                        Log.w(TAG, "hr=" + rawHr + " motion suspect (motionG=" + Math.round(accMotionG*100)/100.0 + "), keep last=" + lastHr);
                    } else {
                        lastHr = rawHr;
                        sendMetric("hr", lastHr);
                        maybeAlertHr(lastHr);
                    }
                    VPOperateManager.getInstance().stopDetectHeart(new IBleWriteResponse() {
                        @Override public void onResponse(int code) { startSpo2(); }
                    });
                } else if (!hrDone && d.getHeartStatus() == EHeartStatus.STATE_HEART_WEAR_ERROR) {
                    Log.w(TAG, "hr wear error");
                    VPOperateManager.getInstance().stopDetectHeart(new IBleWriteResponse() {
                        @Override public void onResponse(int code) { startSpo2(); }
                    });
                }
            }
        });
        // 心率 90 秒兜底
        ui.postDelayed(new Runnable() { @Override public void run() { if (!hrDone && hrRequested) { Log.w(TAG, "hr timeout"); startSpo2(); } } }, 90000);
    }

    private void startSpo2() {
        if (!connected) { return; }
        if (roundLight) { finishRound(); return; }
        if (spo2Requested) return;
        spo2Requested = true;
        spo2Done = false;
        VPOperateManager.getInstance().startDetectSPO2H(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) sendToast("血氧指令发送失败"); }
        }, new ISpo2hDataListener() {
            @Override public void onSpO2HADataChange(Spo2hData d) {
                if (!spo2Done && d.getDeviceState() == EDeviceStatus.FREE && d.getValue() > 0) {
                    spo2Done = true;
                    Log.w(TAG, "spo2=" + d.getValue());
                    lastSpo2 = d.getValue();
                    sendMetric("spo2", lastSpo2);
                    maybeAlertSpo2(lastSpo2);
                    VPOperateManager.getInstance().stopDetectSPO2H(new IBleWriteResponse() {
                        @Override public void onResponse(int code) { maybeBp(); }
                    }, new ISpo2hDataListener() {
                        @Override public void onSpO2HADataChange(Spo2hData s) { }
                    });
                } else if (!spo2Done && d.getDeviceState() == EDeviceStatus.UNPASS_WEAR) {
                    Log.w(TAG, "spo2 wear error");
                }
            }
        });
        // 血氧 3 分钟兜底
        ui.postDelayed(new Runnable() { @Override public void run() { if (!spo2Done && spo2Requested) { Log.w(TAG, "spo2 timeout"); maybeBp(); } } }, 180000);
    }

    private void maybeAlertSugar(float sugar) {
        if (sugar <= 0 || sugar >= sugarThresh) { sugarStage = 0; return; }
        long now = System.currentTimeMillis();
        if (sugarStage == 0) {
            sugarStage = 1; lastSugarAlertAt = now;
            writeAlarmLog("SUGAR LOW STAGE1 sugar=" + sugar + " thresh=" + sugarThresh);
            sendBandMsg("血糖偏低 " + sugar + "，请先坐下吃点东西；如连续偏低会提醒家人");
        } else if (now - lastSugarAlertAt >= 30000L) {
            sugarStage = 0; lastSugarAlertAt = now;
            writeAlarmLog("SUGAR LOW STAGE2 sugar=" + sugar + " thresh=" + sugarThresh);
            sendFamilySms("【我在】连续血糖偏低提醒：刚才血糖 " + sugar + "（低于 " + sugarThresh + "），请尽快联系确认。");
            cloudAlert("sugar_low", "连续血糖偏低", String.valueOf(sugar));
        }
    }

    private void maybeBp() {
        if (!connected) return;
        if (bpFailCount >= 2 || (lastBpAt > 0 && System.currentTimeMillis() - lastBpAt < MIN30)) { maybeHrv(); return; }
        if (bpRequested) return;
        bpRequested = true;
        bpDone = false;
        bpTmpH = -1; bpTmpL = -1;
        Log.w(TAG, "bp detect start");
        VPOperateManager.getInstance().startDetectBP(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "bp cmd fail " + code); maybeHrv(); } }
        }, new IBPDetectDataListener() {
            @Override public void onDataChange(BpData d) {
                Log.w(TAG, "bp data: " + d);
                if (bpDone) return;
                try {
                    if (d.getStatus() != EBPDetectStatus.STATE_BP_NORMAL) return;
                    int hp = d.getHighPressure(), lp = d.getLowPressure();
                    if (hp > 0 && lp > 0) { bpTmpH = hp; bpTmpL = lp; }
                    boolean have = false; int pr = 0;
                    try { have = d.isHaveProgress(); pr = d.getProgress(); } catch (Exception e) { have = false; }
                    if (bpTmpH > 0 && bpTmpL > 0 && (!have || pr >= 100)) {
                        if (!validBp(bpTmpH, bpTmpL)) {
                            Log.w(TAG, "bp invalid " + bpTmpH + "/" + bpTmpL + ", ignored");
                            bpDone = true;
                            bpFailCount++;
                            stopBp();
                            return;
                        }
                        bpDone = true;
                        bpFailCount = 0;
                        lastBpAt = System.currentTimeMillis();
                        lastBpH = bpTmpH; lastBpL = bpTmpL;
                        Log.w(TAG, "bp=" + lastBpH + "/" + lastBpL);
                        sendMetric("bpHigh", lastBpH);
                        sendMetric("bpLow", lastBpL);
                        stopBp();
                    }
                } catch (Exception e) { Log.w(TAG, "bp parse err " + e); }
            }
        }, EBPDetectModel.DETECT_MODEL_PUBLIC);
        ui.postDelayed(new Runnable() { @Override public void run() {
            if (!bpDone && bpRequested) {
                Log.w(TAG, "bp timeout tmp=" + bpTmpH + "/" + bpTmpL);
                if (validBp(bpTmpH, bpTmpL)) {
                    bpDone = true;
                    bpFailCount = 0;
                    lastBpAt = System.currentTimeMillis();
                    lastBpH = bpTmpH; lastBpL = bpTmpL;
                    sendMetric("bpHigh", lastBpH);
                    sendMetric("bpLow", lastBpL);
                } else {
                    bpFailCount++;
                }
                stopBp();
            }
        } }, 75000);
    }

    // 血压合理性校验：明显不可能的值（如30/20）视为无效，不写入CSV也不更新显示
    private boolean validBp(int h, int l) { return h >= 60 && h <= 250 && l >= 35 && l <= 160 && h > l; }

    private void stopBp() {
        if (hrvRequested) return;
        bpRequested = false;
        VPOperateManager.getInstance().stopDetectBP(new IBleWriteResponse() {
            @Override public void onResponse(int code) { }
        }, EBPDetectModel.DETECT_MODEL_PUBLIC);
        maybeHrv();
    }

    private void maybeHrv() {
        if (!connected) return;
        if (lastHrvAt > 0 && System.currentTimeMillis() - lastHrvAt < MIN30) { maybeStress(); return; }
        if (hrvRequested) return;
        hrvRequested = true;
        hrvDone = false;
        hrvOriginBest = -1;
        hrvOriginList.clear();
        hrvDetectMedian = -1;
        hrvOriginReadStarted = false;
        hrvFallback = false;
        hrvDetectVals.clear();
        hrvDetectRunning = true;
        Log.w(TAG, "hrv app detect start");
        VPOperateManager.getInstance().startDetectHrv(new BleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "hrv detect cmd fail " + code); finishHrv(); } }
        }, new IHrvDetectListener() {
            @Override public void onHrvDetect(int v) { if (v >= 10 && v <= 250) hrvDetectVals.add(v); }
            @Override public void onDetectFailed(HrvDetectState s) { Log.w(TAG, "hrv detect fail " + s); finishHrv(); }
            @Override public void onDetectStop() { finishHrv(); }
        });
        ui.postDelayed(new Runnable() { @Override public void run() { if (hrvRequested && hrvDetectRunning) { Log.w(TAG, "hrv detect timeout"); finishHrv(); } } }, 30000);
    }


    private void finishHrv() {
        if (!hrvRequested) return;
        if (hrvDetectRunning) {
            hrvDetectRunning = false;
            try {
                VPOperateManager.getInstance().stopDetectHrv(new BleWriteResponse() {
                    @Override public void onResponse(int code) { }
                }, new IHrvDetectListener() {
                    @Override public void onHrvDetect(int v) { }
                    @Override public void onDetectFailed(HrvDetectState s) { }
                    @Override public void onDetectStop() { }
                });
            } catch (Exception e) { Log.w(TAG, "hrv detect stop err " + e); }
        }
        if (!hrvDetectVals.isEmpty()) {
            java.util.ArrayList<Integer> vals = new java.util.ArrayList<>(hrvDetectVals);
            java.util.Collections.sort(vals);
            hrvDetectMedian = vals.get(vals.size() / 2);
            Log.w(TAG, "hrv raw=" + vals + " detectMedian=" + hrvDetectMedian);
        }
        // 优先用手环 RR 间期算标准 RMSSD，比直接平均“瞬时 HRV”稳定得多。
        if (!hrvOriginList.isEmpty()) {
            int take = Math.min(3, hrvOriginList.size());
            java.util.List<HRVOriginData> latest = hrvOriginList.subList(hrvOriginList.size() - take, hrvOriginList.size());
            int rmssd = calcHrvRmssd(latest);
            if (rmssd >= 15 && rmssd <= 200) {
                lastHrv = rmssd;
                lastHrvAt = System.currentTimeMillis();
                hrvDone = true;
                sendMetric("hrv", lastHrv);
                storeHrv(lastHrv);
                Log.w(TAG, "hrv=" + lastHrv + " (rmssd)");
                hrvRequested = false;
                maybeStress();
                return;
            }
            Log.w(TAG, "hrv rmssd invalid=" + rmssd + ", using detectMedian=" + hrvDetectMedian);
        }
        if (hrvDetectMedian >= 15 && hrvDetectMedian <= 200) {
            lastHrv = hrvDetectMedian;
            lastHrvAt = System.currentTimeMillis();
            hrvDone = true;
            sendMetric("hrv", lastHrv);
                storeHrv(lastHrv);
            Log.w(TAG, "hrv=" + lastHrv + " (detect median)");
            hrvRequested = false;
            maybeStress();
            return;
        }
        if (!hrvOriginReadStarted) {
            hrvOriginReadStarted = true;
            hrvOriginBest = -1;
            Log.w(TAG, "hrv origin read day=1");
            VPOperateManager.getInstance().readHRVOrigin(new IBleWriteResponse() {
                @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "hrv origin cmd fail " + code); finishHrv(); } }
            }, new IHRVOriginDataListener() {
                @Override public void onHRVOriginListener(HRVOriginData d) {
                    if (d == null) return;
                    hrvOriginList.add(d);
                    Log.w(TAG, "hrv origin: " + d);
                    int hv = d.getHrvValue(); if (hv >= 10 && hv <= 250) hrvOriginBest = hv;
                }
                @Override public void onDayHrvScore(int day, String date, int score) { }
                @Override public void onReadOriginProgress(float p) { }
                @Override public void onReadOriginProgressDetail(int day, String date, int allPack, int curPack) { }
                @Override public void onReadOriginComplete() { Log.w(TAG, "hrv origin read complete"); finishHrv(); }
            }, 1);
            ui.postDelayed(new Runnable() { @Override public void run() { if (hrvRequested) { Log.w(TAG, "hrv origin timeout"); finishHrv(); } } }, 20000);
            return;
        }
        hrvRequested = false;
        if (hrvOriginBest > 0) {
            lastHrv = hrvOriginBest;
            lastHrvAt = System.currentTimeMillis();
            hrvDone = true;
            sendMetric("hrv", lastHrv);
                storeHrv(lastHrv);
            Log.w(TAG, "hrv=" + lastHrv + " (origin history)");
        }
        maybeStress();
    }

    // 用 RR 间期算 RMSSD（毫秒）。RR 原始值是 0.1 秒单位，所以乘 10。
    private int calcHrvRmssd(java.util.List<HRVOriginData> list) {
        try {
            if (list == null || list.isEmpty()) return -1;
            int[] raw = com.veepoo.protocol.util.HrvScoreUtil.getRRDataList(new java.util.ArrayList<>(list));
            int prev = -1;
            java.util.ArrayList<Integer> diffs = new java.util.ArrayList<>();
            for (int r : raw) {
                int ms = r * 10;
                if (ms < 300 || ms > 2000) continue;
                if (prev > 0) {
                    int diff = Math.abs(ms - prev);
                    if (diff <= 250 && diff <= prev * 0.45) diffs.add(diff);
                }
                prev = ms;
            }
            if (diffs.size() < 15) return -1;
            double sum = 0;
            for (int d : diffs) sum += (double) d * d;
            return (int) Math.round(Math.sqrt(sum / diffs.size()));
        } catch (Exception e) {
            Log.w(TAG, "hrv rmssd calc err " + e);
            return -1;
        }
    }

    // ===== 压力：进度到成功或90秒超时 =====
    private void maybeStress() {
        if (!connected) return;
        if (!stressSupported) { maybeSugar(); return; }
        if (lastStressAt > 0 && System.currentTimeMillis() - lastStressAt < MIN30) { maybeSugar(); return; }
        if (stressRequested) return;
        stressRequested = true;
        stressDone = false;
        Log.w(TAG, "stress detect start");
        VPOperateManager.getInstance().startDetectPressure(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "stress cmd fail " + code); finishStress(); } }
        }, new IPressureDetectListener() {
            @Override public void onDetecting(int progress) { Log.w(TAG, "stress progress=" + progress); }
            @Override public void onDetectSuccess(int pressure) {
                if (stressDone) return;
                stressDone = true;
                lastStress = pressure;
                lastStressAt = System.currentTimeMillis();
                sendMetric("stress", lastStress);
                Log.w(TAG, "stress=" + lastStress);
                finishStress();
            }
            @Override public void onDetectFailed(PressureDetectState st) { Log.w(TAG, "stress fail: " + st); finishStress(); }
            @Override public void onDetectStop() { Log.w(TAG, "stress stop"); finishStress(); }
        });
        ui.postDelayed(new Runnable() { @Override public void run() { if (!stressDone && stressRequested) { Log.w(TAG, "stress timeout"); finishStress(); } } }, 90000);
    }


    private void finishStress() {
        if (!stressRequested) return;
        stressRequested = false;
        if (!stressDone) {
            try { VPOperateManager.getInstance().stopDetectPressure(new IBleWriteResponse() { @Override public void onResponse(int code) { } }); } catch (Exception ignored) { }
        }
        maybeSugar();
    }


    // ===== 血糖：进度到100或90秒超时取有效值 =====
    private void maybeSugar() {
        if (!connected) return;
        if (!sugarSupported) { maybeSleep(); return; }
        if (lastSugarAt > 0 && System.currentTimeMillis() - lastSugarAt < MIN30) { maybeSleep(); return; }
        if (sugarRequested) return;
        sugarRequested = true;
        sugarDone = false;
        sugarTmp = -1;
        Log.w(TAG, "sugar detect start");
        VPOperateManager.getInstance().startBloodGlucoseDetect(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "sugar cmd fail " + code); finishSugar(); } }
        }, new AbsBloodGlucoseChangeListener() {
            @Override public void onBloodGlucoseDetect(int progress, float value, EBloodGlucoseRiskLevel level) {
                Log.w(TAG, "sugar progress=" + progress + " value=" + value);
                if (sugarDone) return;
                if (value > 0) sugarTmp = value;
                if (progress >= 100 && value > 0) {
                    sugarDone = true;
                    lastSugar = value;
                    lastSugarAt = System.currentTimeMillis();
                    Intent i = new Intent(ACTION_UPDATE);
                    i.putExtra("sugar", lastSugar);
                    i.putExtra("upd", nowHM());
                    LocalBroadcastManager.getInstance(VpCollectorService.this).sendBroadcast(i);
                    Log.w(TAG, "sugar=" + lastSugar);
                    maybeAlertSugar(lastSugar);
                    finishSugar();
                }
            }
            @Override public void onDetectError(int opt, EBloodGlucoseStatus status) { Log.w(TAG, "sugar error opt=" + opt + " status=" + status); finishSugar(); }
        });
        ui.postDelayed(new Runnable() { @Override public void run() { if (!sugarDone && sugarRequested) { Log.w(TAG, "sugar timeout tmp=" + sugarTmp); if (sugarTmp > 0) { sugarDone = true; lastSugar = sugarTmp; lastSugarAt = System.currentTimeMillis(); maybeAlertSugar(sugarTmp); } finishSugar(); } } }, 90000);
    }


    private void finishSugar() {
        if (!sugarRequested) return;
        sugarRequested = false;
        if (!sugarDone) {
            try { VPOperateManager.getInstance().stopBloodGlucoseDetect(new IBleWriteResponse() { @Override public void onResponse(int code) { } }, new AbsBloodGlucoseChangeListener() { }); } catch (Exception ignored) { }
        }
        maybeTemperature();
    }


        private boolean accRunning=false;
    private boolean fallMonitorOn=true; // 实时跌倒监测开关
    private long lastFallAlertAt=0;
    private int fallConsecHigh=0;
    private boolean fallPromptActive=false;
    private double fallPromptG=0;
    private Runnable fallPromptTimer=null;
    private String familyPhone1=null; // 儿子
    private String familyPhone2=null; // 女儿
    private float accPeakG=0;
    // ===== 三闸门降级版跌倒判定（v1.13）：撞击候选 -> 撞后静止 -> 姿态翻转 =====
    private int fallGateStage = 0; // 0=待机 1=撞击候选，等待静止+姿态确认
    private long fallCandAt = 0;
    private double fallCandG = 0;
    private final double[] preUnit = new double[3];
    private double stillSumX = 0, stillSumY = 0, stillSumZ = 0;
    private int stillCount = 0;
    private static final double FALL_STILL_DA = 0.20;      // 静止门限：|g-1|<0.2
    private static final int FALL_STILL_N = 3;             // 连续3个静止样本（约3秒）
    private static final double FALL_POSTURE_DELTA = 0.6;  // 姿态矢量差阈值（倒地判定）
    private static final long FALL_GATE_WINDOW = 10000L;   // 撞击后10秒内必须静止，否则视为自己爬起来了
    private final java.util.ArrayDeque<double[]> preUnitBuf = new java.util.ArrayDeque<double[]>();

    // 杩愬姩寮哄害绐楀彛锛氱敤浜庤繃婊ゆ檭鍔ㄥ鑷寸殑蹇冪巼浼樊锛坴1.11b锛?
    private final java.util.ArrayList<Double> accGWindow = new java.util.ArrayList<>();
    private double accMotionG = 0;
    private long lastStepReadAt=0;
    private static final long MIN10 = 10 * 60 * 1000L;
    private double accBaselineMg=0;
    private int accCalibN=0;

    private int accX=-9999, accY=-9999, accZ=-9999;
    private long lastAccSampleAt=0; // 最近一次三轴样本时间，用于看门狗
    private long lastHighAt=0; // 上一次超阈值样本时间，用于连续确认
    public void startAccTest() { startFallMonitor(); }
    public void startFallMonitor() {
        if (!connected) return;
        if (accRunning) return;
        accRunning = true;
        lastAccSampleAt = System.currentTimeMillis(); // 视为流刚启动；若之后2分钟无样本，看门狗会重启
        Log.w(TAG, "fall monitor start");
        VPOperateManager.getInstance().startGsensorSport(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "acc cmd fail " + code); stopFallMonitor(); restartStep(); } }
        }, new ISportDataListener() {
            @Override public void onSportDataChange(SportData d) {
                if (d == null) return;
                int x=d.getTriaxialX(), y=d.getTriaxialY(), z=d.getTriaxialZ();
                accX=x; accY=y; accZ=z;
                lastAccSampleAt = System.currentTimeMillis();
                Log.w(TAG, "acc x=" + x + " y=" + y + " z=" + z);
                double mg = Math.sqrt((double)x*x + (double)y*y + (double)z*z);
                // Calibrate 1G from the first resting samples.
                if (accCalibN < 8 && mg >= 80 && mg <= 800) {
                    accBaselineMg = (accBaselineMg * accCalibN + mg) / (accCalibN + 1);
                    accCalibN++;
                    if (accCalibN == 8) Log.w(TAG, "acc baseline mg=" + Math.round(accBaselineMg));
                }
                double baseline = accBaselineMg > 50 ? accBaselineMg : 245.0;
                double g = mg / baseline;
                if (g > accPeakG) accPeakG = (float)g;
                accGWindow.add(g);
                if (accGWindow.size() > 8) accGWindow.remove(0);
                double sumDev = 0;
                for (Double gv : accGWindow) sumDev += Math.abs(gv - 1.0);
                accMotionG = sumDev / accGWindow.size();try {
                    if (d.getStep() >= 0 && d.getStep() != lastSteps) { lastSteps = d.getStep(); sendMetric("steps", lastSteps); }
                    if (d.getDis() >= 0) { lastDis = (int) d.getDis(); sendMetric("dis", lastDis); }
                    if (d.getKcal() >= 0) { lastKcal = (int) d.getKcal(); sendMetric("kcal", lastKcal); }
                } catch (Exception ignore) {}
                // ===== 跌倒判定（v1.13 三闸门降级版）=====
                // 1. 撞击候选：>4G 直接候选；2.5~4G 需 1.5 秒内连续两个样本（防磕碰）
                // 2. 撞后静止：10 秒内连续 3 个样本近似静止（人没爬起来继续动）
                // 3. 姿态翻转：静止后重力方向与撞击前差异大（真的倒了），才进入 30 秒确认
                long nowAcc = System.currentTimeMillis();
                double ux = x / mg, uy = y / mg, uz = z / mg; // 归一化重力方向
                if (fallGateStage == 1) {
                    fallGateCheck(nowAcc, g, ux, uy, uz);
                } else if (fallMonitorOn && g >= fallGThresh) {
                    if (nowAcc - lastHighAt > 1500) fallConsecHigh = 0;
                    lastHighAt = nowAcc;
                    fallConsecHigh++;
                    if (g > 4.0 || fallConsecHigh >= 2) {
                        fallGateEnter(nowAcc, g, ux, uy, uz);
                    }
                } else {
                    fallConsecHigh = 0;
                }
                // 维护"撞击前姿态"窗口（当前样本处理完后入队，因此候选时拿到的是撞击前方向）
                preUnitBuf.addLast(new double[]{ux, uy, uz});
                if (preUnitBuf.size() > 5) preUnitBuf.removeFirst();
                // 转发给UI（限频约1秒1条，避免刷屏）
                long now = System.currentTimeMillis();
                if (now - lastAccUiAt > 1000) { lastAccUiAt = now;
                    Intent ai = new Intent(ACTION_UPDATE);
                    ai.putExtra("x", x); ai.putExtra("y", y); ai.putExtra("z", z);
                    ai.putExtra("g", Math.round(g*100)/100.0);
                    LocalBroadcastManager.getInstance(VpCollectorService.this).sendBroadcast(ai);
                }
            }
        });
    }
    private long lastAccUiAt = 0;
    /** 撞击候选：记录撞击前的重力方向，进入"等待静止+姿态确认"阶段 */
    private void fallGateEnter(long now, double g, double ux, double uy, double uz) {
        double sx = 0, sy = 0, sz = 0; int n = 0;
        for (double[] v : preUnitBuf) { sx += v[0]; sy += v[1]; sz += v[2]; n++; }
        if (n > 0) { preUnit[0] = sx / n; preUnit[1] = sy / n; preUnit[2] = sz / n; }
        else { preUnit[0] = 0; preUnit[1] = 0; preUnit[2] = -1; }
        fallGateStage = 1; fallCandAt = now; fallCandG = g;
        stillCount = 0; stillSumX = 0; stillSumY = 0; stillSumZ = 0;
        Log.w(TAG, "FALL GATE candidate g=" + Math.round(g * 10) / 10.0 + " wait stillness+posture");
        writeAlarmLog("FALL GATE CANDIDATE g=" + Math.round(g * 10) / 10.0);
    }

    /** 静止+姿态两道闸门：全部通过才真正触发摔倒告警 */
    private void fallGateCheck(long now, double g, double ux, double uy, double uz) {
        double da = Math.abs(g - 1.0);
        if (now - fallCandAt > FALL_GATE_WINDOW) {
            Log.w(TAG, "FALL GATE timeout: still moving after impact, reset");
            writeAlarmLog("FALL GATE TIMEOUT still-moving");
            fallGateStage = 0; fallConsecHigh = 0;
            return;
        }
        if (da < FALL_STILL_DA) {
            stillCount++; stillSumX += ux; stillSumY += uy; stillSumZ += uz;
            if (stillCount >= FALL_STILL_N) {
                double px = stillSumX / stillCount, py = stillSumY / stillCount, pz = stillSumZ / stillCount;
                double delta = Math.sqrt(Math.pow(px - preUnit[0], 2) + Math.pow(py - preUnit[1], 2) + Math.pow(pz - preUnit[2], 2));
                fallGateStage = 0; fallConsecHigh = 0;
                if (delta > FALL_POSTURE_DELTA) {
                    Log.w(TAG, "FALL GATE PASS posture delta=" + Math.round(delta * 100) / 100.0);
                    writeAlarmLog("FALL GATE PASS delta=" + Math.round(delta * 100) / 100.0);
                    onFallDetected(fallCandG);
                } else {
                    Log.w(TAG, "FALL GATE BLOCK posture delta=" + Math.round(delta * 100) / 100.0);
                    writeAlarmLog("FALL GATE BLOCK posture delta=" + Math.round(delta * 100) / 100.0);
                }
            }
        } else {
            Log.w(TAG, "FALL GATE reset: moving again da=" + Math.round(da * 100) / 100.0);
            writeAlarmLog("FALL GATE RESET moving da=" + Math.round(da * 100) / 100.0);
            fallGateStage = 0; fallConsecHigh = 0;
        }
    }

    private void onFallDetected(double g) {
        fallConsecHigh = 0;
        long now = System.currentTimeMillis();
        if (fallPromptActive) {
            Log.w(TAG, "fall ignored: confirm prompt active g=" + g);
            writeAlarmLog("FALL IGNORED confirm active g=" + Math.round(g*10)/10.0);
            return;
        }
        if (now - lastFallAlertAt < 2 * 60 * 1000L) {
            Log.w(TAG, "fall suppressed cooldown g=" + g);
            writeAlarmLog("FALL SUPPRESSED cooldown g=" + Math.round(g*10)/10.0);
            return;
        }
        lastFallAlertAt = now;
        cloudAlert("fall_prompt", "疑似摔倒", String.valueOf(Math.round(g*10)/10.0));
        fallPromptActive = true;
        fallPromptG = g;
        Log.w(TAG, "FALL DETECTED g=" + g);
        writeAlarmLog("FALL DETECTED g=" + Math.round(g*10)/10.0 + " stage=PROMPT phone1=" + familyPhone1);
        sendBandMsg("疑似摔倒！合加速度 " + Math.round(g*10)/10.0 + "G。请在手机上确认是否没事；30秒没确认会自动通知家人。");
        Intent pi = new Intent(ACTION_UPDATE);
        pi.putExtra("fallPrompt", Math.round(g*10)/10.0);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pi);
        if (fallPromptTimer != null) ui.removeCallbacks(fallPromptTimer);
        fallPromptTimer = new Runnable() {
            @Override public void run() {
                if (!fallPromptActive) return;
                fallPromptActive = false;
                double gg = fallPromptG;
                writeAlarmLog("FALL TIMEOUT g=" + Math.round(gg*10)/10.0 + " phone1=" + familyPhone1);
                Log.w(TAG, "FALL TIMEOUT g=" + gg);
                sendFallSms(gg);
                cloudAlert("fall_timeout", "疑似摔倒未确认", String.valueOf(Math.round(gg*10)/10.0));
                sendFallCall();
                Intent ti = new Intent(ACTION_UPDATE);
                ti.putExtra("fallTimeout", Math.round(gg*10)/10.0);
                LocalBroadcastManager.getInstance(VpCollectorService.this).sendBroadcast(ti);
                sendToast("30秒未确认，已自动通知家人");
            }
        };
        ui.postDelayed(fallPromptTimer, 30000L);
    }

    private void confirmFall() {
        if (!fallPromptActive) return;
        fallPromptActive = false;
        if (fallPromptTimer != null) ui.removeCallbacks(fallPromptTimer);
        double g = fallPromptG;
        Log.w(TAG, "FALL CONFIRMED g=" + g);
        writeAlarmLog("FALL CONFIRMED g=" + Math.round(g*10)/10.0 + " phone1=" + familyPhone1);
        Intent ni = new Intent(ACTION_UPDATE);
        ni.putExtra("fallNotice", Math.round(g*10)/10.0);
        LocalBroadcastManager.getInstance(this).sendBroadcast(ni);
        sendToast("已通知家人：刚才摔倒但已自行确认");
        cloudAlert("fall_confirm", "摔倒已自行确认", String.valueOf(Math.round(g*10)/10.0));
    }

    private void helpFall() {
        if (!fallPromptActive) return;
        fallPromptActive = false;
        if (fallPromptTimer != null) ui.removeCallbacks(fallPromptTimer);
        double g = fallPromptG;
        Log.w(TAG, "FALL HELP REQUEST g=" + g);
        writeAlarmLog("FALL HELP REQUEST g=" + Math.round(g*10)/10.0 + " phone1=" + familyPhone1);
        sendFallSms(g);
        cloudAlert("fall_help", "老人主动求助", String.valueOf(Math.round(g*10)/10.0));
        sendFallCall();
        sendToast("已立即通知家人：老人请求帮助");
    }
    private void sendFallSms(double g) {
        try {
            String msg = "【我在】疑似跌倒提醒：老人合加速度达到 " + Math.round(g*10)/10.0 + "G。请尽快联系确认。";
            android.telephony.SmsManager sm = (Build.VERSION.SDK_INT >= 31)
                    ? getSystemService(android.telephony.SmsManager.class)
                    : android.telephony.SmsManager.getDefault();
            int seq = 0;
            if (familyPhone1 != null && familyPhone1.length() >= 11) {
                Log.w(TAG, "SMS SEND begin p1=" + familyPhone1 + " len=" + msg.length()); writeAlarmLog("SMS SEND begin p1=" + familyPhone1);
                sendSmsDirect(sm, familyPhone1, msg, 1001 + (seq++ * 2));
            } else {
                Log.w(TAG, "SMS SEND skipped p1 missing/invalid=" + familyPhone1); writeAlarmLog("SMS SEND skipped p1=" + familyPhone1);
            }
            if (familyPhone2 != null && familyPhone2.length() >= 11) {
                Log.w(TAG, "SMS SEND begin p2=" + familyPhone2 + " len=" + msg.length()); writeAlarmLog("SMS SEND begin p2=" + familyPhone2);
                sendSmsDirect(sm, familyPhone2, msg, 1002 + (seq++ * 2));
            } else {
                Log.w(TAG, "SMS SEND skipped p2 missing/invalid=" + familyPhone2); writeAlarmLog("SMS SEND skipped p2=" + familyPhone2);
            }
        } catch (Exception e) { Log.w(TAG, "fall sms err " + e); }
    }

    // 连续异常第2次：给已保存的紧急联系人都发短信
    private void sendFamilySms(String msg) {
        try {
            android.telephony.SmsManager sm = (Build.VERSION.SDK_INT >= 31)
                    ? getSystemService(android.telephony.SmsManager.class)
                    : android.telephony.SmsManager.getDefault();
            int seq = 0;
            if (familyPhone1 != null && familyPhone1.length() >= 11) {
                sendSmsDirect(sm, familyPhone1, msg, 3001 + (seq++ * 2));
            }
            if (familyPhone2 != null && familyPhone2.length() >= 11) {
                sendSmsDirect(sm, familyPhone2, msg, 3002 + (seq++ * 2));
            }
            if (seq == 0) writeAlarmLog("FAMILY SMS skipped: no valid phone");
        } catch (Exception e) {
            Log.w(TAG, "family sms err " + e);
            writeAlarmLog("FAMILY SMS error " + e);
        }
    }

    private void loadThresholds() {
        try {
            android.content.SharedPreferences sp = getSharedPreferences("fg_thresh", Context.MODE_PRIVATE);
            spo2Thresh = clamp(sp.getInt("spo2", 90), 70, 99);
            hrHighThresh = clamp(sp.getInt("hrHigh", 140), 80, 220);
            hrLowThresh = clamp(sp.getInt("hrLow", 45), 30, 90);
            sugarThresh = clampF(sp.getFloat("sugarLow", 3.9f), 2.0f, 6.0f);
            fallGThresh = clampF(sp.getFloat("fallG", 2.5f), 1.5f, 5.0f);
            if (hrLowThresh >= hrHighThresh) hrLowThresh = hrHighThresh - 10;
            Log.w(TAG, "thresholds loaded spo2=" + spo2Thresh + " hrHigh=" + hrHighThresh + " hrLow=" + hrLowThresh + " sugarLow=" + sugarThresh + " fallG=" + fallGThresh);
        } catch (Exception e) { Log.w(TAG, "threshold load err " + e); }
    }

    private int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
    private float clampF(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    // 设置页保存阈值：App持久化 + 手环固件报警同步
    private final android.content.BroadcastReceiver rxThresh = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            try {
                int spo2 = clamp(i.getIntExtra("spo2", spo2Thresh), 70, 99);
                int high = clamp(i.getIntExtra("hrHigh", hrHighThresh), 80, 220);
                int low = clamp(i.getIntExtra("hrLow", hrLowThresh), 30, 90);
                float sugar = clampF((float) i.getDoubleExtra("sugarLow", sugarThresh), 2.0f, 6.0f);
                float fall = clampF((float) i.getDoubleExtra("fallG", fallGThresh), 1.5f, 5.0f);
                if (low >= high) { sendToast("心率下限要小于心率上限"); return; }
                spo2Thresh = spo2; hrHighThresh = high; hrLowThresh = low;
                sugarThresh = sugar; fallGThresh = fall;
                hrHighStage = 0; hrLowStage = 0; spo2Stage = 0; sugarStage = 0;
                getSharedPreferences("fg_thresh", Context.MODE_PRIVATE).edit()
                        .putInt("spo2", spo2).putInt("hrHigh", high).putInt("hrLow", low)
                        .putFloat("sugarLow", sugar).putFloat("fallG", fall).apply();
                Log.w(TAG, "thresholds saved spo2=" + spo2 + " hrHigh=" + high + " hrLow=" + low + " sugarLow=" + sugar + " fallG=" + fall);
                writeAlarmLog("THRESH SET spo2=" + spo2 + " hrHigh=" + high + " hrLow=" + low + " sugarLow=" + sugar + " fallG=" + fall);
                if (connected) setupHeartWarning();
                sendToast("告警阈值已保存，立即生效");
            } catch (Exception e) { Log.w(TAG, "set thresh err " + e); }
        }
    };

    private void sendSmsDirect(android.telephony.SmsManager sm, String phone, String msg, int requestCode) {
        try {
            android.app.PendingIntent sent = android.app.PendingIntent.getBroadcast(
                    this, requestCode,
                    new Intent("com.family.guardian.SMS_SENT").putExtra("phone", phone),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            java.util.ArrayList<String> parts = sm.divideMessage(msg);
            if (parts.size() <= 1) {
                sm.sendTextMessage(phone, null, msg, sent, null);
            } else {
                java.util.ArrayList<android.app.PendingIntent> sents = new java.util.ArrayList<android.app.PendingIntent>();
                for (int i = 0; i < parts.size(); i++) sents.add(sent);
                sm.sendMultipartTextMessage(phone, null, parts, sents, null);
            }
            Log.w(TAG, "SMS SEND issued phone=" + phone + " parts=" + parts.size()); writeAlarmLog("SMS SEND issued phone=" + phone);
        } catch (Exception e) {
            Log.w(TAG, "SMS SEND error phone=" + phone + " err=" + e); writeAlarmLog("SMS SEND error phone=" + phone);
        }
    }
    private void sendFallCall() {
        try {
            if (familyPhone1 == null || familyPhone1.length() < 11) {
                Log.w(TAG, "CALL skipped p1 missing/invalid=" + familyPhone1); writeAlarmLog("CALL skipped p1=" + familyPhone1);
                return;
            }
            android.net.Uri uri = android.net.Uri.parse("tel:" + familyPhone1);
            boolean granted = checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            Log.w(TAG, "CALL SEND begin p1=" + familyPhone1 + " granted=" + granted); writeAlarmLog("CALL SEND begin p1=" + familyPhone1 + " granted=" + granted);
            if (Build.VERSION.SDK_INT >= 26 && granted) {
                android.telecom.TelecomManager tm = getSystemService(android.telecom.TelecomManager.class);
                if (tm != null) {
                    tm.placeCall(uri, null);
                    Log.w(TAG, "CALL SEND issued via TelecomManager"); writeAlarmLog("CALL SEND issued via TelecomManager phone=" + familyPhone1);
                    return;
                }
            }
            Intent ci = new Intent(android.content.Intent.ACTION_CALL);
            ci.setData(uri);
            ci.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(ci);
            Log.w(TAG, "CALL SEND issued via ACTION_CALL"); writeAlarmLog("CALL SEND issued via ACTION_CALL phone=" + familyPhone1);
        } catch (Exception e) { Log.w(TAG, "fall call err " + e); }
    }
    public void stopFallMonitor() {
        if (!accRunning) return;
        accRunning = false;
        try { VPOperateManager.getInstance().stopGsensorSport(new IBleWriteResponse() { @Override public void onResponse(int code) { } }, new ISportDataListener() { @Override public void onSportDataChange(SportData d) { } }); } catch (Exception ignored) { }
    }
    private void restartStep() {
        try { VPOperateManager.getInstance().readSportStep(new IBleWriteResponse() { @Override public void onResponse(int code) { } }, new ISportDataListener() { @Override public void onSportDataChange(SportData d) { } }); } catch (Exception ignored) { }
    }
    private void endAccTest() {
        stopFallMonitor(); restartStep();
        sendToast("加速度采集结束 x=" + accX + " y=" + accY + " z=" + accZ);
    }
    private void maybeTemperature() {
        if (!connected) return;
        if (!tempSupported) { maybeSleep(); return; }
        if (lastTempAt > 0 && System.currentTimeMillis() - lastTempAt < MIN30) { maybeSleep(); return; }
        if (tempRequested) return;
        tempRequested = true;
        tempDone = false;
        tempTmp = -1;
        Log.w(TAG, "temp detect start");
        VPOperateManager.getInstance().startDetectTempture(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) { Log.w(TAG, "temp cmd fail " + code); finishTemperature(); } }
        }, new ITemptureDetectDataListener() {
            @Override public void onDataChange(TemptureDetectData d) {
                if (d == null) return;
                Log.w(TAG, "temp data: oprate=" + d.getOprate() + " state=" + d.getDeviceState() + " progress=" + d.getProgress() + " temp=" + d.getTempture());
                if (tempDone) return;
                if (d.getTempture() > 25 && d.getTempture() < 45) tempTmp = d.getTempture();
                if (d.getProgress() >= 100 && tempTmp > 0) {
                    tempDone = true;
                    lastTemp = tempTmp;
                    lastTempAt = System.currentTimeMillis();
                    Intent ti = new Intent(ACTION_UPDATE);
                    ti.putExtra("temp", lastTemp);
                    ti.putExtra("upd", nowHM());
                    LocalBroadcastManager.getInstance(VpCollectorService.this).sendBroadcast(ti);
                    Log.w(TAG, "temp=" + lastTemp);
                    finishTemperature();
                }
            }
        });
        ui.postDelayed(new Runnable() { @Override public void run() {
            if (!tempDone && tempRequested) { Log.w(TAG, "temp timeout tmp=" + tempTmp);
                if (tempTmp > 0) { tempDone = true; lastTemp = tempTmp; lastTempAt = System.currentTimeMillis(); }
                finishTemperature();
            }
        } }, 30000);
    }
      private void finishTemperature() {
        if (!tempRequested) return;
        tempRequested = false;
        if (!tempDone) {
            try { VPOperateManager.getInstance().stopDetectTempture(new IBleWriteResponse() { @Override public void onResponse(int code) { } }, new ITemptureDetectDataListener() { @Override public void onDataChange(TemptureDetectData d) { } }); } catch (Exception ignored) { }
        }
        maybeSleep();
    }
      private void maybeSleep() {
        if (!connected) return;
        if (sleepMin > 0 && System.currentTimeMillis() - lastSleepAt < MIN60) { finishRound(); return; }
        if (sleepRequested) return;
        sleepRequested = true;
        sleepBest = null;
        sleepBestDay = null;
        Log.w(TAG, "sleep read start");
        VPOperateManager.getInstance().readSleepData(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) Log.w(TAG, "sleep cmd fail " + code); }
        }, new ISleepDataListener() {
            @Override public void onSleepDataChange(String day, SleepData sd) {
                Log.w(TAG, "sleep day=" + day + " data=" + sd);
                if (sd == null) return;
                int all = sd.getAllSleepTime();
                // 只接受 30~960 分钟的合理睡眠时长
                if (all < 30 || all > 960) return;
                // 关键修复：优先选最近一天(day数字越小越新)，同一天才比时长。
                // 原逻辑挑"总时长最长"，昨晚数据没同步完时会被前一天的旧数据压住。
                int curDay = 99, bestDay = 99;
                try { curDay = Integer.parseInt(day.trim()); } catch (Exception e) { }
                if (sleepBestDay != null) { try { bestDay = Integer.parseInt(sleepBestDay.trim()); } catch (Exception e) { } }
                boolean better = (sleepBest == null)
                        || (curDay < bestDay)
                        || (curDay == bestDay && all > sleepBest.getAllSleepTime());
                if (better) { sleepBest = sd; sleepBestDay = day; }
            }
            @Override public void onSleepProgress(float progress) { }
            @Override public void onSleepProgressDetail(String day, int packagenumber) { }
            @Override public void onReadSleepComplete() {
                Log.w(TAG, "sleep read complete");
                ui.postDelayed(new Runnable() { @Override public void run() { pickSleep(); } }, 800);
            }
        }, watchDay);
        ui.postDelayed(new Runnable() { @Override public void run() { if (sleepRequested) pickSleep(); } }, 30000);
    }

    private void pickSleep() {
        if (!sleepRequested) return;
        sleepRequested = false;
        if (sleepWaiter != null) { ui.removeCallbacks(sleepWaiter); sleepWaiter = null; }
        if (sleepBest != null) {
            sleepMin = sleepBest.getAllSleepTime();
            sleepDeep = sleepBest.getDeepSleepTime();
            sleepLight = sleepBest.getLowSleepTime();
            sleepWakeN = sleepBest.getWakeCount();
            try {
                TimeData dn = sleepBest.getSleepDown();
                TimeData up = sleepBest.getSleepUp();
                sleepDown = dn == null ? null : String.format(Locale.CHINA, "%02d:%02d", dn.getHour(), dn.getMinute());
                sleepUp = up == null ? null : String.format(Locale.CHINA, "%02d:%02d", up.getHour(), up.getMinute());
            } catch (Exception e) {
                Log.w(TAG, "sleep time err: " + e);
                sleepDown = null; sleepUp = null;
            }
            // 睡眠补救：用手环报醒之后自己的心率/步数，修正被误判成"清醒"的时间
            correctSleepEnd();
            // 睡眠分期：官方Demo编码，精准睡眠每点1分钟(0深睡/1浅睡/2快速眼动/3失眠/4苏醒)
            sleepRem = -1; sleepAwakeMin = -1; sleepSeg = null;
            try {
                String line = sleepBest.getSleepLine();
                if (line != null && !line.isEmpty()) {
                    int rem = 0, aw = 0;
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == '2') rem++;
                        else if (c == '3' || c == '4') aw++;
                    }
                    sleepRem = rem; sleepAwakeMin = aw;
                    TimeData dn = sleepBest.getSleepDown();
                    int baseMin = (dn == null) ? 0 : dn.getHour() * 60 + dn.getMinute();
                    StringBuilder sb = new StringBuilder();
                    char prev = 0; int segStart = 0;
                    for (int i = 0; i <= line.length(); i++) {
                        char c = (i < line.length()) ? line.charAt(i) : 0;
                        if (c != prev) {
                            if (prev != 0) {
                                if (sb.length() > 0) sb.append(';');
                                sb.append(fmtClock(baseMin + segStart)).append('-').append(fmtClock(baseMin + i)).append('|').append(prev);
                            }
                            prev = c; segStart = i;
                        }
                    }
                    if (sleepExtFrom != null && sleepUp != null && !sleepUp.equals(sleepExtFrom)) {
                        if (sb.length() > 0) sb.append(';');
                        sb.append(sleepExtFrom).append('-').append(sleepUp).append("|9");
                    }
                    sleepSeg = sb.toString();
                    Log.w(TAG, "sleep stages: len=" + line.length() + " deep=" + sleepDeep + " light=" + sleepLight + " rem=" + rem + " awake=" + aw);
                }
            } catch (Exception e) { Log.w(TAG, "sleep stage err " + e); }
            lastSleepAt = System.currentTimeMillis();
            roundHasSleep = true;

            // 醒来检测：今天出现新的"睡醒时间"→ 未来30分钟每3分钟采一次
            if (sleepUp != null && sleepMin > 0) {
                String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
                String segKey = todayKey + " " + sleepUp;
                if (!segKey.equals(lastSleepUpKey)) {
                    lastSleepUpKey = segKey;
                    wakeBoostUntil = System.currentTimeMillis() + 30 * 60 * 1000L;
                    Log.w(TAG, "wake detected at " + sleepUp + " -> boost 30min every 3min");
                    sendToast("检测到睡醒，接下来30分钟每3分钟采集一次");
                }
            }            Log.w(TAG, "sleep all=" + sleepMin + " deep=" + sleepDeep + " light=" + sleepLight + " wake=" + sleepWakeN + " " + sleepDown + "-" + sleepUp);
            Intent i = new Intent(ACTION_UPDATE);
            i.putExtra("sleepMin", sleepMin);
            if (sleepDeep >= 0) i.putExtra("sleepDeep", sleepDeep);
            if (sleepLight >= 0) i.putExtra("sleepLight", sleepLight);
            if (sleepRem >= 0) i.putExtra("sleepRem", sleepRem);
            if (sleepAwakeMin >= 0) i.putExtra("sleepAwake", sleepAwakeMin);
            if (sleepSeg != null) i.putExtra("sleepSeg", sleepSeg);
            if (sleepWakeN >= 0) i.putExtra("sleepWake", sleepWakeN);
            if (sleepDown != null) i.putExtra("sleepDown", sleepDown);
            if (sleepUp != null) i.putExtra("sleepUp", sleepUp);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
        finishRound();
    }

    // 睡眠补救：手环常把"躺着没动"误判为清醒（如11-9凌晨把5:16后仍在睡的71分钟算成清醒）。
    // 判断依据是App自己每5分钟采的数据：醒来时间点之后，只要步数不涨、心率仍在睡眠基线附近，
    // 就把睡眠结束时间往后延；连续2行(约10分钟)心率偏高或步数明显增长才判为真醒。
    private void correctSleepEnd() {
        try {
            if (sleepUp == null || sleepMin <= 0) return;
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
            String all = csvReadAll();
            if (all == null || all.isEmpty()) return;
            String[] lines = all.split("\n");
            int baseSum = 0, baseN = 0;          // 睡眠基线心率：今天醒来时间之前的夜间行
            int startSteps = -1, lastSteps = -1; // 步数是累计值
            String lastSleepTime = null;         // 最后一个仍像"睡着"的时间点
            String firstAfterWake = null;        // 醒来后第一条数据时间，用来防止断连后误补
            int overCount = 0;                   // 连续心率偏高计数，达到2行判清醒
            for (String line0 : lines) {
                String line = line0.trim();
                if (line.isEmpty()) continue;
                String[] cl = line.split(",");
                if (cl.length < 5 || !cl[0].startsWith(today)) continue;
                String hhmm = cl[0].substring(11, 16);
                int hr = 0;
                try { hr = Integer.parseInt(cl[1]); } catch (Exception e) { continue; }
                if (hr < 30 || hr > 220) continue;
                int steps = lastSteps;
                try { steps = Integer.parseInt(cl[4]); } catch (Exception e) { steps = lastSteps; }
                if (hhmm.compareTo(sleepUp) < 0) {       // 醒来之前：统计睡眠基线心率
                    baseSum += hr; baseN++;
                    lastSteps = steps;
                    continue;
                }
                if (firstAfterWake == null) firstAfterWake = hhmm;
                if (startSteps < 0) startSteps = steps;
                int stepGrow = steps - startSteps;
                int baseAvg = baseN > 0 ? baseSum / baseN : 80;
                if (stepGrow > 10) break;                // 步数涨了=真的起来走了
                if (hr > baseAvg + 10) {                 // 心率高于睡眠基线
                    overCount++;
                    if (overCount >= 2) break;           // 连续2行才算醒，单次翻身不算
                } else {
                    overCount = 0;
                    lastSleepTime = hhmm;                // 仍像睡着，记录时间点
                }
                lastSteps = steps;
            }
            if (firstAfterWake != null && minutesBetween(sleepUp, firstAfterWake) > 30) {
                Log.w(TAG, "sleep correction skip: first data after wake too far " + sleepUp + " -> " + firstAfterWake);
                return;
            }
            if (lastSleepTime != null) {
                int ext = minutesBetween(sleepUp, lastSleepTime);
                if (ext >= 15 && ext <= 120) {
                    Log.w(TAG, "sleep corrected +" + ext + "min by own hr/steps: " + sleepUp + " -> " + lastSleepTime);
                    sleepMin += ext;
                    sleepExtFrom = sleepUp;   // 记录补救段起点，时间轴里画成"推断"
                    sleepUp = lastSleepTime;
                } else {
                    Log.w(TAG, "sleep correction skip ext=" + ext);
                }
            }
        } catch (Exception e) { Log.w(TAG, "sleep correct err " + e); }
    }

    private int minutesBetween(String a, String b) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.CHINA);
            java.util.Date da = f.parse(a), db = f.parse(b);
            return (int) Math.round((db.getTime() - da.getTime()) / 60000.0);
        } catch (Exception e) { return 0; }
    }

    // 分钟数转 HH:mm（跨午夜自动回绕）
    private String fmtClock(int totalMin) {
        int m = ((totalMin % 1440) + 1440) % 1440;
        return String.format(Locale.CHINA, "%02d:%02d", m / 60, m % 60);
    }

    // ===== 一轮结束：写 CSV + 推送波形/历史 =====
    private void finishRound() {
        releaseWake();
        flowStarted = false;
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
        csvAppend(new String[]{
                ts,
                lastHr > 0 ? String.valueOf(lastHr) : "",
                lastSpo2 > 0 ? String.valueOf(lastSpo2) : "",
                lastBatt >= 0 ? String.valueOf(lastBatt) : "",
                lastSteps >= 0 ? String.valueOf(lastSteps) : "",
                lastBpH > 0 ? String.valueOf(lastBpH) : "",
                lastBpL > 0 ? String.valueOf(lastBpL) : "",
                lastHrv > 0 ? String.valueOf(lastHrv) : "",
                roundHasSleep && sleepMin > 0 ? String.valueOf(sleepMin) : "",
                lastSugar > 0 ? String.format(Locale.CHINA, "%.1f", lastSugar) : "",
                lastStress > 0 ? String.valueOf(lastStress) : "",
                lastTemp > 0 ? String.format(Locale.CHINA, "%.1f", lastTemp) : ""
        });
        Log.w(TAG, "round done (light=" + roundLight + ") csv appended");
        // 当日波形序列
        String today = ts.substring(0, 10);
        if (!today.equals(seqDate)) { seqDate = today; while (seq.length() > 0) seq.remove(0); }
        try {
            JSONObject e = new JSONObject();
            e.put("t", ts.substring(11, 16));
            if (lastHr > 0) e.put("hr", lastHr);
            if (lastSpo2 > 0) e.put("spo2", lastSpo2);
            if (lastBpH > 0) e.put("bph", lastBpH);
            if (lastBpL > 0) e.put("bpl", lastBpL);
            if (lastHrv > 0) e.put("hrv", lastHrv);
            if (lastSugar > 0) e.put("sugar", lastSugar);
            if (lastStress > 0) e.put("stress", lastStress);
            if (lastTemp > 0) e.put("temp", Math.round(lastTemp * 10) / 10.0);
            seq.put(e);
            while (seq.length() > 300) seq.remove(0); // 防止无限增长
        } catch (Exception ex) { Log.w(TAG, "seq err " + ex); }
        String doneMsg = bpDone ? "血压采集完成" : (hrvDone ? "HRV采集完成" : (sugarDone ? "血糖采集完成" : (stressDone ? "压力采集完成" : (spo2Done ? "心率血氧采集完成" : null))));
        if (doneMsg != null) sendToast(doneMsg);
        roundBoost = false;
        broadcastAll();
        maybeEnableAutoHrv();
    }

    // ===== 广播 =====
    private Intent bcast() { return new Intent(ACTION_UPDATE); }
    private void sendB() { LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_UPDATE)); }
    private void dbg(String m) {
        try {
            java.io.File dir = getExternalFilesDir(null);
            java.io.File f = new java.io.File(dir == null ? getFilesDir() : dir, "debug.log");
            FileWriter w = new FileWriter(f, true);
            w.write(new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date()) + " " + m + "\n");
            w.close();
        } catch (Exception e) { }
    }

    private void storeHrv(int hv) {
        if (hv < 5 || hv > 400) return;
        lastHrv = hv;
        lastHrvAt = System.currentTimeMillis();
        sendMetric("hrv", hv);
        Log.w(TAG, "hrv=" + hv + " (from manual test)");
        try {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
            String today = ts.substring(0, 10);
            if (!today.equals(seqDate)) { seqDate = today; while (seq.length() > 0) seq.remove(0); }
            try {
                JSONObject e = new JSONObject();
                e.put("t", ts.substring(11, 16));
                e.put("hrv", lastHrv);
                if (lastHr > 0) e.put("hr", lastHr);
                seq.put(e);
                while (seq.length() > 300) seq.remove(0);
            } catch (Exception ex) { Log.w(TAG, "hrv seq err " + ex); }
            csvAppend(new String[]{
                    ts,
                    lastHr > 0 ? String.valueOf(lastHr) : "",
                    lastSpo2 > 0 ? String.valueOf(lastSpo2) : "",
                    lastBatt >= 0 ? String.valueOf(lastBatt) : "",
                    lastSteps >= 0 ? String.valueOf(lastSteps) : "",
                    lastBpH > 0 ? String.valueOf(lastBpH) : "",
                    lastBpL > 0 ? String.valueOf(lastBpL) : "",
                    String.valueOf(lastHrv),
                    "",
                    lastSugar > 0 ? String.format(Locale.CHINA, "%.1f", lastSugar) : "",
                    lastStress > 0 ? String.valueOf(lastStress) : ""
            });
            broadcastAll();
        } catch (Exception e) { Log.w(TAG, "storeHrv csv err " + e); }
    }

    private void sendMetric(String k, int v) {
        Intent i = new Intent(ACTION_UPDATE);
        i.putExtra(k, v);
        i.putExtra("upd", nowHM());
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
    private void sendToast(String m) {
        Log.w(TAG, "toast: " + m);
        Intent i = new Intent(ACTION_UPDATE);
        i.putExtra("toast", m);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
    private String nowHM() {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
    }

    // 启动时从CSV把今天的记录读回内存波形：断连/服务重启后波形仍显示全天全部数据
    private void loadSeqFromCsv() {
        try {
            String all = csvReadAll();
            if (all == null || all.isEmpty()) return;
            // 修复旧版9列表头：补上"血糖,压力"两列名（只改表头，不动数据）
            int hn = all.indexOf('\n');
            if (hn > 0) {
                String head = all.substring(0, hn);
                if (head.startsWith("时间,") && !head.contains("血糖")) {
                    all = "时间,心率,血氧,电量,步数,血压高压,血压低压,HRV,睡眠分钟,血糖,压力,体温" + all.substring(hn);
                    csvWriteAll(all);
                    Log.w(TAG, "csv header repaired to 11 cols");
                }
            }
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
            String[] lines = all.split("\n");
            seqDate = today;
            for (int li = 1; li < lines.length; li++) {
                String line = lines[li].trim();
                if (line.isEmpty()) continue;
                String[] cl = line.split(",");
                if (cl.length < 9 || !cl[0].startsWith(today)) continue;
                try {
                    JSONObject e = new JSONObject();
                    e.put("t", cl[0].substring(11, 16));
                    if (cl[1].isEmpty()) continue;
                    int hr = Integer.parseInt(cl[1]);
                    if (hr < 30 || hr > 220) continue; // 整行坏数据就跳过
                    e.put("hr", hr);
                    if (cl.length > 2 && !cl[2].isEmpty()) { int v = Integer.parseInt(cl[2]); if (v >= 50 && v <= 100) e.put("spo2", v); }
                    if (cl.length > 5 && !cl[5].isEmpty()) { int v = Integer.parseInt(cl[5]); if (v >= 60 && v <= 200) e.put("bph", v); }
                    if (cl.length > 6 && !cl[6].isEmpty()) { int v = Integer.parseInt(cl[6]); if (v >= 40 && v <= 130) e.put("bpl", v); }
                    if (cl.length > 7 && !cl[7].isEmpty()) { int v = Integer.parseInt(cl[7]); if (v >= 5 && v <= 400) e.put("hrv", v); }
                    if (cl.length > 9 && !cl[9].isEmpty()) { float v = Float.parseFloat(cl[9]); if (v >= 1f && v <= 35f) e.put("sugar", (double) v); }
                    if (cl.length > 10 && !cl[10].isEmpty()) { int v = Integer.parseInt(cl[10]); if (v >= 0 && v <= 100) e.put("stress", v); }
                    seq.put(e);
                } catch (Exception ignore) {}
            }
            while (seq.length() > 300) seq.remove(0);
            Log.w(TAG, "seq rebuilt from csv rows=" + seq.length());
        } catch (Exception ex) { Log.w(TAG, "seq rebuild err " + ex); }
    }

    private void broadcastAll() {
        Log.w(TAG, "broadcastAll called");
        Intent i = new Intent(ACTION_UPDATE);
        i.putExtra("conn", connected ? 1 : 0);
        i.putExtra("upd", nowHM());
        if (lastHr > 0) i.putExtra("hr", lastHr);
        if (lastSpo2 > 0) i.putExtra("spo2", lastSpo2);
        if (lastBatt >= 0) i.putExtra("batt", lastBatt);
        if (lastSteps >= 0) i.putExtra("steps", lastSteps);
        if (lastDis >= 0) i.putExtra("dis", lastDis);
        if (lastKcal >= 0) i.putExtra("kcal", lastKcal);
        if (lastBpH > 0) { i.putExtra("bpHigh", lastBpH); i.putExtra("bpLow", lastBpL); }
        if (lastHrv > 0) i.putExtra("hrv", lastHrv);
        if (lastStress > 0) i.putExtra("stress", lastStress);
        if (lastSugar > 0) i.putExtra("sugar", lastSugar);
        if (sleepMin > 0) {
            i.putExtra("sleepMin", sleepMin);
            if (sleepDeep >= 0) i.putExtra("sleepDeep", sleepDeep);
            if (sleepLight >= 0) i.putExtra("sleepLight", sleepLight);
            if (sleepRem >= 0) i.putExtra("sleepRem", sleepRem);
            if (sleepAwakeMin >= 0) i.putExtra("sleepAwake", sleepAwakeMin);
            if (sleepSeg != null) i.putExtra("sleepSeg", sleepSeg);
            if (sleepWakeN >= 0) i.putExtra("sleepWake", sleepWakeN);
            if (sleepDown != null) i.putExtra("sleepDown", sleepDown);
            if (sleepUp != null) i.putExtra("sleepUp", sleepUp);
        }
        i.putExtra("seq", seq.toString());
        i.putExtra("hist", computeHistJson());
        i.putExtra("recs", computeRecsJson());
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    // ===== CSV =====
    private File csvFile() { return new File(getExternalFilesDir(null), "vp_data.csv"); }

    private synchronized void csvAppend(String[] cols) {
        try {
            File f = csvFile();
            boolean needHeader = !f.exists() || f.length() == 0;
            FileWriter w = new FileWriter(f, true);
            if (needHeader) w.write("时间,心率,血氧,电量,步数,血压高压,血压低压,HRV,睡眠分钟,血糖,压力,体温\n");
            StringBuilder sb = new StringBuilder();
            for (String c : cols) { sb.append(c == null ? "" : c).append(','); }
            sb.setLength(sb.length() - 1);
            sb.append('\n');
            w.write(sb.toString());
            w.close();
            Log.w(TAG, "csv: " + sb);
        } catch (Exception e) { Log.w(TAG, "csv err " + e); }
    }

    private synchronized void csvWriteAll(String content) {
        try {
            FileWriter w = new FileWriter(csvFile(), false);
            w.write(content);
            w.close();
        } catch (Exception e) { Log.w(TAG, "csv rewrite err " + e); }
    }

    private synchronized String csvReadAll() {
        try {
            File f = csvFile();
            if (!f.exists()) return "";
            BufferedReader r = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) { sb.append(line).append('\n'); }
            r.close();
            return sb.toString();
        } catch (Exception e) { Log.w(TAG, "csv read err " + e); return ""; }
    }

    // 按天聚合 CSV → 最近 7 天 JSON：[{d,hr,spo2,steps,bph,bpl,hrv,sleep}]
    private String computeHistJson() {
        try {
            String all = csvReadAll();
            Map<String, double[]> agg = new HashMap<String, double[]>();
            String[] lines = all.split("\n");
            for (int li = 1; li < lines.length; li++) {
                String line = lines[li].trim();
                if (line.isEmpty()) continue;
                String[] c = line.split(",");
                if (c.length < 9) continue;
                String day = c[0].length() >= 10 ? c[0].substring(0, 10) : c[0];
                double[] a = agg.get(day);
                if (a == null) { a = new double[16]; agg.put(day, a); }
                // 0 hrSum 1 hrN 2 spo2Sum 3 spo2N 4 stepsMax 5 bpH 6 bpL 7 hrvSum 8 hrvN 9 sleepLast 10 sugarSum 11 sugarN 12 stressSum 13 stressN
                if (!c[1].isEmpty()) { a[0] += Double.parseDouble(c[1]); a[1]++; }
                if (!c[2].isEmpty()) { a[2] += Double.parseDouble(c[2]); a[3]++; }
                if (!c[4].isEmpty()) { double s = Double.parseDouble(c[4]); if (s > a[4]) a[4] = s; }
                if (!c[5].isEmpty()) a[5] = Double.parseDouble(c[5]);
                if (!c[6].isEmpty()) a[6] = Double.parseDouble(c[6]);
                if (!c[7].isEmpty()) { a[7] += Double.parseDouble(c[7]); a[8]++; }
                if (!c[8].isEmpty()) a[9] = Double.parseDouble(c[8]);   // 睡眠取当天最后一次非空值(最新读数最准)
                if (c.length > 9 && !c[9].isEmpty()) { a[10] += Double.parseDouble(c[9]); a[11]++; }
                if (c.length > 10 && !c[10].isEmpty()) { a[12] += Double.parseDouble(c[10]); a[13]++; }
                if (c.length > 11 && !c[11].isEmpty()) { a[14] += Double.parseDouble(c[11]); a[15]++; }
            }
            JSONArray out = new JSONArray();
            Object[] keys = agg.keySet().toArray();
            java.util.Arrays.sort(keys);
            int start = Math.max(0, keys.length - 7);
            for (int i = start; i < keys.length; i++) {
                String day = (String) keys[i];
                double[] a = agg.get(day);
                JSONObject o = new JSONObject();
                o.put("d", day.substring(5));
                o.put("hr", a[1] > 0 ? (int) Math.round(a[0] / a[1]) : null);
                o.put("spo2", a[3] > 0 ? (int) Math.round(a[2] / a[3]) : null);
                o.put("steps", a[4] > 0 ? (int) a[4] : null);
                o.put("bph", a[5] > 0 ? (int) a[5] : null);
                o.put("bpl", a[6] > 0 ? (int) a[6] : null);
                o.put("hrv", a[8] > 0 ? (int) Math.round(a[7] / a[8]) : null);
                o.put("sleep", a[9] > 0 ? (int) a[9] : null);
                o.put("sugar", a[11] > 0 ? Math.round((a[10] / a[11]) * 10) / 10.0 : null);
                o.put("stress", a[13] > 0 ? (int) Math.round(a[12] / a[13]) : null);
                o.put("temp", a[15] > 0 ? Math.round((a[14] / a[15]) * 10) / 10.0 : null);
                out.put(o);
            }
            return out.toString();
        } catch (Exception e) { Log.w(TAG, "hist err " + e); return "[]"; }
    }

    // 最近7天完整记录：[{t:"MM-dd HH:mm",hr,spo2,steps,batt,bph,bpl,hrv,sleep}]
    private String computeRecsJson() {
        try {
            String all = csvReadAll();
            String[] lines = all.split("\n");
            String cutoff = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(System.currentTimeMillis() - 7 * 24 * 3600 * 1000L));
            java.util.List<JSONObject> tmp = new java.util.ArrayList<JSONObject>();
            SimpleDateFormat parse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            // Key fix: read backwards so we keep the latest 500 rows, not the earliest 500.
            for (int li = lines.length - 1; li >= 1 && tmp.size() < 500; li--) {
                String line = lines[li].trim();
                if (line.isEmpty()) continue;
                String[] cl = line.split(",");
                if (cl.length < 9) continue;
                if (cl[0].compareTo(cutoff + " 00:00:00") < 0) continue;
                JSONObject o = new JSONObject();
                try { o.put("t", cl[0].substring(5, 16)); } catch (Exception e) { continue; }
                try {
                    if (!cl[1].isEmpty()) o.put("hr", Integer.parseInt(cl[1]));
                    if (!cl[2].isEmpty()) o.put("spo2", Integer.parseInt(cl[2]));
                    if (!cl[3].isEmpty()) o.put("batt", Integer.parseInt(cl[3]));
                    if (!cl[4].isEmpty()) o.put("steps", Integer.parseInt(cl[4]));
                    if (!cl[5].isEmpty()) o.put("bph", Integer.parseInt(cl[5]));
                    if (!cl[6].isEmpty()) o.put("bpl", Integer.parseInt(cl[6]));
                    if (!cl[7].isEmpty()) o.put("hrv", Integer.parseInt(cl[7]));
                    if (!cl[8].isEmpty()) o.put("sleep", Integer.parseInt(cl[8]));
                    if (cl.length > 9 && !cl[9].isEmpty()) o.put("sugar", Double.parseDouble(cl[9]));
                    if (cl.length > 10 && !cl[10].isEmpty()) o.put("stress", Integer.parseInt(cl[10]));
                    if (cl.length > 11 && !cl[11].isEmpty()) o.put("temp", Double.parseDouble(cl[11]));
                    tmp.add(o);
                } catch (Exception ignore) {}
            }
            JSONArray out = new JSONArray();
            for (int i = tmp.size() - 1; i >= 0; i--) out.put(tmp.get(i));
            Log.w(TAG, "recs size=" + out.length() + " first=" + (out.length()>0 ? out.getJSONObject(0).optString("t") : "none") + " last=" + (out.length()>0 ? out.getJSONObject(out.length()-1).optString("t") : "none"));
            return out.toString();
        } catch (Exception e) { Log.w(TAG, "recs err " + e); return "[]"; }
    }    // 一次性检查：确保手环自己的"HRV自动测量"是开着的(30分钟一次)，这样它整晚会存HRV历史
    private void maybeEnableAutoHrv() {
        if (autoHrvChecked || !connected) return;
        autoHrvChecked = true;
        VPOperateManager.getInstance().readAutoMeasureSettingData(new IBleWriteResponse() {
            @Override public void onResponse(int code) { if (code != Code.REQUEST_SUCCESS) Log.w(TAG, "automeasure read fail " + code); }
        }, new IAutoMeasureSettingDataListener() {
            @Override public void onSettingDataChange(java.util.List<AutoMeasureData> list) {
                try {
                    if (list == null) return;
                    for (AutoMeasureData d : list) {
                        if (d.getFunType() == EAutoMeasureType.HRV) {
                            Log.w(TAG, "autoHRV item: open=" + d.isSwitchOpen() + " interval=" + d.getMeasureInterval() + " slot=" + d.getCurrentStartMinute() + "-" + d.getCurrentEndMinute());
                            if (!d.isSwitchOpen()) {
                                d.setSwitchOpen(true);
                                d.setIntervalModify(true);
                                int interval = d.getMeasureInterval();
                                int step = d.getStepUnit();
                                if (interval <= 0 || interval > 60) interval = 30;
                                if (step > 0 && interval % step != 0) interval = ((interval / step) + 1) * step;
                                d.setMeasureInterval(interval);
                                Log.w(TAG, "autoHRV enabling, interval=" + interval);
                                VPOperateManager.getInstance().setAutoMeasureSettingData(new IBleWriteResponse() {
                                    @Override public void onResponse(int code) { }
                                }, d, new IAutoMeasureSettingDataListener() {
                                    @Override public void onSettingDataChange(java.util.List<AutoMeasureData> l) { }
                                    @Override public void onSettingDataChangeFail() { Log.w(TAG, "autoHRV set fail"); }
                                    @Override public void onSettingDataChangeSuccess() { Log.w(TAG, "autoHRV set success"); }
                                });
                            } else {
                                Log.w(TAG, "autoHRV already on");
                            }
                            break;
                        }
                    }
                } catch (Exception e) { Log.w(TAG, "autoHRV parse err " + e); }
            }
            @Override public void onSettingDataChangeFail() { Log.w(TAG, "automeasure read fail"); }
            @Override public void onSettingDataChangeSuccess() { }
        });
    }

    // ===== 高心率/低血氧 → 手环震动（固件心率报警 + 手环消息双保险）=====
    private void setupHeartWarning() {
        try {
            if (heartWaringSupported == null || !heartWaringSupported) { Log.w(TAG, "heart warning unsupported"); return; }
            VPOperateManager.getInstance().settingHeartWarning(new IBleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "heart warning write " + code); }
            }, new IHeartWaringDataListener() {
                @Override public void onHeartWaringDataChange(HeartWaringData d) { Log.w(TAG, "heart warning set: " + d); }
            }, new HeartWaringSetting(hrHighThresh, hrLowThresh, true));
        } catch (Exception e) { Log.w(TAG, "heart warning err " + e); }
    }

    /** 鏈€杩戝嚑绉掓槸鍚﹀湪鍓х儓杩愬姩锛堢敤浜庤繃婊ゅ績鐜囦吉宸級 */
    private boolean isMoving() { return accMotionG > 0.22; }

    // ===== 寰綋妫€鎶ュ憡鍘嗗彶锛堝浜虹灞曠ず鐢紝鏈€澶?0鏉★紝鏈€鏂板湪鍓嶏級=====
    private static final String MICRO_PREF = "fg_micro";
    private void storeMicroReport(String json) {
        try {
            JSONObject o = new JSONObject(json);
            o.put("ts", System.currentTimeMillis());
            o.put("time", new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
            android.content.SharedPreferences sp = getSharedPreferences(MICRO_PREF, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString("hist", "[]"));
            JSONArray next = new JSONArray();
            next.put(o);
            for (int i = 0; i < arr.length() && i < 9; i++) next.put(arr.get(i));
            sp.edit().putString("hist", next.toString()).apply();
            Log.w(TAG, "micro report stored, total=" + next.length());
        } catch (Exception e) { Log.w(TAG, "micro store err " + e); }
    }
    private JSONArray getMicroReports() {
        try { return new JSONArray(getSharedPreferences(MICRO_PREF, Context.MODE_PRIVATE).getString("hist", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void maybeAlertHr(int hr) {
        if (!alertHrOn || hr <= 0) return;
        boolean high = hr >= hrHighThresh;
        boolean low = hr <= hrLowThresh;
        if (!high && !low) { hrHighStage = 0; hrLowStage = 0; return; }
        long now = System.currentTimeMillis();
        if (high) {
            if (hrLowStage != 0) hrLowStage = 0;
            if (hrHighStage == 0) {
                hrHighStage = 1; lastHrAlertAt = now;
                writeAlarmLog("HR HIGH STAGE1 hr=" + hr + " thresh=" + hrHighThresh);
                sendBandMsg("心率偏高 " + hr + "次/分，请休息观察；如连续异常会提醒家人");
            } else if (now - lastHrAlertAt >= 30000L) {
                hrHighStage = 0; lastHrAlertAt = now;
                writeAlarmLog("HR HIGH STAGE2 hr=" + hr + " thresh=" + hrHighThresh);
                sendFamilySms("【我在】连续心率偏高提醒：刚才心率 " + hr + " 次/分（超过 " + hrHighThresh + "），请尽快联系确认。");
                cloudAlert("hr_high", "连续心率偏高", String.valueOf(hr));
            }
        } else {
            if (hrHighStage != 0) hrHighStage = 0;
            if (hrLowStage == 0) {
                hrLowStage = 1; lastHrAlertAt = now;
                writeAlarmLog("HR LOW STAGE1 hr=" + hr + " thresh=" + hrLowThresh);
                sendBandMsg("心率偏低 " + hr + "次/分，请坐下休息；如连续偏低会提醒家人");
            } else if (now - lastHrAlertAt >= 30000L) {
                hrLowStage = 0; lastHrAlertAt = now;
                writeAlarmLog("HR LOW STAGE2 hr=" + hr + " thresh=" + hrLowThresh);
                sendFamilySms("【我在】连续心率偏低提醒：刚才心率 " + hr + " 次/分（低于 " + hrLowThresh + "），请尽快联系确认。");
                cloudAlert("hr_low", "连续心率偏低", String.valueOf(hr));
            }
        }
    }

    private void maybeAlertSpo2(int spo2) {
        if (!alertSpo2On || spo2 <= 0 || spo2 >= spo2Thresh) { spo2Stage = 0; return; }
        long now = System.currentTimeMillis();
        if (spo2Stage == 0) {
            spo2Stage = 1; lastSpo2AlertAt = now;
            writeAlarmLog("SPO2 LOW STAGE1 spo2=" + spo2 + " thresh=" + spo2Thresh);
            sendBandMsg("血氧偏低 " + spo2 + "%，请静坐休息；如连续偏低会提醒家人");
        } else if (now - lastSpo2AlertAt >= 30000L) {
            spo2Stage = 0; lastSpo2AlertAt = now;
            writeAlarmLog("SPO2 LOW STAGE2 spo2=" + spo2 + " thresh=" + spo2Thresh);
            sendFamilySms("【我在】连续血氧偏低提醒：刚才血氧 " + spo2 + "%（低于 " + spo2Thresh + "%），请尽快联系确认。");
            cloudAlert("spo2_low", "连续血氧偏低", String.valueOf(spo2));
        }
    }

    // 加速度/跌倒监测启动指令：来自 APP 页面的“开启/重启监测”按钮
    private final android.content.BroadcastReceiver rxAcc = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            boolean start = i.getBooleanExtra("ui", false);
            if (start) {
                Log.w(TAG, "acc cmd start");
                accRunning = false; // 手动按钮允许重新下发指令
                startFallMonitor();
            }
        }
    };
    // 老人端确认或主动求助
    private final android.content.BroadcastReceiver rxFall = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            String action = i.getAction();
            if (ACTION_FALLCONFIRM.equals(action)) confirmFall();
            else if (ACTION_FALLHELP.equals(action)) helpFall();
        }
    };

    private final android.content.BroadcastReceiver rxFamily = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            familyPhone1 = i.getStringExtra("p1");
            familyPhone2 = i.getStringExtra("p2");
            if (familyPhone1 == null || familyPhone1.isEmpty()) familyPhone1 = null;
            if (familyPhone2 == null || familyPhone2.isEmpty()) familyPhone2 = null;
            try {
                getSharedPreferences("fg_family", Context.MODE_PRIVATE).edit()
                        .putString("p1", familyPhone1 == null ? "" : familyPhone1)
                        .putString("p2", familyPhone2 == null ? "" : familyPhone2).apply();
            } catch (Exception e) { Log.w(TAG, "family save err " + e); }
            Log.w(TAG, "family phones saved: p1=" + familyPhone1 + " p2=" + familyPhone2);
        }
    };

    private void loadFamilyPhones() {
        try {
            android.content.SharedPreferences sp = getSharedPreferences("fg_family", Context.MODE_PRIVATE);
            String p1 = sp.getString("p1", "");
            String p2 = sp.getString("p2", "");
            familyPhone1 = (p1 == null || p1.isEmpty()) ? null : p1;
            familyPhone2 = (p2 == null || p2.isEmpty()) ? null : p2;
            Log.w(TAG, "family phones loaded: p1=" + familyPhone1 + " p2=" + familyPhone2);
        } catch (Exception e) { Log.w(TAG, "family load err " + e); }
    }

    private final android.content.BroadcastReceiver smsResultRx = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int code = getResultCode();
            Log.w(TAG, "SMS RESULT phone=" + i.getStringExtra("phone") + " code=" + code);
            writeAlarmLog("SMS RESULT phone=" + i.getStringExtra("phone") + " code=" + code);
        }
    };
    // 关键报警事件写入手机存储 alarm.log，重启/日志滚动也不会丢
    private void writeAlarmLog(String msg) {
        try {
            java.io.File f = new java.io.File(getExternalFilesDir(null), "alarm.log");
            String line = new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.CHINA).format(new java.util.Date()) + " " + msg + "\n";
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f, true);
            fos.write(line.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) { Log.w(TAG, "alarm log err " + e); }
    }

    private void sendBandMsg(String content) {
        try {
            if (!connected) return;
            Intent ni = new Intent(ACTION_UPDATE); ni.putExtra("familyAlert", content);
            LocalBroadcastManager.getInstance(this).sendBroadcast(ni);
            VPOperateManager.getInstance().sendSocialMsgContent(new IBleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "band msg write " + code); }
            }, new ContentSmsSetting(ESocailMsg.SMS, "健康提醒", "10086", content));
            sendToast("已发手环提醒：" + content);
        } catch (Exception e) { Log.w(TAG, "band msg err " + e); }
    }

    private void sendProg(String key, int v) {
        Intent i = new Intent(ACTION_UPDATE); i.putExtra(key, v);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void sendResult(String key, String json) {
        Intent i = new Intent(ACTION_UPDATE); i.putExtra(key, json == null ? "{\"err\":1}" : json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private String microErrText(EMiniCheckupTestErrorCode e) {
        if (e == EMiniCheckupTestErrorCode.FUNCTION_NOT_SUPPORT) return "手环不支持";
        if (e == EMiniCheckupTestErrorCode.TEST_COMPLETE_NO_RESULT) return "测量完成但没有结果";
        if (e == EMiniCheckupTestErrorCode.DEVICE_BUSY) return "手环正忙";
        if (e == EMiniCheckupTestErrorCode.LOW_POWER) return "手环电量低";
        if (e == EMiniCheckupTestErrorCode.WEARING_ABNORMALITY) return "佩戴不正常";
        if (e == EMiniCheckupTestErrorCode.ECG_LEAD_DETACHMENT) return "导联脱落";
        return String.valueOf(e);
    }

    // ===== 微体检（手环综合检测，约1~2分钟）=====
    private void startMicro() {
        if (!connected) { sendToast("手环未连接，无法微体检"); return; }
        if (microRunning) { sendToast("微体检进行中，请稍候"); return; }
        microRunning = true; manualBusy = true;
        sendToast("微体检开始：戴好手环，坐着别动，约1~2分钟");
        try {
            VPOperateManager.getInstance().startMiniCheckup(new BleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "micro write " + code); }
            }, new IMiniCheckupOptListener() {
                @Override public void onMiniCheckupTestProgress(int p) { sendProg("microProg", p); }
                @Override public void onMiniCheckupStopSuccess() { endMicro(null); }
                @Override public void onMiniCheckupTestFailed(EMiniCheckupTestErrorCode e) { endMicro(null); sendToast("微体检失败：" + microErrText(e)); }
                @Override public void onMiniCheckupSuccess(MiniCheckupResultData d) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("hr", d.getHeartRate());
                        o.put("spo2", d.getBloodOxygen());
                        o.put("stress", d.getStress());
                        o.put("sugarMmol", Math.round(d.getBloodGlucose() * 10) / 10.0);
                        o.put("sugarMgdl", Math.round(d.getBloodGlucose() * 18.0156f));
                        o.put("temp", Math.round(d.getBodyTemperature() * 10) / 10.0);
                        o.put("bph", d.getSystolicBloodPressure());
                        o.put("bpl", d.getDiastolicBloodPressure());
                        o.put("hrv", d.getHrv());
                        if (d.getHrv() >= 5 && d.getHrv() <= 400) storeHrv(d.getHrv());
                        endMicro(o.toString());
                    } catch (Exception e) { Log.w(TAG, "micro result err " + e); endMicro(null); }
                }
                @Override public void onMiniCheckupDetailTestSuccess(MiniCheckupDetailData d) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("hr", d.getHeartRate());
                        o.put("spo2", d.getBloodOxygen());
                        o.put("stress", d.getStress());
                        o.put("sugarMmol", Math.round(d.getBloodGlucose() * 10) / 10.0);
                        o.put("sugarMgdl", Math.round(d.getBloodGlucose() * 18.0156f));
                        o.put("temp", Math.round(d.getBodyTemperature() * 10) / 10.0);
                        int bph = 0, bpl = 0;
                        if (d.getBpPhotoelectric() != null) { bph = d.getBpPhotoelectric().getSystolicBloodPressure(); bpl = d.getBpPhotoelectric().getDiastolicBloodPressure(); }
                        if (bph <= 0 && d.getBpAirPump() != null) { bph = d.getBpAirPump().getSystolicBloodPressure(); bpl = d.getBpAirPump().getDiastolicBloodPressure(); }
                        o.put("bph", bph);
                        o.put("bpl", bpl);
                        o.put("hrv", d.getHrv());
                        if (d.getHrv() >= 5 && d.getHrv() <= 400) storeHrv(d.getHrv());
                        MiniCheckupBloodComponent bc = d.getBloodComponent();
                        if (bc != null) {
                            o.put("uricAcid", Math.round(bc.getUricAcid() * 10) / 10.0);
                            o.put("tcho", Math.round(bc.gettCHO() * 10) / 10.0);
                            o.put("tag", Math.round(bc.gettAG() * 10) / 10.0);
                            o.put("hdl", Math.round(bc.gethDL() * 10) / 10.0);
                            o.put("ldl", Math.round(bc.getlDL() * 10) / 10.0);
                        }
                        MiniCheckupBodyComponent bo = d.getBodyComponent();
                        if (bo != null) {
                            o.put("bmi", Math.round(bo.getBMI() * 10) / 10.0);
                            o.put("bfr", Math.round(bo.getBodyFatRate() * 10) / 10.0);
                            o.put("muscle", Math.round(bo.getMuscleMass() * 10) / 10.0);
                            o.put("water", Math.round(bo.getWaterContent() * 10) / 10.0);
                            o.put("bone", Math.round(bo.getBoneMass() * 100) / 100.0);
                            o.put("protein", Math.round(bo.getProteinMass() * 10) / 10.0);
                            o.put("bmr", Math.round(bo.getBasalMetabolicRate()));
                        }
                        MiniCheckupSkinElectricity se = d.getSkinElectricity();
                        if (se != null) {
                            o.put("emotion", se.getEmotion());
                            o.put("skinWater", se.getSkinMoistureContent());
                            o.put("depression", se.getDepressionRisk());
                            o.put("sympa", se.getSympatheticActivity());
                            o.put("cortisol", se.getCortisolConcentration());
                        }
                        o.put("fatigue", d.getFatigue());
                        endMicro(o.toString());
                    } catch (Exception e) { Log.w(TAG, "micro detail err " + e); endMicro(null); }
                }
            });
            ui.postDelayed(new Runnable() { @Override public void run() { if (microRunning) { endMicro(null); sendToast("微体检超时，未收到结果（手环可能不支持）"); } } }, 300000);
        } catch (Exception e) {
            Log.w(TAG, "micro start err " + e);
            endMicro(null);
            sendToast("微体检启动失败");
        }
    }

    private void endMicro(String json) {
        if (!microRunning) return;
        microRunning = false;
        if (json != null) storeMicroReport(json);
        sendResult("microResult", json);
        manualIdleCheck();
    }

    private void manualIdleCheck() {
        if (!bcRunning && !bodyRunning && !gsrRunning && !ecgRunning && !microRunning) manualBusy = false;
    }

    // ===== 手动单项检测：血液成分（约1分钟） =====
    private void startBc() {
        if (!connected) { sendToast("手环未连接，无法测血液成分"); return; }
        if (bcRunning) { sendToast("血液成分测量进行中，请稍候"); return; }
        if (bcSupported != null && !bcSupported) { sendToast("此手环不支持血液成分"); return; }
        bcRunning = true; manualBusy = true;
        sendToast("血液成分开始：手环戴好，坐着别动，约1分钟");
        try {
            VPOperateManager.getInstance().startDetectBloodComponent(new BleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "bc write " + code); }
            }, false, new IBloodComponentDetectListener() {
                @Override public void onDetecting(int progress, BloodComponent c) { sendProg("bcProg", progress); }
                @Override public void onDetectStop() { endBc(null); }
                @Override public void onDetectFailed(EBloodComponentDetectState st) { sendToast("血液成分失败：" + bcErrText(st)); endBc(null); }
                @Override public void onDetectComplete(BloodComponent c) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("uricAcid", Math.round(c.getUricAcid() * 10) / 10.0);
                        o.put("tcho", Math.round(c.getTCHO() * 10) / 10.0);
                        o.put("tag", Math.round(c.getTAG() * 10) / 10.0);
                        o.put("hdl", Math.round(c.getHDL() * 10) / 10.0);
                        o.put("ldl", Math.round(c.getLDL() * 10) / 10.0);
                        endBc(o.toString());
                    } catch (Exception e) { Log.w(TAG, "bc result err " + e); endBc(null); }
                }
            });
            ui.postDelayed(new Runnable() { @Override public void run() { if (bcRunning) { endBc(null); sendToast("血液成分超时，未收到结果"); } } }, 180000);
        } catch (Exception e) { Log.w(TAG, "bc start err " + e); endBc(null); sendToast("血液成分启动失败"); }
    }

    private void endBc(String json) {
        if (!bcRunning) return;
        bcRunning = false;
        sendResult("bcResult", json);
        manualIdleCheck();
    }

    private String bcErrText(EBloodComponentDetectState st) {
        if (st == EBloodComponentDetectState.BUSY) return "手环正忙";
        if (st == EBloodComponentDetectState.LOW_POWER) return "手环电量低";
        if (st == EBloodComponentDetectState.WEAR_ERROR) return "佩戴不正常，请戴好手环";
        return String.valueOf(st);
    }

    // ===== 手动单项检测：身体成分（约1~2分钟） =====
    private void startBody() {
        if (!connected) { sendToast("手环未连接，无法测身体成分"); return; }
        if (bodyRunning) { sendToast("身体成分测量进行中，请稍候"); return; }
        if (bodySupported != null && !bodySupported) { sendToast("此手环不支持身体成分"); return; }
        bodyRunning = true; manualBusy = true;
        sendToast("身体成分开始：手环戴好，坐着别动，约1~2分钟");
        try {
            VPOperateManager.getInstance().startDetectBodyComponent(new BleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "body write " + code); }
            }, new IBodyComponentDetectListener() {
                @Override public void onDetecting(int progress, int sub) { dbg("body detecting " + progress + "/" + sub); sendProg("bodyProg", progress); sendProg("bodyLead", sub); }
                @Override public void onDetectStop() { dbg("body stop"); endBody(null); }
                @Override public void onDetectFailed(DetectState st) { dbg("body FAILED code=" + st.getCode() + " des=" + st.getDes()); Log.w(TAG, "body detect failed: " + st + " code=" + st.getCode() + " des=" + st.getDes()); sendToast(st.getCode() == 2 ? "身体成分没出结果：测量时手指要按住指环侧面的金属电极，全程保持不动，请再试一次" : "身体成分失败：" + st.getDes()); endBody(null); }
                @Override public void onDetectSuccess(BodyComponent bo) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("bmi", Math.round(bo.getBMI() * 10) / 10.0);
                        o.put("bfr", Math.round(bo.getBodyFatRate() * 10) / 10.0);
                        o.put("muscle", Math.round(bo.getMuscleMass() * 10) / 10.0);
                        o.put("water", Math.round(bo.getWaterContent() * 10) / 10.0);
                        o.put("bone", Math.round(bo.getBoneMass() * 100) / 100.0);
                        o.put("protein", Math.round(bo.getProteinMass() * 10) / 10.0);
                        o.put("bmr", Math.round(bo.getBasalMetabolicRate()));
                        endBody(o.toString());
                    } catch (Exception e) { Log.w(TAG, "body result err " + e); endBody(null); }
                }
            });
            ui.postDelayed(new Runnable() { @Override public void run() { if (bodyRunning) { endBody(null); sendToast("身体成分超时，未收到结果"); } } }, 240000);
        } catch (Exception e) { Log.w(TAG, "body start err " + e); endBody(null); sendToast("身体成分启动失败"); }
    }

    private String downsampleWave(int[] arr) {
        try {
            int nn = arr.length;
            java.util.ArrayList<Integer> clean = new java.util.ArrayList<Integer>();
            for (int i = 0; i < nn; i++) {
                int v = arr[i];
                if (Math.abs(v) < 2147483000) clean.add(v);
            }
            if (clean.size() < 200) return null;
            int outMax = 4000;
            StringBuilder sb = new StringBuilder("[");
            if (clean.size() <= outMax) {
                for (int i = 0; i < clean.size(); i++) { if (i > 0) sb.append(','); sb.append(clean.get(i)); }
            } else {
                int bucket = (int) Math.ceil((double) clean.size() / outMax);
                int cnt = 0;
                for (int st = 0; st < clean.size(); st += bucket) {
                    int en = Math.min(clean.size(), st + bucket);
                    double sum = 0;
                    for (int i = st; i < en; i++) sum += clean.get(i);
                    double mid = sum / (en - st);
                    int pick = clean.get(st); double best = -1;
                    for (int i = st; i < en; i++) { double d = Math.abs(clean.get(i) - mid); if (d > best) { best = d; pick = clean.get(i); } }
                    if (cnt++ > 0) sb.append(',');
                    sb.append(pick);
                }
            }
            sb.append(']');
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private String ecgWaveJson() {
        try {
            int nn = ecgWaveBuf.size();
            if (nn < 200) return null;
            double step = Math.max(1, nn / 1200.0);
            StringBuilder sb = new StringBuilder("[");
            int cnt = 0;
            for (double ix = 0; ix < nn; ix += step) {
                if (cnt++ > 0) sb.append(',');
                sb.append(ecgWaveBuf.get((int) ix));
            }
            sb.append(']');
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private void endBody(String json) {
        if (!bodyRunning) return;
        bodyRunning = false;
        sendResult("bodyResult", json);
        manualIdleCheck();
    }

    // ===== 手动单项检测：皮电/GSR（约1分钟） =====
    private void startGsr() {
        if (!connected) { sendToast("手环未连接，无法测皮电"); return; }
        if (gsrRunning) { sendToast("皮电测量进行中，请稍候"); return; }
        if (gsrSupported != null && !gsrSupported) { sendToast("此手环不支持皮电"); return; }
        gsrRunning = true; manualBusy = true;
        sendToast("皮电开始：手腕放松别动，约1分钟");
        try {
            VPOperateManager.getInstance().startDetectGsr(new BleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "gsr write " + code); }
            }, new IGsrDetectListener() {
                @Override public void onGsrDetectProgress(int p) { sendProg("gsrProg", p); }
                @Override public void onGsrDetectStop() { endGsr(null); }
                @Override public void onGsrDetectFailed(GsrDetectAck ack) { dbg("gsr FAILED " + ack); sendToast("皮电失败：" + (ack == null ? "未知" : ack.getDescription())); endGsr(null); }
                @Override public void onGsrDetectSuccess(GsrDetectResult r) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("emotion", r.getEmotionLevel());
                        o.put("skinWater", r.getSkinMoisture());
                        o.put("depression", r.getDepressionRisk() == DepressionRisk.HIGH ? "高" : (r.getDepressionRisk() == DepressionRisk.MEDIUM ? "中" : "低"));
                        o.put("sympa", r.getSnsActivation());
                        o.put("cortisol", r.getCortisolValue());
                        endGsr(o.toString());
                    } catch (Exception e) { Log.w(TAG, "gsr result err " + e); endGsr(null); }
                }
            });
            ui.postDelayed(new Runnable() { @Override public void run() { if (gsrRunning) { endGsr(null); sendToast("皮电超时，未收到结果"); } } }, 180000);
        } catch (Exception e) { Log.w(TAG, "gsr start err " + e); endGsr(null); sendToast("皮电启动失败"); }
    }

    private void endGsr(String json) {
        if (!gsrRunning) return;
        gsrRunning = false;
        sendResult("gsrResult", json);
        manualIdleCheck();
    }

    // ===== 心电图（约30秒单导联测量）=====
    private void startEcg() {
        if (!connected) { sendToast("手环未连接，无法测心电图"); return; }
        if (ecgRunning) { sendToast("心电图测量进行中"); return; }
        if (ecgSupported != null && !ecgSupported) { sendToast("此手环不支持心电图"); return; }
        ecgRunning = true; manualBusy = true;
        sendToast("心电图开始：手环戴紧，约30秒");
        dbg("ecg startEcg enter");
        ecgWaveBuf.clear();
        try {
            dbg("ecg start");
            VPOperateManager.getInstance().startDetectECG(new BleWriteResponse() {
                @Override public void onResponse(int code) { Log.w(TAG, "ecg write " + code); dbg("ecg write " + code); }
            }, true, new IECGDetectListener() {
                // 心电图手动测量（isNeedCurve=false 不传波形）
                @Override public void onEcgDetectInfoChange(EcgDetectInfo info) { dbg("ecg info " + info); }
                @Override public void onEcgDetectStateChange(EcgDetectState s) {
                    try {
                        dbg("ecg state dataType=" + s.getDataType() + " wear=" + s.getWear() + " prog=" + s.getProgress() + " hr2=" + s.getHr2() + " hrv=" + s.getHrv() + " br2=" + s.getBr2() + " qtc=" + s.getQtc());
                        sendProg("ecgProg", s.getProgress());
                        if (s.getWear() == 1) { sendToast("请戴好手环（导联未贴好）"); stopEcg(); endEcg(null); return; }
                        int dt = s.getDataType();
                        if (dt == 2 || dt == 4) {
                            JSONObject o = new JSONObject();
                            o.put("hr", s.getHr2());
                            if (s.getHrv() != 255) o.put("hrv", s.getHrv());
                            if (s.getBr2() > 0) o.put("br", s.getBr2());
                            o.put("qt", s.getQtc());
                            String wv4 = ecgWaveJson(); if (wv4 != null) o.put("wave", new JSONArray(wv4));
                            o.put("type", "测量完成");
                            o.put("t", System.currentTimeMillis());
                            endEcg(o.toString());
                        } else if (dt == 3) {
                            sendToast("心电图测量失败（手环提示测量不成功，请重试）");
                            endEcg(null);
                        }
                    } catch (Exception e) { Log.w(TAG, "ecg state err " + e); dbg("ecg state ERR " + e); }
                }                @Override public void onEcgDetectResultChange(EcgDetectResult r) {
                    try {
                        dbg("ecg result r=" + r);
                        JSONObject o = new JSONObject();
                        o.put("hr", r.getAveHeart());
                        o.put("hrv", r.getAveHrv());
                        if (r.getAveHrv() >= 5 && r.getAveHrv() <= 400) storeHrv(r.getAveHrv());
                        if (r.getAveResRate() > 0) o.put("br", r.getAveResRate());
                        o.put("qt", r.getAveQT());
                        String wv5 = ecgWaveJson(); if (wv5 != null) o.put("wave", new JSONArray(wv5));
                        o.put("type", r.isSuccess() ? "测量完成" : "有异常提示");
                        o.put("t", System.currentTimeMillis());
                        endEcg(o.toString());
                    } catch (Exception e) { Log.w(TAG, "ecg result err " + e); endEcg(null); }
                }
                @Override public void onEcgDetectDiagnosisChange(EcgDiagnosis d) {
                    try {
                        dbg("ecg diagnosis " + d);
                        if (d == null) return;
                        JSONObject o = new JSONObject();
                        o.put("hr", d.getHeartRate());
                        if (d.getHrv() != 255) { o.put("hrv", d.getHrv()); if (d.getHrv() >= 5 && d.getHrv() <= 400) storeHrv(d.getHrv()); }
                        if (d.getRespRate() > 0) o.put("br", d.getRespRate());
                        o.put("qt", d.getQtTime());
                        o.put("press", d.getPressureIndex());
                        o.put("fatig", d.getFatigueIndex());
                        o.put("myo", d.getMyocarditisRisk());
                        o.put("chd", d.getChdRisk());
                        o.put("ang", d.getAngioscleroticRisk());
                        o.put("dis", d.getDiseaseRisk());
                        o.put("dur", d.getDuration());
                        boolean risk = d.getDiseaseRisk() != 0 || d.getMyocarditisRisk() != 0 || d.getChdRisk() != 0 || d.getAngioscleroticRisk() != 0;
                        int[] fs = d.getFilterSignals();
                        dbg("ecg wave points=" + (fs == null ? -1 : fs.length));
                        String wv6 = (fs != null && fs.length > 200) ? downsampleWave(fs) : ecgWaveJson(); if (wv6 != null) o.put("wave", new JSONArray(wv6));
                        o.put("type", d.isSuccess() ? (risk ? "完成·有异常提示" : "测量完成") : "测量失败");
                        o.put("t", System.currentTimeMillis());
                        endEcg(o.toString());
                    } catch (Exception e) { Log.w(TAG, "ecg diag err " + e); dbg("ecg diag ERR " + e); }
                }
                @Override public void onEcgADCChange(int[] data, int[] power) {
                    if (data == null) return;
                    for (int v : data) { if (v != Integer.MAX_VALUE) { ecgWaveBuf.add(v); if (ecgWaveBuf.size() > 60000) ecgWaveBuf.remove(0); } }
                }
            });
            ui.postDelayed(new Runnable() { @Override public void run() { if (ecgRunning) { stopEcg(); endEcg(null); sendToast("心电图超时，未收到结果（手环可能不支持）"); } } }, 180000);
        } catch (Exception e) {
            Log.w(TAG, "ecg start err " + e);
            endEcg(null);
            sendToast("心电图启动失败");
        }
    }

    private void stopEcg() {
        try {
            VPOperateManager.getInstance().stopDetectECG(new BleWriteResponse() {
                @Override public void onResponse(int code) { }
            }, false, new IECGDetectListener() {
                @Override public void onEcgDetectInfoChange(EcgDetectInfo i) { }
                @Override public void onEcgDetectStateChange(EcgDetectState s) { }
                @Override public void onEcgDetectResultChange(EcgDetectResult r) { }
                @Override public void onEcgDetectDiagnosisChange(EcgDiagnosis d) { }
                @Override public void onEcgADCChange(int[] d, int[] p) { }
            });
        } catch (Exception ignored) { }
    }

    private void endEcg(String json) {
        if (!ecgRunning) return;
        ecgRunning = false;
        manualIdleCheck();
        sendResult("ecgResult", json);
    }

    @Override public void onDestroy() {
        Log.w(TAG, "service onDestroy");
        releaseWake();
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(rx); } catch (Exception ignored) { }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(rx2); } catch (Exception ignored) { }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(rx3); } catch (Exception ignored) { }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(rx4); } catch (Exception ignored) { }
        try { unregisterReceiver(btRx); } catch (Exception ignored) { }
        try { if (mac != null) { VPOperateManager.getInstance().unregisterConnectStatusListener(mac, statusListener); VPOperateManager.getInstance().disconnectWatch(new IBleWriteResponse() {
            @Override public void onResponse(int code) { }
        }); } } catch (Exception ignored) { }
        stopForeground(true);
        super.onDestroy();
    }
}

