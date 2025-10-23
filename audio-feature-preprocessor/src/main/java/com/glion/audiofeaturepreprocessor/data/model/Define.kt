package com.glion.audiofeaturepreprocessor.data.model

/**
 * Project : DSPSample
 * File : Define
 * Created by glion on 2025-10-21
 *
 * Description:
 * - 공통 상수 정의
 *
 * Copyright @2025 Gangglion. All rights reserved
 */

// HPSS 적용 여부
const val APPLY_HPSS = false
// 세그먼트 길이 기본값
const val DEFAULT_SEGMENT_SEC = 18.6f
// 샘플링레이트 Hop 길이 기본값
const val DEFAULT_HOP_SEC = 6.4f
// 최대 세그먼트 수 기본값
const val DEFAULT_MAX_SEGMENT = 3
// 샘플링레이트 기본값
const val DEFAULT_SAMPLE_RATE = 44100
// FFT Window 사이즈 기본값(절반씩 조절 가능. 낮아질 수록 속도 향상 / 품질 저하)
const val FFT_WINDOW_LEN = 1024
// 시간 단위 Hop 기본값
const val DEFAULT_HOP_MS = 25f

// MEL 밴드 수 기본값
const val DEFAULT_N_MELS = 128
// Chroma bin 수 기본값(12음계)
const val DEFAULT_CHROMA_BINS = 12
// Tempogram 사용 Window length 기본값
const val DEFAULT_TEMPO_WIN = 160
