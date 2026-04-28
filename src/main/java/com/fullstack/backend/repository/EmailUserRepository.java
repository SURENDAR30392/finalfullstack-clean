package com.fullstack.backend.repository;

import com.fullstack.backend.entity.EmailUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailUserRepository extends JpaRepository<EmailUser, Long> {

    Optional<EmailUser> findByEmail(String email);
}
