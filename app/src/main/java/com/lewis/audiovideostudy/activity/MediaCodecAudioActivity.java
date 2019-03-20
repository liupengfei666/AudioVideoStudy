package com.lewis.audiovideostudy.activity;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.lewis.audiovideostudy.R;
import com.lewis.audiovideostudy.util.AudioCodecUtil;

/**
 * author: lewis
 * create by: 19-3-20 上午10:17
 * description: MediaCodec AAC硬编硬解
 */
public class MediaCodecAudioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediacodec_audio);
    }

    public void mediacodecAudio(View view) {
        String srcPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp3";
        String dstPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/codec.aac";
        final AudioCodecUtil mAudioCodecUtil = AudioCodecUtil.get();
        mAudioCodecUtil.setEncodeType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mAudioCodecUtil.setIOPath(srcPath, dstPath);
        mAudioCodecUtil.prepare();
        mAudioCodecUtil.startAsync();
        mAudioCodecUtil.setOnCompleteListener(new AudioCodecUtil.OnCompleteListener() {
            @Override
            public void onComplete() {
                Toast.makeText(MediaCodecAudioActivity.this, "完事", Toast.LENGTH_SHORT).show();
                mAudioCodecUtil.release();
            }
        });
    }
}
