/*
 * Copyright (C) 2018 Florian Dreier
 *
 * This file is part of MyTargets.
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package de.dreier.mytargets.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import de.dreier.mytargets.R

class CustomSignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var signaturePath = Path()
    private var signaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.md_black_1000)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isEmptyState = true

    var signatureBitmap: Bitmap? = null
        set(value) {
            field = value
            isEmptyState = value == null
            invalidate()
        }

    val isEmpty: Boolean
        get() = isEmptyState && signaturePath.isEmpty

    val transparentSignatureBitmap: Bitmap
        get() {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Draw signature path
            if (!signaturePath.isEmpty) {
                canvas.drawPath(signaturePath, signaturePaint)
            }
            
            // Draw existing signature bitmap if present
            signatureBitmap?.let { existingBitmap ->
                canvas.drawBitmap(existingBitmap, 0f, 0f, null)
            }
            
            return bitmap
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw existing signature bitmap if present
        signatureBitmap?.let { bitmap ->
            val matrix = Matrix()
            matrix.setRectToRect(
                RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                Matrix.ScaleToFit.CENTER
            )
            canvas.drawBitmap(bitmap, matrix, null)
        }
        
        // Draw current signature path
        if (!signaturePath.isEmpty) {
            canvas.drawPath(signaturePath, signaturePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                signaturePath.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
                isEmptyState = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                signaturePath.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                signaturePath.lineTo(lastTouchX, lastTouchY)
                invalidate()
                return true
            }
        }
        return false
    }

    fun clear() {
        signaturePath.reset()
        signatureBitmap = null
        isEmptyState = true
        invalidate()
    }

    fun setPenColor(color: Int) {
        signaturePaint.color = color
        invalidate()
    }
} 