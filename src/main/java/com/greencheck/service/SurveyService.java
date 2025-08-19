package com.greencheck.service;

import com.greencheck.domain.QuizQuestion;
import com.greencheck.domain.QuizQuestionMessage;
import com.greencheck.domain.enums.SurveyMode;
import com.greencheck.dto.QuestionDto;
import com.greencheck.dto.SurveyDto;
import com.greencheck.dto.SurveyModeDto;
import com.greencheck.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SurveyService {

    private final QuizQuestionRepository quizQuestionRepository;

    public List<SurveyModeDto> getSurveyModes(){
        return Arrays.stream(SurveyMode.values())
                .map(SurveyModeDto::from)
                .collect(Collectors.toList());
    }

    public SurveyDto getSurvey(SurveyMode mode){
        List<QuizQuestion> questions = quizQuestionRepository.findAllByModeWithFetchJoin(mode);

        if(questions.isEmpty()){
            throw new IllegalArgumentException("해당 문답이 존재하지 않습니다."+mode);
        }
        List<QuestionDto> questionDtos = questions.stream()
                .map(QuestionDto::new)
                .collect(Collectors.toList());

        return SurveyDto.builder()
                .surveyTitle(getSurveyTitleByMode(mode))
                .questions(questionDtos)
                .build();
    }

    private String getSurveyTitleByMode(SurveyMode mode) {
        return mode.getName()+"건축물 자가진단";
    }
}