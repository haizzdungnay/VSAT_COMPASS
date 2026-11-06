package com.example.v_sat_compass.data.model.admission;

import com.google.gson.annotations.SerializedName;

public class AdmissionBenchmarkItem {
    @SerializedName("id")
    private Long id;

    @SerializedName("universityCode")
    private String universityCode;

    @SerializedName("universityName")
    private String universityName;

    @SerializedName("region")
    private String region;

    @SerializedName("majorCode")
    private String majorCode;

    @SerializedName("majorName")
    private String majorName;

    @SerializedName("subjectCombo")
    private String subjectCombo;

    @SerializedName("year")
    private Integer year;

    @SerializedName("cutoffScore")
    private Double cutoffScore;

    @SerializedName("notes")
    private String notes;

    public AdmissionBenchmarkItem() {}

    public AdmissionBenchmarkItem(Long id, String universityCode, String universityName,
                                  String region, String majorCode, String majorName,
                                  String subjectCombo, Integer year, Double cutoffScore, String notes) {
        this.id = id;
        this.universityCode = universityCode;
        this.universityName = universityName;
        this.region = region;
        this.majorCode = majorCode;
        this.majorName = majorName;
        this.subjectCombo = subjectCombo;
        this.year = year;
        this.cutoffScore = cutoffScore;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUniversityCode() { return universityCode; }
    public void setUniversityCode(String universityCode) { this.universityCode = universityCode; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMajorCode() { return majorCode; }
    public void setMajorCode(String majorCode) { this.majorCode = majorCode; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getSubjectCombo() { return subjectCombo; }
    public void setSubjectCombo(String subjectCombo) { this.subjectCombo = subjectCombo; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getCutoffScore() { return cutoffScore; }
    public void setCutoffScore(Double cutoffScore) { this.cutoffScore = cutoffScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
