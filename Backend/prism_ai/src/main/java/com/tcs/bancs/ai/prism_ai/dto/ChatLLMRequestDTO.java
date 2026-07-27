package com.tcs.bancs.ai.prism_ai.dto;

import java.util.Optional;

public record ChatLLMRequestDTO(String userQuery, Optional<String> conversationId, Long userId) {}