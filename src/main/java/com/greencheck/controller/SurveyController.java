package com.greencheck.controller;

import com.greencheck.domain.enums.SurveyMode;
import com.greencheck.dto.SurveyDto;
import com.greencheck.dto.SurveyModeDto;
import com.greencheck.dto.SurveyResultDto;
import com.greencheck.dto.SurveyResultRequestDto;
import com.greencheck.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{mode}")
    public ResponseEntity<SurveyDto> getSurvey(@PathVariable String mode){
        try {
            SurveyMode surveyMode = SurveyMode.valueOf(mode.toUpperCase());
            SurveyDto surveyDto = surveyService.getSurvey(surveyMode);
            return ResponseEntity.ok(surveyDto);
        }catch (IllegalArgumentException e){
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/results")
    public ResponseEntity<SurveyResultDto> sendSurveyResults(@RequestBody SurveyResultRequestDto requestDto){
        SurveyResultDto surveyResultDto = surveyService.calculateAndGetResults(requestDto);
        return ResponseEntity.ok(surveyResultDto);
    }
}