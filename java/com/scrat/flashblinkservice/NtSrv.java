package com.scrat.flashblinkservice;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.view.accessibility.AccessibilityEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class NtSrv extends AccessibilityService {
    private Context mContext;
    private SqlHlp dbHelper;
    private ThFlsh mFlashing;
    private boolean bat_lev;

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMMM HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }


    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (getValue(accessibilityEvent.getPackageName().toString(), false)) {
            ContentValues cVal = new ContentValues();
            cVal.put("intent", accessibilityEvent.getPackageName().toString());
            cVal.put("dta", getDateTime());
            dbHelper.post(cVal);
            StartFlash("income_app");
        }
    }

    public void onInterrupt() {
    }

    public void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    public void onServiceConnected() {
        mContext = getApplicationContext();
        dbHelper = new SqlHlp(mContext);

        String cIntent = getValue();
        IntentFilter intentFilters = new IntentFilter();
        intentFilters.addAction("android.intent.action.PHONE_STATE");
        intentFilters.addAction("android.provider.Telephony.SMS_RECEIVED");

        intentFilters.addAction("com.android.deskclock.ALARM_ALERT");
        intentFilters.addAction("com.android.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.htc.worldclock.ALARM_ALERT"); //HTC Работает проверено.
        intentFilters.addAction("com.sonyericsson.alarm.ALARM_ALERT");
        intentFilters.addAction("com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT");
        intentFilters.addAction("zte.com.cn.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.motorola.blur.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.urbandroid.sleep.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.splunchy.android.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.lge.clock.alarmclock");
        intentFilters.addAction("com.tplink.tpclock.ALARM_ALERT"); //TP-LINK Работает проверено.

        intentFilters.addAction("android.intent.action.BATTERY_LOW");
        intentFilters.addAction("android.intent.action.BATTERY_OKAY");

        if (!cIntent.isEmpty()) intentFilters.addAction(cIntent);
        intentFilters.setPriority(0);


        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ContentValues cVal = new ContentValues();
                cVal.put("intent", intent.getAction());
                cVal.put("dta", getDateTime());

                if (Objects.requireNonNull(intent.getAction()).endsWith(".PHONE_STATE")) {
                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (Objects.requireNonNull(phoneState).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        cVal.put("intent", cVal.getAsString("intent").concat(" (").concat(Objects.requireNonNull(phoneState)).concat(")"));
                        StartFlash("income_call");
                    }
                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        cVal.put("intent", cVal.getAsString("intent").concat(" (").concat(phoneState).concat(")"));
                        StartFlash("income_miss");
                    }
                } else if (intent.getAction().endsWith(".SMS_RECEIVED")) StartFlash("income_sms");
                else if (intent.getAction().contains("BATTERY"))
                    bat_lev = intent.getAction().endsWith("LOW");
                else {
                    if (getValue().isEmpty())
                        setValue(intent.getAction());
                    StartFlash("income_alarm");
                }
                dbHelper.post(cVal);
            }
        }, intentFilters);
    }

    private void setValue(String value) {
        getDefaultSharedPreferences(mContext).edit().putString("income_alarm_custom", value).apply();
    }

    private int getValue(String key, int def) {
        return Integer.valueOf(Objects.requireNonNull(getDefaultSharedPreferences(mContext).getString(key, String.valueOf(def))));
    }

    private boolean getValue(String key, boolean def) {
        return getDefaultSharedPreferences(mContext).getBoolean(key, def);
    }

    private String getValue() {
        return String.valueOf(getDefaultSharedPreferences(mContext).getString("income_alarm_custom", ""));
    }

    private void StartFlash(String event) {
        if (getValue(event, false)) {
            int mCameraId = getValue("other_option_camera_id_list", -1);
            if (mCameraId < 0) return;
            boolean mInvert = getValue("invert_mode", false);
            boolean battery = !getValue("safe_mode", true) || !bat_lev;
            int mHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            boolean sleepTimes = !getValue("sleep_mode", true) || (5 < mHour && mHour < 23);
            if (battery && sleepTimes) {
                Integer[] ArrayParam = new Integer[6];
                ArrayParam[0] = getValue(event.concat("_in"), 25);
                ArrayParam[1] = getValue(event.concat("_out"), 50);
                ArrayParam[2] = getValue(event.concat("_strobe"), false) ? 1 : 0;
                ArrayParam[3] = getValue(event.concat("_count"), 10);
                ArrayParam[4] = getValue(event.concat("_wait"), 1000);
                ArrayParam[5] = getValue(event.concat("_repeat"), true) ? 1 : 0;

                if (mFlashing != null) {
                    if (!mFlashing.getResult()) mFlashing = null;
                    else {
                        mFlashing.SetParam((Integer[]) ArrayParam);
                        return;
                    }
                }
                mFlashing = new ThFlsh(mContext, mCameraId, mInvert);
                mFlashing.executeOnExecutor(THREAD_POOL_EXECUTOR, (Integer[]) ArrayParam);
            }
        }
    }
}