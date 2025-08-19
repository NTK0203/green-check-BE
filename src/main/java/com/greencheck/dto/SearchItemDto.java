package com.greencheck.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchItemDto {
    private Long id;
    private String label;
    private String name;
    private String address;
    private Double lat;
    private Double lng;
}
