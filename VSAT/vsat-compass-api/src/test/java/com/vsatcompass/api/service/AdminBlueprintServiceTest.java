package com.vsatcompass.api.service;

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
import com.vsatcompass.api.service.impl.AdminBlueprintServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBlueprintService - Blueprint CRUD and V-SAT Matrix generation")
class AdminBlueprintServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ExamBlueprintRepository examBlueprintRepository;

    @InjectMocks
    private AdminBlueprintServiceImpl blueprintService;

    private Subject mathSubject;
    private Subject engSubject;
    private List<BlueprintResponse.BlueprintCellResponse> standardCells;

    @BeforeEach
    void setUp() {
        mathSubject = Subject.builder().id(1L).code("MATH").name("Toán").build();
        engSubject = Subject.builder().id(2L).code("ENG").name("Tiếng Anh").build();

        standardCells = List.of(
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(1L).topicId(1L).topicName("Phần I").questionType(QuestionType.TRUE_FALSE)
                        .cognitiveLevel(CognitiveLevel.UNDERSTANDING).difficulty(Difficulty.MEDIUM).questionCount(9)
                        .build(),
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(2L).topicId(2L).topicName("Phần II").questionType(QuestionType.SINGLE_CHOICE)
                        .cognitiveLevel(CognitiveLevel.RECOGNITION).difficulty(Difficulty.EASY).questionCount(6)
                        .build(),
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(3L).topicId(3L).topicName("Phần III").questionType(QuestionType.MATCHING)
                        .cognitiveLevel(CognitiveLevel.APPLICATION).difficulty(Difficulty.HARD).questionCount(5)
                        .build(),
                BlueprintResponse.BlueprintCellResponse.builder()
                        .id(4L).topicId(4L).topicName("Phần IV").questionType(QuestionType.SHORT_ANSWER)
                        .cognitiveLevel(CognitiveLevel.HIGH_APPLICATION).difficulty(Difficulty.VERY_HARD).questionCount(5)
                        .build()
        );
    }

    @Test
    @DisplayName("listBlueprints: reads from DB and auto-generates missing subjects")
    void listBlueprints_readsFromDbAndGeneratesMissing() {
        ExamBlueprint mathBp = ExamBlueprint.builder()
                .id(10L)
                .code("BP_MATH_DB")
                .name("Ma trận Toán DB")
                .subjectId(1L)
                .totalQuestions(25)
                .maxRawScore(new BigDecimal("150.00"))
                .version(1)
                .cellsJson(new ArrayList<>())
                .build();

        when(subjectRepository.findAll()).thenReturn(List.of(mathSubject, engSubject));
        when(examBlueprintRepository.findAll()).thenReturn(List.of(mathBp));

        List<BlueprintResponse> responses = blueprintService.listBlueprints(null);

        assertThat(responses).hasSize(2);
        BlueprintResponse mathResp = responses.stream().filter(r -> r.getSubjectId().equals(1L)).findFirst().orElseThrow();
        assertThat(mathResp.getCode()).isEqualTo("BP_MATH_DB");
        assertThat(mathResp.getCells()).hasSize(4);

        BlueprintResponse engResp = responses.stream().filter(r -> r.getSubjectId().equals(2L)).findFirst().orElseThrow();
        assertThat(engResp.getCode()).isEqualTo("VSAT_2026_BP_ENG");
        assertThat(engResp.getCells()).hasSize(4);
    }

    @Test
    @DisplayName("getBlueprint: prioritizes DB record over subject fallback")
    void getBlueprint_prioritizesDbRecord() {
        ExamBlueprint mathBp = ExamBlueprint.builder()
                .id(100L)
                .code("BP_MATH_CUSTOM")
                .name("Custom Math Blueprint")
                .subjectId(1L)
                .totalQuestions(25)
                .maxRawScore(new BigDecimal("150.00"))
                .version(2)
                .cellsJson(standardCells)
                .build();

        when(examBlueprintRepository.findById(100L)).thenReturn(Optional.of(mathBp));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));

        BlueprintResponse response = blueprintService.getBlueprint(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCode()).isEqualTo("BP_MATH_CUSTOM");
        assertThat(response.getSubjectCode()).isEqualTo("MATH");
        assertThat(response.getCells()).hasSize(4);
        verify(examBlueprintRepository).findById(100L);
    }

    @Test
    @DisplayName("getBlueprint: falls back to Subject when not in DB")
    void getBlueprint_fallbackToSubject() {
        when(examBlueprintRepository.findById(1L)).thenReturn(Optional.empty());
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));

        BlueprintResponse response = blueprintService.getBlueprint(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("VSAT_2026_BP_MATH");
        assertThat(response.getSubjectCode()).isEqualTo("MATH");
        assertThat(response.getCells()).hasSize(4);
    }

    @Test
    @DisplayName("getBlueprint: throws NOT_FOUND when neither DB nor Subject exists")
    void getBlueprint_notFound_throwsException() {
        when(examBlueprintRepository.findById(999L)).thenReturn(Optional.empty());
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blueprintService.getBlueprint(999L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("createBlueprint: saves and returns blueprint successfully")
    void createBlueprint_success() {
        BlueprintUpsertRequest request = BlueprintUpsertRequest.builder()
                .code("BP_NEW")
                .name("New Blueprint")
                .subjectId(1L)
                .totalQuestions(25)
                .maxRawScore(new BigDecimal("150.00"))
                .cells(standardCells)
                .build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));
        when(examBlueprintRepository.findByCode("BP_NEW")).thenReturn(Optional.empty());
        when(examBlueprintRepository.save(any(ExamBlueprint.class))).thenAnswer(inv -> {
            ExamBlueprint bp = inv.getArgument(0);
            bp.setId(50L);
            return bp;
        });

        BlueprintResponse response = blueprintService.createBlueprint(request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getCode()).isEqualTo("BP_NEW");
        assertThat(response.getName()).isEqualTo("New Blueprint");
        assertThat(response.getTotalQuestions()).isEqualTo(25);
        verify(examBlueprintRepository).save(any(ExamBlueprint.class));
    }

    @Test
    @DisplayName("createBlueprint: duplicate code throws 409 CONFLICT")
    void createBlueprint_duplicateCode_throwsConflict() {
        BlueprintUpsertRequest request = BlueprintUpsertRequest.builder()
                .code("BP_EXISTING")
                .name("New Blueprint")
                .subjectId(1L)
                .totalQuestions(25)
                .cells(standardCells)
                .build();

        ExamBlueprint existing = ExamBlueprint.builder().id(20L).code("BP_EXISTING").build();
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));
        when(examBlueprintRepository.findByCode("BP_EXISTING")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> blueprintService.createBlueprint(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(examBlueprintRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBlueprint: cell question count mismatch throws 400 BAD_REQUEST")
    void createBlueprint_countMismatch_throwsBadRequest() {
        List<BlueprintResponse.BlueprintCellResponse> invalidCells = List.of(
                BlueprintResponse.BlueprintCellResponse.builder().questionCount(10).build(),
                BlueprintResponse.BlueprintCellResponse.builder().questionCount(10).build()
        );

        BlueprintUpsertRequest request = BlueprintUpsertRequest.builder()
                .code("BP_INVALID_SUM")
                .name("Invalid Sum")
                .subjectId(1L)
                .totalQuestions(25)
                .cells(invalidCells)
                .build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));
        when(examBlueprintRepository.findByCode("BP_INVALID_SUM")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blueprintService.createBlueprint(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("updateBlueprint: updates and saves successfully")
    void updateBlueprint_success() {
        ExamBlueprint existing = ExamBlueprint.builder()
                .id(10L)
                .code("BP_OLD")
                .name("Old Name")
                .subjectId(1L)
                .totalQuestions(25)
                .maxRawScore(new BigDecimal("150.00"))
                .version(1)
                .build();

        BlueprintUpsertRequest request = BlueprintUpsertRequest.builder()
                .code("BP_UPDATED")
                .name("Updated Name")
                .subjectId(1L)
                .totalQuestions(25)
                .cells(standardCells)
                .build();

        when(examBlueprintRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));
        when(examBlueprintRepository.findByCode("BP_UPDATED")).thenReturn(Optional.empty());
        when(examBlueprintRepository.save(any(ExamBlueprint.class))).thenAnswer(inv -> inv.getArgument(0));

        BlueprintResponse response = blueprintService.updateBlueprint(10L, request);

        assertThat(response.getCode()).isEqualTo("BP_UPDATED");
        assertThat(response.getName()).isEqualTo("Updated Name");
        verify(examBlueprintRepository).save(existing);
    }

    @Test
    @DisplayName("updateBlueprint: duplicate code on other blueprint throws 409 CONFLICT")
    void updateBlueprint_duplicateCode_throwsConflict() {
        ExamBlueprint existing = ExamBlueprint.builder().id(10L).code("BP_MINE").subjectId(1L).build();
        ExamBlueprint other = ExamBlueprint.builder().id(20L).code("BP_OTHER").subjectId(1L).build();

        BlueprintUpsertRequest request = BlueprintUpsertRequest.builder()
                .code("BP_OTHER")
                .name("Updated Name")
                .subjectId(1L)
                .totalQuestions(25)
                .cells(standardCells)
                .build();

        when(examBlueprintRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mathSubject));
        when(examBlueprintRepository.findByCode("BP_OTHER")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> blueprintService.updateBlueprint(10L, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("deleteBlueprint: deletes successfully when exists")
    void deleteBlueprint_success() {
        ExamBlueprint existing = ExamBlueprint.builder().id(10L).build();
        when(examBlueprintRepository.findById(10L)).thenReturn(Optional.of(existing));

        blueprintService.deleteBlueprint(10L);

        verify(examBlueprintRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteBlueprint: throws NOT_FOUND when blueprint does not exist")
    void deleteBlueprint_notFound_throwsException() {
        when(examBlueprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blueprintService.deleteBlueprint(99L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
