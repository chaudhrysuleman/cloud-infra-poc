package com.suleman.poc.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record OrderPlacedEvent(
        Long orderId,
        String customerName,
        List<String> items,
        LocalDateTime placedAt
) {
    public static OrderPlacedEvent fromOrder(Order order) {
        return new OrderPlacedEvent(
                order.getId(),
                order.getCustomerName(),
                order.getItems(),
                order.getCreatedAt()
        );
    }
}
