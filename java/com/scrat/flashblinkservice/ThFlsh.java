package com.scrat.flashblinkservice;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;

class ThFlsh extends AsyncTask<Integer, Void, Void> {
    private long timestamp = System.currentTimeMillis();
    private Integer[] Scheme = new Integer[6];
    private SrCtrl isFaceDown;
    private CameraManager mCameraManager;
    private int mCameraId;
    private boolean mInvert;
    private boolean status;



    ThFlsh(Context context, int cameraId, boolean Invert) {
        isFaceDown = new SrCtrl(context);
        mCameraId = cameraId;
        mInvert = Invert;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        status = true;
    }

    boolean getResult() {
        return status;
    }

    void SetParam(Integer... arrInteger) {
        timestamp = System.currentTimeMillis();
        Scheme = arrInteger;
    }

    private boolean setFlashlight(boolean enabled) {
        try {
            mCameraManager.setTorchMode(String.valueOf(mCameraId), enabled);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void SleepTime(int t) {
        long setCurrentMillis = System.currentTimeMillis() + t;
        while ((System.currentTimeMillis() < setCurrentMillis) && isFaceDown.isFacedown(mInvert)) ;
    }

    private void blink(int i, int o) {
        if (setFlashlight(true)) SleepTime(i);
        if (setFlashlight(false)) SleepTime(o);
    }

    private boolean isNotStopFlash() {
        return System.currentTimeMillis() < timestamp + 300000 && isFaceDown.isFacedown(mInvert);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Integer... arrInteger) {
        Scheme = arrInteger;
        while (isNotStopFlash()) {
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
        status = false;
    }


    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        isFaceDown.destroy();
        setFlashlight(false);
        status = false;
    }
}
