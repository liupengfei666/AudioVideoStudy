package com.lewis.audiovideostudy.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.lewis.audiovideostudy.view.PicGLSurfaceView;

/**
 * author: lewis
 * create by: 19-2-19 下午7:47
 * description: openGL 显示一张图片
 */
public class OpenGLShowImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PicGLSurfaceView glSurfaceView = new PicGLSurfaceView(this);
        setContentView(glSurfaceView);
    }
}
