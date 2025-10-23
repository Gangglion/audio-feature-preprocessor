package com.glion.audiofeaturepreprocessor.domain.helper

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.glion.audiofeaturepreprocessor.data.model.AudioData


/**
 * Project : DSPSample
 * File : AudioResampler
 * Created by glion on 2025-10-21
 *
 * Description:
 * - 오디오 파일 디코딩(모노 변환 및 리샘플링)
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
class AudioLoader {
    /**
     * ByteArray로 된 오디오 파일을 디코딩 후 반환
     * @param audioPath 오디오 파일 경로
     */
    fun loadAudio(audioPath: String): AudioData {
        val extractor = MediaExtractor()
        extractor.setDataSource(audioPath)

        // 첫 번째 오디오 트랙 선택
        val trackIndex = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        // PCM 인코딩 형식 확인 (16-bit 보장 여부 체크)
        val pcmEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        else AudioFormat.ENCODING_PCM_16BIT

        if (pcmEncoding != AudioFormat.ENCODING_PCM_16BIT) {
            Log.w("AudioLoader", "⚠️ Unexpected PCM encoding: $pcmEncoding (expected 16-bit PCM)")
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val outputList = mutableListOf<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false

        while (!isEOS) {
            val inputBufferId = codec.dequeueInputBuffer(10000)
            if (inputBufferId >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    val presentationTimeUs = extractor.sampleTime
                    codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                    extractor.advance()
                }
            }

            var outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
            while (outputBufferId >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferId)!!

                when (pcmEncoding) {
                    AudioFormat.ENCODING_PCM_16BIT -> {
                        val shortBuffer = outputBuffer.asShortBuffer()
                        val floatBuffer = FloatArray(shortBuffer.remaining())
                        for (i in floatBuffer.indices) {
                            floatBuffer[i] = shortBuffer.get().toFloat() / 32768f
                        }
                        outputList.addAll(floatBuffer.toList())
                    }

                    AudioFormat.ENCODING_PCM_FLOAT -> {
                        val floatBuffer = outputBuffer.asFloatBuffer()
                        val floatArray = FloatArray(floatBuffer.remaining())
                        floatBuffer.get(floatArray)
                        outputList.addAll(floatArray.toList())
                    }

                    else -> {
                        Log.e("AudioLoader", "❌ Unsupported PCM encoding: $pcmEncoding")
                    }
                }

                codec.releaseOutputBuffer(outputBufferId, false)
                outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return AudioData(
            samples = outputList.toFloatArray(),
            sampleRate = sampleRate,
            numChannels = numChannels
        )
    }
}