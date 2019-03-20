package com.lewis.audiovideostudy;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lewis.audiovideostudy.activity.AudioRecordTrackActivity;
import com.lewis.audiovideostudy.activity.CameraPreviewActivity;
import com.lewis.audiovideostudy.activity.MediaCodecAudioActivity;
import com.lewis.audiovideostudy.activity.MediaExtractorMuxerActivity;
import com.lewis.audiovideostudy.activity.OpenGLCanvasTriangleActivity;
import com.lewis.audiovideostudy.activity.OpenGLShowImageActivity;
import com.lewis.audiovideostudy.activity.ShowPicture;

public class MainActivity extends AppCompatActivity {

    private final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //请求权限，要同意才行（偷懒）
        ActivityCompat.requestPermissions(this, permissions, 100);
    }

    public void showPicture(View view) {
        startActivity(new Intent(this, ShowPicture.class));
    }

    /**
     * 采集PCM数据并播放，并且生成wav文件
     */
    public void getPCMAndPlay(View view) {
        startActivity(new Intent(this, AudioRecordTrackActivity.class));
    }

    /**
     * Camera视频采集
     */
    public void cameraPreview(View view) {
        startActivity(new Intent(this, CameraPreviewActivity.class));
    }

    /**
     * MediaExtractor与MediaMuxer解析、封装Mp4文件
     */
    public void mediaExtractorMuxer(View view) {
        startActivity(new Intent(this, MediaExtractorMuxerActivity.class));
    }

    /**
     * OpenGL绘制三角形
     */
    public void openGLCanvasTriangle(View view) {
        startActivity(new Intent(this, OpenGLCanvasTriangleActivity.class));
    }

    /**
     * OpenGL显示一张图片
     */
    public void openGLDrawImage(View view) {
        startActivity(new Intent(this, OpenGLShowImageActivity.class));
    }

    /**
     * MediaCodec AAC硬编硬解
     */
    public void mediaCodecAAC(View view) {
        startActivity(new Intent(this, MediaCodecAudioActivity.class));
    }
}
