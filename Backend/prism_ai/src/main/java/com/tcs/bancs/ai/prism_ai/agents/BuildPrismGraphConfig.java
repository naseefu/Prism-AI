package com.tcs.bancs.ai.prism_ai.agents;

import java.util.HashMap;
import java.util.Map;

import com.tcs.bancs.ai.prism_ai.agents.nodes.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.tcs.bancs.ai.prism_ai.agents.edges.GuardRailToNext;
import com.tcs.bancs.ai.prism_ai.agents.edges.RouterNodeToNext;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

@Configuration
@RequiredArgsConstructor
public class BuildPrismGraphConfig {
	
	private final RouterNode routerNode;
	private final ChatNamingNode chatNamingNode;
	private final GuardRailsNode guardRailsNode;
	private final AISummaryNode aiSummaryNode;
	private final SystemChatNode systemChatNode;

	@Bean
	public CompiledGraph buildPrismAiGraph() throws GraphStateException {
		
		StateGraph stateGraph = new StateGraph("prism-log-agent", keyStrategyFactory());
		
		stateGraph.addNode("guardrails", node_async(guardRailsNode));
		stateGraph.addNode("router", node_async(routerNode));
		stateGraph.addNode("conversation-chat", node_async(new GeneralChatNode()));
		stateGraph.addNode("chat-naming", node_async(chatNamingNode));

		stateGraph.addNode("ops-chat", node_async(aiSummaryNode));
		stateGraph.addNode("system-chat", node_async(systemChatNode));
		
		stateGraph.addEdge(START, "guardrails");
		
		stateGraph.addConditionalEdges("guardrails", 
				edge_async(new GuardRailToNext()), 
				Map.of(
					"pass","chat-naming",
					"fail",END
				));

		stateGraph.addEdge("chat-naming", "router");
		
		stateGraph.addConditionalEdges("router", 
				edge_async(new RouterNodeToNext()), 
				Map.of(
					"CONVERSATION_CHAT", "conversation-chat",
						"OPS_CHAT","ops-chat", "SYSTEM_CHAT","system-chat"
				));

		stateGraph.addEdge("ops-chat", END);
		stateGraph.addEdge("conversation-chat", END);
		stateGraph.addEdge("system-chat", END);
		
		return stateGraph.compile();
		
	}
	
	public static KeyStrategyFactory keyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("errorLog", new ReplaceStrategy());
            strategies.put("patchAttempt", new ReplaceStrategy());
            strategies.put("buildStatus", new ReplaceStrategy());   // "SUCCESS" | "FAILED"
            strategies.put("retryCount", new ReplaceStrategy());
            return strategies;
        };
    }

}
