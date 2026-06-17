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

    @Test
    @DisplayName("getWeakTopics returns lowest percentages first and respects limit")
    void getWeakTopics_sortsWeakestFirst() {
        when(sessionAnswerRepository.aggregateTopicStatsByUserId(5L)).thenReturn(List.of(
                projection(1L, "Dai so", 10L, 9L),
                projection(2L, "Hinh hoc", 10L, 3L),
                projection(3L, "Xac suat", 10L, 5L)
        ));

        List<TopicStatsResponse> stats = myStatsService.getWeakTopics(5L, 2);

        assertThat(stats).extracting(TopicStatsResponse::getTopicName)
                .containsExactly("Hinh hoc", "Xac suat");
        assertThat(stats).extracting(TopicStatsResponse::getPercentage)
                .containsExactly(30, 50);
    }

    private TopicStatsProjection projection(Long id, String name, Long total, Long correct) {
        return new TopicStatsProjection() {
            @Override public Long getTopicId() { return id; }
            @Override public String getTopicName() { return name; }
            @Override public Long getTotalAttempts() { return total; }
            @Override public Long getCorrectCount() { return correct; }
        };
    }
}
