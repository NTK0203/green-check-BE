package com.greencheck.controller;

import com.greencheck.dto.SurveyModeDto;
import com.greencheck.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/survey")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping("/modes")
    public ResponseEntity<List<SurveyModeDto>> getSurveyModes(){
        List<SurveyModeDto> surveyModes = surveyService.getSurveyModes();
        return ResponseEntity.ok(surveyModes);
    }
}