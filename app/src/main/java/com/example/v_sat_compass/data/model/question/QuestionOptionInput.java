package com.example.v_sat_compass.data.model.question;

import com.google.gson.annotations.SerializedName;

public class QuestionOptionInput {
    @SerializedName("optionLabel")
    private String optionLabel;

    @SerializedName("optionText")
    private String optionText;

    @SerializedName("optionHtml")
    private String optionHtml;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("isCorrect")
    private Boolean isCorrect;

    @SerializedName("displayOrder")
    private Integer displayOrder;

    public QuestionOptionInput() {
    }

    public QuestionOptionInput(
            String optionLabel,
            String optionText,
            String optionHtml,
            String imageUrl,
            Boolean isCorrect,
            Integer displayOrder
    ) {
        this.optionLabel = optionLabel;
        this.optionText = optionText;
        this.optionHtml = optionHtml;
        this.imageUrl = imageUrl;
        this.isCorrect = isCorrect;
        this.displayOrder = displayOrder;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public String getOptionHtml() {
        return optionHtml;
    }

    public void setOptionHtml(String optionHtml) {
        this.optionHtml = optionHtml;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
