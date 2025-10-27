package com.glion.audiofeaturepreprocessor.domain.preprocessor

import com.glion.audiofeaturepreprocessor.data.DEFAULT_HOP_MS
import com.glion.audiofeaturepreprocessor.data.DEFAULT_SAMPLE_RATE
import com.glion.audiofeaturepreprocessor.data.DEFAULT_TEMPO_WIN
import com.glion.audiofeaturepreprocessor.data.FFT_WINDOW_LEN
import kotlin.math.min
import kotlin.math.roundToInt

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

    private val hopLength = ((sampleRate * hopMs / 1000f).roundToInt()).coerceAtLeast(1)

    /**
     * 입력: 오디오 신호(FloatArray)
     * 출력: Tempogram [win] 형태 1D 벡터 (정규화 없음)
     */
    fun extract(signal: FloatArray): FloatArray {
        // 1. Onset envelope 계산
        val onset = onsetEnvelope(signal)

        // 2. Autocorrelation 기반 tempogram
        val tempogram = autocorrelate(onset)

        // 3. Crop to win (padding 금지)
        val len = min(tempogram.size, win)
        val result = FloatArray(len)
        for (i in 0 until len) {
            result[i] = tempogram[i]
        }

        return result
    }

    private fun onsetEnvelope(signal: FloatArray): FloatArray {
        val nFrames = 1 + (signal.size - fftWindowLen) / hopLength
        val envelope = FloatArray(nFrames)
        val buffer = FloatArray(fftWindowLen)

        for (i in 0 until nFrames) {
            val start = i * hopLength
            // segment 부족 시 0-padding 금지
            if (start + fftWindowLen > signal.size) break

            for (j in buffer.indices) {
                buffer[j] = signal[start + j]
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