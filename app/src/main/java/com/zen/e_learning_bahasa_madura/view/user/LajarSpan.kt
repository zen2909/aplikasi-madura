package com.zen.e_learning_bahasa_madura.util

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class LajarSpan : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return 0
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val oldSize = paint.textSize
        val scale = 0.6f              // ukuran kecil
        val offsetY = oldSize * 0.7f  // naik ke atas
        val offsetX = oldSize * 0.55f  // geser ke kiri

        paint.textSize = oldSize * scale
        canvas.drawText("/", x - offsetX, y - offsetY, paint)
        paint.textSize = oldSize
    }
}
