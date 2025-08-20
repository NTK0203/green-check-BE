package com.greencheck.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SurveyResultDto {
    private double score;
    private String title;
    private String description;
    private List<String> strengths;
    private List<String> nextActions;
}
