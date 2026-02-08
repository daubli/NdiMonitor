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

public class OpenGLVideoView extends GLSurfaceView {

    private GLVideoRenderer renderer;

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
        renderer = new GLVideoRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setZOrderOnTop(false);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    public void updateFrame(final ByteBuffer buffer, final int frameWidth, final int frameHeight,
            final FourCCType fourCCType) {
        if (renderer == null || buffer == null) {
            return;
        }

        queueEvent(() -> {
            renderer.render(buffer, frameWidth, frameHeight, fourCCType);
        });
        requestRender();
    }

    public Rect getVideoRect() {
        return renderer.getVideoRect();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
