package de.daubli.ndimonitor;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.daubli.ndimonitor.R;

import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.os.Bundle;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.daubli.ndimonitor.ndi.Ndi;
import de.daubli.ndimonitor.ndi.NdiFinder;
import de.daubli.ndimonitor.ndi.NdiSource;
import de.daubli.ndimonitor.settings.SettingsStore;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private NsdManager nsdManager;
    NdiFinder finder;
    ListView sourceListView;
    TextView refreshHint;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private static NdiSource ndiSource;
    private SettingsStore settingsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        registerNsdKeepAliveService();
        setContentView(R.layout.activity_main);
        this.sourceListView = this.findViewById(R.id.sourceListView);
        this.refreshHint = this.findViewById(R.id.refreshHint);
        Ndi.initialize();

        this.settingsStore = new SettingsStore();
        this.finder = new NdiFinder(false, null, settingsStore.getAdditionalSources());

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
            NdiSource[] sources = refreshAndReturnSources();
            List<String> sourceList =
                    Arrays.stream(sources).map(NdiSource::getSourceName).collect(Collectors.toList());
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

    public static NdiSource getSource() {
        return ndiSource;
    }

    public NdiSource[] refreshAndReturnSources() {
        if (!finder.waitForSources(5000)) {
            // If no new sources were found
        }
        return finder.getCurrentSources();
    }

    public void beginStreaming() {
        Intent streamingIntent = new Intent(this, StreamNDIVideoActivity.class);
        MainActivity.this.startActivity(streamingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        inflater.inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("Settings")) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(settingsIntent);
        } else if (item.getTitle().equals("About")) {
            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
            MainActivity.this.startActivity(aboutIntent);
        }
        return true;
    }

    public static void setCurrentSource(NdiSource ndiSourceToSet) {
        ndiSource = ndiSourceToSet;
    }

    private void registerNsdKeepAliveService() {
        //hack to keep nsd daemon alive
        NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setServiceName("KeepAliveService");
        nsdServiceInfo.setServiceType("_keep_alive._tcp");
        nsdServiceInfo.setPort(12345);

        nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
            }
        });
    }
}