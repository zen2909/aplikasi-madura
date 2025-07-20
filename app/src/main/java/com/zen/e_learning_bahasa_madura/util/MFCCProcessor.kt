package com.zen.e_learning_bahasa_madura.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.*

class MFCCProcessor {

    companion object {
        private const val TAG = "MFCCProcessor"
        private const val SAMPLE_RATE = 16000
        private const val FFT_SIZE = 512
        private const val HOP_SIZE = 256
        private const val N_MFCC = 13
        private const val N_FILTER = 26
        private const val LOW_FREQ = 0.0
        private const val HIGH_FREQ = SAMPLE_RATE / 2.0

        /**
         * Ekstraksi fitur MFCC dari file audio WAV
         */
        fun extractMFCCFromFile(audioFile: File): Array<DoubleArray> {
            return try {
                val audioData = readWavFile(audioFile)
                Log.d(TAG, "Audio data length: ${audioData.size}")

                if (audioData.isEmpty()) {
                    Log.e(TAG, "Audio data is empty")
                    return arrayOf()
                }

                extractMFCC(audioData)
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting MFCC: ${e.message}")
                arrayOf()
            }
        }

        /**
         * Ekstraksi fitur MFCC dari array audio
         */
        fun extractMFCC(audioData: DoubleArray): Array<DoubleArray> {
            try {
                // Pre-emphasis filter
                val preEmphasized = preEmphasis(audioData)

                // Windowing dan FFT
                val frames = frameSignal(preEmphasized, FFT_SIZE, HOP_SIZE)
                Log.d(TAG, "Number of frames: ${frames.size}")

                if (frames.isEmpty()) {
                    return arrayOf()
                }

                // Mel filter bank
                val melFilterBank = createMelFilterBank(N_FILTER, FFT_SIZE, SAMPLE_RATE)

                val mfccFeatures = mutableListOf<DoubleArray>()

                for (frame in frames) {
                    // Apply window function (Hamming)
                    val windowedFrame = applyHammingWindow(frame)

                    // FFT
                    val fftResult = fft(windowedFrame)
                    val powerSpectrum = getPowerSpectrum(fftResult)

                    // Apply mel filter bank
                    val melSpectrum = applyMelFilterBank(powerSpectrum, melFilterBank)

                    // Log
                    val logMelSpectrum = melSpectrum.map { ln(maxOf(it, 1e-10)) }.toDoubleArray()

                    // DCT to get MFCC
                    val mfcc = dct(logMelSpectrum, N_MFCC)
                    mfccFeatures.add(mfcc)
                }

                Log.d(TAG, "MFCC features extracted: ${mfccFeatures.size} frames, ${N_MFCC} coefficients")
                return mfccFeatures.toTypedArray()

            } catch (e: Exception) {
                Log.e(TAG, "Error in MFCC extraction: ${e.message}")
                return arrayOf()
            }
        }

        /**
         * Membaca file WAV dan konversi ke array double
         */
        private fun readWavFile(file: File): DoubleArray {
            try {
                val inputStream = FileInputStream(file)
                val bytes = inputStream.readBytes()
                inputStream.close()

                // Skip WAV header (44 bytes)
                val headerSize = 44
                if (bytes.size <= headerSize) {
                    Log.e(TAG, "File too small or invalid WAV format")
                    return doubleArrayOf()
                }

                val audioBytes = bytes.sliceArray(headerSize until bytes.size)
                val audioData = DoubleArray(audioBytes.size / 2)

                // Convert 16-bit PCM to double
                for (i in audioData.indices) {
                    val sample = (audioBytes[i * 2].toInt() and 0xFF) or
                            (audioBytes[i * 2 + 1].toInt() shl 8)
                    audioData[i] = sample.toShort().toDouble() / 32768.0
                }

                return audioData
            } catch (e: Exception) {
                Log.e(TAG, "Error reading WAV file: ${e.message}")
                return doubleArrayOf()
            }
        }

        /**
         * Pre-emphasis filter
         */
        private fun preEmphasis(signal: DoubleArray, alpha: Double = 0.97): DoubleArray {
            val result = DoubleArray(signal.size)
            result[0] = signal[0]
            for (i in 1 until signal.size) {
                result[i] = signal[i] - alpha * signal[i - 1]
            }
            return result
        }

        /**
         * Frame signal into overlapping windows
         */
        private fun frameSignal(signal: DoubleArray, frameSize: Int, hopSize: Int): List<DoubleArray> {
            val frames = mutableListOf<DoubleArray>()
            var start = 0

            while (start + frameSize <= signal.size) {
                val frame = signal.sliceArray(start until start + frameSize)
                frames.add(frame)
                start += hopSize
            }

            return frames
        }

        /**
         * Apply Hamming window
         */
        private fun applyHammingWindow(frame: DoubleArray): DoubleArray {
            val windowed = DoubleArray(frame.size)
            for (i in frame.indices) {
                val w = 0.54 - 0.46 * cos(2 * PI * i / (frame.size - 1))
                windowed[i] = frame[i] * w
            }
            return windowed
        }

        /**
         * Simple FFT implementation
         */
        fun fft(signal: DoubleArray): Array<Complex> {
            val n = signal.size
            val result = Array(n) { Complex(signal[it], 0.0) }

            if (n <= 1) return result

            // Bit-reversal permutation
            val j = IntArray(n)
            j[0] = 0
            for (i in 1 until n) {
                var bit = n shr 1
                while (j[i - 1] and bit != 0) {
                    j[i - 1] = j[i - 1] xor bit
                    bit = bit shr 1
                }
                j[i] = j[i - 1] xor bit
            }

            for (i in 0 until n) {
                if (i < j[i]) {
                    val temp = result[i]
                    result[i] = result[j[i]]
                    result[j[i]] = temp
                }
            }

            // Cooley-Tukey FFT
            var length = 2
            while (length <= n) {
                val wlen = -2 * PI / length
                val wlenComplex = Complex(cos(wlen), sin(wlen))

                var i = 0
                while (i < n) {
                    var w = Complex(1.0, 0.0)
                    for (j in 0 until length / 2) {
                        val u = result[i + j]
                        val v = result[i + j + length / 2].multiply(w)
                        result[i + j] = u.add(v)
                        result[i + j + length / 2] = u.subtract(v)
                        w = w.multiply(wlenComplex)
                    }
                    i += length
                }
                length *= 2
            }

            return result
        }

        fun fft(input: Array<Complex>): Array<Complex> {
            val n = input.size
            if (n <= 1) return input

            // Bit-reversal permutation
            val result = input.copyOf()
            val j = IntArray(n)
            j[0] = 0
            for (i in 1 until n) {
                var bit = n shr 1
                var idx = j[i - 1]
                while (idx and bit != 0) {
                    idx = idx xor bit
                    bit = bit shr 1
                }
                j[i] = idx xor bit
            }

            for (i in 0 until n) {
                if (i < j[i]) {
                    val temp = result[i]
                    result[i] = result[j[i]]
                    result[j[i]] = temp
                }
            }

            // Cooley-Tukey FFT
            var len = 2
            while (len <= n) {
                val angle = -2.0 * PI / len
                val wLen = Complex(cos(angle), sin(angle))

                var i = 0
                while (i < n) {
                    var w = Complex(1.0, 0.0)
                    for (j in 0 until len / 2) {
                        val u = result[i + j]
                        val v = result[i + j + len / 2].multiply(w)
                        result[i + j] = u.add(v)
                        result[i + j + len / 2] = u.subtract(v)
                        w = w.multiply(wLen)
                    }
                    i += len
                }
                len *= 2
            }

            return result
        }


        /**
         * Get power spectrum from FFT result
         */
        private fun getPowerSpectrum(fftResult: Array<Complex>): DoubleArray {
            val powerSpectrum = DoubleArray(fftResult.size / 2 + 1)
            for (i in powerSpectrum.indices) {
                powerSpectrum[i] = fftResult[i].magnitude().pow(2)
            }
            return powerSpectrum
        }

        /**
         * Create Mel filter bank
         */
        private fun createMelFilterBank(nFilters: Int, fftSize: Int, sampleRate: Int): Array<DoubleArray> {
            val melFilterBank = Array(nFilters) { DoubleArray(fftSize / 2 + 1) }

            // Convert to mel scale
            val melLow = hzToMel(LOW_FREQ)
            val melHigh = hzToMel(HIGH_FREQ)

            // Create mel points
            val melPoints = DoubleArray(nFilters + 2)
            for (i in melPoints.indices) {
                melPoints[i] = melLow + (melHigh - melLow) * i / (nFilters + 1)
            }

            // Convert back to Hz
            val hzPoints = melPoints.map { melToHz(it) }.toDoubleArray()

            // Convert to FFT bin numbers
            val binPoints = hzPoints.map { (it * (fftSize + 1) / sampleRate).toInt() }.toIntArray()

            // Create filters
            for (i in 0 until nFilters) {
                for (j in binPoints[i] until binPoints[i + 1]) {
                    melFilterBank[i][j] = (j - binPoints[i]).toDouble() / (binPoints[i + 1] - binPoints[i])
                }
                for (j in binPoints[i + 1] until binPoints[i + 2]) {
                    melFilterBank[i][j] = (binPoints[i + 2] - j).toDouble() / (binPoints[i + 2] - binPoints[i + 1])
                }
            }

            return melFilterBank
        }

        /**
         * Apply mel filter bank
         */
        private fun applyMelFilterBank(powerSpectrum: DoubleArray, melFilterBank: Array<DoubleArray>): DoubleArray {
            val melSpectrum = DoubleArray(melFilterBank.size)
            for (i in melFilterBank.indices) {
                var sum = 0.0
                for (j in powerSpectrum.indices) {
                    sum += powerSpectrum[j] * melFilterBank[i][j]
                }
                melSpectrum[i] = sum
            }
            return melSpectrum
        }

        /**
         * Discrete Cosine Transform
         */
        private fun dct(input: DoubleArray, numCoeffs: Int): DoubleArray {
            val output = DoubleArray(numCoeffs)
            for (i in 0 until numCoeffs) {
                var sum = 0.0
                for (j in input.indices) {
                    sum += input[j] * cos(PI * i * (j + 0.5) / input.size)
                }
                output[i] = sum
            }
            return output
        }

        /**
         * Convert Hz to Mel scale
         */
        private fun hzToMel(hz: Double): Double {
            return 2595 * log10(1 + hz / 700)
        }

        /**
         * Convert Mel to Hz scale
         */
        private fun melToHz(mel: Double): Double {
            return 700 * (10.0.pow(mel / 2595) - 1)
        }
    }

    /**
     * Complex number class for FFT
     */
    data class Complex(val real: Double, val imag: Double) {
        fun add(other: Complex): Complex {
            return Complex(real + other.real, imag + other.imag)
        }

        fun subtract(other: Complex): Complex {
            return Complex(real - other.real, imag - other.imag)
        }

        fun multiply(other: Complex): Complex {
            return Complex(
                real * other.real - imag * other.imag,
                real * other.imag + imag * other.real
            )
        }

        fun magnitude(): Double {
            return sqrt(real * real + imag * imag)
        }

        fun ifft(input: Array<Complex>): Array<Complex> {
            val conjugated = input.map { Complex(it.real, -it.imag) }.toTypedArray()
            val fftResult = MFCCProcessor.fft(conjugated)
            return fftResult.map { Complex(it.real / input.size, -it.imag / input.size) }.toTypedArray()
        }

    }
}