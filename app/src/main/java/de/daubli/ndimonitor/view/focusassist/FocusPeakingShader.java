package de.daubli.ndimonitor.view.focusassist;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FocusPeakingShader {

    private final int program;
    private int textureId = -1;

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;

    private static final float[] VERTICES = {
            -1f, 1f,   // Top-left
            -1f, -1f,  // Bottom-left
            1f, 1f,   // Top-right
            1f, -1f   // Bottom-right
    };

    private static final float[] TEX_COORDS = {
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
    };

    public FocusPeakingShader() {
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS).position(0);

        String vertexShaderCode =
                "attribute vec4 aPosition;" +
                        "attribute vec2 aTexCoord;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_Position = aPosition;" +
                        "  vTexCoord = aTexCoord;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "varying vec2 vTexCoord;" +
                        "uniform float texelSize;" +
                        "uniform float threshold;" +
                        "void main() {" +
                        "    float weightSum = 0.0;" +
                        "    float blurSum = 0.0;" +

                        "    float kernel[25];" +
                        "    kernel[ 0] = 1.0;  kernel[ 1] = 4.0;  kernel[ 2] = 6.0;  kernel[ 3] = 4.0;  kernel[ 4] = 1.0;" +
                        "    kernel[ 5] = 4.0;  kernel[ 6] = 16.0; kernel[ 7] = 24.0; kernel[ 8] = 16.0; kernel[ 9] = 4.0;" +
                        "    kernel[10] = 6.0;  kernel[11] = 24.0; kernel[12] = 36.0; kernel[13] = 24.0; kernel[14] = 6.0;" +
                        "    kernel[15] = 4.0;  kernel[16] = 16.0; kernel[17] = 24.0; kernel[18] = 16.0; kernel[19] = 4.0;" +
                        "    kernel[20] = 1.0;  kernel[21] = 4.0;  kernel[22] = 6.0;  kernel[23] = 4.0;  kernel[24] = 1.0;" +

                        "    int index = 0;" +
                        "    for (int y = -2; y <= 2; y++) {" +
                        "        for (int x = -2; x <= 2; x++) {" +
                        "            vec2 offset = vec2(float(x), float(y)) * texelSize;" +
                        "            float intensity = texture2D(uTexture, vTexCoord + offset).r;" +
                        "            blurSum += intensity * kernel[index];" +
                        "            weightSum += kernel[index];" +
                        "            index++;" +
                        "        }" +
                        "    }" +

                        "    float blurred = blurSum / weightSum;" +

                        "    float gx = 0.0;" +
                        "    float gy = 0.0;" +

                        "    float kernelX[9];" +
                        "    kernelX[0] = -1.0; kernelX[1] = 0.0; kernelX[2] = 1.0;" +
                        "    kernelX[3] = -2.0; kernelX[4] = 0.0; kernelX[5] = 2.0;" +
                        "    kernelX[6] = -1.0; kernelX[7] = 0.0; kernelX[8] = 1.0;" +

                        "    float kernelY[9];" +
                        "    kernelY[0] = -1.0; kernelY[1] = -2.0; kernelY[2] = -1.0;" +
                        "    kernelY[3] =  0.0; kernelY[4] =  0.0; kernelY[5] =  0.0;" +
                        "    kernelY[6] =  1.0; kernelY[7] =  2.0; kernelY[8] =  1.0;" +

                        "    int i = 0;" +
                        "    for (int y = -1; y <= 1; y++) {" +
                        "        for (int x = -1; x <= 1; x++) {" +
                        "            vec2 offset = vec2(float(x), float(y)) * texelSize;" +
                        "            float intensity = texture2D(uTexture, vTexCoord + offset).r;" +
                        "            gx += intensity * kernelX[i];" +
                        "            gy += intensity * kernelY[i];" +
                        "            i++;" +
                        "        }" +
                        "    }" +

                        "    float edge = length(vec2(gx, gy));" +
                        "    float alpha = edge >= threshold ? edge * 0.7 : 0.0;" +
                        "    gl_FragColor = vec4(1.0, 0.0, 0.0, alpha);" +
                        "}";

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragShader);
        GLES20.glLinkProgram(program);
    }

    private int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public void draw(Bitmap bitmap) {
        if (textureId == -1) {
            textureId = createTexture(bitmap);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
        }

        GLES20.glUseProgram(program);

        int texelSizeHandle = GLES20.glGetUniformLocation(program, "texelSize");
        float texelSize = 1.0f / (float) bitmap.getWidth();
        GLES20.glUniform1f(texelSizeHandle, texelSize);

        int thresholdHandle = GLES20.glGetUniformLocation(program, "threshold");
        if (thresholdHandle != -1) {
            GLES20.glUniform1f(thresholdHandle, 0.7f); // 70% threshold
        }

        int posHandle = GLES20.glGetAttribLocation(program, "aPosition");
        int texHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        int samplerLoc = GLES20.glGetUniformLocation(program, "uTexture");

        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(texHandle);
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(samplerLoc, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(texHandle);
    }

    private int createTexture(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int id = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        return id;
    }
}
