package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatNamingResponseDTO {

    @JsonProperty(value = "chat-name")
    private String chatName;

}
