package com.tcs.bancs.ai.prism_ai.agents.nodes;

import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

public class GeneralChatNode implements NodeAction {

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		return Map.of("error-message", """
				
				Thank you for your question. TCS BaNCS Prism AI is designed specifically for operational monitoring, log analysis, incident investigation, trace analysis, AI summaries, and root cause analysis. Your request appears to be outside the scope of this application.

				Please ask a question related to application logs, trace IDs, incidents, timelines, service interactions, AI summaries, or root cause analysis.
				
				""");
	}

}
