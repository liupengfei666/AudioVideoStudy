package com.lewis.audiovideostudy.util;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author lewis
 * Date: 2019/3/14.
 * Description: 音频编解码工具类
 */
public class AudioCodecUtil {

    private String mEncodeType; //编码格式类型
    private String mSrcPath;    //解码文件路径
    private String mDstPath;    //编码后的文件路径
    private MediaExtractor mExtractor;
    private MediaCodec mDecodeCodec; //解码器
    private MediaCodec mEncodeCodec; //编码器

    private ByteBuffer[] mDecoderInputBuffers;
    private ByteBuffer[] mDecoderOutputBuffers;
    private MediaCodec.BufferInfo mDecoderInfo;
    private ByteBuffer[] mEncoderInputBuffers;
    private ByteBuffer[] mEncoderOutputBuffers;
    private MediaCodec.BufferInfo mEncoderInfo;
    private boolean mCodecOver = false;
    private long mDecodeSize;
    private long mTotalSize;
    private ArrayList<byte[]> mChunkPCMContainer;//PCM数据块容器
    private FileOutputStream mFileOutputStream;
    private BufferedOutputStream mOutputStream;

    private OnCompleteListener mCompleteListener;

    private int key_bit_rate;     //比特率
    private int key_channel_count;//通道数
    private int key_sample_rate;  //采样率

    public static AudioCodecUtil get() {
        return new AudioCodecUtil();
    }

    public void setEncodeType(String encodeType) {
        this.mEncodeType = encodeType;
    }

    /**
     * 设置输出文件路径
     *
     * @param srcPath 源文件路径
     * @param dstPath 目标文件路径
     */
    public void setIOPath(String srcPath, String dstPath) {
        this.mSrcPath = srcPath;
        this.mDstPath = dstPath;
    }

    /**
     * 调用prepare()进行一些初始化操作
     */
    public void prepare() {
        if (TextUtils.isEmpty(mEncodeType)) {
            throw new IllegalArgumentException("mEncodeType can't be null");
        }
        if (mSrcPath == null) {
            throw new IllegalArgumentException("mSrcPath can't be null");
        }
        if (mDstPath == null) {
            throw new IllegalArgumentException("mDstPath can't be null");
        }
        try {
            mFileOutputStream = new FileOutputStream(new File(mDstPath));
            mOutputStream = new BufferedOutputStream(mFileOutputStream, 200 * 1024);
            File file = new File(mSrcPath);
            mTotalSize = file.length();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mChunkPCMContainer = new ArrayList<>();
        initDecoder();
        initEncoder();
    }

    /**
     * 初始化解码器
     */
    private void initDecoder() {
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mSrcPath);
            int trackCount = mExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) { //获取音频轨道
                    mExtractor.selectTrack(i);
                    key_bit_rate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                    key_channel_count = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    key_sample_rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    //创建解码器
                    mDecodeCodec = MediaCodec.createDecoderByType(mime);
                    //第二个参数是surface，解码视频的时候需要，第三个是MediaCrypto, 是关于加密的，最后一个flag填0即可
                    //configure会使MediaCodec进入Configured state
                    mDecodeCodec.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mDecodeCodec == null) {
            Log.e("codec", "create mDecodeCodec failed");
            return;
        }
        //启动MediaCodec,等待传入数据
        //调用此方法之后mediaCodec进入Executing state
        mDecodeCodec.start();
        //MediaCodec在此ByteBuffer[]中获取输入数据
        mDecoderInputBuffers = mDecodeCodec.getInputBuffers();
        //MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        mDecoderOutputBuffers = mDecodeCodec.getOutputBuffers();
        //用于描述解码得到的byte[]数据的相关信息
        mDecoderInfo = new MediaCodec.BufferInfo();

        showLog("buffers:" + mDecoderInputBuffers.length);
    }

    /**
     * 初始化编码器
     */
    private void initEncoder() {
        try {
            //参数对应-> mime type、采样率、声道数
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(mEncodeType, key_sample_rate, key_channel_count);
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, key_bit_rate);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
            mEncodeCodec = MediaCodec.createEncoderByType(mEncodeType);
            //最后一个参数当使用编码器时设置
            mEncodeCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mEncodeCodec == null) {
            Log.e("codec", "create mEncodeCodec failed");
            return;
        }
        mEncodeCodec.start();

        mEncoderInputBuffers = mEncodeCodec.getInputBuffers();
        mEncoderOutputBuffers = mEncodeCodec.getOutputBuffers();
        mEncoderInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 开始转码
     * 音频数据先解码成PCM,PCM数据再编码成MediaFormat.MIMETYPE_AUDIO_AAC格式
     * mp3->pcm->aac
     */
    public void startAsync() {
        showLog("start");
        new Thread(new DecodeRunnable()).start();
        new Thread(new EncodeRunnable()).start();
    }

    /**
     * 解码线程
     */
    private class DecodeRunnable implements Runnable {

        @Override
        public void run() {
            while (!mCodecOver) {
                audioToPCM();
            }
        }
    }

    /**
     * 编码线程
     */
    private class EncodeRunnable implements Runnable {
        @Override
        public void run() {
            long t = System.currentTimeMillis();
            while (!mCodecOver || !mChunkPCMContainer.isEmpty()) {
                pcmToTargetAudio();
            }
            if (mCompleteListener != null) {
                mCompleteListener.onComplete();
            }
            showLog("size:" + mTotalSize + " decodeSize:" + mDecodeSize + "time:" + (System.currentTimeMillis() - t));
        }
    }

    /**
     * 解码音频文件，得到PCM数据
     */
    private void audioToPCM() {
        for (int i = 0; i < mDecoderInputBuffers.length - 1; i++) {
            //获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
            int inputIndex = mDecodeCodec.dequeueInputBuffer(-1);
            if (inputIndex < 0) {
                mCodecOver = true;
                return;
            }
            ByteBuffer inputBuffer = mDecoderInputBuffers[inputIndex];//拿到inputBuffer
            inputBuffer.clear();//清空之前传入inputBuffer内的数据
            //MediaExtractor读取数据到inputBuffer中
            int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {//小于0 代表所有数据已读取完成
                mCodecOver = true;
            } else {
                //通知MediaDecode解码刚刚传入的数据
                mDecodeCodec.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);
                mExtractor.advance();//移动到下一帧
                mDecodeSize += sampleSize;
            }
        }
        //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        int outputIndex = mDecodeCodec.dequeueOutputBuffer(mDecoderInfo, 10000);

        ByteBuffer outputBuffer;
        byte[] chunkPCM;
        while (outputIndex >= 0) {//每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
            outputBuffer = mDecoderOutputBuffers[outputIndex];
            //BufferInfo内定义了此数据块的大小
            chunkPCM = new byte[mDecoderInfo.size];
            //将Buffer内的数据取出到字节数组中
            outputBuffer.get(chunkPCM);
            //数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
            outputBuffer.clear();
            //自己定义的方法，供编码器所在的线程获取数据
            putPCMData(chunkPCM);
            //此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
            mDecodeCodec.releaseOutputBuffer(outputIndex, false);
            //再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
            outputIndex = mDecodeCodec.dequeueOutputBuffer(mDecoderInfo, 10000);
        }
    }

    /**
     * 编码PCM数据，得到{@link #mEncodeType}格式的音频文件，并保存到{@link #mDstPath}
     */
    private void pcmToTargetAudio() {
        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        byte[] chunkPCM;

        for (int i = 0; i < mEncoderInputBuffers.length - 1; i++) {
            chunkPCM = getPCMData();//获取解码器所在线程输出的数据
            if (chunkPCM == null) {
                break;
            }
            //以下操作同解码器
            inputIndex = mEncodeCodec.dequeueInputBuffer(-1);
            inputBuffer = mEncoderInputBuffers[inputIndex];
            inputBuffer.clear();
            inputBuffer.limit(chunkPCM.length);
            //PCM数据填充给inputBuffer
            inputBuffer.put(chunkPCM);
            //通知编码器 编码
            mEncodeCodec.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);
        }

        outputIndex = mEncodeCodec.dequeueOutputBuffer(mEncoderInfo, 10000);
        while (outputIndex > 0) {
            outBitSize = mEncoderInfo.size;
            outPacketSize = outBitSize + 7; //7为ADTS头部的大小
            //拿到输出Buffer
            outputBuffer = mEncoderOutputBuffers[outputIndex];
            outputBuffer.position(mEncoderInfo.offset);
            outputBuffer.limit(mEncoderInfo.offset + outBitSize);
            chunkAudio = new byte[outPacketSize];
            //添加ADTS
            addADTStoPacket(chunkAudio, outPacketSize);
            //将编码得到的AAC数据 取出到byte[]中 偏移量offset=7
            outputBuffer.get(chunkAudio, 7, outBitSize);
            outputBuffer.position(mEncoderInfo.offset);
            try {
                //BufferOutputStream 将文件保存到内存卡中 *.aac
                mOutputStream.write(chunkAudio, 0, chunkAudio.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mEncodeCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mEncodeCodec.dequeueOutputBuffer(mEncoderInfo, 10000);
        }
    }

    /**
     * 将PCM数据存入{@link #mChunkPCMContainer}
     *
     * @param chunkPCM pcm数据块
     */
    private void putPCMData(byte[] chunkPCM) {
        synchronized (AudioCodecUtil.class) { //线程操作，记得加锁
            mChunkPCMContainer.add(chunkPCM);
        }
    }

    /**
     * 从{@link #mChunkPCMContainer}取出PCM数据
     *
     * @return PCM数据块
     */
    private byte[] getPCMData() {
        synchronized (AudioCodecUtil.class) {//线程操作，记得加锁
            showLog("getPCM:" + mChunkPCMContainer.size());
            if (mChunkPCMContainer.isEmpty()) {
                return null;
            }
        }
        //每次取出index 0 的数据
        byte[] chunkPCM = mChunkPCMContainer.get(0);
        //取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
        mChunkPCMContainer.remove(chunkPCM);
        return chunkPCM;
    }

    /**
     * 添加ADTS头
     *
     * @param packet    数据块
     * @param packetLen 数据块长度
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;// AAC LC
        int freqIdx = 4;// 44.1KHz
        int chanCfg = 2;// CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * 释放资源
     */
    public void release() {
        try {
            if (mOutputStream != null) {
                mOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                    mOutputStream = null;
                }
                if (mFileOutputStream != null) {
                    mFileOutputStream.close();
                    mFileOutputStream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mEncodeCodec != null) {
            mEncodeCodec.stop();
            mEncodeCodec.release();
            mEncodeCodec = null;
        }
        if (mDecodeCodec != null) {
            mDecodeCodec.stop();
            mDecodeCodec.release();
            mDecodeCodec = null;
        }
        if (mCompleteListener != null) {
            mCompleteListener = null;
        }
        showLog("release");
    }

    /**
     * 转码完成接口
     */
    public interface OnCompleteListener {
        void onComplete();
    }

    /**
     * 设置转码完成监听
     */
    public void setOnCompleteListener(OnCompleteListener completeListener) {
        this.mCompleteListener = completeListener;
    }

    private void showLog(String msg) {
        Log.e("AudioCodec", msg);
    }
}

