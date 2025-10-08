package com.capstone.ai_interview_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.capstone.ai_interview_be.model.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    UserEntity findByEmail(@Param("email") String email);

    @Query("SELECT u.role FROM UserEntity u WHERE u.email = :email")
    String findRoleByEmail(@Param("email") String email);
}
