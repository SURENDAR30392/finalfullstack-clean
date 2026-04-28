package com.fullstack.backend.dto;

import java.util.List;

public record LoadDataRequest(
        List<FrontendCoursePayload> courses
) {
}
