package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import com.github.helltar.anpaside.Logger;
import com.github.helltar.anpaside.R;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class CodeEditor {

    private Context context;
    private TabHost tabHost;

    private Map<String, Boolean> fileModifiedStatusMap = new HashMap<>();
    private Map<String, Integer> editorIdMap = new HashMap<>();

    private int idCount;

    public CodeEditor(Context context, TabHost tabHost) {
        this.context = context;
        this.tabHost = tabHost;
    }

    public boolean openFile(String filename) {
        if (isFileOpen(filename)) {
            tabHost.setCurrentTabByTag(filename);
            return true;
        }

        String text;

        try {
            text = FileUtils.readFileToString(new File(filename));
        } catch (IOException ioe) {
            Logger.addLog(ioe);
            return false;
        }

        final EditText edtText = createEditText();

        createTabs(filename, new File(filename).getName(), new TabContentFactory() {
                @Override
                public View createTabContent(String p1) {
                    ScrollView sv = new ScrollView(context);
                    sv.addView(edtText);
                    return sv;
                }
            });

        idCount++;
        setEditorViewId(filename, idCount);
        edtText.setId(idCount);

        edtText.addTextChangedListener(inputTextWatcher);
        edtText.setText(text);

        return true;
    }

    TextWatcher inputTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            setFileModifiedStatus(getCurrentFilename(), true);
        }

        @Override
        public void afterTextChanged(final Editable s) {
            highlight(s);
        }
    };

    private EditText createEditText() {
        return
            new EditText(context) {{
                setBackgroundColor(android.R.color.transparent);
                setTextColor(Color.rgb(220, 220, 220));
                setHorizontallyScrolling(true);
                setTypeface(Typeface.MONOSPACE);
                setTextSize(14);
                setGravity(Gravity.TOP);
            }};
    }

    private void createTabs(String tag, String title, TabContentFactory tabContent) {
        TabSpec tabSpec = tabHost.newTabSpec(tag);
        tabSpec.setIndicator(title);
        tabSpec.setContent(tabContent);

        tabHost.addTab(tabSpec);
        tabHost.setCurrentTabByTag(tag);
        tabHost.getCurrentTabView().setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPopupMenu(v);
                    return true;
                }
            });
    }

    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(context, v);
        popupMenu.getMenu().add(R.string.pmenu_tab_close);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    //switch (item.getItemId()) {
                    //  case R.id.miCloseTab:
                    closeFile(getCurrentFilename());
                    return true;
                    //default:
                    //  return false;
                    //}
                }
            });

        popupMenu.show();
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

    private void setColorByRegex(Editable s, String pattern, int rgb) {
        Matcher m = Pattern.compile(pattern).matcher(s.toString());

        while (m.find()) {
            s.setSpan(new ForegroundColorSpan(rgb),
                      m.start(), m.end(),
                      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void highlight(final Editable s) {
        clearSpans(s);

        setColorByRegex(s, "[0-9]", getColorFromRgb(255, 102, 51));
        setColorByRegex(s, "program|begin|end|const|var|function|procedure|if|then|for|while|repeat|until", getColorFromRgb(0, 204, 255));
        setColorByRegex(s, "\\W", getColorFromRgb(150, 150, 150));
        setColorByRegex(s, "\'(.*?)\'", getColorFromRgb(236, 181, 52));
        setColorByRegex(s, "//(.*?)\n", getColorFromRgb(10, 200, 0));
    }

    private boolean saveFile(String filename) {
        try {
            FileUtils.writeStringToFile(new File(getCurrentFilename()),
                                        getCurrentEditor().getText().toString());

            setFileModifiedStatus(getCurrentFilename(), false);

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public boolean saveCurrentFile() {
        return saveFile(getCurrentFilename());
    }

    public EditText getCurrentEditor() {
        return (EditText) tabHost.getCurrentView().findViewById(getEditorViewId(getCurrentFilename()));
    }

    private String getCurrentFilename() {
        return tabHost.getCurrentTabTag();
    }

    private void setFileModifiedStatus(String filename, boolean status) {
        fileModifiedStatusMap.put(filename, status);
    }

    private boolean isFileOpen(String filename) {
        return fileModifiedStatusMap.containsKey(filename);
    }

    private void closeFile(String filename) {
        //fileModifiedStatusMap.remove(filename);
        //editorIdMap.remove(filename);
    }

    private void setEditorViewId(String tag, int id) {
        editorIdMap.put(tag, id);
    }

    private int getEditorViewId(String tag) {
        return editorIdMap.get(tag);
    }

    public boolean isCurrentFileModified() {
        if (!(fileModifiedStatusMap.containsKey(getCurrentFilename()))) {
            return false;
        }

        return fileModifiedStatusMap.get(getCurrentFilename());
    }
}

