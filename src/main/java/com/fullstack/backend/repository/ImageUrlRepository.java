package com.fullstack.backend.repository;

import com.fullstack.backend.entity.ImageUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageUrlRepository extends JpaRepository<ImageUrl, Long> {

    Optional<ImageUrl> findTopByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);

    List<ImageUrl> findAllByOrderByCreatedAtDescIdDesc();

    void deleteByCourseId(Long courseId);
}
