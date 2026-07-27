package com.tcs.bancs.ai.prism_ai.repository;

import com.tcs.bancs.ai.prism_ai.entity.ChatSessions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionsRepo extends JpaRepository<ChatSessions, Long> {
    ChatSessions findByConversationId(String s);
}
