package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Objects;

public class CodeEditText extends androidx.appcompat.widget.AppCompatEditText {

    private final Rect rect;
    private final Paint paint;
    private final Context context;

    public CodeEditText(Context context) {
        super(context);

        this.context = context;

        paint = new Paint();
        rect = new Rect();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(80, 80, 80));
        paint.setTextSize(pxToDp(14));
        paint.setAntiAlias(true);
    }

    private int pxToDp(int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px * density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
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
            setPadding(pxToDp(30), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount < 1000) {
            setPadding(pxToDp(40), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount < 10000) {
            setPadding(pxToDp(45), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount < 100000) {
            setPadding(pxToDp(50), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }

        super.onDraw(canvas);
    }
}
