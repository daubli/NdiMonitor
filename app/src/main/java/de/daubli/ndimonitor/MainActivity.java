package de.daubli.ndimonitor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.daubli.ndimonitor.R;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.os.Bundle;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.daubli.ndimonitor.settings.SettingsStore;
import me.walkerknapp.devolay.*;

public class MainActivity extends AppCompatActivity {

    private NsdManager nsdManager;
    DevolayFinder finder;
    ListView sourceListView;
    TextView refreshHint;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private static DevolaySource source;
    private SettingsStore settingsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        setContentView(R.layout.activity_main);
        this.sourceListView = this.findViewById(R.id.sourceListView);
        this.refreshHint = this.findViewById(R.id.refreshHint);
        Devolay.loadLibraries();
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.settingsStore = new SettingsStore();
        this.finder = new DevolayFinder(true, null, settingsStore.getAdditionalSources());

        mSwipeRefreshLayout = findViewById(R.id.swipeToRefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.cardview_shadow_end_color);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshSourcesAndUpdateList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSourcesAndUpdateList();
    }

    private void refreshSourcesAndUpdateList() {
        final Runnable r = () -> {
            runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(true));
            DevolaySource[] sources = refreshAndReturnSources();
            List<String> sourceList =
                    Arrays.stream(sources).map(DevolaySource::getSourceName).collect(Collectors.toList());
            runOnUiThread(() -> {
                ArrayAdapter<String> refreshedSourceArray =
                        new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, sourceList);
                sourceListView.setAdapter(refreshedSourceArray);

                if (!sourceList.isEmpty()) {
                    refreshHint.setVisibility(View.GONE);
                } else {
                    refreshHint.setVisibility(View.VISIBLE);
                }

                sourceListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
                    setCurrentSource(finder.getCurrentSources()[position]);
                    beginStreaming();
                });
                mSwipeRefreshLayout.setRefreshing(false);
            });
        };
        Thread t = new Thread(r);
        t.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.finder.close();
    }

    public static DevolaySource getSource() {
        return source;
    }

    public DevolaySource[] refreshAndReturnSources() {
        if (!finder.waitForSources(5000)) {
            // If no new sources were found
        }
        return finder.getCurrentSources();
    }

    public void beginStreaming() {
        Intent streamingIntent = new Intent(MainActivity.this, StreamNDIVideoActivity.class);
        MainActivity.this.startActivity(streamingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);

        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("Settings")) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(settingsIntent);
        }
        return true;
    }

    public static void setCurrentSource(DevolaySource sourceToSet) {
        source = sourceToSet;
    }
}