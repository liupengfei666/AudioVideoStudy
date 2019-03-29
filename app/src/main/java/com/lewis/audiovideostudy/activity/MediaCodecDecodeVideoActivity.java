package com.lewis.audiovideostudy.activity;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.lewis.audiovideostudy.R;
import com.lewis.audiovideostudy.decode.DecodeAsync;
import com.lewis.audiovideostudy.decode.DecodeThread;

/**
 * author: lewis
 * create by: 19-3-22 下午1:57
 * description: MediaCodec视频H.264硬解
 */
public class MediaCodecDecodeVideoActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceHolder mSurfaceHolder;
    private String mH264_Path;

    private DecodeThread mDecodeThread; //解码线程

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediacodec_decode_video);
        SurfaceView surfaceView = findViewById(R.id.surfaceview);
        mH264_Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dy_xialu2.mp4";
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    /**
     * 释放资源
     */
    public void releaseDecodeVideo(View view) {
        release();
    }

    /**
     * 开始解码
     */
    private void startDecode() {
        if (mDecodeThread == null) {
            mDecodeThread = new DecodeThread(mH264_Path, mSurfaceHolder.getSurface());
            mDecodeThread.start();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //方式一
//        startDecode();
        //方式二
        DecodeAsync.get().startDecoder(mH264_Path, mSurfaceHolder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private void release() {
        if (mDecodeThread != null) {
            mDecodeThread.release();
        }
    }

    @Override
    public void finish() {
        super.finish();
        //方式一
        release();
        //方式二
        DecodeAsync.get().release();
    }

}
