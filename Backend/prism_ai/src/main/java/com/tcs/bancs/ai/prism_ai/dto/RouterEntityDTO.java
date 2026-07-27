package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterEntityDTO {

	@JsonProperty(value = "trace_id")
	private String traceId;
	private String uuid;
	@JsonProperty(value = "service_name")
	private String serviceName;
	@JsonProperty(value = "span_id")
	private String spanId;
	@JsonProperty(value = "log_level")
	private String logLevel;
	@JsonProperty(value = "error_type")
	private String errorType;
	@JsonProperty(value = "business_entities")
	private List<RouterBusinessEntities> businessEntities;
	@JsonProperty(value = "time_ranges")
	private List<RouterTimeRange> timeRanges;

	
}
