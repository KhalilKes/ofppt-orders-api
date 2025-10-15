package com.ofppt.orders_api.controller;

import com.ofppt.orders_api.models.Order;
import com.ofppt.orders_api.repo.OrderRepo.OrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    @Autowired
    private OrderRepo repository;

    @PostMapping("/orders")
    public Order placeOrder(@RequestBody Order order) {
        return repository.save(order);
    }
}