package com.sesmartcell.notaservice.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Efek klik untuk semua tombol: saat ditekan, elemen sedikit mengecil
 * (scale) selain ripple bawaan Material, sehingga pengguna mendapat
 * feedback visual yang jelas setiap kali tombol disentuh.
 */
fun Modifier.pressClickScale(pressedScale: Float = 0.95f): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }

    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
        .graphicsLayer {
            val scale = if (isPressed) pressedScale else 1f
            scaleX = scale
            scaleY = scale
        }
}
