package com.greencheck.dto;

import com.greencheck.domain.QuizOption;
import com.greencheck.domain.QuizQuestion;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class QuestionDto {
    private Long questionId;
    private String content;
    private List<OptionDto> options;

    public QuestionDto(QuizQuestion entity){
        this.questionId = entity.getId();
        this.content = entity.getContent();
        this.options = entity.getOptions().stream()
                .map(OptionDto::new)
                .collect(Collectors.toList());
    }
}
