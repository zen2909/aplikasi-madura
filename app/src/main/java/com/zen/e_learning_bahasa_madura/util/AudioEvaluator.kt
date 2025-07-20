package com.zen.e_learning_bahasa_madura.util

import android.util.Log
import java.io.File
import kotlin.math.*

class AudioEvaluator {

    companion object {
        private const val TAG = "AudioEvaluator"

        /**
         * Ekstraksi fitur MFCC dari file audio
         */
        fun extractMFCCFromFile(audioFile: File): Array<DoubleArray> {
            return try {
                Log.d(TAG, "Extracting MFCC from file: ${audioFile.absolutePath}")

                if (!audioFile.exists()) {
                    Log.e(TAG, "Audio file does not exist: ${audioFile.absolutePath}")
                    return arrayOf()
                }

                val mfccFeatures = MFCCProcessor.extractMFCCFromFile(audioFile)
                Log.d(TAG, "MFCC extraction completed. Features: ${mfccFeatures.size} frames")

                mfccFeatures
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting MFCC: ${e.message}")
                e.printStackTrace()
                arrayOf()
            }
        }

        /**
         * Hitung similarity menggunakan DTW
         */
        fun calculateSimilarity(
            userFeatures: Array<DoubleArray>,
            referenceFeatures: Array<DoubleArray>
        ): Int {
            return try {
                Log.d(TAG, "Calculating similarity using DTW")
                Log.d(TAG, "User features: ${userFeatures.size} frames")
                Log.d(TAG, "Reference features: ${referenceFeatures.size} frames")

                if (userFeatures.isEmpty() || referenceFeatures.isEmpty()) {
                    Log.e(TAG, "Empty features detected")
                    return 0
                }

                // Validasi dimensi fitur
                if (userFeatures.isNotEmpty() && referenceFeatures.isNotEmpty()) {
                    val userDim = userFeatures[0].size
                    val refDim = referenceFeatures[0].size
                    Log.d(TAG, "Feature dimensions - User: $userDim, Reference: $refDim")

                    if (userDim == 0 || refDim == 0) {
                        Log.e(TAG, "Invalid feature dimensions")
                        return 0
                    }
                }

                // Hitung similarity score menggunakan DTW
                val similarityScore = DTWAlgorithm.calculateSimilarityScore(
                    userFeatures,
                    referenceFeatures
                )

                Log.d(TAG, "Final similarity score: $similarityScore")
                return similarityScore

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating similarity: ${e.message}")
                e.printStackTrace()
                0
            }
        }

        /**
         * Hitung similarity dengan metode alternatif (backup)
         */
        fun calculateAlternativeSimilarity(
            userFeatures: Array<DoubleArray>,
            referenceFeatures: Array<DoubleArray>
        ): Int {
            return try {
                if (userFeatures.isEmpty() || referenceFeatures.isEmpty()) {
                    return 0
                }

                // Metode sederhana: bandingkan rata-rata fitur
                val userMean = calculateMeanFeatures(userFeatures)
                val refMean = calculateMeanFeatures(referenceFeatures)

                val distance = euclideanDistance(userMean, refMean)
                val similarity = maxOf(0.0, 100.0 - distance * 10.0)

                similarity.roundToInt()

            } catch (e: Exception) {
                Log.e(TAG, "Error in alternative similarity calculation: ${e.message}")
                0
            }
        }

        /**
         * Hitung rata-rata fitur dari semua frame
         */
        private fun calculateMeanFeatures(features: Array<DoubleArray>): DoubleArray {
            if (features.isEmpty()) return doubleArrayOf()

            val numFeatures = features[0].size
            val meanFeatures = DoubleArray(numFeatures)

            for (frame in features) {
                for (i in 0 until minOf(numFeatures, frame.size)) {
                    meanFeatures[i] += frame[i]
                }
            }

            for (i in meanFeatures.indices) {
                meanFeatures[i] /= features.size
            }

            return meanFeatures
        }

        /**
         * Hitung jarak Euclidean antara dua vektor
         */
        private fun euclideanDistance(vector1: DoubleArray, vector2: DoubleArray): Double {
            if (vector1.size != vector2.size) {
                return Double.MAX_VALUE
            }

            var sum = 0.0
            for (i in vector1.indices) {
                val diff = vector1[i] - vector2[i]
                sum += diff * diff
            }

            return sqrt(sum)
        }

        /**
         * Generate feedback dalam bahasa Indonesia
         */
        fun getFeedbackIndonesian(score: Int): String {
            return when {
                score >= 90 -> "Sempurna! Pelafalan sangat akurat."
                score >= 80 -> "Sangat Baik! Sedikit perbaikan diperlukan."
                score >= 70 -> "Baik! Pelafalan Anda cukup jelas."
                score >= 60 -> "Cukup. Cobalah untuk melafalkan lebih jelas."
                score >= 50 -> "Perlu perbaikan. Latihan lebih banyak."
                score >= 30 -> "Kurang baik. Fokus pada artikulasi yang benar."
                else -> "Sangat kurang. Silakan dengarkan dengan seksama dan coba lagi."
            }
        }

        /**
         * Evaluasi komprehensif dengan multiple metrics
         */
        fun comprehensiveEvaluation(
            userFeatures: Array<DoubleArray>,
            referenceFeatures: Array<DoubleArray>
        ): EvaluationResult {
            return try {
                val dtwScore = calculateSimilarity(userFeatures, referenceFeatures)
                val alternativeScore = calculateAlternativeSimilarity(userFeatures, referenceFeatures)

                // Weighted average of different methods
                val finalScore = (dtwScore * 0.8 + alternativeScore * 0.2).roundToInt()

                val confidence = calculateConfidence(userFeatures, referenceFeatures)
                val feedback = getFeedbackIndonesian(finalScore)

                EvaluationResult(
                    score = finalScore,
                    confidence = confidence,
                    feedback = feedback,
                    dtwScore = dtwScore,
                    alternativeScore = alternativeScore
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in comprehensive evaluation: ${e.message}")
                EvaluationResult(
                    score = 0,
                    confidence = 0.0,
                    feedback = "Error dalam evaluasi. Silakan coba lagi.",
                    dtwScore = 0,
                    alternativeScore = 0
                )
            }
        }

        /**
         * Hitung confidence level dari evaluasi
         */
        private fun calculateConfidence(
            userFeatures: Array<DoubleArray>,
            referenceFeatures: Array<DoubleArray>
        ): Double {
            return try {
                if (userFeatures.isEmpty() || referenceFeatures.isEmpty()) {
                    return 0.0
                }

                val frameRatio = minOf(userFeatures.size, referenceFeatures.size).toDouble() /
                        maxOf(userFeatures.size, referenceFeatures.size).toDouble()

                val featureQuality = if (userFeatures.isNotEmpty() && referenceFeatures.isNotEmpty()) {
                    val userVariance = calculateVariance(userFeatures)
                    val refVariance = calculateVariance(referenceFeatures)
                    val varianceRatio = minOf(userVariance, refVariance) / maxOf(userVariance, refVariance)
                    varianceRatio
                } else {
                    0.0
                }

                // Confidence berdasarkan kualitas data
                val confidence = (frameRatio * 0.6 + featureQuality * 0.4) * 100.0

                minOf(100.0, maxOf(0.0, confidence))

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating confidence: ${e.message}")
                0.0
            }
        }

        /**
         * Hitung variance dari fitur untuk quality assessment
         */
        private fun calculateVariance(features: Array<DoubleArray>): Double {
            if (features.isEmpty()) return 0.0

            val meanFeatures = calculateMeanFeatures(features)
            var variance = 0.0

            for (frame in features) {
                for (i in 0 until minOf(meanFeatures.size, frame.size)) {
                    val diff = frame[i] - meanFeatures[i]
                    variance += diff * diff
                }
            }

            return variance / (features.size * meanFeatures.size)
        }

        /**
         * Preprocessing fitur untuk normalisasi
         */
        fun preprocessFeatures(features: Array<DoubleArray>): Array<DoubleArray> {
            if (features.isEmpty()) return features

            val numFeatures = features[0].size
            val means = DoubleArray(numFeatures)
            val stds = DoubleArray(numFeatures)

            // Hitung mean
            for (frame in features) {
                for (i in 0 until minOf(numFeatures, frame.size)) {
                    means[i] += frame[i]
                }
            }
            for (i in means.indices) {
                means[i] /= features.size
            }

            // Hitung standard deviation
            for (frame in features) {
                for (i in 0 until minOf(numFeatures, frame.size)) {
                    val diff = frame[i] - means[i]
                    stds[i] += diff * diff
                }
            }
            for (i in stds.indices) {
                stds[i] = sqrt(stds[i] / features.size)
            }

            // Normalisasi Z-score
            val normalizedFeatures = Array(features.size) { DoubleArray(numFeatures) }
            for (i in features.indices) {
                for (j in 0 until minOf(numFeatures, features[i].size)) {
                    normalizedFeatures[i][j] = if (stds[j] != 0.0) {
                        (features[i][j] - means[j]) / stds[j]
                    } else {
                        features[i][j] - means[j]
                    }
                }
            }

            return normalizedFeatures
        }
    }

    /**
     * Data class untuk hasil evaluasi komprehensif
     */
    data class EvaluationResult(
        val score: Int,
        val confidence: Double,
        val feedback: String,
        val dtwScore: Int,
        val alternativeScore: Int
    )
}