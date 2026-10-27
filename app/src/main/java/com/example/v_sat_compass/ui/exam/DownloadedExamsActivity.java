package com.example.v_sat_compass.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.OfflineExamBundle;
import com.example.v_sat_compass.data.repository.OfflineExamRepository;
import com.example.v_sat_compass.ui.exam.session.ExamSessionActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class DownloadedExamsActivity extends AppCompatActivity {
    private LinearLayout listContainer;
    private TextView tvTotalSize;
    private final OfflineExamRepository offlineExamRepository = OfflineExamRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        loadDownloaded();
    }

    private ScrollView buildView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header);

        TextView title = new TextView(this);
        title.setText("De da tai");
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        MaterialButton back = new MaterialButton(this);
        back.setText("Dong");
        back.setOnClickListener(v -> finish());
        header.addView(back);

        tvTotalSize = new TextView(this);
        tvTotalSize.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvTotalSize.setTextSize(14);
        LinearLayout.LayoutParams totalLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        totalLp.topMargin = dp(10);
        tvTotalSize.setLayoutParams(totalLp);
        root.addView(tvTotalSize);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        listLp.topMargin = dp(12);
        listContainer.setLayoutParams(listLp);
        root.addView(listContainer);
        return scrollView;
    }

    private void loadDownloaded() {
        offlineExamRepository.listDownloaded(this, (exams, totalBytes) -> {
        tvTotalSize.setText("Tổng dung lượng: " + formatBytes(totalBytes));
            renderRows(exams);
        });
    }

    private void renderRows(List<OfflineExamRepository.DownloadedExam> exams) {
        listContainer.removeAllViews();
        if (exams == null || exams.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chua co de nao duoc tai");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(15);
            listContainer.addView(empty);
            return;
        }
        for (OfflineExamRepository.DownloadedExam exam : exams) {
            listContainer.addView(buildRow(exam));
        }
    }

    private MaterialCardView buildRow(OfflineExamRepository.DownloadedExam exam) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(0xFFE0E0E0);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(row);

        TextView title = new TextView(this);
        title.setText(exam.title != null ? exam.title : "De offline");
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(title);

        TextView meta = new TextView(this);
        meta.setText(exam.questionCount + " câu · " + exam.durationMinutes + " phút · " + formatBytes(exam.sizeBytes));
        meta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        meta.setTextSize(13);
        row.addView(meta);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(8);
        actions.setLayoutParams(actionsLp);
        row.addView(actions);

        MaterialButton start = new MaterialButton(this);
        start.setText("Lam bai");
        start.setOnClickListener(v -> openExam(exam.examId));
        actions.addView(start);

        MaterialButton delete = new MaterialButton(this);
        delete.setText("Xóa");
        delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.error)));
        delete.setOnClickListener(v -> offlineExamRepository.delete(this, exam.examId, () -> {
            Toast.makeText(this, "Đã xóa đề offline", Toast.LENGTH_SHORT).show();
            loadDownloaded();
        }));
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        deleteLp.leftMargin = dp(8);
        delete.setLayoutParams(deleteLp);
        actions.addView(delete);
        return card;
    }

    private void openExam(long examId) {
        OfflineExamBundle bundle = offlineExamRepository.loadBundle(this, examId);
        if (bundle == null) {
            Toast.makeText(this, "Khong doc duoc de offline", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ExamSessionActivity.class);
        intent.putExtra("exam_id", examId);
        intent.putExtra("exam_title", bundle.getTitle());
        intent.putExtra("exam_subject", "");
        intent.putExtra("duration_minutes", bundle.getDurationMinutes() != null ? bundle.getDurationMinutes() : 60);
        intent.putExtra("total_questions", bundle.getQuestionCount() != null ? bundle.getQuestionCount() : 0);
        startActivity(intent);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        return String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
