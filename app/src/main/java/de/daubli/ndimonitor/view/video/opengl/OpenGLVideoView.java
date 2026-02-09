package de.daubli.ndimonitor.view.video.opengl;

import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.daubli.ndimonitor.ndi.FourCCType;
import de.daubli.ndimonitor.view.focusassist.FocusAssistOverlay;

public class OpenGLVideoView extends GLSurfaceView {

    private GLVideoRenderer videoRenderer;

    private FocusAssistOverlay focusAssistOverlay;

    public OpenGLVideoView(@NonNull Context context) {
        super(context);
        init();
    }

    public OpenGLVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);

        videoRenderer = new GLVideoRenderer();
        CompositeGLRenderer compositeRenderer = new CompositeGLRenderer(videoRenderer);
        focusAssistOverlay = new FocusAssistOverlay();

        // Example: add your focus assist overlay
        compositeRenderer.addOverlay(focusAssistOverlay);

        setRenderer(compositeRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setZOrderOnTop(false);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    public void updateFrame(final ByteBuffer buffer, final int frameWidth, final int frameHeight,
            final FourCCType fourCCType) {
        if (videoRenderer == null || buffer == null) {
            return;
        }

        queueEvent(() -> videoRenderer.render(buffer, frameWidth, frameHeight, fourCCType));
        requestRender();
    }

    public Rect getVideoRect() {
        return videoRenderer.getVideoRect();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void setFocusAssistEnabled(boolean enabled) {
        if (focusAssistOverlay == null) {
            return;
        }

        queueEvent(() -> focusAssistOverlay.setEnabled(enabled));
        requestRender();
    }
}
