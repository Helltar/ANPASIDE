package com.github.helltar.anpaside.editor;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.helltar.anpaside.editor.Patterns.*;

public class Highlighter {

    public static void highlights(Editable s) {
        clearSpans(s);

        setColorByRegex(s, stringsPattern, getColorFromRgb(255, 204, 51));
        setColorByRegex(s, numbersPattern, getColorFromRgb(255, 102, 51));
        setColorByRegex(s, keywordsPattern, getColorFromRgb(0, 190, 230));
        setColorByRegex(s, commentsPattern, getColorFromRgb(10, 200, 10));
    }

    private static void setColorByRegex(Editable s, Pattern pattern, int rgb) {
        Matcher m = pattern.matcher(s.toString());

        while (m.find()) {
            s.setSpan(new ForegroundColorSpan(rgb),
                      m.start(), m.end(),
                      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static int getColorFromRgb(int red, int green, int blue) {
        return Color.rgb(red, green, blue);
    }

    public static void clearSpans(Editable s) {
        ForegroundColorSpan spans[] = s.getSpans(0, s.length(), ForegroundColorSpan.class);

        for (int i = 0; i < spans.length; i++) {
            s.removeSpan(spans[i]);
        }
    }
}
