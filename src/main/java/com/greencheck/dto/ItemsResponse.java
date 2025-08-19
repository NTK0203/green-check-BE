package com.greencheck.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ItemsResponse<T> {
    private List<T> items;
}
