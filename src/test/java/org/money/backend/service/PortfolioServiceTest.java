package org.money.backend.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mymoney.backend.dao.PortfolioData;
import org.mymoney.backend.service.impl.DefaultPortfolioService;

import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.zip.DataFormatException;

import static java.time.Month.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mymoney.backend.constants.MyMoneyConstants.CANNOT_REBALANCE;
import static org.mymoney.backend.model.Asset.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Spy private PortfolioData portfolioData;
    @InjectMocks private DefaultPortfolioService defaultPortfolioService;

    @BeforeEach
    public void setUp() {
        portfolioData.defaultAssetOrderForIO.add(EQUITY);
        portfolioData.defaultAssetOrderForIO.add(DEBT);
        portfolioData.defaultAssetOrderForIO.add(GOLD);
        defaultPortfolioService = new DefaultPortfolioService(portfolioData);
    }

    @Test
    void testAllocateNull() {
        assertThrows(
                DataFormatException.class,
                () -> defaultPortfolioService.allocate(null),
                "Expected Allocate method to throw Exception.");
    }

    @Test
    void testAllocateCorrectValues() throws DataFormatException {
        List<Double> initialAllocation = Arrays.asList(10d, 20d, 30d);
        defaultPortfolioService.allocate(initialAllocation);
        assertEquals(initialAllocation.size(), portfolioData.initialAllocation.getFunds().size());
        assertEquals(
                initialAllocation.stream().mapToDouble(Double::doubleValue).sum(),
                portfolioData.initialAllocation.getTotalInvestment());
        assertEquals(
                100, portfolioData.desiredWeights.values().stream().mapToDouble(Double::doubleValue).sum());
    }

    @Test
    void testAllocateInCorrectValues() {
        assertThrows(
                DataFormatException.class,
                () -> defaultPortfolioService.allocate(Arrays.asList(10d, 20d, 30d, 40d)),
                "Expected Allocate method to throw Exception.");
    }

    @Test
    void testAllocateAlreadyAllocated() throws DataFormatException {
        List<Double> initialAllocation = Arrays.asList(10d, 20d, 30d);
        defaultPortfolioService.allocate(initialAllocation);

        assertThrows(
                IllegalStateException.class,
                () -> defaultPortfolioService.allocate(initialAllocation),
                "Expected Allocate method to throw Exception.");
    }

    @Test
    void testSipWithNullValues() {
        assertThrows(
                DataFormatException.class,
                () -> defaultPortfolioService.sip(null),
                "Expected Sip method to throw Exception.");
    }

    @Test
    void testSipWithInCorrectValues() {
        assertThrows(
                DataFormatException.class,
                () -> defaultPortfolioService.sip(Arrays.asList(10d, 20d, 30d, 40d)),
                "Expected Sip method to throw Exception.");
    }

    @Test
    void testSipWithCorrectValues() throws DataFormatException {
        List<Double> sipAmounts = Arrays.asList(10d, 20d, 30d);
        defaultPortfolioService.sip(sipAmounts);
        assertEquals(sipAmounts.size(), portfolioData.initialSip.getFunds().size());
        assertEquals(
                sipAmounts.stream().mapToDouble(Double::doubleValue).sum(),
                portfolioData.initialSip.getTotalInvestment());
    }

    @Test
    void testSipAlreadyAllocated() throws DataFormatException {
        List<Double> sipAmounts = Arrays.asList(10d, 20d, 30d);
        defaultPortfolioService.sip(sipAmounts);
        assertThrows(
                IllegalStateException.class,
                () -> defaultPortfolioService.sip(sipAmounts),
                "Expected Sip method to throw Exception.");
    }

    @Test
    void testChangeWithNullValues() {
        assertThrows(
                InputMismatchException.class,
                () -> defaultPortfolioService.change(null, JANUARY),
                "Expected Change method to throw Exception.");
    }

    @Test
    void testChangeWithInCorrectValues() {
        assertThrows(
                DataFormatException.class,
                () -> defaultPortfolioService.change(Arrays.asList(10d, 20d, 30d, 40d), JANUARY),
                "Expected Change method to throw Exception.");
    }

    @Test
    void testChangeWithCorrectValues() throws DataFormatException {
        List<Double> changeRate = Arrays.asList(10d, 20d, 30d);
        defaultPortfolioService.change(changeRate, JANUARY);
        assertEquals(changeRate.size(), portfolioData.monthlyMarketChangeRate.get(JANUARY).size());
    }

    @Test
    void testChangeAlreadyAllocatedForMonth() throws DataFormatException {
        List<Double> changeRate = Arrays.asList(10d, 20d, 30d);
        defaultPortfolioService.change(changeRate, JANUARY);
        assertThrows(
                IllegalStateException.class,
                () -> defaultPortfolioService.change(changeRate, JANUARY),
                "Expected Change method to throw Exception, but it didn't.");
    }

    @Test
    void testBalanceInSufficientData() throws DataFormatException {
        defaultPortfolioService.allocate(Arrays.asList(6000d, 3000d, 1000d));
        defaultPortfolioService.sip(Arrays.asList(2000d, 1000d, 500d));
        assertThrows(
                IllegalStateException.class,
                () -> defaultPortfolioService.balance(JANUARY),
                "Expected Change method to throw Exception, but it didn't.");
    }

    @Test
    void testBalance() throws DataFormatException {
        initializePortfolio();
        assertEquals("10593 7897 2272", defaultPortfolioService.balance(MARCH));
    }

    private void initializePortfolio() throws DataFormatException {
        defaultPortfolioService.allocate(Arrays.asList(6000d, 3000d, 1000d));
        defaultPortfolioService.sip(Arrays.asList(2000d, 1000d, 500d));
        defaultPortfolioService.change(Arrays.asList(4d, 10d, 2d), JANUARY);
        defaultPortfolioService.change(Arrays.asList(-10.00d, 40.00d, 0.00d), FEBRUARY);
        defaultPortfolioService.change(Arrays.asList(12.50d, 12.50d, 12.50d), MARCH);
        defaultPortfolioService.change(Arrays.asList(8.00d, -3.00d, 7.00d), APRIL);
        defaultPortfolioService.change(Arrays.asList(13.00d, 21.00d, 10.50d), MAY);
        defaultPortfolioService.change(Arrays.asList(10.00d, 8.00d, -5.00d), JUNE);
    }

    @Test
    void testReBalance() throws DataFormatException {
        initializePortfolio();
        assertEquals("23619 11809 3936", defaultPortfolioService.reBalance());
    }

    @Test
    void testReBalanceWithInsufficientData() throws DataFormatException {
        defaultPortfolioService.allocate(Arrays.asList(6000d, 3000d, 1000d));
        defaultPortfolioService.sip(Arrays.asList(2000d, 1000d, 500d));
        defaultPortfolioService.change(Arrays.asList(4d, 10d, 2d), JANUARY);
        String result = defaultPortfolioService.reBalance();
        assertEquals(CANNOT_REBALANCE, result);
    }

    @Test
    void testGetSupportedAssetClass() {
        assertEquals(3, defaultPortfolioService.getSupportedAssetClass());
    }
}