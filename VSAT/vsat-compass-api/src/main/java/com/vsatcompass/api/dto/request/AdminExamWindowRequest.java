package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.ExamDeliveryMode;
import com.vsatcompass.api.entity.enums.ResultReleasePolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExamWindowRequest {

    @NotNull(message = "examId kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    private Long examId;

    @NotBlank(message = "windowCode kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    @Size(max = 50, message = "windowCode t\u1ed1i \u0111a 50 k\u00fd t\u1ef1")
    private String windowCode;

    @Size(max = 200, message = "title t\u1ed1i \u0111a 200 k\u00fd t\u1ef1")
    private String title;

    @Builder.Default
    private ExamDeliveryMode mode = ExamDeliveryMode.SCHEDULED_MOCK;

    private OffsetDateTime startAt;
    private OffsetDateTime endAt;

    @Min(value = 0, message = "gracePeriodMinutes ph\u1ea3i >= 0")
    @Builder.Default
    private Integer gracePeriodMinutes = 0;

    @Min(value = 1, message = "maxAttempts ph\u1ea3i >= 1")
    private Integer maxAttempts;

    @Builder.Default
    private ResultReleasePolicy resultPolicy = ResultReleasePolicy.IMMEDIATE_FULL;

    @Builder.Default
    private Boolean hardStopAtEnd = true;

    @Builder.Default
    private Boolean allowOffline = false;
}
