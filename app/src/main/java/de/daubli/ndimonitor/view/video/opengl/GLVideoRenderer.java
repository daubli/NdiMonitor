package de.daubli.ndimonitor.view.video.opengl;

import static de.daubli.ndimonitor.view.video.opengl.OpenGLVideoShader.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import de.daubli.ndimonitor.ndi.FourCCType;

public class GLVideoRenderer implements GLSurfaceView.Renderer {

    private final Object lock = new Object();

    private ByteBuffer pendingFrame;

    private int frameWidth;

    private int frameHeight;

    private FourCCType frameType;

    private int surfaceWidth;

    private int surfaceHeight;

    private boolean frameAvailable = false;

    // Programs
    private int programBgra;

    private int programUyvy;

    // Texture
    private int textureId;

    // Attribute locations
    private int posLocBgra;

    private int texLocBgra;

    private int posLocUyvy;

    private int texLocUyvy;

    // Uniform locations
    private int bgraTexSamplerLoc;

    private int uyvyTexSamplerLoc;

    private int uyvyFrameWidthLoc;

    private FloatBuffer vertexBuffer;

    private FloatBuffer texBuffer;

    private final float[] TEXCOORDS = { 0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f };

    private Rect videoRect = new Rect();

    public GLVideoRenderer() {
        vertexBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

        texBuffer = ByteBuffer.allocateDirect(TEXCOORDS.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texBuffer.put(TEXCOORDS).position(0);
    }

    /** Thread-safe frame submission */
    public void render(ByteBuffer buffer, int width, int height, FourCCType type) {
        synchronized (lock) {
            pendingFrame = buffer;
            frameWidth = width;
            frameHeight = height;
            frameType = type;
            frameAvailable = true;

            if (surfaceWidth > 0 && surfaceHeight > 0) {
                videoRect = scaleAndCenterVideoFrame(surfaceWidth, surfaceHeight, frameWidth, frameHeight);
            }
        }
    }

    public Rect getVideoRect() {
        synchronized (lock) {
            return new Rect(videoRect);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        programBgra = createProgram(VERTEX_SHADER, FRAGMENT_BGRA);
        programUyvy = createProgram(VERTEX_SHADER, FRAGMENT_UYVY);

        // BGRA program locations
        posLocBgra = GLES20.glGetAttribLocation(programBgra, "aPosition");
        texLocBgra = GLES20.glGetAttribLocation(programBgra, "aTexCoord");
        bgraTexSamplerLoc = GLES20.glGetUniformLocation(programBgra, "uTex");

        // UYVY program locations
        posLocUyvy = GLES20.glGetAttribLocation(programUyvy, "aPosition");
        texLocUyvy = GLES20.glGetAttribLocation(programUyvy, "aTexCoord");
        uyvyTexSamplerLoc = GLES20.glGetUniformLocation(programUyvy, "uTex");
        uyvyFrameWidthLoc = GLES20.glGetUniformLocation(programUyvy, "uFrameWidth");

        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);

        synchronized (lock) {
            if (frameWidth > 0 && frameHeight > 0) {
                scaleAndCenterVideoFrame(surfaceWidth, surfaceHeight, frameWidth, frameHeight);
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

        ByteBuffer frame;
        int w, h;
        FourCCType type;

        synchronized (lock) {
            if (!frameAvailable) {
                return;
            }
            frame = pendingFrame;
            w = frameWidth;
            h = frameHeight;
            type = frameType;
            frameAvailable = false;
        }

        uploadFrame(frame, type, w, h);
        draw(type, w, h);
    }

    private void uploadFrame(ByteBuffer frame, FourCCType type, int w, int h) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        if (type == FourCCType.BGRA) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, frame);
        } else {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w / 2, h, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, frame);
        }
    }

    private void draw(FourCCType type, int w, int h) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (type == FourCCType.BGRA) {
            GLES20.glUseProgram(programBgra);

            GLES20.glEnableVertexAttribArray(posLocBgra);
            GLES20.glEnableVertexAttribArray(texLocBgra);

            GLES20.glVertexAttribPointer(posLocBgra, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glVertexAttribPointer(texLocBgra, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

            GLES20.glUniform1i(bgraTexSamplerLoc, 0);
        } else {
            GLES20.glUseProgram(programUyvy);

            GLES20.glEnableVertexAttribArray(posLocUyvy);
            GLES20.glEnableVertexAttribArray(texLocUyvy);

            GLES20.glVertexAttribPointer(posLocUyvy, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glVertexAttribPointer(texLocUyvy, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

            GLES20.glUniform1i(uyvyTexSamplerLoc, 0);
            GLES20.glUniform1f(uyvyFrameWidthLoc, (float) w);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private Rect scaleAndCenterVideoFrame(int surfaceW, int surfaceH, int frameW, int frameH) {
        float viewAspect = (float) surfaceW / surfaceH;
        float frameAspect = (float) frameW / frameH;

        float scaleX = 1f;
        float scaleY = 1f;

        if (frameAspect > viewAspect) {
            scaleY = viewAspect / frameAspect;
        } else {
            scaleX = frameAspect / viewAspect;
        }

        float[] vertices = { -scaleX, -scaleY, scaleX, -scaleY, -scaleX, scaleY, scaleX, scaleY };

        vertexBuffer.clear();
        vertexBuffer.put(vertices).position(0);

        // return the actual video rect in surface coordinates, useful for overlays
        int videoW = (int) (surfaceW * scaleX);
        int videoH = (int) (surfaceH * scaleY);
        int left = (surfaceW - videoW) / 2;
        int top = (surfaceH - videoH) / 2;
        return new Rect(left, top, left + videoW, top + videoH);
    }

    private int createProgram(String vs, String fs) {
        int v = compile(GLES20.GL_VERTEX_SHADER, vs);
        int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        if (p == 0) {
            throw new RuntimeException("Could not create program");
        }
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(p);
            GLES20.glDeleteProgram(p);
            throw new RuntimeException("Could not link program: " + log);
        }
        return p;
    }

    private int compile(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Could not create shader " + type);
        }
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Could not compile shader " + type + ": " + log);
        }
        return shader;
    }
}
