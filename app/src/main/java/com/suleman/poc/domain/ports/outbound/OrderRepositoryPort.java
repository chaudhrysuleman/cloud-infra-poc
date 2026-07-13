package com.suleman.poc.domain.ports.outbound;

import com.suleman.poc.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    List<Order> findAll();
    Optional<Order> findById(Long id);
}
