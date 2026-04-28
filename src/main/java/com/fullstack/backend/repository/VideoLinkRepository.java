package com.fullstack.backend.repository;

import com.fullstack.backend.entity.VideoLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoLinkRepository extends JpaRepository<VideoLink, Long> {

    void deleteByCourseId(Long courseId);

    List<VideoLink> findAllByOrderByCourseNameAscTopicNameAsc();
}
