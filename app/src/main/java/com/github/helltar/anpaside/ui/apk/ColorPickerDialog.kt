package com.github.helltar.anpaside.ui.apk

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.HexColor

private const val HUE_MAX = 360f

private val hueColors =
    listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hue -> Color(hsvColor(hue, 1f, 1f)) }

/**
 * Picks the flat colour behind an exported midlet's icon.
 *
 * HSV rather than three RGB sliders, because the tile is chosen by eye against a sprite: a hue
 * strip plus a saturation/value square is how that is done everywhere else, and both are a couple
 * of gradients on a [Canvas], so nothing has to be added to the project to draw them. The result
 * is always opaque - a launcher icon layer that lets the background through only looks broken.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = rememberSaveable(initialColor) { hsvOf(initialColor) }

    var hue by rememberSaveable(initialColor) { mutableFloatStateOf(initial[0]) }
    var saturation by rememberSaveable(initialColor) { mutableFloatStateOf(initial[1]) }
    var value by rememberSaveable(initialColor) { mutableFloatStateOf(initial[2]) }

    val color = hsvColor(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.text_apk_pick_colour)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(Color(color), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )

                    Text(
                        text = HexColor.format(color),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                SaturationValueArea(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { pickedSaturation, pickedValue ->
                        saturation = pickedSaturation
                        value = pickedValue
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )

                HueStrip(
                    hue = hue,
                    onChange = { picked -> hue = picked },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(color) }) {
                Text(stringResource(R.string.dlg_btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dlg_btn_cancel))
            }
        }
    )
}

@Composable
private fun SaturationValueArea(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(8.dp))
            .pickPosition { position, size ->
                onChange(
                    (position.x / size.width).coerceIn(0f, 1f),
                    1f - (position.y / size.height).coerceIn(0f, 1f)
                )
            }
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color(hsvColor(hue, 1f, 1f)))))
        // value drawn over saturation: the two gradients together are the usual picker square
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        drawMarker(Offset(saturation * size.width, (1f - value) * size.height))
    }
}

@Composable
private fun HueStrip(hue: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .pickPosition { position, size ->
                onChange((position.x / size.width).coerceIn(0f, 1f) * HUE_MAX)
            }
    ) {
        drawRect(Brush.horizontalGradient(hueColors))
        drawMarker(Offset(hue / HUE_MAX * size.width, size.height / 2f))
    }
}

// one gesture for the press and the drag that follows it: a tap has to move the marker without
// waiting for the drag slop, which detectDragGestures on its own would swallow
private fun Modifier.pickPosition(onPosition: (Offset, IntSize) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            onPosition(down.position, size)
            drag(down.id) { change -> onPosition(change.position, size) }
        }
    }

// a two colour ring, so the marker stays visible over both the white and the black corner
private fun DrawScope.drawMarker(center: Offset) {
    val stroke = Stroke(width = 1.dp.toPx())

    drawCircle(Color.White, radius = 8.dp.toPx(), center = center, style = stroke)
    drawCircle(Color.Black, radius = 9.dp.toPx(), center = center, style = stroke)
}

private fun hsvOf(color: Int): FloatArray =
    FloatArray(3).also { hsv -> AndroidColor.colorToHSV(color, hsv) }

private fun hsvColor(hue: Float, saturation: Float, value: Float): Int =
    AndroidColor.HSVToColor(
        floatArrayOf(hue.coerceIn(0f, HUE_MAX), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    )
