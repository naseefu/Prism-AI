package com.tcs.bancs.ai.prism_ai.agents.nodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

@Component
public class GuardRailsNode implements NodeAction  {
	
	private final List<String> keywords;

	public GuardRailsNode(){
		this.keywords = readAllGRKeywords();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		
		String userQuery = state.value("message", "");
		
		if(!ObjectUtils.isEmpty(userQuery)) {
			
			List<String> keywordsInUserQuery = generateCombinations(userQuery);
			
			for(String kw:keywordsInUserQuery) {
				if(keywords.contains(kw)) {
					return Map.of("guardrail-passed", false, "error-message","Request blocked as it violates our policy..");
				}
			}
			
			return Map.of("guardrail-passed", true);
			
		}
		
		return Map.of("guardrail-passed", false, "error-message","Request blocked as it violates our policy..");
	}
	
	public static List<String> readAllGRKeywords(){
		
		List<String> kws = new ArrayList<String>();

		try(Stream<Path> paths = Files.walk(Paths.get("./guardrails"))) {

			paths
			.filter(f->f.toFile().isFile())
			.forEach(path->{
				try {
					List<String> allLines = Files.readAllLines(path);
					
					for(String line:allLines) {
						if(!ObjectUtils.isEmpty(line)) {
							Arrays.stream(line.split(",")).map(String::trim).forEach(kws::add);
						}
					}
				}
				catch (Exception _) {
				}
			});
			
		}
		
	
		catch (Exception _) {
		}
		return kws;
	}
	
	public static List<String> generateCombinations(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return List.of();
        }

        // Clean, lowercase, and split the query into individual words
        List<String> words = Arrays.stream(userQuery.toLowerCase().split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> combinations = new ArrayList<>();
        int n = words.size();

        // Generate all possible n-grams (from length 1 up to total words)
        if(n<30){
			for (int i = 0; i < n; i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = i; j < n; j++) {
					if (j > i) {
						sb.append(" ");
					}
					sb.append(words.get(j));
					combinations.add(sb.toString());
				}
			}
		}
		else {
			combinations.addAll(words);
		}

        return combinations;
    }
	
}
