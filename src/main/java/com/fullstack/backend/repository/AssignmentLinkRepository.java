package com.fullstack.backend.repository;

import com.fullstack.backend.entity.AssignmentLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentLinkRepository extends JpaRepository<AssignmentLink, Long> {

    void deleteByCourseId(Long courseId);

    void deleteByVideoId(Long videoId);
}
