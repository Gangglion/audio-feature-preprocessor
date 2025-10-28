package com.glion.audiofeaturepreprocessor.domain.helper

import com.glion.audiofeaturepreprocessor.data.AudioData
import com.glion.audiofeaturepreprocessor.data.DEFAULT_HOP_SEC
import com.glion.audiofeaturepreprocessor.data.DEFAULT_MAX_SEGMENT
import com.glion.audiofeaturepreprocessor.data.DEFAULT_SEGMENT_SEC
import kotlin.math.min

/**
 * Project : DSPSample
 * File : AudioSegmenter
 * Created by glion on 2025-10-21
 *
 * Description:
 * - 오디오를 세그먼트로 분할
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
class AudioSegmenter {
    /**
     * 오디오를 세그먼트로 분할
     * @param segmentLengthSeconds 세그먼트 길이, 기본값 18.6
     * @param hopLengthSeconds 홉 길이, 기본값 6.4
     * @param maxSegments 최대 세그먼트 개수(기본값 3)
     */
    fun segmenter(
        audioData: AudioData,
        segmentLengthSeconds: Float = DEFAULT_SEGMENT_SEC,
        hopLengthSeconds: Float = DEFAULT_HOP_SEC,
        maxSegments: Int = DEFAULT_MAX_SEGMENT
    ) : List<FloatArray> {
        val segmentLengthSamples = (segmentLengthSeconds * audioData.sampleRate).toInt()
        val hopLengthSamples = (hopLengthSeconds * audioData.sampleRate).toInt()

        val segments = mutableListOf<FloatArray>()
        var start = 0
        while (start < audioData.samples.size && segments.size < maxSegments) {
            val end = min(start + segmentLengthSamples, audioData.samples.size)
            segments.add(audioData.samples.sliceArray(start until end))
            start += hopLengthSamples
        }

        return segments
    }
}