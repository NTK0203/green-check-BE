package com.greencheck.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SurveyMode {
    RESIDENTIAL("주거용"),
    NON_RESIDENTIAL("비주거용");

    private final String name;
}