package com.stockapp.StockAggregator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.StockAggregator.dto.AIAnalysisResponse;
import com.stockapp.StockAggregator.dto.StockAnalysisDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Sends aggregated DTO to OpenRouter (AI reasoning) and parses response.
 */
@Service
public class OpenRouterService {

    @Value("${openrouter.api.url}")
    private String openRouterUrl;

    @Value("${openrouter.api.key}")
    private String openRouterKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenRouterService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends stock analysis to OpenRouter and expects a JSON object back:
     * { "symbol": "...", "decision": "BUY|SELL|HOLD", "reasoning": "..." }
     */
    public AIAnalysisResponse askAIForDecision(StockAnalysisDTO dto) throws Exception {
        String userPrompt = "You are an advanced financial analyst. Given the following stock analysis JSON, " +
                "return ONLY a JSON object in this exact format (no extra text): " +
                "{\"symbol\":\"<symbol>\", \"decision\":\"BUY|SELL|HOLD\", \"reasoning\":\"<detailed explanation>\"}. " +
                "Use the data to produce a single concise decision and a clear reasoning that references fundamentals and indicators. " +
                "Now here is the data: " + mapper.writeValueAsString(dto);

        Map<String, Object> messageSystem = Map.of(
                "role", "system",
                "content", "You are a senior institutional equity analyst."
        );
        Map<String, Object> messageUser = Map.of(
                "role", "user",
                "content", userPrompt
        );
        List<Map<String, Object>> messages = List.of(messageSystem, messageUser);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini"); // OpenRouter supports many models
        body.put("messages", messages);
        body.put("temperature", 0.0);

        // ✅ Required headers for OpenRouter
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openRouterKey);
        headers.set("HTTP-Referer", "http://localhost:8080"); // use your deployed backend domain in production
        headers.set("X-Title", "StockAggregator");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(openRouterUrl, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("OpenRouter request failed: " + response.getStatusCodeValue() + " " + response.getBody());
        }

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
        String content = contentNode.isMissingNode()
                ? root.path("choices").get(0).path("text").asText()
                : contentNode.asText();

        String cleaned = trimCodeFences(content).trim();

        AIAnalysisResponse result;
        try {
            result = mapper.readValue(cleaned, AIAnalysisResponse.class);
        } catch (Exception ex) {
            result = new AIAnalysisResponse();
            result.setSymbol(dto.getSymbol());
            result.setDecision("HOLD");
            result.setReasoning("AI did not return strict JSON. Raw response: " + cleaned);
        }
        return result;
    }

    private String trimCodeFences(String text) {
        if (text.startsWith("```")) {
            int idx = text.indexOf("\n");
            if (idx > 0) text = text.substring(idx + 1);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        }
        return text;
    }
}
