package com.vsatcompass.api.repository.projection;

public interface TopicStatsProjection {

    Long getTopicId();

    String getTopicName();

    Long getTotalAttempts();

    Long getCorrectCount();
}
