package com.lewis.audiovideostudy.activity;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.lewis.audiovideostudy.R;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * MediaExtractor与MediaMuxer解析、封装Mp4文件
 */
public class MediaExtractorMuxerActivity extends AppCompatActivity {

    private final String inputPath = "/storage/emulated/0/dy_xialu2.mp4";
    private final String outVideoPath = "/storage/emulated/0/output_video.mp4";
    private final String outAudioPath = "/storage/emulated/0/output_audio";

    private TextView mExtractorStatus, mMuxerStatus;

    private MediaExtractor mExtractor;
    private MediaMuxer mMuxer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_extractor_muxer);
        mExtractorStatus = findViewById(R.id.extractor_status);
        mMuxerStatus = findViewById(R.id.muxer_status);
    }

    /**
     * 分离视频
     */
    public void extractorVideo(View view) {
        mExtractorStatus.setText("抽取视频开始");
        extractorVideo();
        mExtractorStatus.setText("抽取视频完成");
    }

    /**
     * 分离音频
     */
    public void extractorAudio(View view) {
        mExtractorStatus.setText("抽取音频开始");
        extractorAudio();
        mExtractorStatus.setText("抽取音频完成");
    }

    /**
     * 抽取视频
     */
    private void extractorVideo() {
        int videoTrackIndex = -1;
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(inputPath);
            //获取通道数
            int tractCount = mExtractor.getTrackCount();
            for (int i = 0; i < tractCount; i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }
            //设置视频通道
            mExtractor.selectTrack(videoTrackIndex);
            //初始化MediaMuxer
            mMuxer = new MediaMuxer(outVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //获取MediaFormat
            MediaFormat videoFormat = mExtractor.getTrackFormat(videoTrackIndex);
            //添加通道给muxer
            mMuxer.addTrack(videoFormat);

            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            mMuxer.start();
            //获取相邻视频帧的时间间隔
            long videoSampleTime = getSampleTime(mExtractor, byteBuffer, videoTrackIndex);

            MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();

            while (true) {
                int readSize = mExtractor.readSampleData(byteBuffer, 0);
                if (readSize < 0) {
                    break;
                }
                mExtractor.advance();
                videoInfo.offset = 0;
                videoInfo.size = readSize;
                videoInfo.flags = mExtractor.getSampleFlags();
                videoInfo.presentationTimeUs += videoSampleTime;
                mMuxer.writeSampleData(videoTrackIndex, byteBuffer, videoInfo);
            }
            mMuxer.stop();
            mMuxer.release();
            mExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 抽取音频
     */
    private void extractorAudio() {
        int audioTrackIndex = -1;
        mExtractor = new MediaExtractor();
        try {
            //设置输入源
            mExtractor.setDataSource(inputPath);
            //获取通道数
            int trackCount = mExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            //设置音频通道
            mExtractor.selectTrack(audioTrackIndex);
            MediaFormat audioFormat = mExtractor.getTrackFormat(audioTrackIndex);
            mMuxer = new MediaMuxer(outAudioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //添加通道
            int writeAudio = mMuxer.addTrack(audioFormat);
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            mMuxer.start();

            long audioSampleTime = getSampleTime(mExtractor, byteBuffer, audioTrackIndex);
            MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

            while (true) {
                int readSize = mExtractor.readSampleData(byteBuffer, 0);
                if (readSize < 0) {
                    break;
                }
                mExtractor.advance();
                audioInfo.offset = 0;
                audioInfo.size = readSize;
                audioInfo.flags = mExtractor.getSampleFlags();
                audioInfo.presentationTimeUs += audioSampleTime;
                mMuxer.writeSampleData(writeAudio, byteBuffer, audioInfo);
            }
            mMuxer.stop();
            mMuxer.release();
            mExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取每一帧的时间差
    private long getSampleTime(MediaExtractor extractor, ByteBuffer byteBuffer, int trackIndex) {
        extractor.readSampleData(byteBuffer, 0);
        //跳过I帧，要P帧（视频是由个别I帧和很多P帧组成）h264编码中有IBP帧 I为关键帧。
        if (extractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
            extractor.advance();//跳过
        }
        extractor.readSampleData(byteBuffer, 0);
        //得到第一个P帧的时间戳
        long firstPTS = extractor.getSampleTime();
        //下一帧
        extractor.advance();
        extractor.readSampleData(byteBuffer, 0);
        long secondPTS = extractor.getSampleTime();
        long sampleTime = Math.abs(secondPTS - firstPTS);
        // 重新切换此信道，不然上面跳过了3帧,造成前面的帧数模糊
        extractor.unselectTrack(trackIndex);
        extractor.selectTrack(trackIndex);
        return sampleTime;
    }

    /**
     * 合成视频音频
     */
    public void muxerVideoAudio(View view) {
        mMuxerStatus.setText("开始合成");
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(outVideoPath);
            audioExtractor.setDataSource(outAudioPath);
            //获取要追踪的通道
            int videoTrackIndex = getTrack(videoExtractor, "video/");
            int audioTrackIndex = getTrack(audioExtractor, "audio/");
            //选中通道
            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

            MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
            //创建缓冲区
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            mMuxer = new MediaMuxer("/storage/emulated/0/merge.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //添加通道
            int writeVideoTrack = mMuxer.addTrack(videoExtractor.getTrackFormat(videoTrackIndex));
            int writeAudioTrack = mMuxer.addTrack(audioExtractor.getTrackFormat(audioTrackIndex));
            mMuxer.start();

            long videoSampleTime = getSampleTime(videoExtractor, byteBuffer, videoTrackIndex);

            //顺序写入数据
            while (true) {
                int videoSize = videoExtractor.readSampleData(byteBuffer, 0);
                if (videoSize < 0) {
                    break;
                }
                videoInfo.offset = 0;
                videoInfo.size = videoSize;
                videoInfo.flags = videoExtractor.getSampleFlags();
                videoInfo.presentationTimeUs += videoSampleTime;
                mMuxer.writeSampleData(writeVideoTrack, byteBuffer, videoInfo);
                videoExtractor.advance();
            }

            long audioSampleTime = getSampleTime(audioExtractor, byteBuffer, audioTrackIndex);

            while (true) {
                int audioSize = audioExtractor.readSampleData(byteBuffer, 0);
                if (audioSize < 0) {
                    break;
                }
                audioInfo.offset = 0;
                audioInfo.size = audioSize;
                audioInfo.flags = audioExtractor.getSampleFlags();
                audioInfo.presentationTimeUs += audioSampleTime;
                mMuxer.writeSampleData(writeAudioTrack, byteBuffer, audioInfo);
                audioExtractor.advance();
            }
            SystemClock.sleep(500);
            mMuxer.stop();
            mMuxer.release();
            videoExtractor.release();
            audioExtractor.release();
            mMuxerStatus.setText("合成完毕");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取要追踪的信道
     * @param extractor {@link MediaExtractor}
     * @return track
     */
    private int getTrack(MediaExtractor extractor, String mimeType) {
        int trackCount = extractor.getTrackCount();
        int trackIndex = -1;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }
}
