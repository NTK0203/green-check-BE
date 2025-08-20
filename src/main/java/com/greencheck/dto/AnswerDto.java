package com.greencheck.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerDto {
    private Long questionId;
    private Long optionId;
}