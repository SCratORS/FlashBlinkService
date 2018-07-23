package com.scrat.flashblinkservice;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.hardware.camera2.CameraCharacteristics.*;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class StAct extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private String setPreference;
    private List<ApplicationInfo> appList;
    private PreferenceScreen SelectAppScreen;
    private PreferenceScreen LogsScreen;

    private int getResourceId(String pVariableName) {
        try {
            return getResources().getIdentifier(pVariableName, "xml", getContext().getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreference = getArguments().getString("start");
        addPreferencesFromResource(getResourceId(setPreference));
        if (setPreference.equals("income_app")) {
            SelectAppScreen = (PreferenceScreen) findPreference("income_app_select");
            if (SelectAppScreen != null) {
                SelectAppScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        if (appList == null) {
                            new LoadApplications().execute();
                        }
                        return true;
                    }
                });

            }
        }
        if (setPreference.equals("other_option")) {
            List<String> cameraName = new ArrayList<>();
            List<String> cameraId   = new ArrayList<>();
            cameraName.add(getString(R.string.default_item));
            cameraId.add("-1");
            CameraManager mCameraManager = Objects.requireNonNull((CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE));
            try {
                for (String id : mCameraManager.getCameraIdList()) {
                    CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                    if (Objects.requireNonNull(c.get(FLASH_INFO_AVAILABLE))) {
                        cameraId.add(id);
                        switch (Objects.requireNonNull(c.get(LENS_FACING))) {
                            case LENS_FACING_BACK : cameraName.add(getString(R.string.back_flashlight));
                                break;
                            case LENS_FACING_FRONT : cameraName.add(getString(R.string.front_flashlight));
                                break;
                            case LENS_FACING_EXTERNAL : cameraName.add(getString(R.string.external_flashlight));
                                break;
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            ListPreference flashlight = (ListPreference) findPreference("other_option_camera_id_list");
            flashlight.setEntries(cameraName.toArray(new String[cameraName.size()]));
            flashlight.setEntryValues(cameraId.toArray(new String[cameraId.size()]));
            if (flashlight.getEntries().length > 1) flashlight.setValueIndex(1);

            LogsScreen = (PreferenceScreen) findPreference("logs_screen");
            if (LogsScreen != null) {
                LogsScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        new LoadLogsInfo().execute();
                        return true;
                    }
                });

            }
        }
    }



    @Override
    public void onPause() {
        super.onPause();
        getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SelectAppScreen = null;
        LogsScreen = null;
        appList = null;
        setPreference = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            String str = ((EditTextPreference) pref).getText();
            if (str.equals("")) str = "0";
            if (!key.endsWith("count")) str = str.concat(" ".concat(getString(R.string.msec)));
            pref.setSummary(str);
        } else if (pref instanceof PreferenceScreen) psSetEnDes(key);
          else if (pref instanceof ListPreference)   pref.setSummary(((ListPreference) pref).getEntry());
    }

    private void psSetSummary(String key, String def, String ms) {
        if (key.endsWith("_list")) {
            ListPreference lpr = (ListPreference) findPreference(key);
            if (lpr != null) lpr.setSummary(lpr.getEntry());
        } else {
            EditTextPreference etp = (EditTextPreference) findPreference(key);
            if (etp != null)
                etp.setSummary(getDefaultSharedPreferences(getContext()).getString(key, def).concat(ms));
        }
    }

    private void psSetEnDes(String key) {
        String str = " ".concat(getString(R.string.msec));
        psSetSummary(key.concat("_in"), "25", str);
        psSetSummary(key.concat("_out"), "50", str);
        psSetSummary(key.concat("_count"), "10", "");
        psSetSummary(key.concat("_wait"), "1000", str);
        psSetSummary(key.concat("_camera_id_list"), "-1", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        psSetEnDes(setPreference);
        getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadApplications extends AsyncTask<Void, Void, Void> {

        private final PackageManager packageManager = getContext().getPackageManager();
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo info : appList) {
                if (packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                    SwitchPreference chb = new SwitchPreference(getContext());
                    chb.setKey(info.packageName);
                    chb.setTitle(info.loadLabel(packageManager));
                    chb.setSummary(info.packageName);
                    chb.setIcon(info.loadIcon(packageManager));
                    chb.setLayoutResource(R.layout.sw_p_l);
                    chb.setChecked(false);
                    SelectAppScreen.addPreference(chb);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(getContext(), null, getString(R.string.load_app_info), true);
            super.onPreExecute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadLogsInfo extends AsyncTask<Void, Void, Void> {
        SqlHlp dbHelper = new SqlHlp(getContext());


        @Override
        protected Void doInBackground(Void... params) {
            List<ContentValues> logList = dbHelper.getRecord();
            for (ContentValues cVal : logList) {
                Preference psc = new Preference(getContext());
                psc.setTitle(cVal.getAsString("intent"));
                psc.setSummary(cVal.getAsString("dta"));
                psc.setLayoutResource(R.layout.im_p_l);
                LogsScreen.addPreference(psc);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            LogsScreen.removeAll();
            Preference groups = new PreferenceCategory(getContext());
            groups.setTitle(getString(R.string.group_logs));
            groups.setLayoutResource(R.layout.gr_p_l);
            LogsScreen.addPreference(groups);
            super.onPreExecute();
        }
    }
}

