package com.vsatcompass.api.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicResponse {
    private Long id;
    private Long subjectId;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
}
