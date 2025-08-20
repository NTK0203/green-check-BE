package com.greencheck.dto;

import com.greencheck.domain.QuizOption;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OptionDto {
    private Long optionId;
    private String valueCode;

    public OptionDto(QuizOption entity){
        this.optionId = entity.getId();
        this.valueCode = entity.getValueCode();
    }
}
