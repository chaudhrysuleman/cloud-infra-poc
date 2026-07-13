package com.suleman.poc.domain.ports.outbound;

import com.suleman.poc.domain.model.OrderPlacedEvent;

public interface OrderEventPublisherPort {
    void publishOrderPlaced(OrderPlacedEvent event);
}
