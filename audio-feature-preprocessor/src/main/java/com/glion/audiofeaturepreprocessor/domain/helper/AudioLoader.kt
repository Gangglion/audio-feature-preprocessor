package com.glion.audiofeaturepreprocessor.domain.helper

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.glion.audiofeaturepreprocessor.data.AudioData
import com.glion.audiofeaturepreprocessor.data.DEFAULT_SAMPLE_RATE
import kotlin.math.ceil
import kotlin.math.min


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
     * @param targetSampleRate 원하는 샘플레이트. 기본값 44100
     * @param isMono 모노 변환 여부. 기본값 true
     */
    fun loadAudio(
        audioPath: String,
        targetSampleRate: Int = DEFAULT_SAMPLE_RATE,
        isMono: Boolean = true
    ): AudioData {
        val extractor = MediaExtractor()
        extractor.setDataSource(audioPath)

        // 첫 번째 오디오 트랙 선택
        val trackIndex = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        // PCM 인코딩
        val pcmEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        else AudioFormat.ENCODING_PCM_16BIT

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val tempOutput = mutableListOf<Float>()

        var isEOS = false
        while (!isEOS) {
            val inputBufferId = codec.dequeueInputBuffer(10000)
            if (inputBufferId >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(
                        inputBufferId,
                        0,
                        0,
                        0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    isEOS = true
                } else {
                    val presentationTimeUs = extractor.sampleTime
                    codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                    extractor.advance()
                }
            }

            var outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0)
            while (outputBufferId >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferId)!!

                when (pcmEncoding) {
                    AudioFormat.ENCODING_PCM_16BIT -> {
                        val shortBuffer = outputBuffer.asShortBuffer()
                        val floatBuffer = FloatArray(shortBuffer.remaining())
                        for (i in floatBuffer.indices) {
                            floatBuffer[i] = shortBuffer.get().toFloat() / 32768f
                        }
                        tempOutput.addAll(floatBuffer.toList())
                    }

                    AudioFormat.ENCODING_PCM_FLOAT -> {
                        val floatBuffer = outputBuffer.asFloatBuffer()
                        val floatArray = FloatArray(floatBuffer.remaining())
                        floatBuffer.get(floatArray)
                        tempOutput.addAll(floatArray.toList())
                    }
                }

                codec.releaseOutputBuffer(outputBufferId, false)
                outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        // ---------------------------
        // 모노 변환
        val processed = if (isMono && numChannels > 1) {
            val monoBuffer = FloatArray(tempOutput.size / numChannels)
            for (i in monoBuffer.indices) {
                var sum = 0f
                for (ch in 0 until numChannels) {
                    sum += tempOutput[i * numChannels + ch]
                }
                monoBuffer[i] = sum / numChannels
            }
            monoBuffer
        } else {
            tempOutput.toFloatArray()
        }

        // ---------------------------
        // 리샘플링 (linear interpolation)
        val finalBuffer = if (sampleRate != targetSampleRate) {
            linearResample(processed, sampleRate, targetSampleRate)
        } else {
            processed
        }

        return AudioData(
            samples = finalBuffer,
            sampleRate = targetSampleRate,
            numChannels = if (isMono) 1 else numChannels
        )
    }

    /**
     * 선형 보간 리샘플링
     */
    private fun linearResample(input: FloatArray, srcRate: Int, targetRate: Int): FloatArray {
        val ratio = targetRate.toDouble() / srcRate
        val outputLength = ceil(input.size * ratio).toInt()
        val output = FloatArray(outputLength)
        for (i in output.indices) {
            val srcIndex = i / ratio
            val idx0 = srcIndex.toInt()
            val idx1 = min(idx0 + 1, input.lastIndex)
            val frac = (srcIndex - idx0)
            output[i] = ((1 - frac) * input[idx0] + frac * input[idx1]).toFloat()
        }
        return output
    }
}