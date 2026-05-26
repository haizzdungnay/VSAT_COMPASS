package com.example.v_sat_compass.ui.practice;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamHistoryEntry;
import com.example.v_sat_compass.data.model.Question;
import com.example.v_sat_compass.data.model.TopicStatsResponse;
import com.example.v_sat_compass.data.repository.ExamHistoryRepository;
import com.example.v_sat_compass.data.repository.ExamRepository;
import com.example.v_sat_compass.data.repository.StudentStatsRepository;
import com.example.v_sat_compass.databinding.FragmentPracticeBinding;
import com.example.v_sat_compass.ui.exam.ExamDetailActivity;
import com.example.v_sat_compass.util.NetworkUtils;
import com.example.v_sat_compass.util.OfflineDemoDataHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PracticeFragment extends Fragment {

    public interface LabelProvider {
        String labelFor(int percentage);
    }

    private FragmentPracticeBinding binding;
    private PracticeTopicAdapter adapter;
    private final StudentStatsRepository statsRepository = new StudentStatsRepository();
    private List<Exam> cachedExams = new ArrayList<>();
    private final boolean backendExamContent = ApiClient.USE_BACKEND_EXAM_CONTENT;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPracticeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new PracticeTopicAdapter();
        adapter.setLabelProvider(this::proficiencyLabel);
        adapter.setListener(this::openPracticeForTopic);
        binding.rvPracticeTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPracticeTopics.setAdapter(adapter);
        loadPracticeData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPracticeData();
    }

    private void loadPracticeData() {
        if (getContext() == null) return;
        if (!NetworkUtils.isOnline(requireContext())) {
            preloadExams(this::loadOfflinePracticeData);
            return;
        }
        preloadExams(() -> statsRepository.loadTopicStats(new StudentStatsRepository.TopicStatsCallback() {
            @Override
            public void onSuccess(List<TopicStatsResponse> topics) {
                if (binding == null) return;
                if (topics.isEmpty()) {
                    loadLocalTopicStats();
                } else {
                    renderTopics(topics);
                }
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                loadLocalTopicStats();
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void loadOfflinePracticeData() {
        if (binding == null || getContext() == null) return;
        loadLocalTopicStats(() -> {
            if (binding == null) return;
            Toast.makeText(requireContext(), R.string.offline_demo_mode, Toast.LENGTH_SHORT).show();
        });
    }

    private void preloadExams(Runnable onDone) {
        if (!backendExamContent) {
            cachedExams = ExamRepository.getInstance().getLocalPublishedExams(requireContext());
            onDone.run();
            return;
        }
        ExamRepository.getInstance().loadPublishedExams(null, new ExamRepository.ExamsCallback() {
            @Override
            public void onSuccess(List<Exam> exams) {
                cachedExams = exams != null ? exams : new ArrayList<>();
                onDone.run();
            }

            @Override
            public void onError(String message) {
                cachedExams = ExamRepository.getInstance().getLocalPublishedExams(requireContext());
                onDone.run();
            }
        });
    }

    private void loadLocalTopicStats() {
        loadLocalTopicStats(null);
    }

    private void loadLocalTopicStats(@Nullable Runnable afterRender) {
        if (getContext() == null) return;
        ExamHistoryRepository.getInstance().getAll(requireContext(), historyList -> {
            if (binding == null) return;
            Map<String, int[]> aggregates = new LinkedHashMap<>();
            for (ExamHistoryEntry entry : historyList) {
                Exam examDetail = LocalExamDataSource.getInstance()
                        .getExamDetail(requireContext(), entry.getExamId());
                if (examDetail == null || examDetail.getQuestions() == null) continue;

                Map<Long, Long> selectedAnswers = parseAnswers(entry.getSelectedAnswersJson());
                for (Exam.ExamQuestion eq : examDetail.getQuestions()) {
                    Question q = LocalExamDataSource.getInstance()
                            .getQuestion(requireContext(), eq.getQuestionId());
                    if (q == null) continue;
                    String topicName = q.getTopicName() != null ? q.getTopicName() : "Khác";
                    int[] counts = aggregates.computeIfAbsent(topicName, k -> new int[]{0, 0});
                    counts[1]++;
                    Long selected = selectedAnswers.get(q.getId());
                    Long correct = LocalExamDataSource.getInstance()
                            .getCorrectOptionId(requireContext(), q.getId());
                    if (selected != null && selected.equals(correct)) {
                        counts[0]++;
                    }
                }
            }
            List<TopicStatsResponse> result = new ArrayList<>();
            for (Map.Entry<String, int[]> entry : aggregates.entrySet()) {
                int correct = entry.getValue()[0];
                int total = entry.getValue()[1];
                TopicStatsResponse row = new TopicStatsResponse();
                row.setTopicName(entry.getKey());
                row.setCorrect(correct);
                row.setTotal(total);
                row.setPercentage(total > 0 ? (int) Math.round(correct * 100.0 / total) : 0);
                result.add(row);
            }
            if (result.isEmpty() && !NetworkUtils.isOnline(requireContext())) {
                renderTopics(OfflineDemoDataHelper.getDemoTopicStats());
            } else {
                renderTopics(result);
            }
            if (afterRender != null) {
                afterRender.run();
            }
        });
    }

    private void renderTopics(List<TopicStatsResponse> topics) {
        if (binding == null) return;
        if (topics.isEmpty()) {
            binding.tvPracticeEmpty.setVisibility(View.VISIBLE);
            binding.rvPracticeTopics.setVisibility(View.GONE);
        } else {
            binding.tvPracticeEmpty.setVisibility(View.GONE);
            binding.rvPracticeTopics.setVisibility(View.VISIBLE);
            adapter.setItems(topics);
        }
    }

    private Map<Long, Long> parseAnswers(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            Type type = new TypeToken<HashMap<Long, Long>>() {}.getType();
            Map<Long, Long> parsed = new Gson().fromJson(json, type);
            return parsed != null ? parsed : new HashMap<>();
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private void openPracticeForTopic(TopicStatsResponse topic) {
        if (getContext() == null) return;
        String subjectHint = guessSubjectFromTopic(topic.getTopicName());
        Exam exam = ExamRepository.getInstance().findExamBySubjectName(cachedExams, subjectHint);
        if (exam == null && !cachedExams.isEmpty()) {
            exam = cachedExams.get(0);
        }
        if (exam == null) {
            Toast.makeText(requireContext(), R.string.exam_not_found_offline, Toast.LENGTH_SHORT).show();
            return;
        }
        navigateToExamDetail(exam);
    }

    private String guessSubjectFromTopic(String topicName) {
        if (topicName == null) return "Toán";
        String lower = topicName.toLowerCase();
        if (lower.contains("english") || lower.contains("tiếng anh")) return "Tiếng Anh";
        if (lower.contains("vật") || lower.contains("physics")) return "Vật";
        return "Toán";
    }

    private void navigateToExamDetail(Exam exam) {
        Intent intent = new Intent(requireContext(), ExamDetailActivity.class);
        intent.putExtra("exam_id", exam.getId());
        intent.putExtra("exam_title", exam.getTitle());
        intent.putExtra("exam_description", exam.getDescription());
        intent.putExtra("exam_subject", exam.getSubjectName());
        intent.putExtra("total_questions", exam.getTotalQuestions());
        intent.putExtra("duration_minutes", exam.getDurationMinutes());
        startActivity(intent);
    }

    private String proficiencyLabel(int pct) {
        if (pct == 0) {
            return getString(R.string.practice_proficiency_start);
        } else if (pct < 50) {
            return getString(R.string.practice_proficiency_low, pct);
        } else if (pct < 80) {
            return getString(R.string.practice_proficiency_mid, pct);
        }
        return getString(R.string.practice_proficiency_high, pct);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
