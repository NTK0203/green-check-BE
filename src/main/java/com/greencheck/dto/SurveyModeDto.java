package com.greencheck.dto;

import com.greencheck.domain.enums.SurveyMode;
import lombok.Getter;

@Getter
public class SurveyModeDto {

    private final String mode;
    private final String name;

    private SurveyModeDto(SurveyMode surveyMode){
        this.mode = surveyMode.name().toLowerCase();
        this.name = surveyMode.getName();
    }

    public static SurveyModeDto from(SurveyMode surveyMode){
        return new SurveyModeDto(surveyMode);
    }
}
