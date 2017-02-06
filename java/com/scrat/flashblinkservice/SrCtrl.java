package com.scrat.flashblinkservice;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;

class SrCtrl implements SensorEventListener {
    private final String TAG = "FlashBlinkService.SensorControl";
    private final Context context;
    private SensorManager sensorManager;
    private PowerManager.WakeLock wl;
    private Handler mHandler;
    private Looper mHandlerLooper;
    private boolean faceDown;

    SrCtrl(Context cntx) {
        context = cntx;
    }

    private synchronized void ensureHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mHandlerLooper = thread.getLooper();
            mHandler = new Handler(mHandlerLooper);
        }
    }

    private synchronized void destroySrLisner() {
        if (wl != null) wl.release();
        wl = null;
        if (sensorManager != null) sensorManager.unregisterListener(this);
        sensorManager = null;
        if (mHandlerLooper != null) mHandlerLooper.quit();
        mHandlerLooper = null;
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    private void initialized_sensor() {
        ensureHandler();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL, mHandler);
    }

    boolean isFacedown() {
        if (mHandler != null) return faceDown;
        else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wl.acquire();
            initialized_sensor();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            return faceDown;
        }
    }

    void destroy() {
        destroySrLisner();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        faceDown = sensorEvent.values[2] < 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
