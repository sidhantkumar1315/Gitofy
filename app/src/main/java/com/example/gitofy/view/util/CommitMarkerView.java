package com.example.gitofy.view.util;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.example.gitofy.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommitMarkerView extends MarkerView {

    private final TextView textView;
    private final List<String> dates;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

    public CommitMarkerView(Context context, int layoutResource, List<String> dates) {
        super(context, layoutResource);
        this.dates = dates;
        textView = findViewById(android.R.id.text1);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        try {
            int index = (int) e.getX();
            if (index >= 0 && index < dates.size()) {
                String dateStr = dates.get(index);
                Date date = inputFormat.parse(dateStr);
                String formattedDate = outputFormat.format(date);

                int commits = (int) e.getY();
                String text = formattedDate + "\n" + commits + " commit" + (commits != 1 ? "s" : "");
                textView.setText(text);
            }
        } catch (Exception ex) {
            textView.setText("");
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10f);
    }
}