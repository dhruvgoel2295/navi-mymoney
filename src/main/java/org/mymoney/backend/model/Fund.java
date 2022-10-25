package org.mymoney.backend.model;

import org.springframework.lang.NonNull;

public class Fund {
    @NonNull private Asset asset;
    @NonNull private Double amount;

    public Fund(Asset asset, Double amount){
        this.asset = asset;
        this.amount = amount;
    }

    public Asset getAsset() {
        return this.asset;
    }

    public Double getAmount() {
        return this.amount;
    }

    public void setAsset(@NonNull Asset asset) {
        this.asset = asset;
    }

    public void setAmount(@NonNull Double amount) {
        this.amount = amount;
    }
}
