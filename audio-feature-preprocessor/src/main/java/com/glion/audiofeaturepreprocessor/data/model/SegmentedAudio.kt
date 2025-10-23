package com.glion.audiofeaturepreprocessor.data.model

/**
 * Project : DSPSample
 * File : SegmentedAudio
 * Created by glion on 2025-10-21
 *
 * Description:
 * - 세그먼트 단위로 분할한 Audio Data Class
 *
 * Copyright @2025 Gangglion. All rights reserved
 */
data class SegmentedAudio(
    val segments: List<FloatArray>,
    val sampleRate: Int,
    val numChannels: Int
)
