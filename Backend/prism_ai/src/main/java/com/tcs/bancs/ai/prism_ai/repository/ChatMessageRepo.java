package com.tcs.bancs.ai.prism_ai.repository;

import com.tcs.bancs.ai.prism_ai.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(String s);
}
