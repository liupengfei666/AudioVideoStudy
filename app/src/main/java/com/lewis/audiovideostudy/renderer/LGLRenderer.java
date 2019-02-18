package com.lewis.audiovideostudy.renderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.lewis.audiovideostudy.shapes.Square;
import com.lewis.audiovideostudy.shapes.Triangle;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * author: lewis
 * create by: 19-2-15 下午5:44
 * description: 自己实现的渲染器
 * 注意：在绘制任何图形之前，必须初始化并加载计划绘制的形状。
 * 除非您在程序中使用的形状的结构（原始坐标）在执行过程中发生更改，
 * 否则应在渲染器的onSurfaceCreated（）方法中初始化它们以获得内存和处理效率
 */
public class LGLRenderer implements GLSurfaceView.Renderer {

    private Triangle mTriangle;
    private Square mSquare;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float mMVPMatrix[] = new float[16];
    private final float mProjectionMatrix[] = new float[16];
    private final float mViewMatrix[] = new float[16];
    private final float mRotationMatrix[] = new float[16];

    public volatile float mAngle;

    public float getAngle() {
        return mAngle;
    }

    public void setAngle(float angle) {
        mAngle = angle;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //初始化三角形
        mTriangle = new Triangle();
        //初始化正方形
        mSquare = new Square();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float scratch[] = new float[16];

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0, 0, 0, 0, 1.0f, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        // Create a rotation transformation for the triangle
//        long time = SystemClock.uptimeMillis() % 4000L;
//        float angle = 0.090f * (int) time;
        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        //绘制三角形
        mTriangle.draw(scratch);
    }

    /**
     * 为了绘制形状，您必须编译着色器代码，将它们添加到OpenGL ES程序对象，然后关联该程序。
     * 在绘制对象的构造函数中执行此操作，因此只执行一次
     */
    public static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
