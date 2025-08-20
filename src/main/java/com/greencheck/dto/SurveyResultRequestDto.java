package com.greencheck.dto;

import com.greencheck.domain.enums.SurveyMode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SurveyResultRequestDto {
    private SurveyMode surveyMode;
    private List<AnswerDto> answerList;
}
