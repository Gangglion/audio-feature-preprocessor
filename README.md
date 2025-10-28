# audio-feature-preprocessor
[![](https://jitpack.io/v/gangglion/audio-feature-preprocessor.svg)](https://jitpack.io/#gangglion/audio-feature-preprocessor)
[![](https://img.shields.io/badge/license-GPL%203.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

audio-feature-preprocessor is an Android Library for audio preprocessing and feature extraction.  
It uses TarsosDSP (GPL 3.0) and provides the following features:

- Audio Decoding from files
- Resampling and mono conversion
- Segment splitting
- Log-Mel extraction
- Chroma extraction
- Tempo extraction

Supported Android SDK : min 26, compile 36

## Installation

### 1. Add Jitpack and TarsosDSP repositories
Add the following to your project-level `settings.gradle.kts`
```kotlin
dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven("TarsosDSP repository") { url = uri("https://mvn.0110.be/releases") } // Add this
        maven { url = uri("https://jitpack.io") } // Add this
    }
}
```
### 2. Add the Dependency
In your module-level `build.gradle.kts`
```kotlin
dependencies {
    implementation("com.github.gangglion:audio-feature-preprocessor:TAG")
}
```
> ⚠️ Note : Core functionality depends on TarsosDSP, so make sure the repository is added

## Project Structure
```
audio-feature-preprocessor
    ├─ data
    │   ├─ AudioData
    │   └─ Define
    └─ domain
        ├─ helper
        │   ├─ AudioLoader
        │   └─ AudioSegmenter
        └─ preprocessor
            ├─ ChromaExtractor
            ├─ LogMelExtractor
            └─ TempoExtractor
```

## How to Use
### helper
| Function                                                                                                                                    | Description                                                                                   | Returns                                                                           |
|---------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| `AudioLoader().loadAudio(audioPath: String, targetSampleRate: Int = 44100, isMono: Boolean = true)`                                         | Decodes the audio, resamples it to the target sample rate, and converts it to a mono channel. | An `AudioData` object containing `decodingData`, `sampleRate`, and `numChannels`. |
| `AudioSegmenter().segmenter(audioData: AudioData, segmentLengthSeconds: Float = 18.6, hopLengthSeconds: Float = 6.4, maxSegments: Int = 3)` | Splits the decoded audio into segments.                                                       | `SegmentedAudio` object including `segments`, `sampleRate`, and `numChannels`.    |

### preprocessor
| Function                                                                                                                                                                                      | Description                                                                                            | Returns                                                                                                  |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `ChromaExtractor(sampleRate: Float = 44100, nchroma: Int = 12, fftWindowLen: Int = 1024, hopMs: Float = 25f).extract(signal: FloatArray) : Array<FloatArray>`                                 | Extracts Chromagram from the audio segment.                                                            | 2D array of shape `[C, T]`, where `C` = number of chroma bins (default 12), `T` = number of time frames. |
| `LogMelExtractor(sampleRate: Float = 44100, fftWindowLen: Int = 1024, melBandCount: Int, minFreq: Float, maxFreq: Float, hopMs: Float = 25f).extract(signal: FloatArray) : Array<FloatArray>` | Extracts LogMel Spectrogram from the audio segment. Applies Mel filter and computes power spectrum(dB) | 2D array of shape `[M, T]`, where `M` = number of Mel Band (default 128), `T` = number of time frames.   |
| `TempoExtractor(sampleRate: Float = 44100, fftWindowLen: Int = 1024, hopMs: Float = 25f, win: Int = 160).extract(signal: FloatArray) : Array<FloatArray>`                                     | Extracts Tempogram from the audio segment.                                                             | 1D array of `[L]`, where `L` = number of tempo candidates / tempo bins.                                  |
### AudioUtils
| Function                                           | Description                                                                         | Returns      |
|----------------------------------------------------|-------------------------------------------------------------------------------------|--------------|
| `AudioUtils.applyHannWindow(buffer: FloatArray)`   | Applies a Hann Window to the audio segment before FFT to reduce spectral leakage.   | `FloatArray` |

## License
This project is licensed under the GPL 3.0 License