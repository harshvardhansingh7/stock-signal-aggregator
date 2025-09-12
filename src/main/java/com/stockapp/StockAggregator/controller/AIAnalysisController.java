package com.stockapp.StockAggregator.controller;

import com.stockapp.StockAggregator.dto.AIAnalysisResponse;
import com.stockapp.StockAggregator.dto.StockAnalysisDTO;
import com.stockapp.StockAggregator.service.OpenRouterService;
import com.stockapp.StockAggregator.service.StockAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-analysis")
public class AIAnalysisController {

    private final StockAnalysisService aggregator;
    private final OpenRouterService openRouterService;

    public AIAnalysisController(StockAnalysisService aggregator, OpenRouterService openRouterService) {
        this.aggregator = aggregator;
        this.openRouterService = openRouterService;
    }

    /**
     * 1) GET /ai-analysis/{symbol} -> aggregator fetches data and calls OpenRouter to get final decision
     */
    @GetMapping("/{symbol}")
    public AIAnalysisResponse analyzeWithAI(@PathVariable String symbol) throws Exception {
        StockAnalysisDTO dto = aggregator.analyzeStock(symbol);
        return openRouterService.askAIForDecision(dto);
    }

    /**
     * 2) POST /ai-analysis -> user can send pre-built StockAnalysisDTO (e.g. for testing)
     */
    @PostMapping
    public AIAnalysisResponse analyzeWithAI(@RequestBody StockAnalysisDTO dto) throws Exception {
        return openRouterService.askAIForDecision(dto);
    }
}
