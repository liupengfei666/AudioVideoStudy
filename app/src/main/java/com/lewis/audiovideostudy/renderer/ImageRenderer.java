package com.lewis.audiovideostudy.renderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.lewis.audiovideostudy.App;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * author: lewis
 * create by: 19-2-20 下午2:46
 * description: 显示图片的渲染器
 */
public class ImageRenderer implements GLSurfaceView.Renderer {

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mIndexBuffer;

    //纹理坐标
    private static final float sPicCoords[] = {
            0, 0,
            0, 1,
            1, 0,
            1, 1
    };

    //顶点坐标
    private static final float sPicVertex[] = {
            -1f, 1f,//左上角
            -1f, -1f,//左下角
            1f, 1f,//右上角
            1f, -1f//右下角
    };

    //顶点着色器
    private final String mVertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vCoordinate;" +
                    "uniform mat4 vMatrix;" +
                    "varying vec2 aCoordinate;" +
                    "void main() {" +
                    "   gl_Position = vMatrix * vPosition;" +
                    "   aCoordinate = vCoordinate;" +
                    "}";

    //片段着色器
    private final String mFragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D vTexture;" +
                    "varying vec2 aCoordinate;" +
                    "void main() {" +
                    "   gl_FragColor = texture2D(vTexture, aCoordinate);" +
                    "}";

    private int mPositionHandle;
    private int mTextureHandle;
    private int mCoordinateHandle;
    private int mMatrixHandle;

    private float mMVPMatrix[] = new float[16];
    private float mProjectionMatrix[] = new float[16];
    private float mViewMatrix[] = new float[16];

    private int mProgram;

    private Bitmap mBitmap;
    private float mBitmapRatio;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);//启用纹理
        try {
            mBitmap = BitmapFactory.decodeStream(App.instance().getResources().getAssets().open("human.jpeg"));
            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();
            mBitmapRatio = (float) width / height;
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(sPicVertex.length * 4);
        vertexBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertexBuffer.asFloatBuffer();
        mVertexBuffer.put(sPicVertex);
        mVertexBuffer.position(0);
        ByteBuffer coordinateBuffer = ByteBuffer.allocateDirect(sPicCoords.length * 4);
        coordinateBuffer.order(ByteOrder.nativeOrder());
        mIndexBuffer = coordinateBuffer.asFloatBuffer();
        mIndexBuffer.put(sPicCoords);
        mIndexBuffer.position(0);


        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode);

        //创建执行程序
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "vTexture");
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        if (width > height) {
            if (mBitmapRatio > ratio) {
                Matrix.orthoM(mProjectionMatrix, 0, -ratio * mBitmapRatio, ratio * mBitmapRatio,
                        -1, 1, 3, 7);
            } else {
                Matrix.orthoM(mProjectionMatrix, 0, -ratio / mBitmapRatio, ratio / mBitmapRatio,
                        -1, 1, 3, 7);
            }
        } else {
            if (mBitmapRatio > ratio) {
                Matrix.orthoM(mProjectionMatrix, 0, -1, 1,
                        -1 / ratio * mBitmapRatio, 1 / ratio * mBitmapRatio, 3, 7);
            } else {
                Matrix.orthoM(mProjectionMatrix, 0, -1, 1,
                        -mBitmapRatio / ratio, mBitmapRatio / ratio, 3, 7);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7, 0, 0, 0, 0, 1, 0);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mCoordinateHandle);
        GLES20.glUniform1i(mTextureHandle, 0);
        int textureId = createTexture();
        //传入顶点坐标
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        //传入纹理坐标
        GLES20.glVertexAttribPointer(mCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, mIndexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private int createTexture() {
        int[] texture = new int[1];
        if (mBitmap != null && !mBitmap.isRecycled()) {
            //生成纹理
            GLES20.glGenTextures(1, texture, 0);
            //绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            return texture[0];
        }
        return 0;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
