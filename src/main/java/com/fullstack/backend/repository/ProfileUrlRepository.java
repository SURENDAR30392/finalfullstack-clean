package com.fullstack.backend.repository;

import com.fullstack.backend.entity.ProfileUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileUrlRepository extends JpaRepository<ProfileUrl, Long> {
    Optional<ProfileUrl> findTopByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
