package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import com.github.helltar.anpaside.logging.Logger;
import com.github.helltar.anpaside.R;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class CodeEditor {

    private Context context;
    private TabHost tabHost;

    private Map<String, Boolean> fileModifiedStatusMap = new HashMap<>();

    public CodeEditor(Context context, TabHost tabHost) {
        this.context = context;
        this.tabHost = tabHost;
    }

    public boolean openFile(String filename) {
        if (isFileOpen(filename)) {
            tabHost.setCurrentTabByTag(filename);
            return true;
        }

        String text = "";

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

        setFileModifiedStatus(filename, false);

        edtText.setTag(filename);
        edtText.setText(text);
        edtText.addTextChangedListener(inputTextWatcher);
        edtText.requestFocus();

        new Highlighter(edtText.getEditableText()).execute();

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
            if (!(Highlighter.isRun)) {
                new Highlighter(s).execute();
            }
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
                setDrawingCacheEnabled(true);
                setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);
            }};
    }

    private void createTabs(String tag, String title, TabContentFactory tabContent) {
        tabHost.addTab(tabHost.newTabSpec(tag).setIndicator(title).setContent(tabContent));
        tabHost.setCurrentTabByTag(tag);

        tabHost.getTabWidget().getChildAt(tabHost.getTabWidget().getChildCount() - 1)
            .setOnLongClickListener(new OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    showPopupMenu(v);
                    return true;
                }
            });
    }

    private void showPopupMenu(View v) {
        PopupMenu pm = new PopupMenu(context, v);
        pm.getMenu().add(R.string.pmenu_tab_close);

        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    closeFile(getCurrentFilename());
                    return true;
                }
            });

        pm.show();
    }

    public boolean saveCurrentFile() {
        try {
            FileUtils.writeStringToFile(new File(getCurrentFilename()), getCurrentEditor().getText().toString());
            setFileModifiedStatus(getCurrentFilename(), false);
            return true;
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public EditText getCurrentEditor() {
        return (EditText) tabHost.findViewWithTag(getCurrentFilename());
    }

    private void closeFile(String filename) {
        //...
    }

    private String getCurrentFilename() {
        return tabHost.getCurrentTabTag();
    }

    private void setFileModifiedStatus(String filename, boolean modifiedStatus) {
        fileModifiedStatusMap.put(filename, modifiedStatus);
    }

    private boolean isFileOpen(String filename) {
        return (tabHost.findViewWithTag(filename) != null);
    }

    public boolean isCurrentFileModified() {
        return true;
        // return fileModifiedStatusMap.get(getCurrentFilename());
    }
}

