package com.acme.web;

import com.acme.service.OrderService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService service = new OrderService();

    public String getOrder(long id) {
        return service.findOrder(id);
    }
}
