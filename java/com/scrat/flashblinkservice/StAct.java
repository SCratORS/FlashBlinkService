package com.scrat.flashblinkservice;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.util.List;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class StAct extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private String setPreference;
    private List<ApplicationInfo> appList;
    private PreferenceScreen SelectAppScreen;

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


    @Override
    public void onPause() {
        super.onPause();
        getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SelectAppScreen = null;
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
    }

    private void psSetSummary(String key, String def, String ms) {
        EditTextPreference etp = (EditTextPreference) findPreference(key);
        if (etp != null)
            etp.setSummary(getDefaultSharedPreferences(getContext()).getString(key, def).concat(ms));
    }

    private void psSetEnDes(String key) {
        String str = " ".concat(getString(R.string.msec));
        psSetSummary(key.concat("_in"), "25", str);
        psSetSummary(key.concat("_out"), "50", str);
        psSetSummary(key.concat("_count"), "10", "");
        psSetSummary(key.concat("_wait"), "1000", str);
    }

    @Override
    public void onResume() {
        super.onResume();
        psSetEnDes(setPreference);
        getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }


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

}