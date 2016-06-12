package com.github.helltar.anpaside.editor;

import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import com.github.helltar.anpaside.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.helltar.anpaside.editor.Patterns.*;

/* на данный момент тут происходит что-то странное */

public class Highlighter extends AsyncTask<Void, Void, Void> {

    public static boolean isRun = false;

    private Editable editable;
    private List<String> spanList = new ArrayList<>();
    private StringBuilder currentText;

    public Highlighter(Editable s) {
        editable = s;
        currentText = new StringBuilder(editable.toString());
    }

    private void setColorByRegex(Pattern pattern, int rgb) {
        Matcher m = pattern.matcher(currentText);

        while (m.find()) {
            spanList.add(m.start() + ":" + m.end() + "::" + rgb);
        }
    }

    private int getColorFromRgb(int red, int green, int blue) {
        return Color.rgb(red, green, blue);
    }

    private void clearSpans(Editable s) {
        ForegroundColorSpan spans[] = s.getSpans(0, s.length(), ForegroundColorSpan.class);

        for (int i = 0; i < spans.length; i++) {
            s.removeSpan(spans[i]);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        isRun = true;
    }

    @Override
    protected Void doInBackground(Void[] p1) {
        setColorByRegex(stringsPattern, getColorFromRgb(255, 204, 51));
        setColorByRegex(numbersPattern, getColorFromRgb(255, 102, 51));
        setColorByRegex(keywordsPattern, getColorFromRgb(0, 190, 230));
        setColorByRegex(commentsPattern, getColorFromRgb(10, 200, 10));
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        clearSpans(editable);

        for (int i = 0; i < spanList.size(); i++) {
            String s = spanList.get(i);

            try {
                int start = Integer.parseInt(s.substring(0, s.indexOf(":")));
                int end = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.indexOf("::")));
                int color = Integer.parseInt(s.substring(s.indexOf("::") + 2, s.length()));

                editable.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (IndexOutOfBoundsException e) {
                Logger.addLog(e);
            }
        }

        isRun = false;
    }
}

