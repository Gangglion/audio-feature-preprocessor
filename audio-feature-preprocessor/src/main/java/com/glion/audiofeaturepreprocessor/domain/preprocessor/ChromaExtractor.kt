package com.glion.audiofeaturepreprocessor.domain.preprocessor

import be.tarsos.dsp.util.fft.FFT
import com.glion.audiofeaturepreprocessor.data.DEFAULT_CHROMA_BINS
import com.glion.audiofeaturepreprocessor.data.DEFAULT_HOP_MS
import com.glion.audiofeaturepreprocessor.data.DEFAULT_SAMPLE_RATE
import com.glion.audiofeaturepreprocessor.data.FFT_WINDOW_LEN
import com.glion.audiofeaturepreprocessor.domain.AudioUtils
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Project : DSPSample
 * File : ChromaExtractor
 * Created by glion on 2025-10-21
 *
 * Description:
 * - Chromagram 추출
 * @param sampleRate 샘플링레이트, 기본값 44100
 * @param nChroma 크로마 bin 수, 기본값 12
 * @param fftWindowLen FFT Window 길이
 * @param hopMs 프레임 간 간격
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
class ChromaExtractor(
    private val sampleRate: Float = DEFAULT_SAMPLE_RATE.toFloat(),
    private val nChroma: Int = DEFAULT_CHROMA_BINS,
    private val fftWindowLen: Int = FFT_WINDOW_LEN,
    private val hopMs: Float = DEFAULT_HOP_MS,
) {

    private val hopLength = ((sampleRate * hopMs / 1000f).roundToInt()).coerceAtLeast(1)
    // CQT bin frequencies (log-spaced)
    private val cqtFreqs: FloatArray = createLogFreqBins()

    /**
     * 입력: 조화 성분(FloatArray) → 출력: Chroma [C, T]
     */
    fun extract(signal: FloatArray): Array<FloatArray> {
        val fft = FFT(fftWindowLen)
        val nFrames = 1 + (signal.size - fftWindowLen) / hopLength
        val chroma = Array(nChroma) { FloatArray(nFrames) }

        val buffer = FloatArray(fftWindowLen)
        val spectrum = FloatArray(fftWindowLen / 2)

        for (frame in 0 until nFrames) {
            val start = frame * hopLength
            // segment 부족 시 0-padding 금지
            if (start + fftWindowLen > signal.size) break

            for (i in buffer.indices) buffer[i] = signal[start + i]

            AudioUtils.applyHannWindow(buffer)
            fft.forwardTransform(buffer)
            fft.modulus(buffer, spectrum)

            // FFT bin -> chroma fold
            for (k in spectrum.indices) {
                val freq = k * sampleRate / fftWindowLen
                if (freq <= 0f) continue

                // 가장 가까운 log-bin index
                val binIdx = findClosestLogBin(freq)
                val chromaBin = (binIdx % nChroma + nChroma) % nChroma

                // weighted fold (log-freq gap 사용)
                val weight = 1f - (abs(freq - cqtFreqs.getOrElse(binIdx) { freq }) / (freq))
                chroma[chromaBin][frame] += spectrum[k] * weight
            }
        }

        return chroma
    }

    /**
     * log-spaced frequency bins (근사 CQT)
     */
    private fun createLogFreqBins(): FloatArray {
        val nOctaves = log2(sampleRate / 27.5f).toInt() // A0 ~ Nyquist
        val totalBins = nOctaves * nChroma
        val freqs = FloatArray(totalBins)
        for (i in 0 until totalBins) {
            freqs[i] = 27.5f * 2.0.pow(i.toDouble() / nChroma).toFloat()
        }
        return freqs
    }

    private fun findClosestLogBin(freq: Float): Int {
        var minDiff = Float.MAX_VALUE
        var idx = 0
        for (i in cqtFreqs.indices) {
            val diff = abs(freq - cqtFreqs[i])
            if (diff < minDiff) {
                minDiff = diff
                idx = i
            }
        }
        return idx
    }
}