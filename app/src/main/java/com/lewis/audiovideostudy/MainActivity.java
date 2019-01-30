package com.lewis.audiovideostudy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lewis.audiovideostudy.activity.AudioRecordTrackActivity;
import com.lewis.audiovideostudy.activity.CameraPreviewActivity;
import com.lewis.audiovideostudy.activity.ShowPicture;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}
