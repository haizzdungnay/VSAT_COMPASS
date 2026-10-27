package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.BlueprintResponse;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.CognitiveLevel;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.service.AdminBlueprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBlueprintServiceImpl implements AdminBlueprintService {

    private final SubjectRepository subjectRepository;

    @Override
    public List<BlueprintResponse> listBlueprints(Long subjectId) {
        List<Subject> subjects = subjectRepository.findAll();
        List<BlueprintResponse> result = new ArrayList<>();

        for (Subject sub : subjects) {
            if (subjectId != null && !sub.getId().equals(subjectId)) {
                continue;
            }
            result.add(buildVsat2026Blueprint(sub));
        }
        return result;
    }

    @Override
    public BlueprintResponse getBlueprint(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElse(null);
        if (subject == null) {
            subject = subjectRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> AppException.notFound("Blueprint", id));
        }
        return buildVsat2026Blueprint(subject);
    }

    private BlueprintResponse buildVsat2026Blueprint(Subject subject) {
        List<BlueprintResponse.BlueprintCellResponse> cells = List.of(
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(1L)
                        .topicId(1L)
                        .topicName("Phần I: Đúng / Sai")
                        .questionType(QuestionType.TRUE_FALSE)
                        .cognitiveLevel(CognitiveLevel.UNDERSTANDING)
                        .difficulty(Difficulty.MEDIUM)
                        .questionCount(9)
                        .build(),
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(2L)
                        .topicId(2L)
                        .topicName("Phần II: MCQ 4 lựa chọn")
                        .questionType(QuestionType.SINGLE_CHOICE)
                        .cognitiveLevel(CognitiveLevel.RECOGNITION)
                        .difficulty(Difficulty.EASY)
                        .questionCount(6)
                        .build(),
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(3L)
                        .topicId(3L)
                        .topicName("Phần III: Ghép hợp (Matching)")
                        .questionType(QuestionType.MATCHING)
                        .cognitiveLevel(CognitiveLevel.APPLICATION)
                        .difficulty(Difficulty.HARD)
                        .questionCount(5)
                        .build(),
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(4L)
                        .topicId(4L)
                        .topicName("Phần IV: Trả lời ngắn / Điền số")
                        .questionType(QuestionType.SHORT_ANSWER)
                        .cognitiveLevel(CognitiveLevel.HIGH_APPLICATION)
                        .difficulty(Difficulty.VERY_HARD)
                        .questionCount(5)
                        .build()
        );

        return BlueprintResponse.builder()
                .id(subject.getId())
                .code("VSAT_2026_BP_" + subject.getCode())
                .name("Ma trận V-SAT " + subject.getName() + " chuẩn 2026 (GDPT 2018)")
                .subjectId(subject.getId())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .totalQuestions(25)
                .maxRawScore(new BigDecimal("150.00"))
                .version(1)
                .cells(cells)
                .build();
    }
}