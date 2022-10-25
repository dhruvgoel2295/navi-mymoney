package org.mymoney.backend.dao;

import org.mymoney.backend.dto.Portfolio;
import org.mymoney.backend.model.Asset;
import org.springframework.context.annotation.Scope;

import java.time.Month;
import java.util.*;

@Scope("singleton")
public class PortfolioData {
    public TreeMap<Month, Portfolio> monthlyBalance = new TreeMap<>();
    public TreeMap<Month, Map<Asset, Double>> monthlyMarketChangeRate = new TreeMap<>();
    public Portfolio initialAllocation;
    public Portfolio initialSip;
    public Map<Asset, Double> desiredWeights = new HashMap<>();
    public Set<Asset> defaultAssetOrderForIO = new LinkedHashSet<>();


    public TreeMap<Month, Portfolio> getMonthlyBalance() {
        return monthlyBalance;
    }

    public TreeMap<Month, Map<Asset, Double>> getMonthlyMarketChangeRate() {
        return monthlyMarketChangeRate;
    }

    public Map<Asset, Double> getDesiredWeights() {
        return desiredWeights;
    }

    public Portfolio getInitialAllocation() {
        return initialAllocation;
    }

    public Set<Asset> getDefaultAssetOrderForIO() {
        return defaultAssetOrderForIO;
    }

    public Portfolio getInitialSip() {
        return initialSip;
    }
}
