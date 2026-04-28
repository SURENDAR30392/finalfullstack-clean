package com.fullstack.backend.repository;

import com.fullstack.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCreatedById(Long createdById);

    List<Course> findByApprovalStatusOrderByIdDesc(String approvalStatus);

    long countByApprovalStatus(String approvalStatus);
}
