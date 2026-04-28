package com.fullstack.backend.repository;

import com.fullstack.backend.entity.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<CourseVideo, Long> {

    List<CourseVideo> findByCourseIdOrderByIdAsc(Long courseId);

    List<CourseVideo> findByApprovalStatusOrderByIdDesc(String approvalStatus);

    void deleteByCourseId(Long courseId);

    long countByApprovalStatus(String approvalStatus);
}
