package com.ofppt.orders_api.repo.OrderRepo;


import com.ofppt.orders_api.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<Order, Long> {}