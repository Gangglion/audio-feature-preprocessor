package com.glion.audiofeaturepreprocessor.domain.helper

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.resample.RateTransposer
import com.glion.audiofeaturepreprocessor.data.model.DEFAULT_SAMPLE_RATE

/**
 * Project : DSPSample
 * File : AudioResampler
 * Created by glion on 2025-10-22
 *
 * Description:
 * - 오디오 리샘플러
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
class AudioResampler {
    fun resample(
        input: FloatArray,
        numChannels: Int,
        originRate: Float,
        targetRate: Float = DEFAULT_SAMPLE_RATE.toFloat(),
        isMono: Boolean = true
    ): FloatArray {
        val rate = (targetRate / originRate).toDouble()
        val bufferSize = 512
        val outputBuffer = mutableListOf<Float>()

        // RateTransposer 생성
        val transposer = RateTransposer(rate)

        var pos = 0
        while(pos < input.size) {
            val end = (pos + bufferSize).coerceAtMost(input.size)
            val buffer = input.copyOfRange(pos, end)

            // AudioEvent 흉내
            val audioEvent = AudioEvent(TarsosDSPAudioFormat(originRate, 16, numChannels, true, false))
            audioEvent.floatBuffer = buffer

            // RateTransposer 적용
            transposer.process(audioEvent)

            // 결과 수집
            outputBuffer.addAll(audioEvent.floatBuffer.toList())

            pos += bufferSize
        }

        return if(isMono) toMono(outputBuffer.toFloatArray(), numChannels) else outputBuffer.toFloatArray()

    }

    fun toMono(input: FloatArray, numChannels: Int): FloatArray {
        if (numChannels == 1) return input // 이미 모노

        val monoLength = input.size / numChannels
        val mono = FloatArray(monoLength)

        for (i in 0 until monoLength) {
            var sum = 0f
            for (ch in 0 until numChannels) {
                sum += input[i * numChannels + ch]
            }
            mono[i] = sum / numChannels
        }
        return mono
    }
}