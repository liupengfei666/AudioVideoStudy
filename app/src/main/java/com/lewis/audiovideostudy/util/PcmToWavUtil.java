package com.lewis.audiovideostudy.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcmToWavUtil {

    /**
     * 缓存的音频大小
     */
    private int mBufferSize;
    /**
     * 采样率
     */
    private int mSampleRate;
    /**
     * 声道数
     */
    private int mChannel;

    /**
     * @param sampleRate  采样率
     * @param channel     通道数
     * @param audioFormat 音频格式
     */
    public PcmToWavUtil(int sampleRate, int channel, int audioFormat) {
        this.mSampleRate = sampleRate;
        this.mChannel = channel;
        this.mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat);
    }

    public void pcmToWav(String inputFilePath, String outFilePath) {
        FileInputStream inputStream;
        FileOutputStream outputStream;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = mSampleRate;
        int channels = mChannel == AudioFormat.CHANNEL_IN_MONO ? 1 : 2;
        long byteRates = 16 * mSampleRate * channels / 8;
        byte[] buffer = new byte[mBufferSize];
        try {
            inputStream = new FileInputStream(inputFilePath);
            outputStream = new FileOutputStream(outFilePath);
            totalAudioLen = inputStream.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            writeWavFileHeader(outputStream, totalAudioLen, totalDataLen, longSampleRate, channels, byteRates);
            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }
            Log.w("pcm to wav", "完成");
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 写wav头信息
     *
     * @param outputStream   输出流
     * @param totalAudioLen  全部音频长度
     * @param totalDataLen   全部数据长度
     * @param longSampleRate 采样率
     * @param channels       通道数
     * @param byteRates      音频传输速率
     */
    private void writeWavFileHeader(FileOutputStream outputStream, long totalAudioLen, long totalDataLen,
                                    long longSampleRate, int channels, long byteRates) {
        byte[] header = new byte[44];
        //RIFF
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) (totalDataLen >> 8 & 0xff);
        header[6] = (byte) (totalDataLen >> 16 & 0xff);
        header[7] = (byte) (totalDataLen >> 24 & 0xff);
        //WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过度字节
        //4 bytes : size of fmt chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码方式
        header[20] = 1;
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) (longSampleRate >> 8 & 0xff);
        header[26] = (byte) (longSampleRate >> 16 & 0xff);
        header[27] = (byte) (longSampleRate >> 24 & 0xff);
        //音频数据传输速率，采样率×通道数×采样深度/8
        header[28] = (byte) (byteRates & 0xff);
        header[29] = (byte) (byteRates >> 8 & 0xff);
        header[30] = (byte) (byteRates >> 16 & 0xff);
        header[31] = (byte) (byteRates >> 24 & 0xff);
        //确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数×采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) (totalAudioLen >> 8 & 0xff);
        header[42] = (byte) (totalAudioLen >> 16 & 0xff);
        header[43] = (byte) (totalAudioLen >> 24 & 0xff);
        try {
            outputStream.write(header, 0, header.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
