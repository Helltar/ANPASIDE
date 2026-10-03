package com.github.helltar.anpaside.ui.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.GetChars
import android.text.InputType
import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.TransformationMethod
import android.text.style.ForegroundColorSpan
import android.text.style.UpdateLayout
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Scroller
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val INDENT = "    "

// tokens are painted for the visible lines plus this many on each side, and the window is moved
// once fewer than the refresh margin are left, so scrolling never reaches unpainted code
private const val HIGHLIGHT_MARGIN_LINES = 80
private const val HIGHLIGHT_REFRESH_LINES = 25

internal data class EditorPalette(
    val text: Int,
    val background: Int,
    val accent: Int,
    val keyword: Int,
    val string: Int,
    val number: Int,
    val comment: Int,
    val lineNumber: Int
)

// the code field itself. a platform EditText and not a compose text field, because its layout
// is a DynamicLayout that lays out again only the lines an edit touched, while a compose field
// lays the whole file out on every keystroke. the view scrolls itself and draws the line
// numbers and the fold markers into its own left padding, which the text is clipped out of.
// EditorDocument stays the owner of the text: the view pushes every edit into it and takes
// back whatever was changed from outside
@SuppressLint("AppCompatCustomView", "ViewConstructor")
internal class CodeEditorView(context: Context) : EditText(context) {

    var onSaveShortcut: () -> Unit = {}

    // the super constructor already calls into the overrides below, before any of this exists
    private var ready = false

    private var document: EditorDocument? = null

    // the document value the view is known to match, compared by identity
    private var synced: TextFieldValue? = null
    private var applying = false
    private var editing = false

    private var blocks: List<PascalFoldBlock> = emptyList()
    private var markerBlocks: List<PascalFoldBlock> = emptyList()
    private var lineStarts = IntArray(1)
    private val handledCaretRequests = WeakHashMap<EditorDocument, Int>()

    private var fontSize = -1
    private var highlighterEnabled = false
    private var lineNumbersEnabled = false
    private var wordWrapEnabled: Boolean? = null
    private var palette: EditorPalette? = null

    private var highlightWindow = HighlightWindow(0, 0)
    private var highlightPending = false
    private var pendingScroll: Pair<Int, Int>? = null
    private var caretRevealPending = false

    // the platform brings the caret into view after every measure, wanted or not
    private var revealBlocked = false

    private val density = resources.displayMetrics.density
    private val gutterGap = 6 * density
    private val foldGutterWidth = 20 * density
    private val contentTop = (6 * density).roundToInt()
    private val contentEnd = (4 * density).roundToInt()
    private var gutterWidth = 0

    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.MONOSPACE
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1.5f * density
    }

    // an EditText follows the finger but stops dead when it lifts, flinging is added here
    private val scroller = Scroller(context)
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downScrollY = 0
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var panning = false
    private var longPressed = false
    private var pressedFold: PascalFoldBlock? = null

    init {
        // with wrap_content the text view asks for a new layout pass on every span that is
        // added or removed, and each of those passes ends with a scroll back to the caret
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        background = null
        gravity = Gravity.TOP or Gravity.START
        typeface = Typeface.MONOSPACE
        includeFontPadding = false
        isSaveEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // has to follow inputType, which resets the transformation of a multi-line field
        transformationMethod = FoldTransformation

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                editing = true
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable) {
                editing = false

                if (!applying) {
                    pushText()
                }
            }
        })

        ready = true
    }

    fun configure(
        fontSize: Int,
        highlighterEnabled: Boolean,
        lineNumbersEnabled: Boolean,
        wordWrapEnabled: Boolean,
        palette: EditorPalette
    ) {
        var gutterChanged = false
        var highlightChanged = false

        if (fontSize != this.fontSize) {
            this.fontSize = fontSize
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat())
            lineHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                fontSize * 1.4f,
                resources.displayMetrics
            ).roundToInt()
            gutterPaint.textSize = textSize
            gutterChanged = true
        }

        if (lineNumbersEnabled != this.lineNumbersEnabled) {
            this.lineNumbersEnabled = lineNumbersEnabled
            gutterChanged = true
        }

        if (wordWrapEnabled != this.wordWrapEnabled) {
            this.wordWrapEnabled = wordWrapEnabled
            setHorizontallyScrolling(!wordWrapEnabled)

            if (wordWrapEnabled) {
                scrollTo(0, scrollY)
            }
        }

        if (palette != this.palette) {
            this.palette = palette
            setTextColor(palette.text)
            setBackgroundColor(palette.background)
            highlightColor = (palette.accent and 0x00FFFFFF) or 0x55000000
            gutterPaint.color = palette.lineNumber
            markerPaint.color = palette.lineNumber

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                textCursorDrawable?.setTint(palette.accent)
                textSelectHandle?.setTint(palette.accent)
                textSelectHandleLeft?.setTint(palette.accent)
                textSelectHandleRight?.setTint(palette.accent)
            }

            highlightChanged = true
        }

        if (highlighterEnabled != this.highlighterEnabled) {
            this.highlighterEnabled = highlighterEnabled
            highlightChanged = true
        }

        if (gutterChanged) {
            updateGutterWidth()
        }

        if (highlightChanged) {
            rehighlight()
        }
    }

    // called on every recomposition that read the document, so it has to be cheap when
    // nothing was changed from outside
    fun bind(document: EditorDocument) {
        val value = document.value

        if (document !== this.document) {
            val scrollX = document.horizontalScrollOffset
            val scrollY = document.verticalScrollOffset

            this.document = document
            scroller.forceFinished(true)
            applying = true
            setText(value.text)
            setSelectionFrom(value)
            applying = false
            synced = value
            textChanged()
            restoreScroll(scrollX, scrollY)
        } else if (value !== synced) {
            applyExternal(value)
        }

        syncFolds()

        // a tab that was sent to a line before it was first shown still has to go there
        if ((handledCaretRequests[document] ?: 0) != document.caretRequest) {
            handledCaretRequests[document] = document.caretRequest
            revealCaretLine()
        }
    }

    private val source: String
        get() = synced?.text.orEmpty()

    private fun pushText() {
        val document = document ?: return
        val value = TextFieldValue(
            text = text.toString(),
            selection = TextRange(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        )

        scroller.forceFinished(true)
        synced = value
        document.onValueChange(value)
        textChanged()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        // while the text is being replaced the selection is reported against a text the
        // document has not seen yet, the edit itself carries the final selection
        if (!ready || applying || editing || selStart < 0 || selEnd < 0) {
            return
        }

        val current = synced ?: return
        val selection = TextRange(selStart, selEnd)

        if (selection == current.selection) {
            return
        }

        val value = current.copy(selection = selection)
        synced = value
        document?.onValueChange(value)

        // a caret that lands inside a folded block opens it
        syncFolds()
    }

    // only what differs is replaced, so the layout reflows those lines and nothing else
    private fun applyExternal(value: TextFieldValue) {
        val editable = text ?: return
        val newText = value.text
        val common = min(editable.length, newText.length)
        var start = 0

        while (start < common && editable[start] == newText[start]) {
            start++
        }

        var suffix = 0

        while (suffix < common - start &&
            editable[editable.length - suffix - 1] == newText[newText.length - suffix - 1]
        ) {
            suffix++
        }

        val changed = editable.length != newText.length || start != common

        applying = true

        if (changed) {
            editable.replace(start, editable.length - suffix, newText, start, newText.length - suffix)
        }

        setSelectionFrom(value)
        applying = false
        synced = value

        if (changed) {
            textChanged()
        }
    }

    private fun setSelectionFrom(value: TextFieldValue) {
        val editable = text ?: return

        Selection.setSelection(
            editable,
            value.selection.start.coerceIn(0, editable.length),
            value.selection.end.coerceIn(0, editable.length)
        )
    }

    private fun textChanged() {
        val text = source

        lineStarts = text.lineStarts()
        blocks = PascalFolding.findBlocks(text)
        updateGutterWidth()
        syncFolds()
        rehighlight()
    }

    private fun updateGutterWidth() {
        val numbers =
            if (lineNumbersEnabled) {
                gutterPaint.measureText("0".repeat(lineStarts.size.toString().length)) + gutterGap
            } else {
                0f
            }
        val width = (numbers + foldGutterWidth).roundToInt()

        // changing the padding throws the layout away, so it is only touched when it differs
        if (width != gutterWidth || paddingTop != contentTop) {
            gutterWidth = width
            setPadding(width, contentTop, contentEnd, 0)
        }
    }

    // folded blocks are spans on the text, which keeps them in place while the text around
    // them is edited; FoldedText reads them back when the layout asks for characters
    private fun syncFolds() {
        val document = document ?: return
        val editable = text ?: return
        val active = PascalFolding.activeBlocks(blocks, document.collapsedFoldStarts)
        val wanted = active.mapTo(HashSet()) { packRange(it.hiddenStart, it.hiddenEnd) }
        var changed = false

        markerBlocks = PascalFolding.visibleBlocks(blocks, active)

        for (span in editable.getSpans(0, editable.length, FoldSpan::class.java)) {
            if (!wanted.remove(packRange(editable.getSpanStart(span), editable.getSpanEnd(span)))) {
                editable.removeSpan(span)
                changed = true
            }
        }

        for (range in wanted) {
            val start = (range shr 32).toInt()
            val end = range.toInt()

            if (start < end && end <= editable.length) {
                editable.setSpan(FoldSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                changed = true
            }
        }

        if (changed) {
            rehighlight()
        }

        invalidate()
    }

    // spans that are still right are left alone: after a keystroke that is nearly all of them,
    // and every span added or removed is a round of callbacks into the text view
    private fun rehighlight() {
        val editable = text ?: return
        val palette = palette ?: return
        val layout = layout
        val existing = editable.getSpans(0, editable.length, SyntaxSpan::class.java)

        if (!highlighterEnabled) {
            existing.forEach(editable::removeSpan)
            highlightPending = false
            return
        }

        if (layout == null) {
            highlightPending = true
            return
        }

        highlightPending = false
        highlightWindow = windowAround(layout, HIGHLIGHT_MARGIN_LINES)

        val stale = HashMap<Long, SyntaxSpan>(existing.size * 2)

        for (span in existing) {
            val range = packRange(editable.getSpanStart(span), editable.getSpanEnd(span))
            stale.put(range, span)?.let(editable::removeSpan)
        }

        for (token in PascalHighlighter.tokens(source, highlightWindow)) {
            if (token.end > editable.length) {
                break
            }

            val color =
                when (token.kind) {
                    TokenKind.KEYWORD -> palette.keyword
                    TokenKind.STRING -> palette.string
                    TokenKind.NUMBER -> palette.number
                    TokenKind.COMMENT -> palette.comment
                }
            val range = packRange(token.start, token.end)

            if (stale[range]?.foregroundColor == color) {
                stale.remove(range)
            } else {
                editable.setSpan(
                    SyntaxSpan(color),
                    token.start,
                    token.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        stale.values.forEach(editable::removeSpan)
    }

    // a window that reaches the last line is open-ended, so typing at the end of the file does
    // not outgrow it with every character
    private fun windowAround(layout: Layout, marginLines: Int): HighlightWindow {
        val top = totalPaddingTop
        val first = layout.getLineForVertical(scrollY - top) - marginLines
        val last = layout.getLineForVertical(scrollY + height - top) + marginLines

        return HighlightWindow(
            start = if (first <= 0) 0 else layout.getLineStart(first),
            end = if (last >= layout.lineCount - 1) Int.MAX_VALUE else layout.getLineEnd(last)
        )
    }

    override fun onScrollChanged(horiz: Int, vert: Int, oldHoriz: Int, oldVert: Int) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert)

        if (!ready) {
            return
        }

        // setting the text of another tab scrolls to the top, which is not that tab's position
        if (!applying) {
            document?.updateVerticalScroll(vert)
            document?.updateHorizontalScroll(horiz)
        }

        val layout = layout

        if (highlighterEnabled && layout != null) {
            val needed = windowAround(layout, HIGHLIGHT_REFRESH_LINES)

            if (needed.start < highlightWindow.start || needed.end > highlightWindow.end) {
                rehighlight()
            }
        }
    }

    private fun restoreScroll(x: Int, y: Int) {
        blockRevealForThisFrame()

        if (layout == null || !isLaidOut) {
            pendingScroll = x to y
        } else {
            scrollTo(x, y.coerceIn(0, maxScrollY()))
        }
    }

    // a third of the viewport above the line, one pinned to the very top reads as if the code
    // before it were missing
    private fun revealCaretLine() {
        val layout = layout

        if (layout == null || !isLaidOut) {
            caretRevealPending = true
            return
        }

        caretRevealPending = false
        pendingScroll = null

        val line = layout.getLineForOffset(selectionStart.coerceAtLeast(0))
        scrollTo(scrollX, (layout.getLineTop(line) - height / 3).coerceIn(0, maxScrollY()))
    }

    private fun maxScrollY(): Int {
        val layout = layout ?: return 0
        return max(0, layout.height + totalPaddingTop + totalPaddingBottom - height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!ready || layout == null) {
            return
        }

        pendingScroll?.let { (x, y) ->
            pendingScroll = null
            blockRevealForThisFrame()
            scrollTo(x, y.coerceIn(0, maxScrollY()))
        }

        if (caretRevealPending) {
            revealCaretLine()
        }

        if (highlightPending) {
            rehighlight()
        }
    }

    // the keyboard or the log panel taking away part of the view only follows a caret that
    // was on screen before it did, so a panel opening on a reader who has scrolled somewhere
    // else leaves the code where it is, and a view that grows never scrolls at all
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (!ready || oldh <= 0 || h == oldh) {
            return
        }

        if (!(hasFocus() && h < oldh && caretVisibleWithin(oldh))) {
            blockRevealForThisFrame()
        }
    }

    private fun caretVisibleWithin(viewHeight: Int): Boolean {
        val layout = layout ?: return false
        val line = layout.getLineForOffset(selectionEnd.coerceAtLeast(0))
        val top = layout.getLineTop(line) + totalPaddingTop
        val bottom = layout.getLineBottom(line) + totalPaddingTop

        return top >= scrollY && bottom <= scrollY + viewHeight
    }

    private fun blockRevealForThisFrame() {
        revealBlocked = true
        post { revealBlocked = false }
    }

    override fun bringPointIntoView(offset: Int): Boolean =
        if (revealBlocked || !scroller.isFinished || velocityTracker != null) {
            false
        } else {
            super.bringPointIntoView(offset)
        }

    // the platform drags the caret along when a touch scrolls it out of sight; in code the
    // caret marks where the work is, and reading elsewhere must not move it
    override fun moveCursorToVisibleOffset(): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        when {
            keyCode == KeyEvent.KEYCODE_TAB && !event.isCtrlPressed && !event.isAltPressed -> {
                text?.replace(
                    min(selectionStart, selectionEnd).coerceAtLeast(0),
                    max(selectionStart, selectionEnd).coerceAtLeast(0),
                    INDENT
                )
                true
            }

            keyCode == KeyEvent.KEYCODE_S && event.isCtrlPressed -> {
                onSaveShortcut()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_DOWN) {
            scroller.forceFinished(true)
            pressedFold = foldMarkerAt(event.x, event.y)

            if (pressedFold == null) {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                downScrollY = scrollY
            }

            downX = event.x
            downY = event.y
            panning = false
            longPressed = false

            // the navigation drawer around the editor claims every sideways drag it sees;
            // while there is a line to pan, the gesture has to be kept from the first event
            if (wordWrapEnabled == false && maxScrollX() > 0) {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }

        pressedFold?.let { block ->
            if (action == MotionEvent.ACTION_UP) {
                document?.toggleFold(block)
                syncFolds()
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                pressedFold = null
            }

            return true
        }

        velocityTracker?.addMovement(event)

        // a sideways drag over a focused field is taken by the platform to move the caret,
        // which leaves no way to pan a long line; the gesture is taken away from it here
        // and the view is scrolled by hand instead, unless a long press started a selection
        if (!panning && !longPressed && wordWrapEnabled == false &&
            action == MotionEvent.ACTION_MOVE &&
            abs(event.x - downX) > touchSlop &&
            abs(event.x - downX) > abs(event.y - downY)
        ) {
            val cancel = MotionEvent.obtain(event)
            cancel.action = MotionEvent.ACTION_CANCEL
            super.onTouchEvent(cancel)
            cancel.recycle()
            panning = true
            lastX = event.x
            lastY = event.y
        }

        val handled =
            if (panning) {
                if (action == MotionEvent.ACTION_MOVE) {
                    scrollTo(
                        (scrollX - (event.x - lastX)).roundToInt().coerceIn(0, maxScrollX()),
                        (scrollY - (event.y - lastY)).roundToInt().coerceIn(0, maxScrollY())
                    )
                    lastX = event.x
                    lastY = event.y
                }

                true
            } else {
                super.onTouchEvent(event)
            }

        if (action == MotionEvent.ACTION_UP) {
            velocityTracker?.let { tracker ->
                tracker.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())

                val velocity = tracker.yVelocity

                // only a drag that scrolled is flung, not one that selected text
                if (abs(velocity) > minFlingVelocity && scrollY != downScrollY && !hasSelection()) {
                    scroller.fling(
                        scrollX, scrollY,
                        0, -velocity.toInt(),
                        scrollX, scrollX,
                        0, maxScrollY()
                    )
                    postInvalidateOnAnimation()
                }
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            velocityTracker?.recycle()
            velocityTracker = null
        }

        return handled
    }

    override fun performLongClick(): Boolean {
        longPressed = true
        return super.performLongClick()
    }

    // how far the lines on screen reach, the same bound the platform uses for its own drag
    private fun maxScrollX(): Int {
        val layout = layout ?: return 0
        val top = totalPaddingTop
        val first = layout.getLineForVertical(scrollY - top)
        val last = layout.getLineForVertical(scrollY + height - top)
        var right = 0f

        for (line in first..last) {
            right = max(right, layout.getLineRight(line))
        }

        return max(0, right.toInt() + 1 + totalPaddingLeft + totalPaddingRight - width)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }

    private fun foldMarkerAt(x: Float, y: Float): PascalFoldBlock? {
        val layout = layout ?: return null

        if (x < gutterWidth - foldGutterWidth || x >= gutterWidth) {
            return null
        }

        val line = layout.getLineForVertical((y + scrollY - totalPaddingTop).toInt())
        val start = layout.getLineStart(line)
        val end = layout.getLineEnd(line)

        return markerBlocks.firstOrNull { it.startOffset in start until end }
    }

    // the canvas is already moved by the scroll offset, so the gutter is drawn at scrollX to
    // stay put while the text slides sideways
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val layout = layout ?: return
        val top = totalPaddingTop
        val first = layout.getLineForVertical(scrollY - top)
        val last = layout.getLineForVertical(scrollY + height - top)
        val display = layout.text
        val numberEnd = scrollX + gutterWidth - foldGutterWidth - gutterGap
        val markerCenter = scrollX + gutterWidth - foldGutterWidth / 2f
        val collapsed = document?.collapsedFoldStarts.orEmpty()
        var marker = 0

        for (line in first..last) {
            val start = layout.getLineStart(line)
            val end = layout.getLineEnd(line)

            // a wrapped line continues the one above and carries no number of its own
            if (lineNumbersEnabled && (start == 0 || display[start - 1] == '\n')) {
                canvas.drawText(
                    lineStarts.lineAt(start).toString(),
                    numberEnd,
                    (top + layout.getLineBaseline(line)).toFloat(),
                    gutterPaint
                )
            }

            while (marker < markerBlocks.size && markerBlocks[marker].startOffset < start) {
                marker++
            }

            if (marker < markerBlocks.size && markerBlocks[marker].startOffset < end) {
                drawFoldMarker(
                    canvas = canvas,
                    centerX = markerCenter,
                    centerY = top + (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f,
                    collapsed = markerBlocks[marker].startOffset in collapsed
                )
            }
        }
    }

    private fun drawFoldMarker(canvas: Canvas, centerX: Float, centerY: Float, collapsed: Boolean) {
        val half = 3.5f * density

        if (collapsed) {
            canvas.drawLine(centerX - half / 2f, centerY - half, centerX + half / 2f, centerY, markerPaint)
            canvas.drawLine(centerX + half / 2f, centerY, centerX - half / 2f, centerY + half, markerPaint)
        } else {
            canvas.drawLine(centerX - half, centerY - half / 2f, centerX, centerY + half / 2f, markerPaint)
            canvas.drawLine(centerX, centerY + half / 2f, centerX + half, centerY - half / 2f, markerPaint)
        }
    }
}

private class SyntaxSpan(color: Int) : ForegroundColorSpan(color)

// UpdateLayout makes the layout reflow the range when the span is added or removed
private class FoldSpan : UpdateLayout

private object FoldTransformation : TransformationMethod {

    override fun getTransformation(source: CharSequence, view: View): CharSequence =
        if (source is Spannable) FoldedText(source) else source

    override fun onFocusChanged(
        view: View,
        sourceText: CharSequence,
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: android.graphics.Rect?
    ) = Unit
}

// what the layout draws instead of the source: the same text, of the same length, with every
// folded range read as its placeholder. it stays a Spanned so the syntax colors still apply
private class FoldedText(private val source: Spannable) : CharSequence, GetChars, Spanned by source {

    override val length: Int
        get() = source.length

    override fun get(index: Int): Char {
        for (span in source.getSpans(index, index + 1, FoldSpan::class.java)) {
            val start = source.getSpanStart(span)
            val end = source.getSpanEnd(span)

            if (index in start until end) {
                return PascalFolding.placeholderChar(index - start, end - start)
            }
        }

        return source[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        val chars = CharArray(endIndex - startIndex)
        getChars(startIndex, endIndex, chars, 0)
        return String(chars)
    }

    override fun getChars(start: Int, end: Int, dest: CharArray, destoff: Int) {
        TextUtils.getChars(source, start, end, dest, destoff)

        for (span in source.getSpans(start, end, FoldSpan::class.java)) {
            val spanStart = source.getSpanStart(span)
            val spanEnd = source.getSpanEnd(span)

            for (index in max(spanStart, start) until min(spanEnd, end)) {
                dest[destoff + index - start] =
                    PascalFolding.placeholderChar(index - spanStart, spanEnd - spanStart)
            }
        }
    }

    override fun toString(): String = subSequence(0, length).toString()
}

private fun packRange(start: Int, end: Int): Long =
    (start.toLong() shl 32) or (end.toLong() and 0xFFFFFFFFL)

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
