package com.stockapp.StockAggregator.controller;


import com.stockapp.StockAggregator.dto.StockAnalysisDTO;
import com.stockapp.StockAggregator.service.StockAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
public class StockAnalysisController {

    private final StockAnalysisService service;

    public StockAnalysisController(StockAnalysisService service) {
        this.service = service;
    }

    /**
     * Rule-based aggregation (no AI) — returns the merged DTO
     */
    @GetMapping("/{symbol}")
    public StockAnalysisDTO analyze(@PathVariable String symbol) {
        return service.analyzeStock(symbol);
    }
}
