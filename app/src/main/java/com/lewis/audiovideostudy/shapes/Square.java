package com.lewis.audiovideostudy.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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
    }
}
