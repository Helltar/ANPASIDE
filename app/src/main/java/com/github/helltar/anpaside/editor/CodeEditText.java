package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Gravity;

public class CodeEditText extends android.widget.EditText {

    private Rect rect;
    private Paint paint;

    public CodeEditText(Context context) {
        super(context);

        rect = new Rect();
        paint = new Paint();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(80, 80, 80));
        paint.setTextSize(24);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        setBackgroundColor(android.R.color.transparent);
        setGravity(Gravity.TOP);
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
            } else if (getText().charAt(getLayout().getLineStart(i) - 1) == '\n') {
                canvas.drawText("" + lineNumber, rect.left, baseline, paint);
                ++lineNumber;
            }
        }   

        if (lineCount < 100) {
            setPadding(50, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount > 99 && lineCount < 1000) {
            setPadding(70, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount > 999 && lineCount < 10000) {
            setPadding(90, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        } else if (lineCount > 9999 && lineCount < 100000) {
            setPadding(110, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }

        super.onDraw(canvas);
    }
}
