package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.ExamDeliveryMode;
import com.vsatcompass.api.entity.enums.ResultReleasePolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExamWindowResponse {
    private Long id;
    private Long examId;
    private String examCode;
    private String examTitle;
    private Long subjectId;
    private String subjectName;
    private String windowCode;
    private String title;
    private String name;
    private ExamDeliveryMode mode;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private Integer gracePeriodMinutes;
    private Integer maxAttempts;
    private ResultReleasePolicy resultPolicy;
    private Boolean hardStopAtEnd;
    private Boolean allowOffline;
    private String status;
    private long activeCandidates;
    private long totalSubmissions;
    private OffsetDateTime createdAt;
}
