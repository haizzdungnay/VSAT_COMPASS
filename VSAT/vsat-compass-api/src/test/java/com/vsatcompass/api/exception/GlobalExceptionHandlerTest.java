package com.vsatcompass.api.exception;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - Optimistic Locking & TD-2/TD-4 Integrity Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleOptimisticLocking with ObjectOptimisticLockingFailureException returns 409 and CONCURRENT_MODIFICATION")
    void handleOptimisticLocking_withObjectOptimisticLockingFailureException_returnsConflict() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException(Exam.class, 100L);

        ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLocking(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().getError().getMessage())
                .isEqualTo("Dữ liệu vừa được cập nhật bởi một phiên làm việc khác. Vui lòng tải lại trang.");
    }

    @Test
    @DisplayName("handleOptimisticLocking with OptimisticLockingFailureException returns 409 and CONCURRENT_MODIFICATION")
    void handleOptimisticLocking_withOptimisticLockingFailureException_returnsConflict() {
        OptimisticLockingFailureException ex =
                new OptimisticLockingFailureException("Concurrent modification detected");

        ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLocking(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    @DisplayName("Exam and Question entities have @Version annotation on lockVersion field")
    void examAndQuestion_haveVersionAnnotationOnLockVersion() throws NoSuchFieldException {
        // Exam verification
        Field examLockVersion = Exam.class.getDeclaredField("lockVersion");
        assertThat(examLockVersion.isAnnotationPresent(Version.class))
                .as("Exam.lockVersion must be annotated with @Version")
                .isTrue();
        Column examColumn = examLockVersion.getAnnotation(Column.class);
        assertThat(examColumn).isNotNull();
        assertThat(examColumn.name()).isEqualTo("lock_version");
        assertThat(examColumn.nullable()).isFalse();

        Field examBusinessVersion = Exam.class.getDeclaredField("version");
        assertThat(examBusinessVersion.isAnnotationPresent(Version.class))
                .as("Exam.version is a business field and must NOT have @Version")
                .isFalse();

        // Question verification
        Field questionLockVersion = Question.class.getDeclaredField("lockVersion");
        assertThat(questionLockVersion.isAnnotationPresent(Version.class))
                .as("Question.lockVersion must be annotated with @Version")
                .isTrue();
        Column questionColumn = questionLockVersion.getAnnotation(Column.class);
        assertThat(questionColumn).isNotNull();
        assertThat(questionColumn.name()).isEqualTo("lock_version");
        assertThat(questionColumn.nullable()).isFalse();

        Field questionBusinessVersion = Question.class.getDeclaredField("version");
        assertThat(questionBusinessVersion.isAnnotationPresent(Version.class))
                .as("Question.version is a business field and must NOT have @Version")
                .isFalse();
    }

    @Test
    @DisplayName("All 12 Flyway migration files exist in classpath db/migration/")
    void flywayMigrationFiles_twelveFilesExistOnClasspath() {
        List<String> expectedFiles = List.of(
                "V1__create_tickets.sql",
                "V2__add_leaderboard_opt_in.sql",
                "V3__add_vsat_domain_types.sql",
                "V4__extend_questions_for_vsat.sql",
                "V5__create_vsat_question_tables.sql",
                "V6__create_vsat_scoring_and_sections.sql",
                "V7__create_exam_blueprints_and_variants.sql",
                "V8__backfill_legacy_data.sql",
                "V9__add_vsat_constraints.sql",
                "V10__seed_vsat_blueprints.sql",
                "V11__add_exam_window_title.sql",
                "V12__add_lock_version.sql"
        );

        ClassLoader classLoader = getClass().getClassLoader();
        for (String fileName : expectedFiles) {
            URL resource = classLoader.getResource("db/migration/" + fileName);
            assertThat(resource)
                    .as("Flyway migration file db/migration/" + fileName + " must exist on classpath")
                    .isNotNull();
        }
    }
}
