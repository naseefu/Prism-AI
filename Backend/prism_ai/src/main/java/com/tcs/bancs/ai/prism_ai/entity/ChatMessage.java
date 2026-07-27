package com.tcs.bancs.ai.prism_ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.ai.chat.messages.MessageType;

import java.time.Instant;

@Entity
@Table(name = "chat_history")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private MessageType role;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant createdAt;

    private String conversationId;

}
