package com.zen.e_learning_bahasa_madura.util

import android.util.Log
import kotlin.math.*

class DTWAlgorithm {

    companion object {
        private const val TAG = "DTWAlgorithm"

        /**
         * Hitung jarak DTW antara dua sequence fitur MFCC
         */
        fun calculateDTWDistance(
            sequence1: Array<DoubleArray>,
            sequence2: Array<DoubleArray>
        ): Double {
            if (sequence1.isEmpty() || sequence2.isEmpty()) {
                Log.e(TAG, "One or both sequences are empty")
                return Double.MAX_VALUE
            }

            val n = sequence1.size
            val m = sequence2.size
            val numFeatures = minOf(
                sequence1[0].size,
                sequence2[0].size
            )

            Log.d(TAG, "DTW calculation: seq1=$n frames, seq2=$m frames, features=$numFeatures")

            // Inisialisasi DTW matrix
            val dtwMatrix = Array(n + 1) { DoubleArray(m + 1) { Double.MAX_VALUE } }
            dtwMatrix[0][0] = 0.0

            // Isi DTW matrix
            for (i in 1..n) {
                for (j in 1..m) {
                    val cost = euclideanDistance(
                        sequence1[i - 1].sliceArray(0 until numFeatures),
                        sequence2[j - 1].sliceArray(0 until numFeatures)
                    )

                    val minPrevious = minOf(
                        dtwMatrix[i - 1][j],     // insertion
                        dtwMatrix[i][j - 1],     // deletion
                        dtwMatrix[i - 1][j - 1]  // match
                    )

                    dtwMatrix[i][j] = cost + minPrevious
                }
            }

            val distance = dtwMatrix[n][m]
            Log.d(TAG, "DTW distance: $distance")
            return distance
        }

        /**
         * Hitung jarak DTW dengan constraint path dan normalisasi
         */
        fun calculateNormalizedDTWDistance(
            sequence1: Array<DoubleArray>,
            sequence2: Array<DoubleArray>
        ): Double {
            if (sequence1.isEmpty() || sequence2.isEmpty()) {
                return Double.MAX_VALUE
            }

            val rawDistance = calculateDTWDistance(sequence1, sequence2)
            val pathLength = sequence1.size + sequence2.size

            // Normalisasi berdasarkan panjang path
            val normalizedDistance = rawDistance / pathLength

            Log.d(TAG, "Normalized DTW distance: $normalizedDistance")
            return normalizedDistance
        }

        /**
         * Hitung similarity score dari DTW distance (0-100)
         */
        fun calculateSimilarityScore(
            sequence1: Array<DoubleArray>,
            sequence2: Array<DoubleArray>
        ): Int {
            try {
                if (sequence1.isEmpty() || sequence2.isEmpty()) {
                    Log.e(TAG, "Empty sequences for similarity calculation")
                    return 0
                }

                val normalizedDistance = calculateNormalizedDTWDistance(sequence1, sequence2)

                // Konversi distance ke similarity score (0-100)
                // Semakin kecil distance, semakin tinggi similarity
                val maxDistance = 10.0 // Threshold untuk distance maksimum
                val similarity = maxOf(0.0, 100.0 - (normalizedDistance / maxDistance) * 100.0)

                val score = similarity.roundToInt()
                Log.d(TAG, "Similarity score: $score (distance: $normalizedDistance)")

                return minOf(100, maxOf(0, score))

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating similarity score: ${e.message}")
                return 0
            }
        }

        /**
         * Hitung jarak Euclidean antara dua vektor fitur
         */
        private fun euclideanDistance(vector1: DoubleArray, vector2: DoubleArray): Double {
            if (vector1.size != vector2.size) {
                Log.w(TAG, "Vector sizes don't match: ${vector1.size} vs ${vector2.size}")
                return Double.MAX_VALUE
            }

            var sum = 0.0
            for (i in vector1.indices) {
                val diff = vector1[i] - vector2[i]
                sum += diff * diff
            }

            return sqrt(sum)
        }
    }
}