package com.lewis.audiovideostudy.activity;

import android.Manifest;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lewis.audiovideostudy.R;
import com.lewis.audiovideostudy.util.PcmToWavUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 录制PCM数据并播放，实现PCM数据转WAV
 */
public class AudioRecordTrackActivity extends AppCompatActivity {

    /**
     * 采样率选择有很多8000,11025，16000,22050,44100,48000等
     * 这儿选择44.1kHz可以保证设备可用，其他的需要看设备支不支持
     */
    private static final int SAMPLE_RATE = 44100;

    /**
     * 声道数CHANNEL_IN_MONO and CHANNEL_IN_STEREO，其中CHANNEL_IN_MONO为单声道，保证了设备支持，
     * CHANNEL_IN_STEREO为立体声，不保证设备支持
     */
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 音频格式ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
     * 选择ENCODING_PCM_16BIT也是保证设备可用
     */
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private TextView mRecordStatus;
    private TextView mPlayStatus;

    private boolean isRecording;//是否正在录制
    private boolean isPlaying;//是否正在播放

    private AudioRecordThread mAudioRecordThread; //录制线程
    private AudioTrackPlayThread mAudioTrackPlayThread;//播放线程

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_track);
        mRecordStatus = findViewById(R.id.record_status);
        mPlayStatus = findViewById(R.id.play_status);
        //请求权限
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE/*读写权限*/,
                        Manifest.permission.RECORD_AUDIO/*麦克风权限*/},
                100);

    }

    /**
     * 开始录制
     * 因为read函数是阻塞的，最好在独立线程采集录音数据
     */
    public void startRecord(View view) {
        if (isPlaying) {
            Toast.makeText(this, "正在播放", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRecording) {
            Toast.makeText(this, "正在录制", Toast.LENGTH_SHORT).show();
            return;
        }
        //1
//        RecordThread recordThread = new RecordThread();
//        recordThread.execute();
        //2
        if (null != mAudioRecordThread) {
            //若线程不为空,则中断线程
            mAudioRecordThread.interrupt();
            mAudioRecordThread = null;
        }
        mAudioRecordThread = new AudioRecordThread();
        mAudioRecordThread.start();
        mRecordStatus.setText(getString(R.string.recording));
    }

    public void stopRecord(View view) {
        //1
//        if (isRecording) {
//            isRecording = false;
//            mRecordStatus.setText(getString(R.string.stop_recording));
//        }
        //2
        if (null != mAudioRecordThread) {
            mAudioRecordThread.interrupt();
            mAudioRecordThread = null;
        }
        isRecording = false;
    }

    public void startPlay(View view) {
        if (isPlaying) {
            Toast.makeText(this, "正在播放", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRecording) {
            Toast.makeText(this, "正在录制", Toast.LENGTH_SHORT).show();
            return;
        }
        //1
//        PlayThread playThread = new PlayThread();
//        playThread.execute();
        //2
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "audio.pcm");
        mAudioTrackPlayThread = new AudioTrackPlayThread(file);
        mAudioTrackPlayThread.start();
        mPlayStatus.setText(getString(R.string.playing));
    }

    public void stopPlay(View view) {
        //1
//        if (isPlaying) {
//            isPlaying = false;
//            mPlayStatus.setText(getString(R.string.stop_play));
//        }
        if (null != mAudioTrackPlayThread) {
            mAudioTrackPlayThread.interrupt();
            mAudioTrackPlayThread = null;
        }
        isPlaying = false;
    }

    /**
     * pcm转wav
     */
    public void pcm2wav(View view) {
        String input = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "record.pcm";
        String output = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "record.wav";
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        pcmToWavUtil.pcmToWav(input, output);
    }

    /**
     * 采集PCM数据线程
     */
    private class RecordThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            //返回成功创建AudioRecord对象所需要的最小缓冲区大小
            //注意：这个大小并不保证在负荷下的流畅录制，应根据预期的频率来选择更高的值，AudioRecord实例在推送新数据时使用此值。
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            //创建AudioRecord
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC/*用麦克风采集*/, SAMPLE_RATE,
                    CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
            //API提示创建AudioRecord后立马调用getState获取其可用状态
            int state = audioRecord.getState();
            Log.w("record state", "state =" + state);
            //这儿为了方便看到采集的pcm数据直接写入到sd卡中
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "record.pcm");
            if (file.exists()) {
                boolean isDelete = file.delete();
                Log.w("file delete", "isDelete = " + isDelete);
            }
            try {
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                DataOutputStream outputStream = new DataOutputStream(bufferedOutputStream);
                short[] buffer = new short[minBufferSize];
                audioRecord.startRecording();
                isRecording = true;
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, minBufferSize);
                    for (int i = 0; i < read; i++) {
                        outputStream.writeShort(buffer[i]);
                    }
                }
                isRecording = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecordStatus.setText(getString(R.string.stop_recording));
                    }
                });
                audioRecord.stop();
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 播放PCM数据
     */
    private class PlayThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "record.wav");
            if (!file.exists()) {
                Log.w("file state", "file not found");
                return null;
            }
            int channelMask = AudioFormat.CHANNEL_OUT_MONO;
            //创建AudioTrack
            AudioTrack mTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AUDIO_FORMAT)
                            .setChannelMask(channelMask)
                            .build(), (int) file.length(), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            int state = mTrack.getState();
            Log.w("state", "state=" + state);
            try {
                DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                short[] buffer = new short[(int) (file.length() / 2)];
                mTrack.play();
                isPlaying = true;
                int read = 0;
                while (isPlaying && inputStream.available() > 0) {
                    buffer[read] = inputStream.readShort();
                    read++;
                }
                mTrack.write(buffer, 0, buffer.length);
                Log.w("play state", "complete");
                isPlaying = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayStatus.setText(getString(R.string.stop_play));
                    }
                });
                mTrack.stop();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 录制线程
     */
    private class AudioRecordThread extends Thread {
        AudioRecord mAudioRecord;
        int minBufferSize = 10240;

        AudioRecordThread() {
            /*
             * 获取音频缓冲最小的大小
             */
            minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            /*
             * 参数1：音频源
             * 参数2：采样率 主流是44100
             * 参数3：声道设置 MONO单声道 STEREO立体声
             * 参数4：编码格式和采样大小 编码格式为PCM,主流大小为16BIT
             * 参数5：采集数据需要的缓冲区大小
             */
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
        }

        @Override
        public void run() {
            //将状态置为录制

            Log.d("audio", "录制开始");
            try {
                byte[] bytes = new byte[minBufferSize];

                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "audio.pcm");

                FileOutputStream fos = new FileOutputStream(file);

                //开始录制
                mAudioRecord.startRecording();
                isRecording = true;

                while (isRecording && !isInterrupted()) {
                    int read = mAudioRecord.read(bytes, 0, bytes.length);
                    //若读取数据没有出现错误，将数据写入文件
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        fos.write(bytes, 0, read);
                        fos.flush();
                    }
                }
                mAudioRecord.stop();//停止录制
                fos.close();//关流

            } catch (Exception e) {
                e.printStackTrace();

            }
            isRecording = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecordStatus.setText(getString(R.string.stop_recording));
                }
            });
            Log.d("audio", "录制结束");
        }

    }

    /**
     * 播放音频线程
     */
    private class AudioTrackPlayThread extends Thread {
        AudioTrack mAudioTrack;
        int minBufferSize = 10240;
        File autoFile; //要播放的文件

        //        @RequiresApi(api = Build.VERSION_CODES.M)
        AudioTrackPlayThread(File file) {
            setPriority(MAX_PRIORITY);
            autoFile = file;
            //播放缓冲的最小大小
            minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT);
            // 创建用于播放的 AudioTrack
            mAudioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            //我发现这个参数设置会影响播放，如果不行就多试几个，我在这儿花了好多时间
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(), minBufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

        }

        @Override
        public void run() {

            Log.d("audio", "播放开始");
            try {
                FileInputStream fis = new FileInputStream(autoFile);
                mAudioTrack.play();
                isPlaying = true;
                byte[] bytes = new byte[minBufferSize];

                while (isPlaying) {
                    int read = fis.read(bytes);
                    //若读取有错则跳过
                    if (AudioTrack.ERROR_INVALID_OPERATION == read
                            || AudioTrack.ERROR_BAD_VALUE == read) {
                        continue;
                    }

                    if (read != 0 && read != -1) {
                        mAudioTrack.write(bytes, 0, minBufferSize);
                    }
                    if (read == -1) {
                        isPlaying = false;
                    }
                }
                mAudioTrack.stop();
                mAudioTrack.release();//释放资源
                fis.close();//关流

            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPlayStatus.setText(getString(R.string.stop_play));
                }
            });
            Log.d("audio", "播放停止");
        }
    }
}
