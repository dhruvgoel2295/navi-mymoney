package org.mymoney.backend.dto;

import org.mymoney.backend.model.Fund;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.stream.Collectors;

public class Portfolio implements Cloneable {
    @NonNull private final List<Fund> funds;

    public Portfolio(List<Fund> funds) {
        this.funds = funds;
    }

    public List<Fund> getFunds() {
        return this.funds;
    }

    @Override
    public String toString() {
        return funds.stream()
                .map(entity -> Integer.toString((int) Math.floor(entity.getAmount())))
                .collect(Collectors.joining(" "));
    }

    @Override
    public Portfolio clone() {
        return new Portfolio(
                funds.stream()
                        .map(e -> new Fund(e.getAsset(), e.getAmount()))
                        .collect(Collectors.toList()));
    }

    public double getTotalInvestment() {
        return funds.stream().mapToDouble(Fund::getAmount).sum();
    }
}
