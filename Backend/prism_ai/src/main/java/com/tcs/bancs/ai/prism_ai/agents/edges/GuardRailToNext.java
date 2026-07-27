package com.tcs.bancs.ai.prism_ai.agents.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class GuardRailToNext implements EdgeAction  {

	@Override
	public String apply(OverAllState state) {
		
		boolean isGuardrailPassed = state.value("guardrail-passed", true);
		
		if(isGuardrailPassed) {
			return "pass";
		}
		
		return "fail";
	}

}
