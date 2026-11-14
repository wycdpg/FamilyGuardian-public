package com.family.guardian;

import android.content.Context;
import android.content.SharedPreferences;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.model.object.GetObjectV2Input;
import com.volcengine.tos.model.object.GetObjectV2Output;
import com.volcengine.tos.model.object.PutObjectInput;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** 云端同步：老人端上传 latest.json，家人端下载同一份文件（火山引擎对象存储 TOS） */
public class CloudSync {
    private static final String REGION = "cn-beijing";
    private static final String ENDPOINT = "https://tos-cn-beijing.volces.com";

    // 使用前请改成自己的云存储配置；不要把真实 AK/SK 提交到公开仓库
    static final String BUCKET = "TODO_BUCKET";
    static final String AK = "TODO_ACCESS_KEY";
    static final String SK = "TODO_SECRET_KEY";

    static boolean configured() {
        return BUCKET != null && !BUCKET.startsWith("TODO")
                && AK != null && !AK.startsWith("TODO")
                && SK != null && !SK.startsWith("TODO");
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("fg_cloud", Context.MODE_PRIVATE);
    }

    static String role(Context c) { return sp(c).getString("role", ""); }
    static void saveRole(Context c, String r) { sp(c).edit().putString("role", r == null ? "" : r).apply(); }
    static String code(Context c) { return sp(c).getString("code", ""); }
    static void saveCode(Context c, String s) { sp(c).edit().putString("code", s == null ? "" : s.trim()).apply(); }
    static void saveLatest(Context c, String json) { sp(c).edit().putString("latest", json == null ? "" : json).apply(); }
    static String latest(Context c) { return sp(c).getString("latest", ""); }

    private static String objectKey(Context c) {
        return code(c) + "/latest.json";
    }

    /** 老人端上传：把整理好的 JSON 写到 家庭码/latest.json（覆盖旧文件） */
    static void upload(Context c, JSONObject data) throws Exception {
        byte[] bytes = data.toString().getBytes("UTF-8");
        TOSV2 tos = new TOSV2ClientBuilder().build(REGION, ENDPOINT, AK, SK);
        try {
            PutObjectInput in = new PutObjectInput();
            in.setBucket(BUCKET);
            in.setKey(objectKey(c));
            in.setContent(new ByteArrayInputStream(bytes));
            in.setContentLength(bytes.length);
            tos.putObject(in);
        } finally {
            try { tos.close(); } catch (Exception ignore) { }
        }
    }

    /** 家人端下载：读到 家庭码/latest.json 的内容字符串 */
    static String download(Context c) throws Exception {
        TOSV2 tos = new TOSV2ClientBuilder().build(REGION, ENDPOINT, AK, SK);
        try {
            GetObjectV2Output out = tos.getObject(new GetObjectV2Input().setBucket(BUCKET).setKey(objectKey(c)));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            InputStream in = out.getContent();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            out.close();
            return bo.toString("UTF-8");
        } finally {
            try { tos.close(); } catch (Exception ignore) { }
        }
    }

    // ===== 远程设置与多码支持（v1.14）=====
    private static String configKeyFor(String code) { return code + "/config.json"; }
    private static String configKey(Context c) { return configKeyFor(code(c)); }

    static void saveConfigLocal(Context c, String code, String json) {
        sp(c).edit().putString("config_" + code, json == null ? "" : json).apply();
    }

    static String configLocal(Context c, String code) {
        return sp(c).getString("config_" + code, "");
    }

    /** 家人端保存设置：本地缓存 + 上传到 家庭码/config.json */
    static void saveConfig(Context c, String code, JSONObject cfg) throws Exception {
        cfg.put("ts", System.currentTimeMillis());
        cfg.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA).format(new java.util.Date()));
        byte[] bytes = cfg.toString().getBytes("UTF-8");
        TOSV2 tos = new TOSV2ClientBuilder().build(REGION, ENDPOINT, AK, SK);
        try {
            PutObjectInput in = new PutObjectInput();
            in.setBucket(BUCKET);
            in.setKey(configKeyFor(code));
            in.setContent(new ByteArrayInputStream(bytes));
            in.setContentLength(bytes.length);
            tos.putObject(in);
        } finally {
            try { tos.close(); } catch (Exception ignore) { }
        }
        saveConfigLocal(c, code, cfg.toString());
    }

    static String downloadConfig(Context c, String code) throws Exception {
        TOSV2 tos = new TOSV2ClientBuilder().build(REGION, ENDPOINT, AK, SK);
        try {
            GetObjectV2Output out = tos.getObject(new GetObjectV2Input().setBucket(BUCKET).setKey(configKeyFor(code)));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            InputStream in = out.getContent();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            out.close();
            return bo.toString("UTF-8");
        } catch (com.volcengine.tos.TosServerException se) {
            if (se.getStatusCode() == 404) return null;
            throw se;
        } finally {
            try { tos.close(); } catch (Exception ignore) { }
        }
    }

    // ===== 多码支持：家人端可关注多位老人 =====
    static String codesJson(Context c) { return sp(c).getString("codes", "[]"); }

    static void saveCodesJson(Context c, String s) { sp(c).edit().putString("codes", s == null ? "[]" : s).apply(); }

    /** 返回 [{code,name},...]；兼容老版本单一家庭码 */
    static java.util.List<String[]> codeList(Context c) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        try {
            JSONArray arr = new JSONArray(codesJson(c));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String code = o.getString("code");
                if (code != null && code.trim().length() > 0) out.add(new String[]{code.trim(), o.optString("name", "家人")});
            }
        } catch (Exception ignore) { }
        if (out.isEmpty() && code(c).length() > 0) out.add(new String[]{code(c), "家人"});
        return out;
    }

    static void saveLatestFor(Context c, String code, String json) {
        sp(c).edit().putString("latest_" + code, json == null ? "" : json).apply();
    }

    static String latestFor(Context c, String code) {
        return sp(c).getString("latest_" + code, "");
    }

    /** 老人端：健康数据上传开关（默认开启） */
    static boolean logUploadOn(Context c) { return sp(c).getBoolean("logUploadOn", true); }

    static void setLogUploadOn(Context c, boolean on) { sp(c).edit().putBoolean("logUploadOn", on).apply(); }

    /** 通用上传：把字节写到指定 key */
    static void uploadBytes(Context c, String key, byte[] bytes) throws Exception {
        TOSV2 tos = new TOSV2ClientBuilder().build(REGION, ENDPOINT, AK, SK);
        try {
            PutObjectInput in = new PutObjectInput();
            in.setBucket(BUCKET);
            in.setKey(key);
            in.setContent(new ByteArrayInputStream(bytes));
            in.setContentLength(bytes.length);
            tos.putObject(in);
        } finally {
            try { tos.close(); } catch (Exception ignore) { }
        }
    }

    /** 下载指定家庭码的 latest.json；不存在(404)返回 null */
    static String download(Context c, String code) throws Exception {
        TOSV2 tos = new TOSV2ClientBuilder().build(REGION, ENDPOINT, AK, SK);
        try {
            GetObjectV2Output out = tos.getObject(new GetObjectV2Input().setBucket(BUCKET).setKey(objectKeyFor(code)));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            InputStream in = out.getContent();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            out.close();
            return bo.toString("UTF-8");
        } catch (com.volcengine.tos.TosServerException se) {
            if (se.getStatusCode() == 404) return null;
            throw se;
        } finally {
            try { tos.close(); } catch (Exception ignore) { }
        }
    }

    private static String objectKeyFor(String code) { return code + "/latest.json"; }

    /** 记录一条异常提醒（最多保留 20 条，最新在前） */
    static synchronized void addAlert(Context c, JSONObject alert) {
        try {
            SharedPreferences sp = sp(c);
            JSONArray arr = new JSONArray(sp.getString("alerts", "[]"));
            JSONArray next = new JSONArray();
            next.put(alert);
            for (int i = 0; i < arr.length() && i < 19; i++) next.put(arr.get(i));
            sp.edit().putString("alerts", next.toString()).apply();
        } catch (Exception ignore) { }
    }

    static JSONArray getAlerts(Context c) {
        try { return new JSONArray(sp(c).getString("alerts", "[]")); } catch (Exception e) { return new JSONArray(); }
    }
}