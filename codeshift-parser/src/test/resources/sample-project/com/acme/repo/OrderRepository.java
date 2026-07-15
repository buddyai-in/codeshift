package com.acme.repo;

public class OrderRepository {
    public String load(long id) {
        return "order-" + id;
    }
}
