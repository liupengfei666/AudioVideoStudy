package com.lewis.audiovideostudy.shapes;

import android.opengl.GLES20;

import com.lewis.audiovideostudy.renderer.LGLRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL;

/**
 * author: lewis
 * create by: 19-2-15 下午6:43
 * description: 定义一个方形
 */
public class Square {

    private FloatBuffer mVertexBuffer;
    private ShortBuffer mDrawListBuffer;

    //在这个数组中一个顶点有几个坐标
    static final int COORDS_PER_VERTEX = 3;
    static float mSuqareCoords[] = {
            -0.5f, 0.5f, 0.0f,   // top left
            -0.5f, -0.5f, 0.0f,  // bottom left
            0.5f, -0.5f, 0.0f,   // bottom right
            0.5f, 0.5f, 0.0f     // top right
    };

    // order to draw vertices
    private short mDrawOrder[] = {0, 1, 2, 0, 2, 3};

    private final String mVertexShaderCode =
            "attribute vec4 vPosition;" +
                    "uniform mat4 vMatrix;" +
                    "void main() {" +
                    "   gl_Position = vMatrix * vPosition;" +
                    "}";

    private final String mFragShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "   gl_FragColor = vColor;" +
                    "}";

    private int mProgram;

    private int mPositionHandle;
    private int mColorHandle;

    private float mViewMatrix[] = new float[16];
    private float mProjectMatrix[] = new float[16];
    private float mMVPMatrix[] = new float[16];

    //顶点之间的偏移量
    private final int mVertexStride = COORDS_PER_VERTEX * 4;//每个顶点4个字节

    private int mMatrixHandle;

    //设置颜色，依次为红绿蓝透明通道
    float mColor[] = {1.0f, 1.0f, 1.0f, 1.0f};

    public Square() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer buffer = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                mSuqareCoords.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = buffer.asFloatBuffer();
        mVertexBuffer.put(mSuqareCoords);
        mVertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer mListBuffer = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                mDrawOrder.length * 2);
        mListBuffer.order(ByteOrder.nativeOrder());
        mDrawListBuffer = mListBuffer.asShortBuffer();
        mDrawListBuffer.put(mDrawOrder);
        mDrawListBuffer.position(0);

        int vertexShader = LGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
        int fragmentShader = LGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, mFragShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix) {
        //将程序添加到OpenGL ES 2.0环境
        GLES20.glUseProgram(mProgram);
        //获取vMatrix
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mvpMatrix, 0);
        //获取顶点着色器的vPosition
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //准备三角形的坐标数据
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, mVertexStride, mVertexBuffer);
        //获取vColor
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        //设置绘制三角形的颜色
        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0);
        //索引法绘制正方形
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mDrawOrder.length, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
