package com.fullstack.backend.repository;

import com.fullstack.backend.entity.VideoProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {

    Optional<VideoProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    List<VideoProgress> findAllByUserIdAndCourseId(Long userId, Long courseId);

    List<VideoProgress> findByVideoId(Long videoId);

    List<VideoProgress> findByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);

    void deleteByUserId(Long userId);

    void deleteByVideoId(Long videoId);
}
