package com.greencheck.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SurveyDto {
    private final String surveyTitle;
    private final List<QuestionDto> questions;
}
