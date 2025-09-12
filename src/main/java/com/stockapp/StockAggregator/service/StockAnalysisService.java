package com.stockapp.StockAggregator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.StockAggregator.dto.StockAnalysisDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregates stock-related data from external StockDetails API.
 */
@Service
public class StockAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${stockdetailsapi.url}")
    private String stockDetailsBase;

    public StockAnalysisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public StockAnalysisDTO analyzeStock(String symbol) {
        StockAnalysisDTO dto = new StockAnalysisDTO();
        dto.setSymbol(symbol);

        try {
            // Live price endpoint - adapt to your StockDetailsAPI endpoint path
            JsonNode live = restTemplate.getForObject(stockDetailsBase + "/api/price/" + symbol + "/live", JsonNode.class);
            if (live != null) {
                if (live.has("price")) dto.setLivePrice(live.get("price").asDouble());
                if (live.has("timestamp")) { /* could store */ }
            }

            // Today's high/low
            JsonNode today = restTemplate.getForObject(stockDetailsBase + "/api/price/" + symbol + "/today", JsonNode.class);
            if (today != null) {
                if (today.has("high")) dto.setDayHigh(today.get("high").asDouble());
                if (today.has("low")) dto.setDayLow(today.get("low").asDouble());
            }

            // 52 week
            JsonNode w52 = restTemplate.getForObject(stockDetailsBase + "/api/price/" + symbol + "/52week", JsonNode.class);
            if (w52 != null) {
                if (w52.has("week52High")) dto.setWeek52High(w52.get("week52High").asDouble());
                else if (w52.has("high")) dto.setWeek52High(w52.get("high").asDouble());
                if (w52.has("week52Low")) dto.setWeek52Low(w52.get("week52Low").asDouble());
                else if (w52.has("low")) dto.setWeek52Low(w52.get("low").asDouble());
            }

            // Fundamentals (raw)
            Object fundamentals = restTemplate.getForObject(stockDetailsBase + "/api/fundamentals/" + symbol, Object.class);
            dto.setFundamentals(fundamentals);

            // Indicators - fetch latest values (assumes your endpoints return DTO-like objects)
            Map<String, StockAnalysisDTO.IndicatorDetail> indicators = new HashMap<>();

            // RSI
            JsonNode rsiJson = safeGet(stockDetailsBase + "/api/indicators/" + symbol + "/rsi");
            if (rsiJson != null) {
                Double rsiValue = extractNumericValue(rsiJson);
                StockAnalysisDTO.IndicatorDetail rsiDetail = interpretRSI(rsiValue);
                indicators.put("rsi", rsiDetail);
            }

            // SMA (example: 20)
            JsonNode smaJson = safeGet(stockDetailsBase + "/api/indicators/" + symbol + "/sma:20");
            if (smaJson != null) {
                Double smaValue = extractNumericValue(smaJson);
                StockAnalysisDTO.IndicatorDetail smaDetail = interpretSMA(smaValue, dto.getLivePrice());
                indicators.put("sma_20", smaDetail);
            }

            // EMA (example: 14)
            JsonNode emaJson = safeGet(stockDetailsBase + "/api/indicators/" + symbol + "/ema:14");
            if (emaJson != null) {
                Double emaValue = extractNumericValue(emaJson);
                StockAnalysisDTO.IndicatorDetail emaDetail = interpretEMA(emaValue, dto.getLivePrice());
                indicators.put("ema_14", emaDetail);
            }

            // MACD (fetch latest macd object)
            JsonNode macdJson = safeGet(stockDetailsBase + "/api/indicators/" + symbol + "/macd:12:26:9");
            if (macdJson != null) {
                // macdJson might be { "date":"..", "macd":..., "signal":..., "histogram":... }
                StockAnalysisDTO.IndicatorDetail macdDetail = interpretMACD(macdJson);
                indicators.put("macd", macdDetail);
            }

            dto.setIndicators(indicators);

            // Combine decisions using a weighted strategy
            dto.setOverallDecision(combineStrategy(indicators));

        } catch (RestClientException ex) {
            // handle remote errors gracefully
            throw new RuntimeException("Failed to fetch data from StockDetails API: " + ex.getMessage(), ex);
        }

        return dto;
    }

    private JsonNode safeGet(String url) {
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractNumericValue(JsonNode node) {
        // Tries common keys: value, price, close, macd (if scalar)
        if (node == null) return null;
        if (node.has("value")) return node.get("value").asDouble();
        if (node.has("price")) return node.get("price").asDouble();
        if (node.has("close")) return node.get("close").asDouble();
        if (node.has("macd")) return node.get("macd").asDouble();
        // if node is a plain number
        if (node.isNumber()) return node.asDouble();
        return null;
    }

    // Interpretations: market-aware meaning strings
    private StockAnalysisDTO.IndicatorDetail interpretRSI(Double value) {
        StockAnalysisDTO.IndicatorDetail detail = new StockAnalysisDTO.IndicatorDetail();
        detail.setValue(value);
        if (value == null) {
            detail.setMeaning("RSI not available");
            detail.setRecommendation("HOLD");
            return detail;
        }

        if (value < 30) {
            detail.setMeaning(String.format("RSI %.2f: Deep/strong oversold conditions often indicate a potential bullish reversal. Contrarian traders may see buying opportunity.", value));
            detail.setRecommendation("BUY");
        } else if (value < 40) {
            detail.setMeaning(String.format("RSI %.2f: Mildly oversold; watch for reversal signals and confirm with volume and other indicators.", value));
            detail.setRecommendation("BUY");
        } else if (value <= 60) {
            detail.setMeaning(String.format("RSI %.2f: Neutral zone — the market is balanced between buyers and sellers.", value));
            detail.setRecommendation("HOLD");
        } else if (value <= 70) {
            detail.setMeaning(String.format("RSI %.2f: Mildly overbought; monitor for weakening momentum.", value));
            detail.setRecommendation("HOLD");
        } else {
            detail.setMeaning(String.format("RSI %.2f: Overbought conditions suggest possible pullback or correction; risk of near-term selling.", value));
            detail.setRecommendation("SELL");
        }
        return detail;
    }

    private StockAnalysisDTO.IndicatorDetail interpretSMA(Double sma, Double price) {
        StockAnalysisDTO.IndicatorDetail detail = new StockAnalysisDTO.IndicatorDetail();
        detail.setValue(sma);
        if (sma == null || price == null) {
            detail.setMeaning("SMA or price not available");
            detail.setRecommendation("HOLD");
            return detail;
        }
        if (price > sma) {
            detail.setMeaning(String.format("Price (%.2f) above SMA (%.2f): short-to-mid-term momentum is bullish.", price, sma));
            detail.setRecommendation("BUY");
        } else if (price < sma) {
            detail.setMeaning(String.format("Price (%.2f) below SMA (%.2f): short-to-mid-term momentum is bearish.", price, sma));
            detail.setRecommendation("SELL");
        } else {
            detail.setMeaning(String.format("Price equals SMA: neutral trend.", price));
            detail.setRecommendation("HOLD");
        }
        return detail;
    }

    private StockAnalysisDTO.IndicatorDetail interpretEMA(Double ema, Double price) {
        StockAnalysisDTO.IndicatorDetail detail = new StockAnalysisDTO.IndicatorDetail();
        detail.setValue(ema);
        if (ema == null || price == null) {
            detail.setMeaning("EMA or price not available");
            detail.setRecommendation("HOLD");
            return detail;
        }
        if (price > ema) {
            detail.setMeaning(String.format("Price (%.2f) above EMA (%.2f): recent momentum bullish.", price, ema));
            detail.setRecommendation("BUY");
        } else if (price < ema) {
            detail.setMeaning(String.format("Price (%.2f) below EMA (%.2f): recent momentum bearish.", price, ema));
            detail.setRecommendation("SELL");
        } else {
            detail.setMeaning("Price equals EMA: neutral short-term momentum.");
            detail.setRecommendation("HOLD");
        }
        return detail;
    }

    private StockAnalysisDTO.IndicatorDetail interpretMACD(JsonNode macdNode) {
        StockAnalysisDTO.IndicatorDetail detail = new StockAnalysisDTO.IndicatorDetail();
        detail.setValue(macdNode);
        // If macdNode contains macd, signal, histogram
        if (macdNode == null) {
            detail.setMeaning("MACD not available");
            detail.setRecommendation("HOLD");
            return detail;
        }

        double macd = macdNode.has("macd") ? macdNode.get("macd").asDouble() : macdNode.asDouble();
        double signal = macdNode.has("signal") ? macdNode.get("signal").asDouble() : 0.0;
        double hist = macdNode.has("histogram") ? macdNode.get("histogram").asDouble() : (macd - signal);

        String meaning = String.format("MACD = %.4f, Signal = %.4f, Histogram = %.4f. ", macd, signal, hist);

        if (macd > signal && hist > 0) {
            meaning += "MACD above signal and positive histogram indicates bullish momentum and potential continuation.";
            detail.setRecommendation("BUY");
        } else if (macd < signal && hist < 0) {
            meaning += "MACD below signal and negative histogram indicates bearish momentum and potential continuation.";
            detail.setRecommendation("SELL");
        } else {
            meaning += "Mixed MACD signals; treat with caution and confirm with other indicators.";
            detail.setRecommendation("HOLD");
        }
        detail.setMeaning(meaning);
        return detail;
    }

    /**
     * Combine indicators using a weighted strategy (institutional-like).
     * - Give higher weight to trend-following signals (SMA/EMA/MACD)
     * - Momentum indicators (RSI) weigh moderately
     * This is a simple example. You can tune weights or add more complex strategies.
     */
    private String combineStrategy(Map<String, StockAnalysisDTO.IndicatorDetail> indicators) {
        double score = 0.0;

        // weight settings (example)
        double wRSI = 1.0;
        double wSMA = 1.5;
        double wEMA = 1.2;
        double wMACD = 1.8;

        StockAnalysisDTO.IndicatorDetail rsi = indicators.get("rsi");
        StockAnalysisDTO.IndicatorDetail sma = indicators.get("sma_20");
        StockAnalysisDTO.IndicatorDetail ema = indicators.get("ema_14");
        StockAnalysisDTO.IndicatorDetail macd = indicators.get("macd");

        if (rsi != null) score += weightRecommendation(rsi.getRecommendation()) * wRSI;
        if (sma != null) score += weightRecommendation(sma.getRecommendation()) * wSMA;
        if (ema != null) score += weightRecommendation(ema.getRecommendation()) * wEMA;
        if (macd != null) score += weightRecommendation(macd.getRecommendation()) * wMACD;

        // Final decision by threshold
        if (score >= 2.0) return "Overall: BUY (weighted indicators bullish)";
        if (score <= -2.0) return "Overall: SELL (weighted indicators bearish)";
        return "Overall: HOLD (mixed or neutral signals)";
    }

    // Map textual recommendation into numeric weight
    private double weightRecommendation(String rec) {
        if (rec == null) return 0.0;
        return switch (rec.toUpperCase()) {
            case "BUY" -> 1.0;
            case "SELL" -> -1.0;
            default -> 0.0;
        };
    }
}
