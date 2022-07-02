package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CodeEditText extends androidx.appcompat.widget.AppCompatEditText {

    private final Rect rect = new Rect();
    private final Paint paint = new Paint();
    private final Context context;
    private int lineCountFontSize = 14;

    public CodeEditText(@NonNull Context context, int lineCountFontSize) {
        super(context);
        this.context = context;
        this.lineCountFontSize = lineCountFontSize;
        initPaint();
    }

    public CodeEditText(@NonNull Context context) {
        super(context);
        this.context = context;
        initPaint();
    }

    private void initPaint() {
        paint.setAntiAlias(true);
        paint.setColor(Color.rgb(80, 80, 80));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(pxToDp(lineCountFontSize));
    }

    private int pxToDp(int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px * density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int baseline;
        int lineCount = getLineCount();
        int lineNumber = 1;

        for (int i = 0; i < lineCount; ++i) {
            baseline = getLineBounds(i, null);

            if (i == 0) {
                canvas.drawText("" + lineNumber, rect.left, baseline, paint);
                ++lineNumber;
            } else if (Objects.requireNonNull(getText()).charAt(getLayout().getLineStart(i) - 1) == '\n') {
                canvas.drawText("" + lineNumber, rect.left, baseline, paint);
                ++lineNumber;
            }
        }

        if (lineCount < 100) {
            setPadding(pxToDp(10 + lineCountFontSize), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount < 1000) {
            setPadding(pxToDp(25 + lineCountFontSize), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount < 10000) {
            setPadding(pxToDp(35 + lineCountFontSize), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount < 100000) {
            setPadding(pxToDp(45 + lineCountFontSize), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
    }
}
