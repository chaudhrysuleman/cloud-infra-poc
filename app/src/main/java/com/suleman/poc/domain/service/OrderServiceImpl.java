package com.suleman.poc.domain.service;

import com.suleman.poc.domain.model.Order;
import com.suleman.poc.domain.model.OrderPlacedEvent;
import com.suleman.poc.domain.model.OrderStatus;
import com.suleman.poc.domain.ports.inbound.ManageOrdersUseCase;
import com.suleman.poc.domain.ports.outbound.InvoiceStoragePort;
import com.suleman.poc.domain.ports.outbound.OrderEventPublisherPort;
import com.suleman.poc.domain.ports.outbound.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderServiceImpl implements ManageOrdersUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final InvoiceStoragePort invoiceStoragePort;

    @Autowired
    public OrderServiceImpl(OrderRepositoryPort orderRepositoryPort,
                            OrderEventPublisherPort orderEventPublisherPort,
                            InvoiceStoragePort invoiceStoragePort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.orderEventPublisherPort = orderEventPublisherPort;
        this.invoiceStoragePort = invoiceStoragePort;
    }

    @Override
    public Order createOrder(String customerName, List<String> items) {
        log.info("🛒 Creating new order for customer: {}", customerName);
        Order order = new Order(customerName, items);
        Order savedOrder = orderRepositoryPort.save(order);

        // Publish Order Placed Event
        try {
            OrderPlacedEvent event = OrderPlacedEvent.fromOrder(savedOrder);
            orderEventPublisherPort.publishOrderPlaced(event);
            log.info("📢 Published OrderPlacedEvent for order ID: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("❌ Failed to publish OrderPlacedEvent for order ID: {}. Error: {}", savedOrder.getId(), e.getMessage());
            // In a production app, we would use Outbox Pattern to ensure eventual consistency
        }

        return savedOrder;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepositoryPort.findAll();
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
    }

    @Override
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("🔄 Updating order status: id={}, status={}", orderId, status);
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepositoryPort.save(order);
    }

    @Override
    public void updateInvoiceS3Key(Long orderId, String s3Key) {
        log.info("📝 Attaching invoice key to order: id={}, key={}", orderId, s3Key);
        Order order = getOrderById(orderId);
        order.setInvoiceS3Key(s3Key);
        orderRepositoryPort.save(order);
    }

    @Override
    public String getInvoiceDownloadUrl(Long orderId) {
        Order order = getOrderById(orderId);
        if (order.getInvoiceS3Key() == null) {
            throw new RuntimeException("Invoice not generated yet for order ID: " + orderId);
        }
        return invoiceStoragePort.getPresignedDownloadUrl(order.getInvoiceS3Key());
    }
}
