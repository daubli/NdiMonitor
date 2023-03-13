package de.daubli.ndimonitor;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import org.apache.commons.lang3.StringUtils;
import com.daubli.ndimonitor.R;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends Activity {

    EditText additionalSourcesEditText;
    SharedPreferences prefs;
    public static final String ADDITIONAL_SOURCES_KEY = "com.daubli.ndimonitor.additionalsources";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(
                "de.daubli.ndimonitor_preferences", Context.MODE_PRIVATE);

        additionalSourcesEditText = findViewById(R.id.additionalSourcesEditText);

        Button button = findViewById(R.id.saveBtn);
        button.setOnClickListener((View v) -> {
            String additionalSourcesValue = trimAndFormatAdditionalSourcesValue();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(ADDITIONAL_SOURCES_KEY, additionalSourcesValue);
            editor.apply();
            this.finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.contains(ADDITIONAL_SOURCES_KEY)) {
            this.additionalSourcesEditText.setText(prefs.getString(ADDITIONAL_SOURCES_KEY, ""));
        }
    }

    private String trimAndFormatAdditionalSourcesValue() {
        String rawValue = additionalSourcesEditText.getText().toString();
        return StringUtils.deleteWhitespace(rawValue);
    }
}
