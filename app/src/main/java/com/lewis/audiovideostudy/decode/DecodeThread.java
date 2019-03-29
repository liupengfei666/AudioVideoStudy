package com.lewis.audiovideostudy.decode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: lewis
 * create by: 19-3-27 下午2:38
 * description: 视频解码线程
 */
public class DecodeThread extends Thread {

    private MediaCodec mDecodeMediaCodec; //解码器
    private boolean isDecodeFinish;       //解码完成
    private MediaExtractor mExtractor;
    private String mVideoPath;            //解码视频路径
    private Surface mSurface;             //显示

    public DecodeThread(String videoPath, Surface surface) {
        mVideoPath = videoPath;
        mSurface = surface;
    }

    @Override
    public void run() {
        super.run();
        initManager();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long startTime = System.currentTimeMillis();
        while (!isDecodeFinish) {
            int inputIndex = mDecodeMediaCodec.dequeueInputBuffer(10000);
            if (inputIndex >= 0) {
                ByteBuffer buffer = mDecodeMediaCodec.getInputBuffer(inputIndex);
                int sampleSize = mExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    mDecodeMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isDecodeFinish = true;
                } else {
                    mDecodeMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
                    mExtractor.advance();
                }
            }
            int outputIndex = mDecodeMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startTime) {
                try {
                    sleep(10);//这儿如果不休眠，可能会播放很快
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (outputIndex >= 0) {
                mDecodeMediaCodec.releaseOutputBuffer(outputIndex, true);
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("decode", "BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }
        mDecodeMediaCodec.stop();
        mDecodeMediaCodec.release();
        mExtractor.release();
    }

    private void initManager() {
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mVideoPath); //设置视频源
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mExtractor.selectTrack(i);

                    mDecodeMediaCodec = MediaCodec.createDecoderByType(mime);
                    mDecodeMediaCodec.configure(format, mSurface, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mDecodeMediaCodec == null) {
            Log.e("decode", "MediaCodec is null");
        }

        mDecodeMediaCodec.start();
    }

    public void release() {
        isDecodeFinish = true;
    }
}
