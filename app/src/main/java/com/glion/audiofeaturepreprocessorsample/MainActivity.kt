package com.glion.audiofeaturepreprocessorsample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.glion.audiofeaturepreprocessorsample.ui.theme.AudioFeaturePreprocessorSampleTheme
import com.glion.audiofeaturepreprocessor.domain.helper.AudioLoader
import com.glion.audiofeaturepreprocessor.domain.helper.AudioResampler
import com.glion.audiofeaturepreprocessor.domain.helper.AudioSegmenter
import com.glion.audiofeaturepreprocessor.domain.preprocessor.ChromaExtractor
import com.glion.audiofeaturepreprocessor.domain.preprocessor.LogMelExtractor
import com.glion.audiofeaturepreprocessor.domain.preprocessor.TempoExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioFeaturePreprocessorSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // asset 내 오디오 파일 가져와 캐시저장소에 저장
        val bytes = assets.open("sample_music.mp3").use { it.readBytes() }
        val tempFile = File(cacheDir, "sample_music.mp3")
        FileOutputStream(tempFile).use { it.write(bytes) }

        lifecycleScope.launch(Dispatchers.Default) {
            // 1. 오디오 디코딩
            val decodedMonoAudio = withContext(Dispatchers.IO) {
                AudioLoader().loadAudio(audioPath = tempFile.absolutePath)
            }
            Log.d("shhan", "################## 오디오 디코딩 완료. 전처리 시작 ##################")

            val elapsed = measureTimeMillis {
                // 2. 세그먼트 나누기
                val audioSegments = AudioSegmenter().segmenter(decodedMonoAudio)

                // 3. 각 세그먼트 모노 변환 + 리샘플링(병렬 처리)
                val resamplingSegments = audioSegments.segments.map { segment ->
                    async {
                        AudioResampler().resample(
                            input = segment,
                            numChannels = audioSegments.numChannels,
                            originRate = audioSegments.sampleRate.toFloat(),
                            isMono = true
                        )
                    }
                }.awaitAll()

                // 각 세그먼트당 전처리(병렬처리)
                val preprocessingResult = processSegments(resamplingSegments)

                Log.d("shhan", "################## 전처리 결과 ##################")
                Log.d("shhan", "세그먼트 수 : ${preprocessingResult.size}")
                Log.d("shhan", "첫번쨰 세그먼트 전처리 결과")
                Log.d("shhan", "LogMel : ${preprocessingResult[0].first}")
                Log.d("shhan", "Chroma : ${preprocessingResult[0].second}")
                Log.d("shhan", "Tempo : ${preprocessingResult[0].third}")
                Log.d("shhan", "두번째 세그먼트 전처리 결과")
                Log.d("shhan", "LogMel : ${preprocessingResult[1].first}")
                Log.d("shhan", "Chroma : ${preprocessingResult[1].second}")
                Log.d("shhan", "Tempo : ${preprocessingResult[1].third}")
                Log.d("shhan", "세번째 세그먼트 전처리 결과")
                Log.d("shhan", "LogMel : ${preprocessingResult[2].first}")
                Log.d("shhan", "Chroma : ${preprocessingResult[2].second}")
                Log.d("shhan", "Tempo : ${preprocessingResult[2].third}")
            }

            Log.d("shhan", "################## 오디오 전처리 수행 시간 :: ${elapsed}ms ##################")
        }
    }

    private suspend fun processSegments(segments: List<FloatArray>) =
        coroutineScope {
            val logMelExtractor = LogMelExtractor()
            val chromaExtractor = ChromaExtractor()
            val tempoExtractor = TempoExtractor()
            segments.map { segment ->
                async(Dispatchers.Default) {
                    // 각 세그먼트에서 동시에 처리
                    val logMel = async { logMelExtractor.extract(segment) }
                    val chroma = async { chromaExtractor.extract(segment) }
                    val tempo = async { tempoExtractor.extract(segment) }

                    Triple(logMel.await(), chroma.await(), tempo.await())
                }
            }.awaitAll() // 모든 세그먼트 결과 기다림
        }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AudioFeaturePreprocessorSampleTheme {
        Greeting("Android")
    }
}