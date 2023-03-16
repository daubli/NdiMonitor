package de.daubli.ndimonitor.settings;

import static de.daubli.ndimonitor.NdiMonitorApplication.getAppContext;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsStore {

    private final SharedPreferences sharedPreferences;

    private static final String ADDITIONAL_SOURCES_KEY = "com.daubli.ndimonitor.additionalsources";

    public SettingsStore() {
        sharedPreferences = getAppContext().getSharedPreferences("de.daubli.ndimonitor_preferences",
                Context.MODE_PRIVATE);
    }

    public String getAdditionalSources() {
        if (sharedPreferences.contains(ADDITIONAL_SOURCES_KEY)) {
            return sharedPreferences.getString(ADDITIONAL_SOURCES_KEY, "");
        }

        return "";
    }

    public void setAdditionalSources(String additionalSources) {
        String additionalSourcesValue = trimAndFormatAdditionalSourcesValue(additionalSources);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ADDITIONAL_SOURCES_KEY, additionalSourcesValue);
        editor.apply();
    }

    private String trimAndFormatAdditionalSourcesValue(String additionalSources) {
        return StringUtils.deleteWhitespace(additionalSources);
    }
}
