package com.example.v_sat_compass.ui.progress;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ProgressTrendChartView extends View {
    private static final int MAX_SCORE = 1200;

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> scores = new ArrayList<>();

    public ProgressTrendChartView(Context context) {
        super(context);
        init();
    }

    public ProgressTrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressTrendChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setScores(List<Integer> values) {
        scores.clear();
        if (values != null) scores.addAll(values);
        invalidate();
    }

    private void init() {
        axisPaint.setColor(Color.rgb(117, 117, 117));
        axisPaint.setStrokeWidth(dp(1));
        gridPaint.setColor(Color.rgb(230, 230, 238));
        gridPaint.setStrokeWidth(dp(1));
        linePaint.setColor(Color.rgb(74, 58, 186));
        linePaint.setStrokeWidth(dp(3));
        linePaint.setStyle(Paint.Style.STROKE);
        pointPaint.setColor(Color.rgb(39, 174, 96));
        pointPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.rgb(117, 117, 117));
        textPaint.setTextSize(dp(11));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float left = dp(36);
        float right = width - dp(12);
        float top = dp(12);
        float bottom = height - dp(28);

        drawGrid(canvas, left, right, top, bottom);
        if (scores.size() < 2) {
            canvas.drawText("Need 2 exams", left + dp(8), (top + bottom) / 2f, textPaint);
            return;
        }

        float lastX = 0f;
        float lastY = 0f;
        for (int i = 0; i < scores.size(); i++) {
            float x = left + (right - left) * i / Math.max(1, scores.size() - 1);
            float y = bottom - (bottom - top) * clampScore(scores.get(i)) / MAX_SCORE;
            if (i > 0) {
                canvas.drawLine(lastX, lastY, x, y, linePaint);
            }
            canvas.drawCircle(x, y, dp(4), pointPaint);
            lastX = x;
            lastY = y;
        }
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom) {
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            int label = MAX_SCORE - (MAX_SCORE * i / 4);
            canvas.drawLine(left, y, right, y, i == 4 ? axisPaint : gridPaint);
            canvas.drawText(String.valueOf(label), dp(2), y + dp(4), textPaint);
        }
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawText("old", left, bottom + dp(20), textPaint);
        canvas.drawText("new", right - dp(24), bottom + dp(20), textPaint);
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(MAX_SCORE, score));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
