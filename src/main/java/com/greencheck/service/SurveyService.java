package com.greencheck.service;

import com.greencheck.domain.enums.SurveyMode;
import com.greencheck.dto.SurveyModeDto;
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

    public List<SurveyModeDto> getSurveyModes(){
        return Arrays.stream(SurveyMode.values())
                .map(SurveyModeDto::from)
                .collect(Collectors.toList());
    }
}
