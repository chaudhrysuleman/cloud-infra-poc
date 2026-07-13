package com.suleman.poc.adapters.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suleman.poc.domain.model.OrderPlacedEvent;
import com.suleman.poc.domain.ports.inbound.ManageOrdersUseCase;
import com.suleman.poc.domain.ports.outbound.InvoiceStoragePort;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class InvoiceConsumer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceConsumer.class);
    
    private final ManageOrdersUseCase manageOrdersUseCase;
    private final InvoiceStoragePort invoiceStoragePort;
    private final ObjectMapper objectMapper;

    @Autowired
    public InvoiceConsumer(
            ManageOrdersUseCase manageOrdersUseCase,
            InvoiceStoragePort invoiceStoragePort,
            ObjectMapper objectMapper) {
        this.manageOrdersUseCase = manageOrdersUseCase;
        this.invoiceStoragePort = invoiceStoragePort;
        this.objectMapper = objectMapper;
    }

    @SqsListener("invoice-queue")
    public void handleOrderPlaced(String message) {
        try {
            String payload = message;
            if (message.contains("\"Type\"") && message.contains("\"Message\"")) {
                var jsonNode = objectMapper.readTree(message);
                payload = jsonNode.get("Message").asText();
            }

            OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
            log.info("🧾 [InvoiceConsumer] Received OrderPlacedEvent: orderId={}", event.orderId());

            // Generate invoice text
            String invoiceContent = generateInvoice(event);

            // Upload to S3 via Outbound Port
            String s3Key = "invoices/order-" + event.orderId() + ".txt";
            invoiceStoragePort.uploadInvoice(s3Key, invoiceContent);

            // Update S3 key in database via Inbound Port Use Case
            manageOrdersUseCase.updateInvoiceS3Key(event.orderId(), s3Key);

            log.info("✅ [InvoiceConsumer] Invoice uploaded to S3 for order {}: key={}", event.orderId(), s3Key);

        } catch (Exception e) {
            log.error("❌ [InvoiceConsumer] Failed to process message: {}", e.getMessage(), e);
            throw new RuntimeException("Invoice processing failed", e);
        }
    }

    private String generateInvoice(OrderPlacedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("        TOO GOOD TO GO - INVOICE        \n");
        sb.append("========================================\n");
        sb.append("Order ID:    ").append(event.orderId()).append("\n");
        sb.append("Customer:    ").append(event.customerName()).append("\n");
        sb.append("Date:        ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Items:\n");
        for (int i = 0; i < event.items().size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(event.items().get(i)).append("\n");
        }
        sb.append("----------------------------------------\n");
        sb.append("Status:      PAID\n");
        sb.append("========================================\n");
        sb.append("Thank you for saving food! 🌱\n");
        return sb.toString();
    }
}
