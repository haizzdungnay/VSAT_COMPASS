package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicStatsResponse {

    @JsonProperty("topic_id")
    private Long topicId;

    @JsonProperty("topic_name")
    private String topicName;

    private int correct;

    private int total;

    private int percentage;
}
