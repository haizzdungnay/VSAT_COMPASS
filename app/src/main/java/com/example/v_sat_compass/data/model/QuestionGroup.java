package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class QuestionGroup {
    @SerializedName("id")
    private Long id;

    @SerializedName("code")
    private String code;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("title")
    private String title;

    @SerializedName("passageText")
    private String passageText;

    @SerializedName("passageHtml")
    private String passageHtml;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("audioUrl")
    private String audioUrl;

    @SerializedName("audioDurationSec")
    private Integer audioDurationSec;

    @SerializedName("maxPlays")
    private Integer maxPlays = 2;

    public QuestionGroup() {}

    public QuestionGroup(Long id, String code, String title, String passageText) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.passageText = passageText;
    }

    public QuestionGroup(Long id, String code, String title, String passageText,
                         String audioUrl, Integer audioDurationSec, Integer maxPlays) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.passageText = passageText;
        this.audioUrl = audioUrl;
        this.audioDurationSec = audioDurationSec;
        this.maxPlays = maxPlays != null ? maxPlays : 2;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPassageText() { return passageText; }
    public void setPassageText(String passageText) { this.passageText = passageText; }

    public String getPassageHtml() { return passageHtml; }
    public void setPassageHtml(String passageHtml) { this.passageHtml = passageHtml; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Integer getAudioDurationSec() { return audioDurationSec; }
    public void setAudioDurationSec(Integer audioDurationSec) { this.audioDurationSec = audioDurationSec; }

    public Integer getMaxPlays() { return maxPlays != null ? maxPlays : 2; }
    public void setMaxPlays(Integer maxPlays) { this.maxPlays = maxPlays != null ? maxPlays : 2; }
}
