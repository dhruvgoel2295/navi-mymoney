package org.mymoney.backend.service.impl;


import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import org.mymoney.backend.dao.PortfolioData;
import org.mymoney.backend.dto.Portfolio;
import org.mymoney.backend.model.Asset;
import org.mymoney.backend.model.Fund;
import org.mymoney.backend.service.PortfolioService;

import org.springframework.stereotype.Service;

import java.time.Month;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.DataFormatException;

import static org.mymoney.backend.constants.MyMoneyConstants.CANNOT_REBALANCE;

@Service
public class DefaultPortfolioService implements PortfolioService {
    private final PortfolioData portfolioData;

    static Logger log = Logger.getLogger(DefaultPortfolioService.class.getName());


    public DefaultPortfolioService(PortfolioData portfolioData) {
        this.portfolioData = portfolioData;
    }
    @Override
    public void allocate(List<Double> allocations) throws DataFormatException {
        if (Objects.nonNull(portfolioData.initialAllocation)) {
            throw new IllegalStateException("The funds are already Allocated once.");
        }
        portfolioData.initialAllocation = createMyMoneyFundsWithDefaultOrdering(allocations);
        portfolioData.desiredWeights = calculateDesiredWeight();
        log.info(
                String.format("Portfolio initialized with initial allocation of %s and desired weights: %s",
                portfolioData.initialAllocation,
                portfolioData.desiredWeights));
    }

    private Map<Asset, Double> calculateDesiredWeight() {
        if (Objects.isNull(portfolioData.initialAllocation)) {
            throw new IllegalStateException("The funds are not yet Allocated");
        }
        return portfolioData.initialAllocation.getFunds().stream()
                .collect(
                        Collectors.toMap(
                                Fund::getAsset,
                                e -> e.getAmount() * 100 / portfolioData.initialAllocation.getTotalInvestment()));
    }

    private Portfolio createMyMoneyFundsWithDefaultOrdering(List<Double> allocations)
            throws DataFormatException {
        return createMyMoneyFunds(portfolioData.defaultAssetOrderForIO, allocations);
    }

    private Portfolio createMyMoneyFunds(
            Set<Asset> assetOrderForIO, List<Double> allocations) throws DataFormatException {
        validateInputs(assetOrderForIO, allocations);
        List<Fund> fundEntityList =
                Streams.zip(assetOrderForIO.stream(), allocations.stream(), Fund::new)
                        .collect(Collectors.toList());
        return new Portfolio(fundEntityList);
    }


    private void validateInputs(Set<Asset> assetOrderForIO, List<Double> allocations)
            throws DataFormatException {
        if (Objects.isNull(allocations) || allocations.size() != assetOrderForIO.size()) {
            throw new DataFormatException("The input is not in the desired format");
        }
    }

    @Override
    public void sip(List<Double> sips) throws DataFormatException {
        // Since sip always starts from Feb, we disallow entering multiple sips
        if (Objects.nonNull(portfolioData.initialSip)) {
            throw new IllegalStateException("The SIP is already registered once");
        }
        portfolioData.initialSip = createMyMoneyFundsWithDefaultOrdering(sips);
        log.info(String.format("Portfolio initialized with a monthly SIP of %s ", portfolioData.initialSip));
    }

    @Override
    public void change(List<Double> rates, Month month)
            throws IllegalStateException, DataFormatException {
        if (Objects.nonNull(portfolioData.monthlyMarketChangeRate.getOrDefault(month, null))) {
            throw new IllegalStateException(
                    "The Rate of Change for month " + month.name() + " is already present. Malformed Input.");
        }
        if (Objects.isNull(rates) || Objects.isNull(month)) {
            throw new InputMismatchException("Parameter is null.");
        }
        if (rates.size() != portfolioData.defaultAssetOrderForIO.size()) {
            throw new DataFormatException("Malformed Input.");
        }
        Map<Asset, Double> change =
                Streams.zip(portfolioData.defaultAssetOrderForIO.stream(), rates.stream(), Maps::immutableEntry)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        portfolioData.monthlyMarketChangeRate.put(month, change);
    }


    @Override
    public String balance(Month month) {
        updateBalance();
        Portfolio fund =
                Optional.ofNullable(portfolioData.monthlyBalance.get(month))
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "The balance is requested for the month of "
                                                        + month.name()
                                                        + "without any data"));
        return fund.toString();
    }

    private void updateBalance() {
        Map.Entry<Month, Portfolio> lastCalculatedBalance =
                portfolioData.monthlyBalance.lastEntry();
        Map.Entry<Month, Map<Asset, Double>> lastKnownChange =
                portfolioData.monthlyMarketChangeRate.lastEntry();
        if (Objects.isNull(lastKnownChange)) {
            throw new IllegalStateException("Rate of Change is not defined");
        }
        if (Objects.isNull(lastCalculatedBalance)) {
            Portfolio myMoneyFund =
                    calculateBalance(
                            portfolioData.initialAllocation,
                            null,
                            portfolioData.monthlyMarketChangeRate.get(Month.JANUARY));
            portfolioData.monthlyBalance.put(Month.JANUARY, myMoneyFund);
            lastCalculatedBalance = portfolioData.monthlyBalance.lastEntry();
        }
        if (lastCalculatedBalance.getKey() != lastKnownChange.getKey()) {
            Month startMonth = lastCalculatedBalance.getKey();
            Month endMonth = lastKnownChange.getKey();
            for (int index = startMonth.getValue(); index < endMonth.getValue(); index++) {
                Month lastUpdatedMonth = Month.of(index);
                Month currentCalculationMonth = Month.of(index + 1);
                log.info(String.format("Calculating balance for %s", currentCalculationMonth));
                Portfolio carryOverBalance =
                        portfolioData.monthlyBalance.get(lastUpdatedMonth).clone();
                Map<Asset, Double> changeRate =
                        portfolioData.monthlyMarketChangeRate.get(currentCalculationMonth);
                Portfolio availableBalance =
                        calculateBalance(carryOverBalance, portfolioData.initialSip, changeRate);
                if (shouldReBalance(currentCalculationMonth)) {
                    availableBalance = doReBalance(availableBalance);
                }
                portfolioData.monthlyBalance.putIfAbsent(currentCalculationMonth, availableBalance);
            }
        }
    }

    private Portfolio calculateBalance(
            Portfolio carryOverBalance,
            Portfolio monthlySip,
            Map<Asset, Double> changeRate) {
        log.info(String.format(
                "Updating balance of %s with sip of %s and market change rate of %s",
                carryOverBalance,
                monthlySip,
                changeRate));
        Portfolio balAfterSip = applySipInvestment(carryOverBalance, monthlySip);
        return applyMarketChange(balAfterSip, changeRate);
    }

    private Portfolio applyMarketChange(
            Portfolio carryOverBalance, Map<Asset, Double> changeRate) {
        List<Fund> funds = carryOverBalance.getFunds();
        funds.forEach(
                entity -> {
                    double rate = changeRate.get(entity.getAsset());
                    double updatedAmount = entity.getAmount() * (1 + rate / 100);
                    entity.setAmount(Math.floor(updatedAmount));
                });
        return carryOverBalance;
    }


    private Portfolio applySipInvestment(
            Portfolio carryOverBalance, Portfolio initialSip) {
        List<Fund> funds = carryOverBalance.getFunds();
        if (Objects.nonNull(initialSip)) {
            IntStream.range(0, funds.size())
                    .forEach(
                            index -> {
                                Fund fundEntity = funds.get(index);
                                double sipAmount = initialSip.getFunds().get(index).getAmount();
                                fundEntity.setAmount(Math.floor(fundEntity.getAmount() + sipAmount));
                            });
        }
        return carryOverBalance;
    }

    @Override
    public String reBalance() {
        updateBalance();
        Month lastUpdatedMonth = portfolioData.monthlyBalance.lastEntry().getKey();
        Month lastRebalancedMonth = getLastReBalancedMonth(lastUpdatedMonth);
        Portfolio balance = portfolioData.monthlyBalance.getOrDefault(lastRebalancedMonth, null);
        return Objects.nonNull(balance) ? balance.toString() : CANNOT_REBALANCE;
    }

    private Month getLastReBalancedMonth(Month month) {
        return month == Month.DECEMBER ? month : Month.JUNE;
    }


    private boolean shouldReBalance(Month month) {
        // The re-balancing happens only on 6 and 12 months.
        return month.equals(Month.JUNE) || month.equals(Month.DECEMBER);
    }

    private Portfolio doReBalance(Portfolio currentFunds) {
        List<Fund> funds = currentFunds.getFunds();
        double totalInvestment = currentFunds.getTotalInvestment();
        funds.forEach(
                entity -> {
                    double desiredWeight = portfolioData.desiredWeights.get(entity.getAsset());
                    entity.setAmount(Math.floor(totalInvestment * desiredWeight / 100));
                });
        log.info(String.format(
                "Re-balanced the current total balance of %s to desired weights of %s to %s",
                currentFunds.getTotalInvestment(),
                portfolioData.desiredWeights,
                currentFunds));
        return currentFunds;
    }

    @Override
    public int getSupportedAssetClass() {
        return portfolioData.defaultAssetOrderForIO.size();
    }
}
