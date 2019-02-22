package com.lewis.audiovideostudy.view;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.lewis.audiovideostudy.renderer.ImageRenderer;

/**
 * author: lewis
 * create by: 19-2-20 下午2:48
 * description: 显示图片的GLSurfaceView
 */
public class PicGLSurfaceView extends GLSurfaceView {
    public PicGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        ImageRenderer renderer = new ImageRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
