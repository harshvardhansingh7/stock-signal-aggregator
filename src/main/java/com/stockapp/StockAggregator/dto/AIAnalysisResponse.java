package com.stockapp.StockAggregator.dto;

public class AIAnalysisResponse {
    private String symbol;
    private String decision;  // BUY / SELL / HOLD
    private String reasoning; // detailed explanation

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}
