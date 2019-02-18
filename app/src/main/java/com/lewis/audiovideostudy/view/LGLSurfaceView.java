package com.lewis.audiovideostudy.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.lewis.audiovideostudy.renderer.LGLRenderer;

public class LGLSurfaceView extends GLSurfaceView {

    private LGLRenderer mLGLRenderer;

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    public LGLSurfaceView(Context context) {
        super(context);
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mLGLRenderer = new LGLRenderer();
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mLGLRenderer);
        // Render the view only when there is a change in the drawing data.
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }

                mLGLRenderer.setAngle(mLGLRenderer.getAngle() + (dx + dy) * TOUCH_SCALE_FACTOR);
                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;

        return true;
    }
}
