package org.money.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mymoney.backend.dao.PortfolioData;
import org.mymoney.backend.service.PortfolioManager;
import org.mymoney.backend.service.impl.DefaultPortfolioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mymoney.backend.model.Asset.*;


class PortfolioManagerTest {
    @Mock private PortfolioData portfolioData;
    @Mock private DefaultPortfolioService defaultPortfolioService;
    @Spy private PortfolioManager portfolioManager;

    @BeforeEach
    public void setUp() {
        portfolioData = new PortfolioData();
        portfolioData.defaultAssetOrderForIO.add(EQUITY);
        portfolioData.defaultAssetOrderForIO.add(DEBT);
        portfolioData.defaultAssetOrderForIO.add(GOLD);
        defaultPortfolioService = new DefaultPortfolioService(portfolioData);
        portfolioManager = new PortfolioManager(defaultPortfolioService);
    }

    @Test
    void testExecuteCommandsFromFileWithInvalidFile() {
        assertThrows(
                IOException.class,
                () -> portfolioManager.executeCommandsFromFile("inputFile"),
                "Expected Allocate method to throw Exception, but it didn't.");
    }

    @Test
    void testExecuteCommandsFromFileWithValidFile() throws IOException {
        String inputFile =
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("testInputFile"))
                        .getFile();
        String outputFile =
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("testOutputFile"))
                        .getFile();
        List<String> output = portfolioManager.executeCommandsFromFile(inputFile);
        try (Stream<String> lines = Files.lines(Paths.get(outputFile))) {
            String expectedResult = lines.map(String::trim).collect(Collectors.joining(";"));
            String result =
                    output.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .collect(Collectors.joining(";"));
            assertEquals(expectedResult, result);
        }
    }
}

