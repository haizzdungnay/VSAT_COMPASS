package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class TopicStatsResponse {

    @SerializedName("topic_id")
    private Long topicId;

    @SerializedName("topic_name")
    private String topicName;

    private int correct;
    private int total;
    private int percentage;

    public Long getTopicId() { return topicId; }
    public String getTopicName() { return topicName; }
    public int getCorrect() { return correct; }
    public int getTotal() { return total; }
    public int getPercentage() { return percentage; }

    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public void setCorrect(int correct) { this.correct = correct; }
    public void setTotal(int total) { this.total = total; }
    public void setPercentage(int percentage) { this.percentage = percentage; }
}
