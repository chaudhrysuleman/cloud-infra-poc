package com.suleman.poc.adapters.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suleman.poc.domain.model.OrderPlacedEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SqsListener("notification-queue")
    public void handleOrderPlaced(String message) {
        try {
            // SQS messages forwarded via SNS are wrapped in an SNS Envelope JSON.
            // However, Spring Cloud AWS 3.x SQS integration can sometimes unwrap them,
            // or we can unwrap manually. To be completely robust, we will check if the
            // message is wrapped in an SNS JSON structure (which has a "Message" field).
            String payload = message;
            if (message.contains("\"Type\"") && message.contains("\"Message\"")) {
                var jsonNode = objectMapper.readTree(message);
                payload = jsonNode.get("Message").asText();
            }

            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            log.info("📱 [NotificationConsumer] Received OrderPlacedEvent: orderId={}", event.orderId());

            String notificationMessage = String.format(
                    "🎉 Hi %s! Your order #%d with %d item(s) has been placed. We're preparing your surprise bag! 🌱",
                    event.customerName(),
                    event.orderId(),
                    event.items().size()
            );

            log.info("📲 [NotificationConsumer] Push notification sent: {}", notificationMessage);

        } catch (Exception e) {
            log.error("❌ [NotificationConsumer] Failed to process message: {}", e.getMessage(), e);
            throw new RuntimeException("Notification processing failed", e);
        }
    }
}
