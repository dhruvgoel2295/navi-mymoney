package org.mymoney.backend.service;

import org.apache.logging.log4j.util.Strings;
import org.mymoney.backend.MyMoneyApplication;
import org.mymoney.backend.model.Command;
import org.mymoney.backend.model.Command.*;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Month;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;


@Service
public class PortfolioManager {
    private final PortfolioService portfolioService;
    static Logger log = Logger.getLogger(MyMoneyApplication.class.getName());

    public PortfolioManager(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }


    public List<String> executeCommandsFromFile(String filename) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            List<String> outputs =
                    lines
                            .filter(l -> Strings.isNotBlank(l))
                            .map(this::processLineAsCommand)
                            .collect(Collectors.toList());
            display(outputs);
            return outputs;

        } catch (IOException e) {
            log.severe("Invalid File. Please check the path & name for input file provided.");
            throw new IOException("Invalid File");
        }
    }

    public String processLineAsCommand(String line) {
        String output = null;
        int supportedAssetClass = portfolioService.getSupportedAssetClass();
        String[] commandAndInputs = line.trim().split(" ");
        try {
            Command command = Command.valueOf(commandAndInputs[0]);
            switch (command) {
                case ALLOCATE:
                    validateInputSize(commandAndInputs, supportedAssetClass);
                    List<Double> allocations = getDoubles(1, supportedAssetClass, commandAndInputs);
                    portfolioService.allocate(allocations);
                    break;
                case SIP:
                    validateInputSize(commandAndInputs, supportedAssetClass);
                    List<Double> sips = getDoubles(1, supportedAssetClass, commandAndInputs);
                    portfolioService.sip(sips);
                    break;
                case CHANGE:
                    validateInputSize(commandAndInputs, supportedAssetClass + 1);
                    List<Double> rates =
                            Arrays.stream(commandAndInputs)
                                    .skip(1)
                                    .limit(supportedAssetClass)
                                    .map(str -> Double.parseDouble(str.replace("%", "")))
                                    .collect(Collectors.toList());
                    Month month = Month.valueOf(commandAndInputs[supportedAssetClass + 1]);
                    portfolioService.change(rates, month);
                    break;
                case BALANCE:
                    validateInputSize(commandAndInputs, 1);
                    month = Month.valueOf(commandAndInputs[1]);
                    output = portfolioService.balance(month);
                    break;
                case REBALANCE:
                    output = portfolioService.reBalance();
                    break;
                default:
                    throw new DataFormatException("Invalid Command " + command + " supplied");
            }
        } catch (Exception e) {
            System.out.println(
                    "Error Occurred while processing " + String.join(" ", commandAndInputs) + e.getMessage());
        }
        return output;
    }

    private List<Double> getDoubles(int skip, int limit, String[] commandAndInputs) {
        return Arrays.stream(commandAndInputs)
                .skip(skip)
                .limit(limit)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }
    private void validateInputSize(String[] commandAndInputs, int size) {
        if (commandAndInputs.length != size + 1) {
            throw new InputMismatchException(
                    "Please check the command " + String.join(" ", commandAndInputs));
        }
    }

    private static void display(List<String> outputs) {
        outputs.stream().filter(Objects::nonNull).forEach(System.out::println);
    }
}
