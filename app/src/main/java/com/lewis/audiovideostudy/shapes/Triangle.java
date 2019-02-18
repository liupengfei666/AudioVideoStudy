package com.lewis.audiovideostudy.shapes;

import android.opengl.GLES20;

import com.lewis.audiovideostudy.renderer.LGLRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * author: lewis
 * create by: 19-2-15 下午6:07
 * description: define triangle
 * <p>
 * 1.您需要至少一个顶点着色器来绘制形状，并使用一个片段着色器来为该形状着色。
 * 必须编译这些着色器，然后将其添加到OpenGL ES程序中，然后使用该程序绘制形状。
 * 2.使用OpenGL ES绘制形状需要您指定几个参数来告诉渲染管道您想要绘制什么以及如何绘制它。
 * 由于绘图选项可能因形状而异，因此最好让形状类包含自己的绘图逻辑。
 */
public class Triangle {

    private FloatBuffer mFloatBuffer;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    // 三角形顶点坐标，逆时针方向
    static float mTriangleCoords[] = {
            0.0f, 0.622008459f, 0.0f,   // top
            -0.5f, -0.311004243f, 0.0f, // bottom left
            0.5f, -0.311004243f, 0.0f   // bottom right
    };

    // Set color with red, green, blue and alpha (opacity) values
    float mColor[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    private final String mVertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "   gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    // Use to access and set the view transformation
    private int mMVPMatrixHandle;

    private final String mFragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "   gl_FragColor = vColor;" +
                    "}";

    private final int mProgram;

    private int mPositionHandle;
    private int mColorHandle;

    private final int mVertexCount = mTriangleCoords.length / COORDS_PER_VERTEX;
    private final int mVertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    public Triangle() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer buffer = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                mTriangleCoords.length * 4);
        // use the device hardware's native byte order
        buffer.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        mFloatBuffer = buffer.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        mFloatBuffer.put(mTriangleCoords);
        // set the buffer to read the first coordinate
        mFloatBuffer.position(0);

        int vertexShader = LGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
        int fragmentShader = LGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode);

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);
    }

    /**
     * 绘制三角形
     */
    public void draw(float[] mvpMatrix) { // pass in the calculated transformation matrix
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, mVertexStride, mFloatBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
