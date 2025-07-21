package com.zen.e_learning_bahasa_madura.util

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.zen.e_learning_bahasa_madura.util.MFCCProcessor.Companion.fft
import com.zen.e_learning_bahasa_madura.util.MFCCProcessor.Complex
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log2

class AudioRecorderUtil {

    companion object {
        private const val TAG = "AudioRecorderUtil"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        private var audioRecord: AudioRecord? = null
        private var isRecording = false
        private var recordingThread: Thread? = null

        /**
         * Mulai recording audio
         */
        @Throws(SecurityException::class)
        fun startRecording(outputFile: File) {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )

                Log.d(TAG, "Buffer size: $bufferSize")

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed")
                    return
                }

                audioRecord?.startRecording()
                isRecording = true

                BacksoundManager.pause()

                Log.d(TAG, "Recording started to: ${outputFile.absolutePath}")

                recordingThread = Thread {
                    writeAudioDataToFile(outputFile, bufferSize)
                }
                recordingThread?.start()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording: ${e.message}")
                throw e
            }
        }

        /**
         * Stop recording audio
         */
        fun stopRecording() {
            try {
                isRecording = false

                audioRecord?.apply {
                    if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        stop()
                    }
                    release()
                }
                audioRecord = null

                recordingThread?.join()
                recordingThread = null

                Log.d(TAG, "Recording stopped")

                BacksoundManager.resume()

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording: ${e.message}")
            }
        }

        /**
         * Tulis data audio ke file WAV
         */
        private fun writeAudioDataToFile(outputFile: File, bufferSize: Int) {
            try {
                val buffer = ByteArray(bufferSize)
                val audioData = mutableListOf<Byte>()

                while (isRecording) {
                    val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                    if (bytesRead > 0) {
                        audioData.addAll(buffer.take(bytesRead))
                    }
                }

                // Konversi ke WAV format
                writeWavFile(outputFile, audioData.toByteArray())

            } catch (e: Exception) {
                Log.e(TAG, "Error writing audio data: ${e.message}")
            }
        }

        /**
         * Tulis file WAV dengan header yang proper
         */
        fun writeWavFile(outputFile: File, audioData: ByteArray) {
            try {
                val fileSize = audioData.size + 36
                val byteRate = SAMPLE_RATE * 1 * 16 / 8 // SampleRate * NumChannels * BitsPerSample/8
                val blockAlign = 1 * 16 / 8 // NumChannels * BitsPerSample/8

                FileOutputStream(outputFile).use { fos ->
                    // WAV Header
                    fos.write("RIFF".toByteArray())
                    fos.write(intToByteArray(fileSize))
                    fos.write("WAVE".toByteArray())

                    // Format chunk
                    fos.write("fmt ".toByteArray())
                    fos.write(intToByteArray(16)) // Size of format chunk
                    fos.write(shortToByteArray(1)) // Audio format (1 = PCM)
                    fos.write(shortToByteArray(1)) // Number of channels
                    fos.write(intToByteArray(SAMPLE_RATE)) // Sample rate
                    fos.write(intToByteArray(byteRate)) // Byte rate
                    fos.write(shortToByteArray(blockAlign.toShort())) // Block align
                    fos.write(shortToByteArray(16)) // Bits per sample

                    // Data chunk
                    fos.write("data".toByteArray())
                    fos.write(intToByteArray(audioData.size))
                    fos.write(audioData)
                }

                Log.d(TAG, "WAV file written: ${outputFile.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Error writing WAV file: ${e.message}")
                throw e
            }
        }

        /**
         * Konversi int ke byte array (little endian)
         */
        private fun intToByteArray(value: Int): ByteArray {
            return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
        }

        /**
         * Konversi short ke byte array (little endian)
         */
        private fun shortToByteArray(value: Short): ByteArray {
            return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
        }

        /**
         * Validasi file audio
         */
        fun validateAudioFile(file: File): Boolean {
            try {
                if (!file.exists()) {
                    Log.e(TAG, "Audio file does not exist: ${file.absolutePath}")
                    return false
                }

                if (file.length() < 44) { // Minimal size for WAV header
                    Log.e(TAG, "Audio file too small: ${file.length()} bytes")
                    return false
                }

                val bytes = file.readBytes()

                // Check WAV header
                val riffHeader = String(bytes.sliceArray(0..3))
                val waveHeader = String(bytes.sliceArray(8..11))

                if (riffHeader != "RIFF" || waveHeader != "WAVE") {
                    Log.e(TAG, "Invalid WAV file format")
                    return false
                }

                Log.d(TAG, "Audio file validation passed: ${file.absolutePath}")
                return true

            } catch (e: Exception) {
                Log.e(TAG, "Error validating audio file: ${e.message}")
                return false
            }
        }

        /**
         * Hitung durasi audio dalam detik
         */
        fun getAudioDuration(file: File): Double {
            try {
                if (!validateAudioFile(file)) {
                    return 0.0
                }

                val fileSize = file.length()
                val audioDataSize = fileSize - 44 // Minus WAV header
                val duration = audioDataSize.toDouble() / (SAMPLE_RATE * 2) // 16-bit = 2 bytes per sample

                Log.d(TAG, "Audio duration: $duration seconds")
                return duration

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating audio duration: ${e.message}")
                return 0.0
            }
        }

        /**
         * Normalize audio level
         */
        fun normalizeAudio(audioData: ByteArray): ByteArray {
            try {
                val samples = ShortArray(audioData.size / 2)

                // Convert bytes to shorts
                for (i in samples.indices) {
                    samples[i] = ((audioData[i * 2].toInt() and 0xFF) or
                            (audioData[i * 2 + 1].toInt() shl 8)).toShort()
                }

                // Find max amplitude
                var maxAmplitude = 0
                for (sample in samples) {
                    maxAmplitude = maxOf(maxAmplitude, abs(sample.toInt()))
                }

                if (maxAmplitude == 0) {
                    return audioData
                }

                // Normalize
                val scaleFactor = 32767.0 / maxAmplitude
                val normalizedData = ByteArray(audioData.size)

                for (i in samples.indices) {
                    val normalizedSample = (samples[i] * scaleFactor).toInt().toShort()
                    normalizedData[i * 2] = (normalizedSample.toInt() and 0xFF).toByte()
                    normalizedData[i * 2 + 1] = (normalizedSample.toInt() shr 8).toByte()
                }

                return normalizedData

            } catch (e: Exception) {
                Log.e(TAG, "Error normalizing audio: ${e.message}")
                return audioData
            }
        }

        /**
         * Apply noise reduction (simple high-pass filter)
         */
        fun applyNoiseReduction(audioData: ByteArray): ByteArray {
            return try {
                val sampleCount = audioData.size / 2
                val signal = DoubleArray(sampleCount)

                // Convert byte to double PCM
                for (i in 0 until sampleCount) {
                    val low = audioData[2 * i].toInt() and 0xFF
                    val high = audioData[2 * i + 1].toInt()
                    val sample = (high shl 8) or low
                    signal[i] = sample.toShort() / 32768.0
                }

                // FFT size (next power of 2)
                val fftSize = 1 shl (ceil(log2(sampleCount.toDouble())).toInt())
                val paddedSignal = signal.copyOf(fftSize)

                // FFT
                val spectrum = fft(paddedSignal)

                val freqResolution = SAMPLE_RATE / fftSize.toDouble()
                val lowCut = 300
                val highCut = 3400

                val lowBin = (lowCut / freqResolution).toInt()
                val highBin = (highCut / freqResolution).toInt()

                // Apply band-pass: zero out all outside [lowBin..highBin]
                for (i in spectrum.indices) {
                    if (i < lowBin || i > highBin) {
                        spectrum[i] = Complex(0.0, 0.0)
                    }
                }

                // Inverse FFT
                val cleanedSignal = ifft(spectrum)

                // Back to PCM 16-bit
                val output = ByteArray(sampleCount * 2)
                for (i in 0 until sampleCount) {
                    val sample = (cleanedSignal[i].real * 32768.0).toInt().coerceIn(-32768, 32767).toShort()
                    output[2 * i] = (sample.toInt() and 0xFF).toByte()
                    output[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }

                output

            } catch (e: Exception) {
                Log.e(TAG, "FFT band-pass error: ${e.message}")
                audioData
            }
        }

        private fun ifft(input: Array<Complex>): Array<Complex> {
            val conjugated = input.map { Complex(it.real, -it.imag) }.toTypedArray()
            val fftResult = MFCCProcessor.fft(conjugated)
            return fftResult.map { Complex(it.real / input.size, -it.imag / input.size) }.toTypedArray()
        }
    }
}