package com.stockapp.StockAggregator.dto;

import java.util.Map;

public class StockAnalysisDTO {
    private String symbol;
    private Double livePrice;
    private Double dayHigh;
    private Double dayLow;
    private Double week52High;
    private Double week52Low;
    private Object fundamentals; // raw fundamentals JSON from StockDetails API
    private Map<String, IndicatorDetail> indicators; // keys: rsi, sma, ema, macd, ...
    private String overallDecision; // rule-based decision

    // getters & setters

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Double getLivePrice() { return livePrice; }
    public void setLivePrice(Double livePrice) { this.livePrice = livePrice; }

    public Double getDayHigh() { return dayHigh; }
    public void setDayHigh(Double dayHigh) { this.dayHigh = dayHigh; }

    public Double getDayLow() { return dayLow; }
    public void setDayLow(Double dayLow) { this.dayLow = dayLow; }

    public Double getWeek52High() { return week52High; }
    public void setWeek52High(Double week52High) { this.week52High = week52High; }

    public Double getWeek52Low() { return week52Low; }
    public void setWeek52Low(Double week52Low) { this.week52Low = week52Low; }

    public Object getFundamentals() { return fundamentals; }
    public void setFundamentals(Object fundamentals) { this.fundamentals = fundamentals; }

    public Map<String, IndicatorDetail> getIndicators() { return indicators; }
    public void setIndicators(Map<String, IndicatorDetail> indicators) { this.indicators = indicators; }

    public String getOverallDecision() { return overallDecision; }
    public void setOverallDecision(String overallDecision) { this.overallDecision = overallDecision; }

    public static class IndicatorDetail {
        private Object value;        // might be Number or Map for MACD
        private String meaning;      // human explanation
        private String recommendation; // BUY/SELL/HOLD

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }

        public String getMeaning() { return meaning; }
        public void setMeaning(String meaning) { this.meaning = meaning; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
}
