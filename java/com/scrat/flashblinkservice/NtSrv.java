package com.scrat.flashblinkservice;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.Calendar;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class NtSrv extends AccessibilityService {
    private SrCtrl isFaceDown;
    private CameraManager mCameraManager;
    private Flashing flashing;
    private boolean flashblink;
    private boolean bat_lev;

    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (getValue(accessibilityEvent.getPackageName().toString(), false))
            StartFlash("income_app");
    }

    public void onInterrupt() {
    }

    public void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    public void onServiceConnected() {
        isFaceDown = new SrCtrl(getApplicationContext());
        IntentFilter intentFilters = new IntentFilter();
        intentFilters.addAction("android.intent.action.PHONE_STATE");
        intentFilters.addAction("android.provider.Telephony.SMS_RECEIVED");

        intentFilters.addAction("com.htc.worldclock.ALARM_ALERT");
        intentFilters.addAction("com.sonyericsson.alarm.ALARM_ALERT");
        intentFilters.addAction("com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT");
        intentFilters.addAction("com.htc.android.worldclock.ALARM_ALERT");
        intentFilters.addAction("zte.com.cn.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.motorola.blur.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.urbandroid.sleep.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.splunchy.android.alarmclock.ALARM_ALERT");
        intentFilters.addAction("com.lge.clock.alarmclock");

        intentFilters.addAction("android.intent.action.ACTION_BATTERY_LOW");
        intentFilters.addAction("android.intent.action.ACTION_BATTERY_OKAY");
        intentFilters.setPriority(0);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().endsWith(".PHONE_STATE")) {
                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING))
                        StartFlash("income_call");
                    else {
                        flashblink = false;
                        if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE))
                            StartFlash("income_miss");
                    }
                } else if (intent.getAction().endsWith(".SMS_RECEIVED")) StartFlash("income_sms");
                else if (intent.getAction().endsWith(".ALARM_ALERT")) StartFlash("income_alarm");
                else bat_lev = intent.getAction().endsWith("LOW");
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
            if (mCameraManager != null) mCameraManager.setTorchMode("0", enabled);
            return true;
        } catch (CameraAccessException e) {
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
            if (flashing != null) flashing.cancel(true);
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
                flashing = new Flashing();
                flashing.execute((Integer[]) ArrayParam);
            }
        }
    }

    private class Flashing extends AsyncTask<Integer, Void, Void> {
        private final long timeshtamp = System.currentTimeMillis();

        private void SleepTime(int t) {
            if (!(isCancelled() || !flashblink))
                try {
                    Thread.sleep(t);
                } catch (InterruptedException ignored) {
                }
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
            if (flashblink) CameraRelease();
            flashblink = FlashlightInitialized();
        }

        @Override
        protected Void doInBackground(Integer... arrInteger) {
            while (isNotStopFlash() && isFaceDown.isFacedown()) {
                if (arrInteger[2] != 0) {
                    for (int i = 0; i < arrInteger[3]; i++) {
                        if (isNotStopFlash()) blink(arrInteger[0], arrInteger[1]);
                        else return null;
                    }
                    if (isNotStopFlash()) SleepTime(arrInteger[4]);
                } else blink(arrInteger[0], arrInteger[1]);
                if (arrInteger[5] == 0) return null;
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
