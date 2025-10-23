package com.glion.audiofeaturepreprocessor.data.model

/**
 * Project : DSPSample
 * File : AudioData
 * Created by glion on 2025-10-21
 *
 * Description:
 * - 오디오 데이터
 * @param samples 오디오데이터
 * @param sampleRate 샘플링레이트
 * @param numChannels 채널수
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val numChannels: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioData

        if (sampleRate != other.sampleRate) return false
        if (numChannels != other.numChannels) return false
        if (!samples.contentEquals(other.samples)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + numChannels
        result = 31 * result + samples.contentHashCode()
        return result
    }
}