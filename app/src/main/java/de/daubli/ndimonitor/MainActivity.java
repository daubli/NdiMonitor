package de.daubli.ndimonitor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.android.material.snackbar.Snackbar;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.daubli.ndimonitor.ndi.Ndi;
import de.daubli.ndimonitor.ndi.NdiFinder;
import de.daubli.ndimonitor.ndi.NdiSource;
import de.daubli.ndimonitor.settings.SettingsStore;
import de.daubli.ndimonitor.sources.VideoSource;
import de.daubli.ndimonitor.usb.UsbDevicePermissionHandler;
import de.daubli.ndimonitor.uvc.UVCSource;
import de.daubli.ndimonitor.uvc.UVCSourceFinder;

public class MainActivity extends AppCompatActivity {

    NdiFinder finder;

    UVCSourceFinder uvcSourceFinder;

    ListView sourceListView;

    TextView refreshHint;

    SwipeRefreshLayout mSwipeRefreshLayout;

    private static VideoSource selectedSource;

    private SettingsStore settingsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.sourceListView = this.findViewById(R.id.sourceListView);
        this.refreshHint = this.findViewById(R.id.refreshHint);
        Ndi.initialize();
        this.settingsStore = new SettingsStore();
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

            // Close previous finder
            if (this.finder != null) {
                this.finder.close();
                this.finder = null;
            }

            // Initialize new finders (ensure they are fully ready before proceeding)
            NdiFinder newFinder = new NdiFinder(false, null, settingsStore.getAdditionalSources());
            UVCSourceFinder newUvcFinder = new UVCSourceFinder(this);

            // Assign to class fields only after successful creation
            this.finder = newFinder;
            this.uvcSourceFinder = newUvcFinder;

            // Now it's safe to use the finder
            de.daubli.ndimonitor.sources.VideoSource[] sources = refreshAndReturnSources();

            // Store actual VideoSource objects for later reference
            List<de.daubli.ndimonitor.sources.VideoSource> sourceList = Arrays.asList(sources);
            List<String> sourceNames = sourceList.stream().map(de.daubli.ndimonitor.sources.VideoSource::getSourceName)
                    .collect(Collectors.toList());

            runOnUiThread(() -> {
                ArrayAdapter<String> refreshedSourceArray = new ArrayAdapter<>(this,
                        R.layout.support_simple_spinner_dropdown_item, sourceNames);
                sourceListView.setAdapter(refreshedSourceArray);

                refreshHint.setVisibility(sourceNames.isEmpty() ? View.VISIBLE : View.GONE);

                sourceListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
                    VideoSource selectedSource = sourceList.get(position);

                    if (selectedSource instanceof NdiSource) {
                        try {
                            selectVideoSource(finder.getFromQueriedSources(selectedSource.getSourceName()));
                            beginStreaming();
                        } catch (IllegalArgumentException iag) {
                            Snackbar.make(sourceListView, "Source unavailable. Please refresh.", Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    } else {
                        selectVideoSource(selectedSource);
                        UsbDevicePermissionHandler permissionHandler = new UsbDevicePermissionHandler(this);
                        permissionHandler.requestPermission(((UVCSource) selectedSource).getUsbDevice(),
                                new UsbDevicePermissionHandler.Callback() {

                                    @Override
                                    public void onPermissionGranted(UsbDevice device) {
                                        beginStreaming();
                                    }

                                    @Override
                                    public void onPermissionDenied(UsbDevice device) {
                                        Snackbar.make(sourceListView, "USB permission denied.", Snackbar.LENGTH_LONG)
                                                .show();
                                    }
                                });
                    }
                });

                mSwipeRefreshLayout.setRefreshing(false);
            });
        };
        new Thread(r).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.finder.close();
    }

    public static VideoSource getSource() {
        return selectedSource;
    }

    public de.daubli.ndimonitor.sources.VideoSource[] refreshAndReturnSources() {
        finder.waitForSources(5000);

        VideoSource[] ndiSources = finder.getCurrentSources();
        UVCSource[] uvcSources = uvcSourceFinder.getUvcSources();

        int totalLength = ndiSources.length + uvcSources.length;
        de.daubli.ndimonitor.sources.VideoSource[] resultVideoSources = new de.daubli.ndimonitor.sources.VideoSource[totalLength];

        System.arraycopy(ndiSources, 0, resultVideoSources, 0, ndiSources.length);
        System.arraycopy(uvcSources, 0, resultVideoSources, ndiSources.length, uvcSources.length);

        return resultVideoSources;
    }

    public void beginStreaming() {
        Intent streamingIntent = new Intent(this, StreamVideoActivity.class);
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

    public static void selectVideoSource(VideoSource videoSource) {
        selectedSource = videoSource;
    }
}
