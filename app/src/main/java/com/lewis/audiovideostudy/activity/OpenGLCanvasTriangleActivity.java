package com.lewis.audiovideostudy.activity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.lewis.audiovideostudy.view.LGLSurfaceView;

/**
 * openGL canvas triangle
 */
public class OpenGLCanvasTriangleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it as the ContentView for this Activity.
        GLSurfaceView mGLSurfaceView = new LGLSurfaceView(this);
        setContentView(mGLSurfaceView);
    }
}
