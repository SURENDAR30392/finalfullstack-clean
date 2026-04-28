package com.fullstack.backend.repository;

import com.fullstack.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findByUserId(Long userId);

    List<Enrollment> findByCourseCreatedById(Long instructorId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    void deleteByCourseId(Long courseId);

    void deleteByUserId(Long userId);
}
