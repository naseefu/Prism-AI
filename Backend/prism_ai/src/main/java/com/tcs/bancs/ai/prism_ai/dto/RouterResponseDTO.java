package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterResponseDTO {
	
	private String intent;
	private String confidence;
	private RouterEntityDTO entities;
	private boolean resolvedFromHistory;
	@JsonProperty(value = "requires_log_search")
	private boolean requiresLogSearch;
	private String reason;


}
