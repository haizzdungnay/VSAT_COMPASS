package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.BlueprintUpsertRequest;
import com.vsatcompass.api.dto.response.BlueprintResponse;
import com.vsatcompass.api.entity.ExamBlueprint;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.CognitiveLevel;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamBlueprintRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.service.AdminBlueprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminBlueprintServiceImpl implements AdminBlueprintService {

    private final SubjectRepository subjectRepository;
    private final ExamBlueprintRepository examBlueprintRepository;

    public AdminBlueprintServiceImpl(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
        this.examBlueprintRepository = null;
    }

    @Autowired
    public AdminBlueprintServiceImpl(
            SubjectRepository subjectRepository,
            @Autowired(required = false) ExamBlueprintRepository examBlueprintRepository) {
        this.subjectRepository = subjectRepository;
        this.examBlueprintRepository = examBlueprintRepository;
    }

    @Override
    @Cacheable(value = "blueprints", key = "#subjectId != null ? #subjectId : 'ALL'")
    public List<BlueprintResponse> listBlueprints(Long subjectId) {
        List<Subject> subjects = subjectRepository.findAll();
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));

        List<BlueprintResponse> result = new ArrayList<>();
        Set<Long> coveredSubjectIds = new HashSet<>();

        if (examBlueprintRepository != null) {
            List<ExamBlueprint> blueprints = (subjectId != null)
                    ? examBlueprintRepository.findBySubjectId(subjectId)
                    : examBlueprintRepository.findAll();

            for (ExamBlueprint bp : blueprints) {
                if (subjectId != null && !bp.getSubjectId().equals(subjectId)) {
                    continue;
                }
                coveredSubjectIds.add(bp.getSubjectId());
                Subject sub = subjectMap.get(bp.getSubjectId());
                if (sub == null) {
                    sub = subjectRepository.findById(bp.getSubjectId()).orElse(null);
                }
                List<BlueprintResponse.BlueprintCellResponse> cells = bp.getCellsJson();
                if (cells == null || cells.isEmpty()) {
                    cells = defaultCells();
                }
                result.add(mapToResponse(bp, sub, cells));
            }
        }

        for (Subject sub : subjects) {
            if (subjectId != null && !sub.getId().equals(subjectId)) {
                continue;
            }
            if (!coveredSubjectIds.contains(sub.getId())) {
                result.add(buildVsat2026Blueprint(sub));
            }
        }

        return result;
    }

    @Override
    @Cacheable(value = "blueprintDetail", key = "#id")
    public BlueprintResponse getBlueprint(Long id) {
        if (examBlueprintRepository != null) {
            Optional<ExamBlueprint> bpOpt = examBlueprintRepository.findById(id);
            if (bpOpt.isPresent()) {
                ExamBlueprint bp = bpOpt.get();
                Subject sub = subjectRepository.findById(bp.getSubjectId()).orElse(null);
                List<BlueprintResponse.BlueprintCellResponse> cells = bp.getCellsJson();
                if (cells == null || cells.isEmpty()) {
                    cells = defaultCells();
                }
                return mapToResponse(bp, sub, cells);
            }
        }

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Blueprint", id));
        return buildVsat2026Blueprint(subject);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "blueprints", allEntries = true),
            @CacheEvict(value = "blueprintDetail", allEntries = true)
    })
    public BlueprintResponse createBlueprint(BlueprintUpsertRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> AppException.notFound("Subject", request.getSubjectId()));

        if (examBlueprintRepository != null && examBlueprintRepository.findByCode(request.getCode()).isPresent()) {
            throw AppException.conflict("Mã ma trận blueprint đã tồn tại: " + request.getCode());
        }

        int totalQuestions = request.getTotalQuestions() != null ? request.getTotalQuestions() : 25;
        validateCellQuestionCounts(request.getCells(), totalQuestions);

        List<BlueprintResponse.BlueprintCellResponse> cells = (request.getCells() != null)
                ? request.getCells()
                : defaultCells();

        ExamBlueprint blueprint = ExamBlueprint.builder()
                .code(request.getCode())
                .name(request.getName())
                .subjectId(request.getSubjectId())
                .scoringProfileId(request.getScoringProfileId())
                .totalQuestions(totalQuestions)
                .maxRawScore(request.getMaxRawScore() != null ? request.getMaxRawScore() : new BigDecimal("150.00"))
                .version(request.getVersion() != null ? request.getVersion() : 1)
                .cellsJson(cells)
                .build();

        if (examBlueprintRepository != null) {
            blueprint = examBlueprintRepository.save(blueprint);
        }

        return mapToResponse(blueprint, subject, cells);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "blueprints", allEntries = true),
            @CacheEvict(value = "blueprintDetail", allEntries = true)
    })
    public BlueprintResponse updateBlueprint(Long id, BlueprintUpsertRequest request) {
        if (examBlueprintRepository == null) {
            throw AppException.notFound("Blueprint", id);
        }

        ExamBlueprint blueprint = examBlueprintRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Blueprint", id));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> AppException.notFound("Subject", request.getSubjectId()));

        examBlueprintRepository.findByCode(request.getCode())
                .filter(b -> !b.getId().equals(id))
                .ifPresent(b -> {
                    throw AppException.conflict("Mã ma trận blueprint đã tồn tại: " + request.getCode());
                });

        int totalQuestions = request.getTotalQuestions() != null ? request.getTotalQuestions() : 25;
        validateCellQuestionCounts(request.getCells(), totalQuestions);

        blueprint.setCode(request.getCode());
        blueprint.setName(request.getName());
        blueprint.setSubjectId(request.getSubjectId());
        if (request.getScoringProfileId() != null) {
            blueprint.setScoringProfileId(request.getScoringProfileId());
        }
        blueprint.setTotalQuestions(totalQuestions);
        if (request.getMaxRawScore() != null) {
            blueprint.setMaxRawScore(request.getMaxRawScore());
        }
        if (request.getVersion() != null) {
            blueprint.setVersion(request.getVersion());
        }
        blueprint.setCellsJson(request.getCells());

        blueprint = examBlueprintRepository.save(blueprint);
        return mapToResponse(blueprint, subject, blueprint.getCellsJson());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "blueprints", allEntries = true),
            @CacheEvict(value = "blueprintDetail", allEntries = true)
    })
    public void deleteBlueprint(Long id) {
        if (examBlueprintRepository == null) {
            throw AppException.notFound("Blueprint", id);
        }

        ExamBlueprint blueprint = examBlueprintRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Blueprint", id));

        examBlueprintRepository.delete(blueprint);
    }

    private void validateCellQuestionCounts(List<BlueprintResponse.BlueprintCellResponse> cells, int expectedTotal) {
        int cellSum = (cells != null)
                ? cells.stream().mapToInt(c -> c.getQuestionCount() != null ? c.getQuestionCount() : 0).sum()
                : 0;
        if (cellSum != expectedTotal) {
            throw AppException.badRequest(
                    "Tổng số câu hỏi trong ma trận (" + cellSum + ") không khớp với totalQuestions (" + expectedTotal + ")");
        }
    }

    private List<BlueprintResponse.BlueprintCellResponse> defaultCells() {
        return List.of(
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
    }

    private BlueprintResponse buildVsat2026Blueprint(Subject subject) {
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
                .cells(defaultCells())
                .build();
    }

    private BlueprintResponse mapToResponse(
            ExamBlueprint bp,
            Subject subject,
            List<BlueprintResponse.BlueprintCellResponse> cells) {
        return BlueprintResponse.builder()
                .id(bp.getId())
                .code(bp.getCode())
                .name(bp.getName())
                .subjectId(bp.getSubjectId())
                .subjectCode(subject != null ? subject.getCode() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .totalQuestions(bp.getTotalQuestions())
                .maxRawScore(bp.getMaxRawScore())
                .version(bp.getVersion())
                .cells(cells)
                .build();
    }
}
