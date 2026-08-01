package com.github.helltar.anpaside.ui.editor

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.helltar.anpaside.ui.theme.LocalSyntaxColors
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

private const val INDENT = "    "
private val GutterGap = 6.dp
private val FoldGutterWidth = 20.dp

// a little air between the tabs and the first line so the code does not sit right against them
private val ContentTop = 6.dp

// code field with syntax highlighting, line numbers and folding in a fixed left gutter.
// the field grows inside a scrolling column so the caret-move scroll and the gutter can be
// driven from the same vertical offset. with word wrap off the field also grows horizontally
// inside a horizontalScroll, while the gutter stays pinned to the left (drawn as an overlay,
// not inside the field, so it never scrolls sideways with the text)
@Composable
fun CodeEditor(
    file: EditorDocument,
    fontSize: Int,
    highlighterEnabled: Boolean,
    lineNumbersEnabled: Boolean,
    wordWrapEnabled: Boolean,
    onSaveShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalSyntaxColors.current
    val density = LocalDensity.current
    val verticalScroll = remember(file) { ScrollState(file.verticalScrollOffset) }
    val horizontalScroll = remember(file) { ScrollState(file.horizontalScrollOffset) }
    val foldBlocks = remember(file.value.text) { PascalFolding.findBlocks(file.value.text) }
    val activeFoldBlocks = remember(foldBlocks, file.collapsedFoldStarts) {
        PascalFolding.activeBlocks(foldBlocks, file.collapsedFoldStarts)
    }
    val visibleFoldBlocks = remember(foldBlocks, activeFoldBlocks) {
        PascalFolding.visibleBlocks(foldBlocks, activeFoldBlocks)
    }

    LaunchedEffect(file, foldBlocks) {
        file.retainFoldStarts(foldBlocks.mapTo(mutableSetOf(), PascalFoldBlock::startOffset))
    }

    LaunchedEffect(file, verticalScroll) {
        snapshotFlow { verticalScroll.value }.collect(file::updateVerticalScroll)
    }

    LaunchedEffect(file, horizontalScroll) {
        snapshotFlow { horizontalScroll.value }.collect(file::updateHorizontalScroll)
    }

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

    val transformation = remember(highlighterEnabled, syntaxColors, activeFoldBlocks) {
        PascalVisualTransformation(
            colors = syntaxColors.takeIf { highlighterEnabled },
            activeFoldBlocks = activeFoldBlocks
        )
    }
    val foldOffsetMapping = remember(file.value.text, activeFoldBlocks) {
        PascalFolding.transform(AnnotatedString(file.value.text), activeFoldBlocks).offsetMapping
    }

    var editorLayout by remember(file) { mutableStateOf<EditorLayout?>(null) }

    // the caret was moved from outside (a tap on a compiler error), the field itself does
    // not scroll, so the surrounding column has to be moved to the line the caret is on
    LaunchedEffect(file, file.caretRequest) {
        if (file.caretRequest == 0) {
            return@LaunchedEffect
        }

        val measured = snapshotFlow { editorLayout }
            .filterNotNull()
            .filter { it.sourceText == file.value.text }
            .first()

        val transformedCaret = measured.offsetMapping.originalToTransformed(
            file.value.selection.start
        )
        val line = measured.text.getLineForOffset(transformedCaret)

        // a third of the viewport above it, a line pinned to the very top reads as if
        // the code before it were missing
        val target = measured.text.getLineTop(line) - verticalScroll.viewportSize / 3

        verticalScroll.animateScrollTo(target.toInt().coerceAtLeast(0))
    }

    val digits = remember(file.value.text) {
        (file.value.text.count { it == '\n' } + 1).toString().length
    }

    val numberGutterWidth =
        if (lineNumbersEnabled) {
            with(density) { gutterPaint.measureText("0".repeat(digits)).toDp() } + GutterGap
        } else {
            0.dp
        }
    val gutterWidth = numberGutterWidth + FoldGutterWidth
    val contentTopPx = with(density) { ContentTop.toPx() }

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
                        sourceText = file.value.text,
                        offsetMapping = foldOffsetMapping,
                        numberedVisualLines =
                            if (lineNumbersEnabled) {
                                it.findNumberedVisualLines(file.value.text, foldOffsetMapping)
                            } else {
                                NumberedVisualLines.EMPTY
                            },
                        foldMarkers = it.findFoldMarkers(
                            blocks = visibleFoldBlocks,
                            collapsedStarts = file.collapsedFoldStarts,
                            offsetMapping = foldOffsetMapping
                        )
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
                                file.insertText(INDENT)
                                true
                            }

                            event.key == Key.S && event.isCtrlPressed -> {
                                onSaveShortcut()
                                true
                            }

                            else -> false
                        }
                    }
                    .padding(top = ContentTop, end = 4.dp)
            )
        }

        val layout = editorLayout

        Spacer(
            Modifier
                .fillMaxHeight()
                .width(gutterWidth)
                .pointerInput(file, layout) {
                    detectTapGestures { position ->
                        if (layout == null || position.x < size.width - FoldGutterWidth.toPx()) {
                            return@detectTapGestures
                        }

                        val layoutY = position.y + verticalScroll.value - contentTopPx
                        val marker = layout.foldMarkers.firstOrNull { foldMarker ->
                            layoutY >= layout.text.getLineTop(foldMarker.visualLine) &&
                                    layoutY < layout.text.getLineBottom(foldMarker.visualLine)
                        }

                        marker?.let { file.toggleFold(it.block) }
                    }
                }
                .drawBehind {
                    if (layout != null) {
                        if (lineNumbersEnabled) {
                            drawLineNumbers(
                                layout = layout.text,
                                numberedVisualLines = layout.numberedVisualLines,
                                paint = gutterPaint,
                                gutterEnd = size.width - FoldGutterWidth.toPx() - GutterGap.toPx(),
                                scrollOffset = verticalScroll.value,
                                viewportHeight = viewportPx,
                                topOffset = ContentTop.toPx()
                            )
                        }

                        drawFoldMarkers(
                            layout = layout.text,
                            markers = layout.foldMarkers,
                            color = syntaxColors.lineNumber,
                            centerX = size.width - FoldGutterWidth.toPx() / 2f,
                            scrollOffset = verticalScroll.value,
                            viewportHeight = viewportPx,
                            topOffset = ContentTop.toPx()
                        )
                    }
                }
        )
    }
}

private data class EditorLayout(
    val text: TextLayoutResult,
    val sourceText: String,
    val offsetMapping: OffsetMapping,
    val numberedVisualLines: NumberedVisualLines,
    val foldMarkers: List<FoldMarker>
)

private data class NumberedVisualLines(
    val visualLines: IntArray,
    val sourceLines: IntArray
) {
    companion object {
        val EMPTY = NumberedVisualLines(IntArray(0), IntArray(0))
    }
}

private data class FoldMarker(
    val block: PascalFoldBlock,
    val visualLine: Int,
    val collapsed: Boolean
)

// rebuilt only when text layout changes; scrolling can then skip straight to visible source lines.
private fun TextLayoutResult.findNumberedVisualLines(
    sourceText: String,
    offsetMapping: OffsetMapping
): NumberedVisualLines {
    val transformedText = layoutInput.text.text
    val visualLines = IntArray(lineCount)
    val sourceLines = IntArray(lineCount)
    val sourceLineStarts = sourceText.lineStarts()
    var numberedLine = 0

    for (visualLine in 0 until lineCount) {
        val start = getLineStart(visualLine)

        if (visualLine == 0 || (start > 0 && transformedText[start - 1] == '\n')) {
            visualLines[numberedLine] = visualLine
            sourceLines[numberedLine] = sourceLineStarts.lineAt(
                offsetMapping.transformedToOriginal(start)
            )
            numberedLine++
        }
    }

    return NumberedVisualLines(
        visualLines = visualLines.copyOf(numberedLine),
        sourceLines = sourceLines.copyOf(numberedLine)
    )
}

private fun TextLayoutResult.findFoldMarkers(
    blocks: List<PascalFoldBlock>,
    collapsedStarts: Set<Int>,
    offsetMapping: OffsetMapping
): List<FoldMarker> {
    val result = mutableListOf<FoldMarker>()
    val occupiedLines = mutableSetOf<Int>()

    for (block in blocks) {
        val transformedOffset = offsetMapping.originalToTransformed(block.startOffset)
        val visualLine = getLineForOffset(transformedOffset)

        if (occupiedLines.add(visualLine)) {
            result += FoldMarker(
                block = block,
                visualLine = visualLine,
                collapsed = block.startOffset in collapsedStarts
            )
        }
    }

    return result
}

private fun String.lineStarts(): IntArray {
    val result = IntArray(count { it == '\n' } + 1)
    var line = 1

    for (offset in indices) {
        if (this[offset] == '\n') {
            result[line++] = offset + 1
        }
    }

    return result
}

private fun IntArray.lineAt(offset: Int): Int {
    val found = binarySearch(offset)
    return if (found >= 0) found + 1 else -found - 1
}

// the gutter is a fixed overlay in viewport space, so line positions are shifted up by the
// vertical scroll and down by the top inset the text field carries
private fun DrawScope.drawLineNumbers(
    layout: TextLayoutResult,
    numberedVisualLines: NumberedVisualLines,
    paint: Paint,
    gutterEnd: Float,
    scrollOffset: Int,
    viewportHeight: Int,
    topOffset: Float
) {
    val firstVisible = layout.getLineForVerticalPosition(scrollOffset.toFloat())
    val lastVisible = layout.getLineForVerticalPosition((scrollOffset + viewportHeight).toFloat())
    val found = numberedVisualLines.visualLines.binarySearch(firstVisible)
    var numberedLine = if (found >= 0) found else -found - 1

    drawIntoCanvas { canvas ->
        while (numberedLine < numberedVisualLines.visualLines.size) {
            val visualLine = numberedVisualLines.visualLines[numberedLine]

            if (visualLine > lastVisible) {
                break
            }

            canvas.nativeCanvas.drawText(
                numberedVisualLines.sourceLines[numberedLine].toString(),
                gutterEnd,
                layout.getLineBaseline(visualLine) - scrollOffset + topOffset,
                paint
            )
            numberedLine++
        }
    }
}

private fun DrawScope.drawFoldMarkers(
    layout: TextLayoutResult,
    markers: List<FoldMarker>,
    color: Color,
    centerX: Float,
    scrollOffset: Int,
    viewportHeight: Int,
    topOffset: Float
) {
    val halfSize = 3.5.dp.toPx()
    val strokeWidth = 1.5.dp.toPx()

    for (marker in markers) {
        val lineTop = layout.getLineTop(marker.visualLine)
        val lineBottom = layout.getLineBottom(marker.visualLine)

        if (lineBottom < scrollOffset || lineTop > scrollOffset + viewportHeight) {
            continue
        }

        val centerY = (lineTop + lineBottom) / 2f - scrollOffset + topOffset

        if (marker.collapsed) {
            drawLine(
                color = color,
                start = Offset(centerX - halfSize / 2f, centerY - halfSize),
                end = Offset(centerX + halfSize / 2f, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(centerX + halfSize / 2f, centerY),
                end = Offset(centerX - halfSize / 2f, centerY + halfSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        } else {
            drawLine(
                color = color,
                start = Offset(centerX - halfSize, centerY - halfSize / 2f),
                end = Offset(centerX, centerY + halfSize / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(centerX, centerY + halfSize / 2f),
                end = Offset(centerX + halfSize, centerY - halfSize / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
