package com.example.v_sat_compass.data.local;

import android.content.Context;

import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.Question;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalExamDataSource {

    private static final String SAMPLE_ASSET_PREFIX = "sample_";
    private static final String SAMPLE_ASSET_SUFFIX = ".json";
    private static final String SAMPLE_ASSET_FILE = "sample_math_exam.json";
    private static final LocalExamDataSource INSTANCE = new LocalExamDataSource();

    private final Gson gson = new Gson();

    private boolean loaded;
    private final List<Exam> exams = new ArrayList<>();
    private final Map<Long, Question> questionsById = new HashMap<>();
    private final Map<Long, Long> correctOptionByQuestionId = new HashMap<>();

    private LocalExamDataSource() {}

    public static LocalExamDataSource getInstance() {
        return INSTANCE;
    }

    public synchronized List<Exam> getPublishedExams(Context context) {
        ensureLoaded(context);
        return new ArrayList<>(exams);
    }

    public synchronized Exam getExamDetail(Context context, long examId) {
        ensureLoaded(context);
        for (Exam exam : exams) {
            if (exam.getId() != null && exam.getId() == examId) {
                return exam;
            }
        }
        return exams.isEmpty() ? null : exams.get(0);
    }

    public synchronized Question getQuestion(Context context, long questionId) {
        ensureLoaded(context);
        return questionsById.get(questionId);
    }

    public synchronized Long getCorrectOptionId(Context context, long questionId) {
        ensureLoaded(context);
        return correctOptionByQuestionId.get(questionId);
    }

    private void ensureLoaded(Context context) {
        if (loaded) {
            return;
        }

        try {
            exams.clear();
            questionsById.clear();
            correctOptionByQuestionId.clear();

            List<String> examAssets = getExamAssetFiles(context);
            for (String assetFile : examAssets) {
                String json = readAssetAsString(context, assetFile);
                parseSampleExamJson(json, assetFile);
            }
            loaded = true;
        } catch (Exception ex) {
            exams.clear();
            questionsById.clear();
            correctOptionByQuestionId.clear();
            loaded = true;
        }
    }

    private List<String> getExamAssetFiles(Context context) {
        List<String> files = new ArrayList<>();
        try {
            String[] assets = context.getAssets().list("");
            if (assets != null) {
                for (String assetName : assets) {
                    if (assetName.startsWith(SAMPLE_ASSET_PREFIX) && assetName.endsWith(SAMPLE_ASSET_SUFFIX)) {
                        files.add(assetName);
                    }
                }
            }
        } catch (IOException ignored) {
        }

        if (files.isEmpty()) {
            files.add(SAMPLE_ASSET_FILE);
        }
        Collections.sort(files);
        return files;
    }

    private void parseSampleExamJson(String json, String sourceFileName) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject sourceExam = root.getAsJsonObject("exam");
        JsonArray sourceQuestions = root.getAsJsonArray("questions");

        if (sourceExam == null || sourceQuestions == null) {
            return;
        }

        long examId = sourceExam.has("id") ? sourceExam.get("id").getAsLong() : Math.abs(sourceFileName.hashCode());

        JsonObject examObj = new JsonObject();
        examObj.addProperty("id", examId);
        examObj.addProperty("title", getString(sourceExam, "title", "Đề thi mẫu"));
        examObj.addProperty("description", getString(sourceExam, "description", ""));
        examObj.addProperty("exam_code", getString(sourceExam, "exam_code", "VSAT-SAMPLE"));
        examObj.addProperty("subject_name", getString(sourceExam, "subject_name", "Toán học"));
        examObj.addProperty("total_questions", sourceQuestions.size());
        examObj.addProperty("duration_minutes", getInt(sourceExam, "duration_minutes", 90));
        examObj.addProperty("status", getString(sourceExam, "status", "PUBLISHED"));

        JsonArray examQuestions = new JsonArray();

        int fallbackOrder = 1;
        for (JsonElement element : sourceQuestions) {
            JsonObject sourceQuestion = element.getAsJsonObject();
            int order = getInt(sourceQuestion, "order", fallbackOrder++);
            long questionId = (examId * 100000L) + order;

            JsonObject examQuestionObj = new JsonObject();
            examQuestionObj.addProperty("question_id", questionId);
            examQuestionObj.addProperty("question_code", getString(sourceQuestion, "code", "Q-" + questionId));
            examQuestionObj.addProperty("question_order", order);
            examQuestions.add(examQuestionObj);

            JsonObject questionObj = new JsonObject();
            questionObj.addProperty("id", questionId);
            questionObj.addProperty("question_code", getString(sourceQuestion, "code", "Q-" + questionId));
            questionObj.addProperty("question_text", getString(sourceQuestion, "content", ""));
            questionObj.addProperty("explanation", getString(sourceQuestion, "explanation", ""));
            questionObj.addProperty("subject_name", getString(sourceExam, "subject_name", ""));
            questionObj.addProperty("topic_name", getString(sourceQuestion, "topic", ""));

            JsonArray convertedOptions = new JsonArray();
            JsonArray sourceOptions = sourceQuestion.getAsJsonArray("options");
            String correctLabel = getString(sourceQuestion, "correct", "");

            if (sourceOptions != null) {
                int optionIndex = 0;
                for (JsonElement optionElement : sourceOptions) {
                    optionIndex++;
                    JsonObject sourceOption = optionElement.getAsJsonObject();
                    String label = getString(sourceOption, "label", "");
                    long optionId = (questionId * 10) + optionIndex;

                    JsonObject optionObj = new JsonObject();
                    optionObj.addProperty("id", optionId);
                    optionObj.addProperty("option_label", label);
                    optionObj.addProperty("option_text", getString(sourceOption, "content", ""));
                    optionObj.addProperty("is_correct", label.equalsIgnoreCase(correctLabel));
                    optionObj.addProperty("display_order", optionIndex);
                    convertedOptions.add(optionObj);

                    if (label.equalsIgnoreCase(correctLabel)) {
                        correctOptionByQuestionId.put(questionId, optionId);
                    }
                }
            }

            questionObj.add("options", convertedOptions);
            Question convertedQuestion = gson.fromJson(questionObj, Question.class);
            questionsById.put(questionId, convertedQuestion);
        }

        examObj.add("questions", examQuestions);
        Exam convertedExam = gson.fromJson(examObj, Exam.class);

        exams.add(convertedExam);
    }

    private String readAssetAsString(Context context, String assetName) throws IOException {
        try (InputStream input = context.getAssets().open(assetName);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString();
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
