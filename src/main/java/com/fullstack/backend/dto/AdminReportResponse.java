package com.fullstack.backend.dto;

import java.util.List;
import java.util.Map;

public record AdminReportResponse(
        String id,
        String title,
        List<AdminReportItemResponse> items,
        List<Map<String, Object>> rows
) {
}
