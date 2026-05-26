package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.TopicStatsResponse;
import com.vsatcompass.api.repository.SessionAnswerRepository;
import com.vsatcompass.api.repository.projection.TopicStatsProjection;
import com.vsatcompass.api.service.impl.MyStatsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyStatsServiceTest {

    @Mock SessionAnswerRepository sessionAnswerRepository;

    @InjectMocks MyStatsServiceImpl myStatsService;

    @Test
    @DisplayName("getTopicStats maps projection to percentage")
    void getTopicStats_mapsProjection() {
        TopicStatsProjection projection = new TopicStatsProjection() {
            @Override public Long getTopicId() { return 10L; }
            @Override public String getTopicName() { return "Giải tích"; }
            @Override public Long getTotalAttempts() { return 4L; }
            @Override public Long getCorrectCount() { return 3L; }
        };
        when(sessionAnswerRepository.aggregateTopicStatsByUserId(5L)).thenReturn(List.of(projection));

        List<TopicStatsResponse> stats = myStatsService.getTopicStats(5L);

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getTopicName()).isEqualTo("Giải tích");
        assertThat(stats.get(0).getCorrect()).isEqualTo(3);
        assertThat(stats.get(0).getTotal()).isEqualTo(4);
        assertThat(stats.get(0).getPercentage()).isEqualTo(75);
    }
}
