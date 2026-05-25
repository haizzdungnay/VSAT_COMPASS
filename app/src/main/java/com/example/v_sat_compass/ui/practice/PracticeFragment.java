package com.example.v_sat_compass.ui.practice;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.databinding.FragmentPracticeBinding;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PracticeFragment extends Fragment {

    private FragmentPracticeBinding binding;

    private static class TopicProgress {
        int correct = 0;
        int total = 0;
        
        int getPercentage() {
            if (total == 0) return 0;
            return (int) (correct * 100.0 / total);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPracticeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPracticeStats();
        setupPracticeClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPracticeStats();
    }

    private void loadPracticeStats() {
        if (getContext() == null) return;

        ExamHistoryRepository.getInstance().getAll(requireContext(), historyList -> {
            if (binding == null) return;

            Map<String, TopicProgress> progressMap = calculateTopicProgress(historyList);
            bindProgressData(progressMap);
        });
    }

    private Map<String, TopicProgress> calculateTopicProgress(List<ExamHistoryEntry> historyList) {
        Map<String, TopicProgress> map = new HashMap<>();
        map.put("Hình học không gian", new TopicProgress());
        map.put("Logarit & Hàm số mũ", new TopicProgress());
        map.put("Số học & Đại số", new TopicProgress());
        map.put("Giải tích", new TopicProgress());
        map.put("Xác suất & Thống kê", new TopicProgress());
        map.put("Vật lí hạt nhân", new TopicProgress());

        if (getContext() == null) return map;

        for (ExamHistoryEntry entry : historyList) {
            Exam examDetail = LocalExamDataSource.getInstance().getExamDetail(requireContext(), entry.getExamId());
            if (examDetail == null || examDetail.getQuestions() == null) continue;

            Map<Long, Long> selectedAnswers = new HashMap<>();
            String answersJson = entry.getSelectedAnswersJson();
            if (answersJson != null && !answersJson.isEmpty()) {
                try {
                    Type type = new TypeToken<HashMap<Long, Long>>() {}.getType();
                    Map<Long, Long> parsed = new Gson().fromJson(answersJson, type);
                    if (parsed != null) selectedAnswers = parsed;
                } catch (Exception ignored) {}
            }

            for (Exam.ExamQuestion eq : examDetail.getQuestions()) {
                Question q = LocalExamDataSource.getInstance().getQuestion(requireContext(), eq.getQuestionId());
                if (q == null) continue;

                String topicCategory = classifyTopic(q.getTopicName());
                TopicProgress progress = map.get(topicCategory);
                if (progress == null) {
                    progress = new TopicProgress();
                    map.put(topicCategory, progress);
                }

                Long selectedOpt = selectedAnswers.get(q.getId());
                Long correctOpt = LocalExamDataSource.getInstance().getCorrectOptionId(requireContext(), q.getId());
                boolean isCorrect = selectedOpt != null && selectedOpt.equals(correctOpt);

                progress.total++;
                if (isCorrect) progress.correct++;
            }
        }
        return map;
    }

    private String classifyTopic(String topicName) {
        if (topicName == null) return "Số học & Đại số";
        String lower = topicName.toLowerCase();
        if (lower.contains("logarithm") || lower.contains("hàm số mũ") || lower.contains("logarit")) {
            return "Logarit & Hàm số mũ";
        }
        if (lower.contains("đạo hàm") || lower.contains("cực trị") || lower.contains("giới hạn") 
                || lower.contains("tích phân") || lower.contains("tiệm cận") || lower.contains("diện tích") 
                || lower.contains("thể tích") || lower.contains("giải tích")) {
            return "Giải tích";
        }
        if (lower.contains("hoán vị") || lower.contains("tổ hợp") || lower.contains("xác suất") 
                || lower.contains("chỉnh hợp")) {
            return "Xác suất & Thống kê";
        }
        if (lower.contains("vectơ") || lower.contains("tọa độ") || lower.contains("đường thẳng") 
                || lower.contains("đường tròn") || lower.contains("khoảng cách") || lower.contains("hình học") 
                || lower.contains("hình cầu") || lower.contains("hình chóp") || lower.contains("elip") 
                || lower.contains("hình trụ")) {
            return "Hình học không gian";
        }
        if (lower.contains("hạt nhân") || lower.contains("vật lí") || lower.contains("vật lý")) {
            return "Vật lí hạt nhân";
        }
        return "Số học & Đại số";
    }

    private void bindProgressData(Map<String, TopicProgress> progressMap) {
        // Space Geometry
        TopicProgress geo = progressMap.get("Hình học không gian");
        int geoPct = geo != null ? geo.getPercentage() : 0;
        binding.pbSpaceGeometry.setProgress(geoPct);
        binding.tvSpaceGeometryPercent.setText(geoPct + "%");
        binding.tvSpaceGeometryLabel.setText(getProficiencyLabel(geoPct));

        // Logarithm
        TopicProgress log = progressMap.get("Logarit & Hàm số mũ");
        int logPct = log != null ? log.getPercentage() : 0;
        binding.pbLogarithm.setProgress(logPct);
        binding.tvLogarithmPercent.setText(logPct + "%");
        binding.tvLogarithmLabel.setText(getProficiencyLabel(logPct));

        // Algebra
        TopicProgress alg = progressMap.get("Số học & Đại số");
        int algPct = alg != null ? alg.getPercentage() : 0;
        binding.pbAlgebra.setProgress(algPct);
        binding.tvAlgebraPercent.setText(algPct + "%");

        // Calculus
        TopicProgress calc = progressMap.get("Giải tích");
        int calcPct = calc != null ? calc.getPercentage() : 0;
        binding.pbCalculus.setProgress(calcPct);
        binding.tvCalculusPercent.setText(calcPct + "%");

        // Probability
        TopicProgress prob = progressMap.get("Xác suất & Thống kê");
        int probPct = prob != null ? prob.getPercentage() : 0;
        binding.pbProbability.setProgress(probPct);
        binding.tvProbabilityPercent.setText(probPct + "%");

        // Physics
        TopicProgress phys = progressMap.get("Vật lí hạt nhân");
        int physPct = phys != null ? phys.getPercentage() : 0;
        binding.pbPhysics.setProgress(physPct);
        binding.tvPhysicsPercent.setText(physPct + "%");
    }

    private String getProficiencyLabel(int pct) {
        if (pct == 0) {
            return "Mức độ thành thạo: 0% - Hãy bắt đầu luyện tập!";
        } else if (pct < 50) {
            return "Mức độ thành thạo: " + pct + "% - Cần cố gắng!";
        } else if (pct < 80) {
            return "Mức độ thành thạo: " + pct + "% - Tiến bộ tốt!";
        } else {
            return "Mức độ thành thạo: " + pct + "% - Rất xuất sắc!";
        }
    }

    private void setupPracticeClickListeners() {
        // Math exam ID = 1
        View.OnClickListener startMathExam = v -> navigateToExamDetail(1L);
        binding.btnSpaceGeometryPractice.setOnClickListener(startMathExam);
        binding.btnLogarithmPractice.setOnClickListener(startMathExam);
        binding.btnAlgebraPractice.setOnClickListener(startMathExam);
        binding.btnCalculusPractice.setOnClickListener(startMathExam);
        binding.btnProbabilityPractice.setOnClickListener(startMathExam);

        // Physics exam ID = 3
        binding.btnPhysicsPractice.setOnClickListener(v -> navigateToExamDetail(3L));
    }

    private void navigateToExamDetail(long examId) {
        if (getContext() == null) return;
        Exam exam = LocalExamDataSource.getInstance().getExamDetail(requireContext(), examId);
        if (exam != null) {
            Intent intent = new Intent(getContext(), ExamDetailActivity.class);
            intent.putExtra("exam_id", exam.getId());
            intent.putExtra("exam_title", exam.getTitle());
            intent.putExtra("exam_description", exam.getDescription());
            intent.putExtra("exam_subject", exam.getSubjectName());
            intent.putExtra("total_questions", exam.getTotalQuestions());
            intent.putExtra("duration_minutes", exam.getDurationMinutes());
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}