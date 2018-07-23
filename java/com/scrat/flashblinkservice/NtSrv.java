package com.scrat.flashblinkservice;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.view.accessibility.AccessibilityEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class NtSrv extends AccessibilityService {
    private SrCtrl isFaceDown;
    private CameraManager mCameraManager;
    private volatile Flashing flashing;
    private boolean flashblink;
    private boolean bat_lev;
    private boolean acl_invert;
    private int camId;
    private SqlHlp dbHelper;

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

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMMM HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void onServiceConnected() {
        isFaceDown = new SrCtrl(getApplicationContext());
        dbHelper = new SqlHlp(getApplicationContext());
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
        intentFilters.setPriority(0);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ContentValues cVal = new ContentValues();
                cVal.put("intent", intent.getAction());
                cVal.put("dta", getDateTime());

                if (Objects.requireNonNull(intent.getAction()).endsWith(".PHONE_STATE")) {
                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        cVal.put("intent", cVal.getAsString("intent").concat(" (").concat(phoneState).concat(")"));
                        StartFlash("income_call");
                    } else {
                        flashblink = false;
                        if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                            cVal.put("intent", cVal.getAsString("intent").concat(" (").concat(phoneState).concat(")"));
                            StartFlash("income_miss");
                        }
                    }
                } else if (intent.getAction().endsWith(".SMS_RECEIVED")) StartFlash("income_sms");
                else if (intent.getAction().endsWith(".ALARM_ALERT")) StartFlash("income_alarm");
                else bat_lev = intent.getAction().endsWith("LOW");
                dbHelper.post(cVal);
            }
        }, intentFilters);
    }

    private int getValue(String key, String def) {
        return Integer.valueOf(getDefaultSharedPreferences(getApplicationContext()).getString(key, def));
    }

    private boolean getValue(String key, boolean def) {
        return getDefaultSharedPreferences(getApplicationContext()).getBoolean(key, def);
    }

    private boolean FlashlightInitialized() {
        mCameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        return mCameraManager != null;
    }

    private boolean setFlashlight(boolean enabled) {
        try {
            if (mCameraManager != null) mCameraManager.setTorchMode(String.valueOf(camId), enabled);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void CameraRelease() {
        flashblink = false;
        setFlashlight(false);
        mCameraManager = null;
        flashing = null;
    }

    private void destroy() {
        CameraRelease();
        isFaceDown.destroy();
    }

    private void StartFlash(String event) {
        if (getValue(event, false)) {
            acl_invert = getValue("invert_mode", false);
            camId = getValue("other_option_camera_id_list" , "-1");
            if (camId < 0) return;
            boolean battery = !getValue("safe_mode", true) || !bat_lev;
            int mHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            boolean sleepTimes = !getValue("sleep_mode", true) || (5 < mHour && mHour < 23);
            if (battery && sleepTimes) {
                Integer[] ArrayParam = new Integer[6];
                ArrayParam[0] = getValue(event.concat("_in"), "25");
                ArrayParam[1] = getValue(event.concat("_out"), "50");
                ArrayParam[2] = getValue(event.concat("_strobe"), false) ? 1 : 0;
                ArrayParam[3] = getValue(event.concat("_count"), "10");
                ArrayParam[4] = getValue(event.concat("_wait"), "1000");
                ArrayParam[5] = getValue(event.concat("_repeat"), true) ? 1 : 0;
                if (flashing != null) flashing.SetParam((Integer[]) ArrayParam); else {
                    flashing = new Flashing();
                    flashing.execute((Integer[]) ArrayParam);
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Flashing extends AsyncTask<Integer, Void, Void> {
        private long timeshtamp = System.currentTimeMillis();
        private Integer[] Scheme = new Integer[6];

        void SetParam(Integer... arrInteger) {
            timeshtamp = System.currentTimeMillis();
            Scheme = arrInteger;
            isFaceDown.addWakeTime();
        }

        private void SleepTime(int t) {
            long setCurrentMillis = System.currentTimeMillis() + t;
            while (!(isCancelled() || !flashblink) && (System.currentTimeMillis() < setCurrentMillis));
        }

        private void blink(int i, int o) {
            if (setFlashlight(true)) SleepTime(i);
            if (setFlashlight(false)) SleepTime(o);
        }

        private boolean isNotStopFlash() {
            return !isCancelled() && flashblink && System.currentTimeMillis() < timeshtamp + 300000;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            flashblink = FlashlightInitialized();
        }

        @Override
        protected Void doInBackground(Integer... arrInteger) {
            Scheme = arrInteger;
            while (isNotStopFlash() && isFaceDown.isFacedown(acl_invert)) {
                if (Scheme[2] != 0) {
                    for (int i = 0; i < Scheme[3]; i++) {
                        if (isNotStopFlash()) blink(Scheme[0], Scheme[1]);
                        else return null;
                    }
                    if (isNotStopFlash()) SleepTime(Scheme[4]);
                } else blink(Scheme[0], Scheme[1]);
                if (Scheme[5] == 0) return null;
            }
            return null;
        }

        @Override
        protected void onCancelled(Void result) {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            destroy();
        }

    }

}
