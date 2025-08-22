package com.greencheck.service;

import com.greencheck.domain.QuizOption;
import com.greencheck.domain.QuizQuestion;
import com.greencheck.domain.QuizQuestionMessage;
import com.greencheck.domain.ScoreBand;
import com.greencheck.domain.enums.SurveyMode;
import com.greencheck.dto.*;
import com.greencheck.domain.repository.QuizQuestionMessageRepository;
import com.greencheck.domain.repository.QuizQuestionRepository;
import com.greencheck.domain.repository.ScoreBandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SurveyService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final ScoreBandRepository scoreBandRepository;
    private final QuizQuestionMessageRepository quizQuestionMessageRepository;

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

    @Transactional
    public SurveyResultDto calculateAndGetResults(SurveyResultRequestDto requestDto){
        //request로 들어온 질문 ID 목록 리스트화
        List<Long> questionIds = requestDto.getAnswerList().stream()
                .map(AnswerDto::getQuestionId)

                .toList();
        //해당 답변의 질문과 메시지를 DB로 조회
        Map<Long, QuizQuestion> questionMap = quizQuestionRepository.findByIdInWithFetchJoin(questionIds).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        Map<Long, QuizQuestionMessage> messageMap = quizQuestionMessageRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(QuizQuestionMessage::getId, Function.identity()));

        //점수 계산
        BigDecimal weightedScoreSum = BigDecimal.ZERO;
        List<String> strengths = new ArrayList<>();
        List<String> nextActions = new ArrayList<>();

        for(AnswerDto answer:requestDto.getAnswerList()){
            QuizQuestion question = questionMap.get(answer.getQuestionId());
            if(question==null)
                continue;
            QuizOption selectedOption = question.getOptions().stream()
                    .filter(opt->opt.getId().equals(answer.getOptionId()))
                    .findFirst()
                    .orElseThrow(()-> new IllegalArgumentException("유효하지 않은 optionID"));

            //reverse 적용 및 점수*가중치 합산
            BigDecimal baseScore = selectedOption.getScore();
            BigDecimal finalScore = question.isReverse() ? BigDecimal.ONE.subtract(baseScore):baseScore;
            weightedScoreSum = weightedScoreSum.add(finalScore.multiply(question.getWeight()));

            //피드백 필터링
            QuizQuestionMessage message = messageMap.get(question.getId());
            if(message != null) {
                if (finalScore.compareTo(new BigDecimal("0.6")) >= 0) {
                    strengths.add(message.getGoodText());
                } else {
                    nextActions.add(message.getImproveText());
                }
            }
        }
        //가중치 총합 계산
        BigDecimal totalWeight = quizQuestionRepository.findAllByMode(requestDto.getSurveyMode()).stream()
                .map(QuizQuestion::getWeight)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        if(totalWeight.compareTo(BigDecimal.ZERO)==0)
            return SurveyResultDto.builder().score(0).title("가중치 연산에 오류가 발생했습니다.").build();

        //최종 점수 계산, 결과 조회
        BigDecimal totalScore = weightedScoreSum.divide(totalWeight,4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        double resultScore = totalScore.setScale(1,RoundingMode.HALF_UP).doubleValue();

        ScoreBand scoreBand = scoreBandRepository.findBandByScore((int)resultScore)
                .orElseThrow(()->new IllegalStateException("점수 구간을 찾을 수 없습니다."));

        return SurveyResultDto.builder()
                .score(resultScore)
                .title(scoreBand.getLabel())
                .description(scoreBand.getDescription())
                .strengths(strengths)
                .nextActions(nextActions)
                .build();
    }
}