package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.TopicStatsResponse;

import java.util.List;

public interface MyStatsService {

    List<TopicStatsResponse> getTopicStats(Long userId);
}
