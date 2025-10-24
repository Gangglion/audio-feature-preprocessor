package com.glion.audiofeaturepreprocessor.domain.preprocessor

import com.glion.audiofeaturepreprocessor.data.DEFAULT_HOP_MS
import com.glion.audiofeaturepreprocessor.data.DEFAULT_SAMPLE_RATE
import com.glion.audiofeaturepreprocessor.data.DEFAULT_TEMPO_WIN
import com.glion.audiofeaturepreprocessor.data.FFT_WINDOW_LEN

/**
 * Project : DSPSample
 * File : TempoExtractor
 * Created by glion on 2025-10-21
 *
 * Description:
 * - Tempogram 추출
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
class TempoExtractor(
    sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val fftWindowLen: Int = FFT_WINDOW_LEN,
    hopMs: Float = DEFAULT_HOP_MS,
    private val win: Int = DEFAULT_TEMPO_WIN
) {

    private val hopLength = (sampleRate * hopMs / 1000f).toInt()

    /**
     * 입력: 오디오 신호(FloatArray)
     * 출력: Tempogram [win] 형태 1D 벡터 (정규화 없음)
     */
    fun extract(signal: FloatArray): FloatArray {
        // 1. Onset envelope 계산
        val onset = onsetEnvelope(signal)

        // 2. Autocorrelation 기반 tempogram
        val tempogram = autocorrelate(onset)

        // 3. Crop / pad to win
        val result = FloatArray(win) { 0f }
        for (i in result.indices) {
            result[i] = if (i < tempogram.size) tempogram[i] else 0f
        }

        return result
    }

    private fun onsetEnvelope(signal: FloatArray): FloatArray {
        val nFrames = 1 + (signal.size - fftWindowLen) / hopLength
        val envelope = FloatArray(nFrames)
        val buffer = FloatArray(fftWindowLen)

        for (i in 0 until nFrames) {
            val start = i * hopLength
            for (j in buffer.indices) {
                buffer[j] = if (start + j < signal.size) signal[start + j] else 0f
            }
            // energy 계산
            var sum = 0f
            for (v in buffer) {
                sum += v * v
            }
            envelope[i] = sum
        }

        return envelope
    }

    private fun autocorrelate(x: FloatArray): FloatArray {
        val n = x.size
        val result = FloatArray(n)
        for (lag in 0 until n) {
            var sum = 0f
            for (i in 0 until n - lag) {
                sum += x[i] * x[i + lag]
            }
            result[lag] = sum
        }
        return result
    }
}