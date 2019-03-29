package com.lewis.audiovideostudy.decode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: lewis
 * create by: 19-3-29 上午11:43
 * description:
 */
public class DecodeAsync {

    private MediaExtractor mExtractor;
    private MediaCodec mDecoderCodec;

    private static class DecoderHolder {
        static DecodeAsync mDecodeAsync = new DecodeAsync();
    }

    public static DecodeAsync get() {
        return DecoderHolder.mDecodeAsync;
    }

    /**
     * 开始
     */
    public void startDecoder(@NonNull String path, @NonNull Surface surface) {
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(path);
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mExtractor.selectTrack(i);

                    mDecoderCodec = MediaCodec.createDecoderByType(mime);
                    mDecoderCodec.configure(format, surface, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mDecoderCodec == null) {
            Log.e("decoder", "MediaCodec is null");
        }
        mDecoderCodec.setCallback(codecCallback);
        mDecoderCodec.start();
    }

    /**
     * 解码回调
     */
    private MediaCodec.Callback codecCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            ByteBuffer buffer = codec.getInputBuffer(index);

            int sampleSize = mExtractor.readSampleData(buffer, index);
            if (sampleSize < 0) {
                mDecoderCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mDecoderCodec.queueInputBuffer(index, 0, sampleSize, mExtractor.getSampleTime(), 0);
                mExtractor.advance();
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            try {
                Thread.sleep(30); //不休眠播放很快
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (index >= 0) {
                codec.releaseOutputBuffer(index, true);
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                release();
            }
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.e("decoder", "error");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            Log.e("decoder", "Format Changed");
        }
    };

    /**
     * 释放资源
     */
    public void release() {
        Log.e("decoder", "释放资源");
        if (mDecoderCodec != null) {
            mDecoderCodec.stop();
            mDecoderCodec.release();
        }
        if (mExtractor != null) {
            mExtractor.release();
        }
    }
}
