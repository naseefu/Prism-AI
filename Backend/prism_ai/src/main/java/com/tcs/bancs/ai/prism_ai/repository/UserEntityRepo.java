package com.tcs.bancs.ai.prism_ai.repository;

import com.tcs.bancs.ai.prism_ai.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRepo extends JpaRepository<UserEntity, Long> {
}
