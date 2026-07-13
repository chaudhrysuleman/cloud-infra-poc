package com.suleman.poc.adapters.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suleman.poc.domain.model.OrderPlacedEvent;
import com.suleman.poc.domain.model.OrderStatus;
import com.suleman.poc.domain.ports.inbound.ManageOrdersUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryConsumer.class);
    private final ManageOrdersUseCase manageOrdersUseCase;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeliveryConsumer(ManageOrdersUseCase manageOrdersUseCase, ObjectMapper objectMapper) {
        this.manageOrdersUseCase = manageOrdersUseCase;
        this.objectMapper = objectMapper;
    }

    @SqsListener("delivery-queue")
    public void handleOrderPlaced(String message) {
        try {
            String payload = message;
            if (message.contains("\"Type\"") && message.contains("\"Message\"")) {
                var jsonNode = objectMapper.readTree(message);
                payload = jsonNode.get("Message").asText();
            }

            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            log.info("📦 [DeliveryConsumer] Received OrderPlacedEvent: orderId={}", event.orderId());

            // Simulate shipping label creation
            String shippingLabel = "SHIP-" + event.orderId() + "-" + System.currentTimeMillis();
            log.info("🏷️  [DeliveryConsumer] Generated shipping label: {}", shippingLabel);

            // Update status to PROCESSING
            manageOrdersUseCase.updateOrderStatus(event.orderId(), OrderStatus.PROCESSING);
            log.info("✅ [DeliveryConsumer] Order {} status updated to PROCESSING", event.orderId());

        } catch (Exception e) {
            log.error("❌ [DeliveryConsumer] Failed to process message: {}", e.getMessage(), e);
            throw new RuntimeException("Delivery processing failed", e);
        }
    }
}
