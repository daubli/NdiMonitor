package de.daubli.ndimonitor.view.video.opengl;

public class OpenGLVideoShader {

    //@formatter:off
    public static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = aPosition;\n" +
                    "    vTexCoord = aTexCoord;\n" +
                    "}";

    public static final String FRAGMENT_BGRA =
            "precision mediump float;\n" +
                    "uniform sampler2D uTex;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    vec4 c = texture2D(uTex, vTexCoord);\n" +
                    "    gl_FragColor = vec4(c.b, c.g, c.r, c.a);\n" +
                    "}";

    public static final String FRAGMENT_UYVY =
            "precision mediump float;\n" +
                    "uniform sampler2D uTex;\n" +
                    "uniform float uFrameWidth;\n" +   // full frame width in pixels
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    float x = vTexCoord.x * uFrameWidth;\n" +      // pixel x in [0, frameWidth)
                    "    float macroX = floor(x / 2.0);\n" +           // macro-pixel index
                    "    float isOdd = mod(floor(x), 2.0);\n" +        // 0 for even, 1 for odd
                    "    float texWidth = uFrameWidth / 2.0;\n" +
                    "    float texX = (macroX + 0.5) / texWidth;\n" +  // center of texel
                    "    vec4 t = texture2D(uTex, vec2(texX, vTexCoord.y));\n" +
                    "    float U = t.r - 0.5;\n" +
                    "    float Y0 = t.g;\n" +
                    "    float V = t.b - 0.5;\n" +
                    "    float Y1 = t.a;\n" +
                    "    float Y = (isOdd < 0.5) ? Y0 : Y1;\n" +
                    "    float r = Y + 1.402 * V;\n" +
                    "    float g = Y - 0.344136 * U - 0.714136 * V;\n" +
                    "    float b = Y + 1.772 * U;\n" +
                    "    gl_FragColor = vec4(r, g, b, 1.0);\n" +
                    "}";
    //@formatter:on
}
