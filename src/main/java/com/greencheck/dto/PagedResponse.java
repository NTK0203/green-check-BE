package com.greencheck.dto;

import java.util.List;

public record PagedResponse<T>(
        int page,
        int size,
        long total,
        List<T> items) {

}
