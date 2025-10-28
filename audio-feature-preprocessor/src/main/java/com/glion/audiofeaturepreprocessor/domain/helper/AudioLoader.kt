package com.glion.audiofeaturepreprocessor.domain.helper

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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
     * 오디오 로드 + 모노 변환 + 리샘플링
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

        val pcmEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING))
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        else AudioFormat.ENCODING_PCM_16BIT

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()

        // 미리 충분히 큰 FloatArray 확보 (안정성 위해 1.5배 정도 여유)
        val estimatedSize = (extractor.getTrackFormat(trackIndex).getLong(MediaFormat.KEY_DURATION) / 1000000.0 * sampleRate * numChannels).toInt()
        val tempOutput = FloatArray((estimatedSize * 1.5).toInt())
        var offset = 0

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
                        System.arraycopy(floatBuffer, 0, tempOutput, offset, floatBuffer.size)
                        offset += floatBuffer.size
                    }

                    AudioFormat.ENCODING_PCM_FLOAT -> {
                        val floatBuffer = outputBuffer.asFloatBuffer()
                        val floatArray = FloatArray(floatBuffer.remaining())
                        floatBuffer.get(floatArray)
                        System.arraycopy(floatArray, 0, tempOutput, offset, floatArray.size)
                        offset += floatArray.size
                    }
                }

                codec.releaseOutputBuffer(outputBufferId, false)
                outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        // 실제 사용된 길이로 자르기
        val decodedSamples = tempOutput.copyOf(offset)

        // ---------------------------
        // 모노 변환
        val processed = if (isMono && numChannels > 1) {
            val monoBuffer = FloatArray(decodedSamples.size / numChannels)
            for (i in monoBuffer.indices) {
                var sum = 0f
                for (ch in 0 until numChannels) {
                    sum += decodedSamples[i * numChannels + ch]
                }
                monoBuffer[i] = sum / numChannels
            }
            monoBuffer
        } else {
            decodedSamples
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
