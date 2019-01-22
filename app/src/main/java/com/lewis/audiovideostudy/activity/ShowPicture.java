package com.lewis.audiovideostudy.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.lewis.audiovideostudy.R;
import com.lewis.audiovideostudy.view.ShowPictureView;

public class ShowPicture extends AppCompatActivity implements SurfaceHolder.Callback {

    private ImageView mImageView;
    private ShowPictureView mShowPicture;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);
        mImageView = findViewById(R.id.image_view);
        mShowPicture = findViewById(R.id.custom_pic_view);
        mSurfaceView = findViewById(R.id.surfaceview);

        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
                SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
                Canvas canvas = surfaceHolder.lockCanvas();
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                canvas.drawBitmap(bitmap, new Matrix(), paint);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //在这儿销毁线程
    }

    /**
     * ImageView显示图片
     */
    public void showPicByImageView(View view) {
        mImageView.setImageResource(R.mipmap.dog);
    }

    /**
     * 点击切换图片
     */
    public void showPicByCustomView(View view) {
        mShowPicture.setBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.human));
    }
}
