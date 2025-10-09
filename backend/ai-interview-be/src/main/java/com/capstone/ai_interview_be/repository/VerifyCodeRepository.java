package com.capstone.ai_interview_be.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.model.VerifyCodeEntity;

public interface VerifyCodeRepository extends JpaRepository<VerifyCodeEntity, Long> {
    @Query("SELECT v FROM VerifyCodeEntity v WHERE v.code = :code")
    Optional<VerifyCodeEntity> findByCode(@Param("code") String code);

    @Query("SELECT v FROM VerifyCodeEntity v WHERE v.user = :user")
    Optional<VerifyCodeEntity> findByUser(@Param("user") UserEntity user);
}
