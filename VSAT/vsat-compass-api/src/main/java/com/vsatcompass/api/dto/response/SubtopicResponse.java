package com.vsatcompass.api.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtopicResponse {
    private Long id;
    private Long topicId;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
}
