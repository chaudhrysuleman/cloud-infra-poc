package com.suleman.poc.adapters.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suleman.poc.domain.model.OrderPlacedEvent;
import com.suleman.poc.domain.ports.outbound.OrderEventPublisherPort;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnsOrderEventPublisher implements OrderEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SnsOrderEventPublisher.class);

    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;
    private final String orderEventsTopicArn;

    @Autowired
    public SnsOrderEventPublisher(
            SnsTemplate snsTemplate,
            ObjectMapper objectMapper,
            @Value("${app.aws.sns.order-events-topic}") String orderEventsTopicArn) {
        this.snsTemplate = snsTemplate;
        this.objectMapper = objectMapper;
        this.orderEventsTopicArn = orderEventsTopicArn;
    }

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            snsTemplate.sendNotification(orderEventsTopicArn, messageBody, "OrderPlaced");
            log.info("📤 Published OrderPlacedEvent to SNS: orderId={}, topic={}", event.orderId(), orderEventsTopicArn);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize OrderPlacedEvent: orderId={}", event.orderId(), e);
            throw new RuntimeException("Failed to serialize and publish event", e);
        }
    }
}
