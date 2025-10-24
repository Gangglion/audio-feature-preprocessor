package com.glion.audiofeaturepreprocessor.domain.preprocessor

import be.tarsos.dsp.util.fft.FFT
import com.glion.audiofeaturepreprocessor.data.DEFAULT_HOP_MS
import com.glion.audiofeaturepreprocessor.data.DEFAULT_N_MELS
import com.glion.audiofeaturepreprocessor.data.DEFAULT_SAMPLE_RATE
import com.glion.audiofeaturepreprocessor.data.FFT_WINDOW_LEN
import com.glion.audiofeaturepreprocessor.domain.AudioUtils
import kotlin.math.ln
import kotlin.math.pow

/**
 * Project : DSPSample
 * File : LogMelExtractor
 * Created by glion on 2025-10-21
 *
 * Description:
 * - LogMel 추출
 * @param sampleRate 샘플링레이트
 * @param fftWindowLen FFT 윈도우 사이즈. 기본값 1024
 * @param melBandCount MEL 밴드 수. 기본값 128
 * @param minFreq
 * @param maxFreq
 * @param hopMs 시간단위 Hop MS(FFT Window 가 겹치거나 이동하는 간격). 기본값 25f
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
class LogMelExtractor(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val fftWindowLen: Int = FFT_WINDOW_LEN,
    private val melBandCount: Int = DEFAULT_N_MELS,
    private val minFreq: Float = 0f,
    private val maxFreq: Float = (DEFAULT_SAMPLE_RATE / 2).toFloat(),
    hopMs: Float = DEFAULT_HOP_MS
) {
    private val hopLength = (sampleRate * hopMs / 1000f).toInt()

    // mel filter bank 생성
    private val melFilterBank: Array<FloatArray> = createMelFilterBank()

    /**
     * 입력 신호 -> logMel [M, T] 형태로 추출
     */
    fun extract(signal: FloatArray): Array<FloatArray> {
        val fft = FFT(fftWindowLen)
        val nFrames = 1 + (signal.size - fftWindowLen) / hopLength
        val logMel = Array(melBandCount) { FloatArray(nFrames) }
        val buffer = FloatArray(fftWindowLen)
        val spectrum = FloatArray(fftWindowLen / 2)

        for (frame in 0 until nFrames) {
            val start = frame * hopLength
            for (i in buffer.indices) buffer[i] = if (start + i < signal.size) signal[start + i] else 0f
            AudioUtils.applyHannWindow(buffer)
            fft.forwardTransform(buffer)
            fft.modulus(buffer, spectrum)
            for (m in 0 until melBandCount) {
                var sum = 0f
                for (k in spectrum.indices) sum += spectrum[k] * melFilterBank[m][k]
                logMel[m][frame] = 10f * ln(sum + 1e-10f) / ln(10f)
            }
        }
        return logMel
    }

    /**
     * Mel 필터뱅크 생성
     */
    private fun createMelFilterBank(): Array<FloatArray> {
        val melMin = hzToMel(minFreq)
        val melMax = hzToMel(maxFreq)
        val melPoints = FloatArray(melBandCount + 2) { i -> melMin + (melMax - melMin) * i / (melBandCount + 1) }
        val hzPoints = melPoints.map { melToHz(it) }
        val binFreqs = FloatArray(fftWindowLen / 2 + 1) { i -> i * sampleRate.toFloat() / fftWindowLen }
        val filterBank = Array(melBandCount) { FloatArray(fftWindowLen / 2 + 1) { 0f } }

        for (m in 0 until melBandCount) {
            val fLeft = hzPoints[m]
            val fCenter = hzPoints[m + 1]
            val fRight = hzPoints[m + 2]
            for (k in binFreqs.indices) {
                val f = binFreqs[k]
                filterBank[m][k] = when {
                    f < fLeft -> 0f
                    f <= fCenter -> (f - fLeft) / (fCenter - fLeft)
                    f <= fRight -> (fRight - f) / (fRight - fCenter)
                    else -> 0f
                }
            }
        }
        return filterBank
    }

    private fun hzToMel(freq: Float) = 2595f * ln(1f + freq / 700f) / ln(10f)
    private fun melToHz(mel: Float) = 700f * (10f.pow(mel / 2595f) - 1f)
}