package com.acme.service;

import com.acme.repo.OrderRepository;
import com.acme.rules.PricingRule;

public class OrderService {
    private final OrderRepository repo = new OrderRepository();
    private final PricingRule pricing = new PricingRule();

    public String findOrder(long id) {
        return repo.load(id) + " @ " + pricing.price(id);
    }
}
