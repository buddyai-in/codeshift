package com.acme.rules;

public class PricingRule {
    public double price(long id) {
        return id % 2 == 0 ? 9.99 : 14.99;
    }
}
