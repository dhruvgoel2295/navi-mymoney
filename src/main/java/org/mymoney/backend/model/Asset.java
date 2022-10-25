package org.mymoney.backend.model;

public enum Asset {
    EQUITY(1, "Equity Asset"),
    DEBT(2, "Debt Asset"),
    GOLD(3, "Gold Asset");

    int value;
    String assetName;
    private Asset(int value, String assetName){
        this.value = value;
        this.assetName = assetName;
    }
    public int value() {
        return this.value;
    }

    public String getAssetName() {
        return this.assetName;
    }
}
