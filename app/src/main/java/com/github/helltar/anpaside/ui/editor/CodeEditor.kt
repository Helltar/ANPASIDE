package com.github.helltar.anpaside.ui.editor

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.helltar.anpaside.ui.theme.LocalSyntaxColors
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

private const val INDENT = "    "
private val GUTTER_GAP = 8.dp

// a little air between the tabs and the first line so the code does not sit right against them
private val CONTENT_TOP = 6.dp

// code field with syntax highlighting and line numbers painted in a fixed left gutter.
// the field grows inside a scrolling column so the caret-move scroll and the gutter can be
// driven from the same vertical offset. with word wrap off the field also grows horizontally
// inside a horizontalScroll, while the gutter stays pinned to the left (drawn as an overlay,
// not inside the field, so it never scrolls sideways with the text)
@Composable
fun CodeEditor(
    file: OpenFile,
    fontSize: Int,
    highlighterEnabled: Boolean,
    lineNumbersEnabled: Boolean,
    wordWrapEnabled: Boolean,
    onSaveShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalSyntaxColors.current
    val density = LocalDensity.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.4f).sp,
        color = MaterialTheme.colorScheme.onSurface
    )

    val gutterPaint = remember(density, fontSize, syntaxColors.lineNumber) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = syntaxColors.lineNumber.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = with(density) { fontSize.sp.toPx() }
            typeface = Typeface.MONOSPACE
        }
    }

    val transformation = remember(highlighterEnabled, syntaxColors) {
        if (highlighterEnabled) PascalVisualTransformation(syntaxColors) else VisualTransformation.None
    }

    var editorLayout by remember { mutableStateOf<EditorLayout?>(null) }

    // the caret was moved from outside (a tap on a compiler error), the field itself does
    // not scroll, so the surrounding column has to be moved to the line the caret is on
    LaunchedEffect(file, file.caretRequest) {
        if (file.caretRequest == 0) {
            return@LaunchedEffect
        }

        val measured = snapshotFlow { editorLayout?.text }
            .filterNotNull()
            .filter { it.layoutInput.text.text == file.value.text }
            .first()

        val line = measured.getLineForOffset(file.value.selection.start)

        // a third of the viewport above it, a line pinned to the very top reads as if
        // the code before it were missing
        val target = measured.getLineTop(line) - verticalScroll.viewportSize / 3

        verticalScroll.animateScrollTo(target.toInt().coerceAtLeast(0))
    }

    val digits = remember(file.value.text) { (file.value.text.count { it == '\n' } + 1).toString().length }

    val gutterWidth =
        if (lineNumbersEnabled) {
            with(density) { gutterPaint.measureText("0".repeat(digits)).toDp() } + GUTTER_GAP
        } else {
            0.dp
        }

    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.surface)) {
        val viewportHeight = maxHeight
        val viewportPx = with(density) { viewportHeight.roundToPx() }
        val textMinWidth = (maxWidth - gutterWidth).coerceAtLeast(0.dp)

        Column(Modifier.verticalScroll(verticalScroll)) {
            // start padding reserves the gutter and stays outside the horizontal scroll, so the
            // text scrolls sideways within its own area and never slides under the line numbers
            val widthModifier =
                if (wordWrapEnabled) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.horizontalScroll(horizontalScroll).widthIn(min = textMinWidth)
                }

            BasicTextField(
                value = file.value,
                onValueChange = file::onValueChange,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                onTextLayout = {
                    editorLayout = EditorLayout(
                        text = it,
                        numberedVisualLines = if (lineNumbersEnabled) it.findNumberedVisualLines() else IntArray(0)
                    )
                },
                modifier = Modifier
                    .padding(start = gutterWidth)
                    .then(widthModifier)
                    .defaultMinSize(minHeight = viewportHeight)
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false

                            event.key == Key.Tab -> {
                                file.insert(INDENT)
                                true
                            }

                            event.key == Key.S && event.isCtrlPressed -> {
                                onSaveShortcut()
                                true
                            }

                            else -> false
                        }
                    }
                    .padding(top = CONTENT_TOP, end = 4.dp)
            )
        }

        if (lineNumbersEnabled) {
            val layout = editorLayout

            Spacer(
                Modifier
                    .fillMaxHeight()
                    .width(gutterWidth)
                    .drawBehind {
                        if (layout != null) {
                            drawLineNumbers(
                                layout = layout.text,
                                numberedVisualLines = layout.numberedVisualLines,
                                paint = gutterPaint,
                                gutterEnd = size.width - GUTTER_GAP.toPx(),
                                scrollOffset = verticalScroll.value,
                                viewportHeight = viewportPx,
                                topOffset = CONTENT_TOP.toPx()
                            )
                        }
                    }
            )
        }
    }
}

private data class EditorLayout(
    val text: TextLayoutResult,
    val numberedVisualLines: IntArray
)

// rebuilt only when text layout changes; scrolling can then skip straight to visible source lines.
private fun TextLayoutResult.findNumberedVisualLines(): IntArray {
    val text = layoutInput.text.text
    val result = IntArray(lineCount)
    var numberedLine = 0

    for (visualLine in 0 until lineCount) {
        val start = getLineStart(visualLine)

        if (visualLine == 0 || (start > 0 && text[start - 1] == '\n')) {
            result[numberedLine++] = visualLine
        }
    }

    return if (numberedLine == result.size) result else result.copyOf(numberedLine)
}

// the gutter is a fixed overlay in viewport space, so line positions are shifted up by the
// vertical scroll and down by the top inset the text field carries
private fun DrawScope.drawLineNumbers(
    layout: TextLayoutResult,
    numberedVisualLines: IntArray,
    paint: Paint,
    gutterEnd: Float,
    scrollOffset: Int,
    viewportHeight: Int,
    topOffset: Float
) {
    val firstVisible = layout.getLineForVerticalPosition(scrollOffset.toFloat())
    val lastVisible = layout.getLineForVerticalPosition((scrollOffset + viewportHeight).toFloat())
    val found = numberedVisualLines.binarySearch(firstVisible)
    var numberedLine = if (found >= 0) found else -found - 1

    drawIntoCanvas { canvas ->
        while (numberedLine < numberedVisualLines.size) {
            val visualLine = numberedVisualLines[numberedLine]

            if (visualLine > lastVisible) {
                break
            }

            canvas.nativeCanvas.drawText(
                (numberedLine + 1).toString(),
                gutterEnd,
                layout.getLineBaseline(visualLine) - scrollOffset + topOffset,
                paint
            )
            numberedLine++
        }
    }
}
