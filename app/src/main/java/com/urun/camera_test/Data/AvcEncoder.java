/*
There is no timestamp information in the raw H.264 elementary stream.
*You need to pass the timestamp through the decoder to MediaMuxer or
* whatever you're using to create your final output. If you don't, the
* player will just pick a rate, or possibly play the frames as fast as it can.
* answer from: http://stackoverflow.com/questions/19915962/android-mediacodec-video-wrong-color-and-playing-too-fast
*/

package com.urun.camera_test.Data;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ahmet on 12/22/2016.
 */
class AvcEncoder {

    private MediaCodec mediaCodec;
    private BufferedOutputStream outputStream;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding

    AvcEncoder() {
        /*Retrieving codec/codecs on device*/
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        Log.d("MediacodecInfo_retr: ", "found codec: " + codecInfo.getName());
        int colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
        Log.d("ColorFormat", "found colorFormat: " + colorFormat);
        File f = new File(Environment.getExternalStorageDirectory(), "/video_encoded.264");
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(f));
            Log.i("AvcEncoder", "outputStream initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Create a MediaCodec for the desired codec, then configure it as an encoder with our desired properties.*/
        try {
            mediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 640, 960);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 19);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    public void close() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void offerEncoder(byte[] input) {
        try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                outputStream.write(outData, 0, outData.length);
                Log.i("AvcEncoder", outData.length + " bytes written");

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            }
            Log.e("from_AvEncoder: ","frame encoded succesfully.");
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /*Below two functions has taken from: https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/EncodeDecodeTest.java*/
    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            return colorFormat;
        }
        return 0;   // not reached
    }
}
