package com.glion.audiofeaturepreprocessor.domain

import kotlin.math.PI
import kotlin.math.cos

/**
 * Project : DSPSample
 * File : AudioUtils
 * Created by shhan on 2025-10-22
 *
 * Description:
 * - 추후 기입
 *
 * Copyright @2025 UBIPLUS. All rights reserved
 */
object AudioUtils {
    /**
     * Hann window 적용
     */
    fun applyHannWindow(buffer: FloatArray) {
        val bufferSize = buffer.size
        for (n in buffer.indices) buffer[n] *= 0.5f * (1 - cos(2 * PI * n / (bufferSize - 1))).toFloat()
    }
}