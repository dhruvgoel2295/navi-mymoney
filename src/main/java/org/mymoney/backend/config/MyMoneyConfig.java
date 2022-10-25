package org.mymoney.backend.config;

import org.mymoney.backend.dao.PortfolioData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mymoney.backend.model.Asset.*;

@Configuration
public class MyMoneyConfig {

    @Bean
    public PortfolioData portfolioData() {
        PortfolioData data = new PortfolioData();
        data.defaultAssetOrderForIO.add(EQUITY);
        data.defaultAssetOrderForIO.add(DEBT);
        data.defaultAssetOrderForIO.add(GOLD);
        return data;
    }
}