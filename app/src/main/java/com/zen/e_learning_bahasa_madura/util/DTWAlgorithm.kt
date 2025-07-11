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
         * Hitung jarak DTW dengan windowing constraint (Sakoe-Chiba band)
         */
        fun calculateConstrainedDTWDistance(
            sequence1: Array<DoubleArray>,
            sequence2: Array<DoubleArray>,
            windowSize: Int = 50
        ): Double {
            if (sequence1.isEmpty() || sequence2.isEmpty()) {
                return Double.MAX_VALUE
            }

            val n = sequence1.size
            val m = sequence2.size
            val numFeatures = minOf(sequence1[0].size, sequence2[0].size)

            // Inisialisasi DTW matrix
            val dtwMatrix = Array(n + 1) { DoubleArray(m + 1) { Double.MAX_VALUE } }
            dtwMatrix[0][0] = 0.0

            // Isi DTW matrix dengan constraint
            for (i in 1..n) {
                val jStart = maxOf(1, i - windowSize)
                val jEnd = minOf(m, i + windowSize)

                for (j in jStart..jEnd) {
                    val cost = euclideanDistance(
                        sequence1[i - 1].sliceArray(0 until numFeatures),
                        sequence2[j - 1].sliceArray(0 until numFeatures)
                    )

                    val candidates = mutableListOf<Double>()

                    // Cek semua kemungkinan path yang valid
                    if (i > 1 && dtwMatrix[i - 1][j] != Double.MAX_VALUE) {
                        candidates.add(dtwMatrix[i - 1][j])
                    }
                    if (j > 1 && dtwMatrix[i][j - 1] != Double.MAX_VALUE) {
                        candidates.add(dtwMatrix[i][j - 1])
                    }
                    if (i > 1 && j > 1 && dtwMatrix[i - 1][j - 1] != Double.MAX_VALUE) {
                        candidates.add(dtwMatrix[i - 1][j - 1])
                    }

                    if (candidates.isNotEmpty()) {
                        dtwMatrix[i][j] = cost + candidates.minOrNull()!!
                    }
                }
            }

            return dtwMatrix[n][m]
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
                val maxDistance = 50.0 // Threshold untuk distance maksimum
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

        /**
         * Hitung jarak Manhattan antara dua vektor fitur
         */
        private fun manhattanDistance(vector1: DoubleArray, vector2: DoubleArray): Double {
            if (vector1.size != vector2.size) {
                return Double.MAX_VALUE
            }

            var sum = 0.0
            for (i in vector1.indices) {
                sum += abs(vector1[i] - vector2[i])
            }

            return sum
        }

        /**
         * Hitung jarak Cosine antara dua vektor fitur
         */
        private fun cosineDistance(vector1: DoubleArray, vector2: DoubleArray): Double {
            if (vector1.size != vector2.size) {
                return Double.MAX_VALUE
            }

            var dotProduct = 0.0
            var norm1 = 0.0
            var norm2 = 0.0

            for (i in vector1.indices) {
                dotProduct += vector1[i] * vector2[i]
                norm1 += vector1[i] * vector1[i]
                norm2 += vector2[i] * vector2[i]
            }

            norm1 = sqrt(norm1)
            norm2 = sqrt(norm2)

            if (norm1 == 0.0 || norm2 == 0.0) {
                return Double.MAX_VALUE
            }

            val cosineSimilarity = dotProduct / (norm1 * norm2)
            return 1.0 - cosineSimilarity
        }

        /**
         * Implementasi alternatif dengan weighted DTW
         */
        fun calculateWeightedDTWDistance(
            sequence1: Array<DoubleArray>,
            sequence2: Array<DoubleArray>,
            weights: DoubleArray = doubleArrayOf(1.0, 1.0, 2.0) // insertion, deletion, match
        ): Double {
            if (sequence1.isEmpty() || sequence2.isEmpty()) {
                return Double.MAX_VALUE
            }

            val n = sequence1.size
            val m = sequence2.size
            val numFeatures = minOf(sequence1[0].size, sequence2[0].size)

            val dtwMatrix = Array(n + 1) { DoubleArray(m + 1) { Double.MAX_VALUE } }
            dtwMatrix[0][0] = 0.0

            for (i in 1..n) {
                for (j in 1..m) {
                    val cost = euclideanDistance(
                        sequence1[i - 1].sliceArray(0 until numFeatures),
                        sequence2[j - 1].sliceArray(0 until numFeatures)
                    )

                    val candidates = listOf(
                        dtwMatrix[i - 1][j] + cost * weights[0],     // insertion
                        dtwMatrix[i][j - 1] + cost * weights[1],     // deletion
                        dtwMatrix[i - 1][j - 1] + cost * weights[2]  // match
                    )

                    dtwMatrix[i][j] = candidates.minOrNull() ?: Double.MAX_VALUE
                }
            }

            return dtwMatrix[n][m]
        }

        /**
         * Debugging function untuk melihat path DTW
         */
        fun getDTWPath(
            sequence1: Array<DoubleArray>,
            sequence2: Array<DoubleArray>
        ): List<Pair<Int, Int>> {
            if (sequence1.isEmpty() || sequence2.isEmpty()) {
                return emptyList()
            }

            val n = sequence1.size
            val m = sequence2.size
            val numFeatures = minOf(sequence1[0].size, sequence2[0].size)

            val dtwMatrix = Array(n + 1) { DoubleArray(m + 1) { Double.MAX_VALUE } }
            dtwMatrix[0][0] = 0.0

            // Fill DTW matrix
            for (i in 1..n) {
                for (j in 1..m) {
                    val cost = euclideanDistance(
                        sequence1[i - 1].sliceArray(0 until numFeatures),
                        sequence2[j - 1].sliceArray(0 until numFeatures)
                    )

                    val minPrevious = minOf(
                        dtwMatrix[i - 1][j],
                        dtwMatrix[i][j - 1],
                        dtwMatrix[i - 1][j - 1]
                    )

                    dtwMatrix[i][j] = cost + minPrevious
                }
            }

            // Backtrack to find path
            val path = mutableListOf<Pair<Int, Int>>()
            var i = n
            var j = m

            while (i > 0 && j > 0) {
                path.add(Pair(i - 1, j - 1))

                val candidates = listOf(
                    Triple(i - 1, j - 1, dtwMatrix[i - 1][j - 1]),
                    Triple(i - 1, j, dtwMatrix[i - 1][j]),
                    Triple(i, j - 1, dtwMatrix[i][j - 1])
                )

                val best = candidates.minByOrNull { it.third }
                if (best != null) {
                    i = best.first
                    j = best.second
                }
            }

            return path.reversed()
        }
    }
}