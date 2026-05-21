package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.SubtopicResponse;
import com.example.v_sat_compass.data.model.TopicResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TopicApi {

    @GET("subjects/{subjectId}/topics")
    Call<ApiResponse<List<TopicResponse>>> listTopics(
            @Path("subjectId") Long subjectId
    );

    @GET("subjects/{subjectId}/topics/{topicId}/subtopics")
    Call<ApiResponse<List<SubtopicResponse>>> listSubtopics(
            @Path("subjectId") Long subjectId,
            @Path("topicId") Long topicId
    );
}
