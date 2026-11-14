package com.family.guardian;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/** 负责网页显示；老人端连接/采集交给 VpCollectorService；家人端只从云端拉数据（v1.11） */
public class MainActivity extends Activity {
    private static final String TAG = "WOZAI";
    private WebView web;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean receiverReady = false;
    private boolean isFamily = false;
    private Runnable familyPoll = null;

    @Override protected void onCreate(Bundle b) {
        Log.w(TAG, "onCreate");
        super.onCreate(b);
        String role = CloudSync.role(this);
        if (role.length() == 0) {
            showRoleDialog();
            return;
        }
        if (CloudSync.code(this).length() == 0) {
            showCodeDialog(role);
            return;
        }
        setupUi(role);
    }

    /** 第一次打开：选择这台手机是老人端还是家人端 */
    private void showRoleDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("选择身份")
                .setMessage("这台手机是谁在用？")
                .setPositiveButton("家人端（子女手机）", (d, w) -> {
                    CloudSync.saveRole(this, "family");
                    showCodeDialog("family");
                })
                .setNegativeButton("老人端（妈妈的手机）", (d, w) -> {
                    CloudSync.saveRole(this, "elder");
                    showCodeDialog("elder");
                })
                .setCancelable(false)
                .show();
    }

    /** 输入一次家庭码（老人端/家人端都要填，且必须一样） */
    private void showCodeDialog(final String role) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("例如 wozai-1011");
        new android.app.AlertDialog.Builder(this)
                .setTitle("输入家庭码")
                .setMessage("老人端和家人端要填同一个家庭码，才能看到同一份数据")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    String code = input.getText().toString().trim();
                    if (code.length() < 3) {
                        android.widget.Toast.makeText(this, "家庭码太短，请重新输入", android.widget.Toast.LENGTH_SHORT).show();
                        showCodeDialog(role);
                        return;
                    }
                    CloudSync.saveCode(this, code);
                    setupUi(role);
                })
                .setCancelable(false)
                .show();
    }

    private void setupUi(String role) {
        Log.w(TAG, "setupUi role=" + role);
        isFamily = "family".equals(role);
        if (isFamily && "[]".equals(CloudSync.codesJson(this)) && CloudSync.code(this).length() > 0) {
            // 老版本升级：把单一家庭码变成家人列表第一项
            try {
                org.json.JSONArray arr = new org.json.JSONArray();
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("code", CloudSync.code(this));
                o.put("name", "家人");
                arr.put(o);
                CloudSync.saveCodesJson(this, arr.toString());
            } catch (Exception ignore) { }
        }
        if (!isFamily) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{android.Manifest.permission.SEND_SMS, android.Manifest.permission.CALL_PHONE}, 1001);
            }
        }
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        setContentView(web);
        web.clearCache(true);
        web.loadUrl(isFamily ? "file:///android_asset/family.html" : "file:///android_asset/app.html");
        web.setWebViewClient(new android.webkit.WebViewClient() {
            @Override public void onPageFinished(android.webkit.WebView v, String url) {
                Log.w(TAG, "page finished");
                if (isFamily) {
                    // 家人端：先把上次缓存的数据画出来，再等轮询
                    pushCachedLatest();
                    return;
                }
                // 老人端：页面就绪 → 向服务要一次完整快照
                Intent snap = new Intent(MainActivity.this, VpCollectorService.class);
                snap.setAction(VpCollectorService.ACTION_SNAPSHOT);
                startService(snap);
            }
        });
        if (isFamily) {
            // 家人端：不开采集，只从云端拉数据
            pollCloud(0);
            return;
        }
        // 老人端：启动前台采集服务（已在运行则复用）
        Intent svc = new Intent(this, VpCollectorService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc); else startService(svc);
        // 请求加入电池优化白名单：用户点一次“允许”，夜里系统就不会拦截采集
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:" + getPackageName())));
            } catch (Exception e) { Log.w(TAG, "battery opt request fail " + e); }
        }
    }

    /** 家人端：把所有家人的缓存 JSON 推给页面 */
    private void pushCachedLatest() {
        final java.util.List<String[]> codes = CloudSync.codeList(this);
        ui.post(new Runnable() { @Override public void run() {
            if (web == null || isFinishing()) return;
            for (String[] e : codes) {
                String json = CloudSync.latestFor(MainActivity.this, e[0]);
                if (json != null && json.length() > 0) {
                    web.evaluateJavascript("onCloudUpdate('" + e[0] + "'," + json + ")", null);
                }
            }
        }});
    }

    /** 家人端：每3分钟把所有家庭码都拉一遍 */
    private void pollCloud(long delayMs) {
        if (familyPoll != null) ui.removeCallbacks(familyPoll);
        familyPoll = new Runnable() {
            @Override public void run() {
                if (!isFamily) return;
                final java.util.List<String[]> codes = CloudSync.codeList(MainActivity.this);
                for (final String[] entry : codes) {
                    new Thread(new Runnable() { @Override public void run() {
                        try {
                            String json = CloudSync.download(MainActivity.this, entry[0]);
                            if (json == null) return;
                            CloudSync.saveLatestFor(MainActivity.this, entry[0], json);
                            maybeNotify(entry[0], entry[1], json);
                            ui.post(new Runnable() { @Override public void run() {
                                if (web != null && !isFinishing()) web.evaluateJavascript("onCloudUpdate('" + entry[0] + "'," + json + ")", null);
                            }});
                            Log.w(TAG, "cloud pull ok " + entry[0]);
                        } catch (Exception e) {
                            Log.w(TAG, "cloud pull err " + entry[0] + " " + e);
                        }
                    }}).start();
                }
                pollCloud(180000L); // 3分钟后继续
            }
        };
        ui.postDelayed(familyPoll, delayMs);
    }

    /** 家人端：发现新异常 → 系统通知（带称呼） */
    private void maybeNotify(String code, String name, String json) {
        try {
            if (json == null) return;
            org.json.JSONObject d = new org.json.JSONObject(json);
            org.json.JSONArray al = d.optJSONArray("alerts");
            if (al == null || al.length() == 0) return;
            org.json.JSONObject a = al.getJSONObject(0);
            long ts = a.optLong("ts", 0);
            android.content.SharedPreferences sp = getSharedPreferences("fg_cloud", Context.MODE_PRIVATE);
            long seen = sp.getLong("lastAlertSeen_" + code, 0);
            if (ts <= seen) return;
            sp.edit().putLong("lastAlertSeen_" + code, ts).apply();
            String who = (name == null || name.length() == 0) ? "家人" : name;
            String msg = who + "：" + a.optString("msg", "老人数据异常");
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String ch = "wozai_alerts";
            if (Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationChannel c = new android.app.NotificationChannel(ch, "异常提醒", android.app.NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(c);
            }
            android.app.Notification.Builder nb = (Build.VERSION.SDK_INT >= 26)
                    ? new android.app.Notification.Builder(this, ch)
                    : new android.app.Notification.Builder(this);
            android.app.Notification n = nb
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("【我在】提醒")
                    .setContentText(msg)
                    .setAutoCancel(true)
                    .build();
            nm.notify(2001, n);
        } catch (Exception e) { Log.w(TAG, "notify err " + e); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (isFamily) {
            pollCloud(0);
            return;
        }
        if (!receiverReady) {
            LocalBroadcastManager.getInstance(this).registerReceiver(rx, new IntentFilter(VpCollectorService.ACTION_UPDATE));
            receiverReady = true;
        }
        // 回到界面时主动要一次快照
        ui.postDelayed(new Runnable() { @Override public void run() {
            Intent snap = new Intent(MainActivity.this, VpCollectorService.class);
            snap.setAction(VpCollectorService.ACTION_SNAPSHOT);
            startService(snap);
        }}, 500);
    }

    @Override protected void onPause() {
        super.onPause();
        if (isFamily) {
            if (familyPoll != null) ui.removeCallbacks(familyPoll);
            familyPoll = null;
            return;
        }
        if (receiverReady) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(rx);
            receiverReady = false;
        }
    }

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i.hasExtra("toast")) jsToast(i.getStringExtra("toast"));
            if (i.hasExtra("conn")) jsSet("setConn", i.getIntExtra("conn", 0) == 1 ? "true" : "false");
            if (i.hasExtra("hr")) jsSet("setHr", String.valueOf(i.getIntExtra("hr", -1)));
            if (i.hasExtra("spo2")) jsSet("setSpo2", String.valueOf(i.getIntExtra("spo2", -1)));
            if (i.hasExtra("steps")) jsSet("setSteps", String.valueOf(i.getIntExtra("steps", -1)));
            if (i.hasExtra("batt")) jsSet("setBatt", String.valueOf(i.getIntExtra("batt", -1)));
            if (i.hasExtra("dis")) jsSet("setDis", String.valueOf(i.getIntExtra("dis", -1)));
            if (i.hasExtra("kcal")) jsSet("setKcal", String.valueOf(i.getIntExtra("kcal", -1)));
            if (i.hasExtra("bpHigh")) {
                int h = i.getIntExtra("bpHigh", -1), l = i.getIntExtra("bpLow", -1);
                jsSet("setBp", String.valueOf(h) + "," + String.valueOf(l));
            }
            if (i.hasExtra("hrv")) jsSet("setHrv", String.valueOf(i.getIntExtra("hrv", -1)));
            if (i.hasExtra("stress")) jsSet("setStress", String.valueOf(i.getIntExtra("stress", -1)));
            if (i.hasExtra("sugar")) jsSet("setSugar", String.valueOf(i.getFloatExtra("sugar", -1)));
            if (i.hasExtra("temp")) jsSet("setTemp", String.valueOf(i.getFloatExtra("temp", -1)));
            if (i.hasExtra("fallPrompt")) jsSet("showFallConfirm", String.valueOf(i.getDoubleExtra("fallPrompt", 0)));
            if (i.hasExtra("fallTimeout")) jsSet("fallPromptTimeout", String.valueOf(i.getDoubleExtra("fallTimeout", 0)));
            if (i.hasExtra("fallNotice")) jsSet("showFallNotice", String.valueOf(i.getDoubleExtra("fallNotice", 0)));
            if (i.hasExtra("familyAlert")) jsSet("pushFamilyNotice", jsStr(i.getStringExtra("familyAlert")));
            if (i.hasExtra("x")) jsSet("setAcc", String.valueOf(i.getIntExtra("x", -9999)) + "," + String.valueOf(i.getIntExtra("y", -9999)) + "," + String.valueOf(i.getIntExtra("z", -9999)) + "," + String.valueOf(i.getDoubleExtra("g", 0)));
            if (i.hasExtra("sleepMin")) {
                String args = String.valueOf(i.getIntExtra("sleepMin", -1))
                        + "," + String.valueOf(i.getIntExtra("sleepDeep", -1))
                        + "," + String.valueOf(i.getIntExtra("sleepLight", -1))
                        + "," + String.valueOf(i.getIntExtra("sleepWake", -1))
                        + "," + jsStr(i.getStringExtra("sleepDown"))
                        + "," + jsStr(i.getStringExtra("sleepUp"))
                        + "," + String.valueOf(i.getIntExtra("sleepRem", -1))
                        + "," + String.valueOf(i.getIntExtra("sleepAwake", -1));
                jsSet("setSleep", args);
            }
            if (i.hasExtra("upd")) jsSet("setUpd", jsStr(i.getStringExtra("upd")));
            if (i.hasExtra("sleepSeg")) jsSet("setSleepSeg", jsStr(i.getStringExtra("sleepSeg")));
            if (i.hasExtra("seq")) jsSet("setSeq", jsStr(i.getStringExtra("seq")));
            if (i.hasExtra("hist")) jsSet("setHist", jsStr(i.getStringExtra("hist")));
            if (i.hasExtra("recs")) jsSet("setRecs", jsStr(i.getStringExtra("recs")));
            if (i.hasExtra("hrvUnsup")) jsSet("setHrvUnsup", "true");
            if (i.hasExtra("microProg")) jsSet("setMicroProg", String.valueOf(i.getIntExtra("microProg", -1)));
            if (i.hasExtra("ecgProg")) jsSet("setEcgProg", String.valueOf(i.getIntExtra("ecgProg", -1)));
            if (i.hasExtra("microResult")) jsSet("setMicroResult", jsStr(i.getStringExtra("microResult")));
            if (i.hasExtra("ecgResult")) jsSet("setEcgResult", jsStr(i.getStringExtra("ecgResult")));
            if (i.hasExtra("bcProg")) jsSet("setBcProg", String.valueOf(i.getIntExtra("bcProg", -1)));
            if (i.hasExtra("bcResult")) jsSet("setBcResult", jsStr(i.getStringExtra("bcResult")));
            if (i.hasExtra("bodyProg")) jsSet("setBodyProg", "{\"p\":" + i.getIntExtra("bodyProg", -1) + ",\"lead\":" + i.getIntExtra("bodyLead", -1) + "}");
            if (i.hasExtra("bodyResult")) jsSet("setBodyResult", jsStr(i.getStringExtra("bodyResult")));
            if (i.hasExtra("gsrProg")) jsSet("setGsrProg", String.valueOf(i.getIntExtra("gsrProg", -1)));
            if (i.hasExtra("gsrResult")) jsSet("setGsrResult", jsStr(i.getStringExtra("gsrResult")));
        }
    };

    private String jsStr(String v) {
        if (v == null) return "null";
        return '"' + v.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private void jsToast(final String msg) {
        Log.w(TAG, "toast: " + msg);
        ui.post(new Runnable() { @Override public void run() {
            if (web == null || isFinishing()) return;
            web.evaluateJavascript("toast('" + msg.replace("'", "") + "')", null);
        }});
    }

    private void jsSet(final String fn, final String value) {
        ui.post(new Runnable() { @Override public void run() {
            if (web == null || isFinishing()) return;
            web.evaluateJavascript(fn + "(" + value + ")", null);
        }});
    }

    private class Bridge {
        @JavascriptInterface public void connectRing() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_RESCAN));
        }
        @JavascriptInterface public void vibrate(int ms) {
            try {
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v == null) return;
                if (Build.VERSION.SDK_INT >= 26) v.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(ms);
            } catch (Exception e) { Log.w(TAG, "vibrate fail " + e); }
        }
        @JavascriptInterface public void micro() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_MICRO));
        }
        @JavascriptInterface public void ecg() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_ECG));
        }
        @JavascriptInterface public void bc() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_BC));
        }
        @JavascriptInterface public void body() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_BODY));
        }
        @JavascriptInterface public void gsr() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_GSR));
        }
        @JavascriptInterface public void setUserInfo(double h, double w, int age, String sex) {
            Intent i = new Intent(VpCollectorService.ACTION_SETUSERINFO);
            i.putExtra("h", (int) h); i.putExtra("w", (int) w); i.putExtra("age", age); i.putExtra("sex", sex);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
        }
        @JavascriptInterface public void acc() {
            Intent i = new Intent(VpCollectorService.ACTION_ACC);
            i.putExtra("ui", true);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
            Intent svc = new Intent(MainActivity.this, VpCollectorService.class);
            startService(svc);
        }
        @JavascriptInterface public void setFamily(String p1, String p2) {
            Intent i = new Intent(VpCollectorService.ACTION_SETFAMILY);
            i.putExtra("p1", p1 == null ? "" : p1);
            i.putExtra("p2", p2 == null ? "" : p2);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
        }
        @JavascriptInterface public void saveEcg(String dataUrl){
            try {
                String b64=dataUrl;
                if(b64.startsWith("data:image/png;base64,")) b64=b64.substring(22);
                byte[] bytes=android.util.Base64.decode(b64,android.util.Base64.DEFAULT);
                java.io.File dir=new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),"FamilyGuardian");
                if(!dir.exists())dir.mkdirs();
                java.io.File f=new java.io.File(dir,"ECG_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",java.util.Locale.CHINA).format(new java.util.Date())+".png");
                java.io.FileOutputStream fos=new java.io.FileOutputStream(f);
                fos.write(bytes);fos.close();
                android.content.Intent scan=new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(android.net.Uri.fromFile(f));
                sendBroadcast(scan);
                jsToast("\u5df2\u4fdd\u5b58\u5230\u76f8\u518c:"+f.getName());
            } catch(Exception e){
                jsToast("\u4fdd\u5b58\u5931\u8d25:"+e.getMessage());
            }
        }
        @JavascriptInterface public void setAlert(String key, boolean on) {
            Intent i = new Intent(VpCollectorService.ACTION_SETALERT);
            if ("tg_spo1".equals(key)) i.putExtra("spo2", on); else i.putExtra("hr", on);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
        }
        @JavascriptInterface public void setThresholds(int spo2, int hrHigh, int hrLow, double sugarLow, double fallG) {
            Intent i = new Intent(VpCollectorService.ACTION_SETTHRESH);
            i.putExtra("spo2", spo2);
            i.putExtra("hrHigh", hrHigh);
            i.putExtra("hrLow", hrLow);
            i.putExtra("sugarLow", sugarLow);
            i.putExtra("fallG", fallG);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
        }
        // ===== 老人端：健康数据上传开关（默认开） =====
        @JavascriptInterface public String getLogUpload() { return CloudSync.logUploadOn(MainActivity.this) ? "1" : "0"; }
        @JavascriptInterface public void setLogUpload(String on) { CloudSync.setLogUploadOn(MainActivity.this, "1".equals(on)); }
        // ===== 家人端专用（v1.14 多码） =====
        @JavascriptInterface public String getCodesJson() { return CloudSync.codesJson(MainActivity.this); }
        @JavascriptInterface public void addCode(String name, String code) {
            try {
                if (code == null || code.trim().length() < 3) { jsToast("家庭码太短，请检查"); return; }
                org.json.JSONArray arr = new org.json.JSONArray(CloudSync.codesJson(MainActivity.this));
                for (int i = 0; i < arr.length(); i++) {
                    if (code.trim().equals(arr.getJSONObject(i).optString("code"))) { jsToast("这个家庭码已经添加过了"); return; }
                }
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("code", code.trim());
                o.put("name", (name == null || name.trim().length() == 0) ? "家人" : name.trim());
                arr.put(o);
                CloudSync.saveCodesJson(MainActivity.this, arr.toString());
                if (CloudSync.code(MainActivity.this).length() == 0) CloudSync.saveCode(MainActivity.this, code.trim());
                jsToast("已添加：" + o.optString("name"));
                ui.post(new Runnable() { @Override public void run() {
                    if (web != null && !isFinishing()) web.evaluateJavascript("onCodesChanged()", null);
                }});
                pollCloud(0);
            } catch (Exception e) { jsToast("添加失败：" + e.getMessage()); }
        }
        @JavascriptInterface public void removeCode(String code) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(CloudSync.codesJson(MainActivity.this));
                org.json.JSONArray next = new org.json.JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    if (!code.equals(arr.getJSONObject(i).optString("code"))) next.put(arr.get(i));
                }
                CloudSync.saveCodesJson(MainActivity.this, next.toString());
                jsToast("已删除：" + code);
                ui.post(new Runnable() { @Override public void run() {
                    if (web != null && !isFinishing()) web.evaluateJavascript("onCodesChanged()", null);
                }});
            } catch (Exception e) { jsToast("删除失败：" + e.getMessage()); }
        }
        @JavascriptInterface public String getCachedJson(String code) { return CloudSync.latestFor(MainActivity.this, code); }
        @JavascriptInterface public String getConfigJson(String code) { return CloudSync.configLocal(MainActivity.this, code); }
        @JavascriptInterface public void saveThresholds(String code, int spo2, int hrHigh, int hrLow, double sugarLow, double fallG) {
            new Thread(new Runnable() { @Override public void run() {
                try {
                    org.json.JSONObject cfg = new org.json.JSONObject();
                    cfg.put("spo2", spo2);
                    cfg.put("hrHigh", hrHigh);
                    cfg.put("hrLow", hrLow);
                    cfg.put("sugarLow", Math.round(sugarLow * 10) / 10.0);
                    cfg.put("fallG", Math.round(fallG * 10) / 10.0);
                    cfg.put("updatedBy", "family");
                    CloudSync.saveConfig(MainActivity.this, code, cfg);
                    jsToast("阈值已保存到云端，对方手机3分钟内自动生效");
                } catch (Exception e) {
                    jsToast("保存失败：" + e.getMessage());
                }
            }}).start();
        }
        @JavascriptInterface public void testBand() {
            Intent svc = new Intent(MainActivity.this, VpCollectorService.class);
            svc.setAction(VpCollectorService.ACTION_TESTBAND);
            startService(svc);
        }
        @JavascriptInterface public void fallConfirmed() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_FALLCONFIRM));
        }
        @JavascriptInterface public void fallHelp() {
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(VpCollectorService.ACTION_FALLHELP));
        }
        @JavascriptInterface public void sendWeChat(String text) {
            try {
                Intent si = new Intent(android.content.Intent.ACTION_SEND);
                si.setType("text/plain");
                si.putExtra(android.content.Intent.EXTRA_TEXT, text == null ? "" : text);
                si.setPackage("com.tencent.mm");
                startActivity(Intent.createChooser(si, "发送微信提醒"));
            } catch (Exception e) {
                jsToast("微信提醒打不开，请先用短信确认：" + e.getMessage());
            }
        }
        @JavascriptInterface public String status() { return "service"; }
        // ===== 家人端专用 =====
        @JavascriptInterface public String getLatestJson() { return CloudSync.latest(MainActivity.this); }
        @JavascriptInterface public String familyCode() { return CloudSync.code(MainActivity.this); }
        @JavascriptInterface public void refreshNow() { pollCloud(0); }
        @JavascriptInterface public void resetRole() {
            CloudSync.saveRole(MainActivity.this, "");
            CloudSync.saveCode(MainActivity.this, "");
            ui.post(new Runnable() { @Override public void run() { recreate(); } });
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    // 注意：不 stopService —— 退出界面后服务继续在后台采集
}