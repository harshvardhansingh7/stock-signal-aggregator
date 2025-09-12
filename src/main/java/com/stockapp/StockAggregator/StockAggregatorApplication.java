package com.stockapp.StockAggregator;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockAggregatorApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		System.setProperty("OPENROUTER_API_KEY", dotenv.get("OPENROUTER_API_KEY"));
		SpringApplication.run(StockAggregatorApplication.class, args);
	}

}
