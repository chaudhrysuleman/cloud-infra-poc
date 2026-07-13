package com.suleman.poc.domain.ports.inbound;

import com.suleman.poc.domain.model.Order;
import com.suleman.poc.domain.model.OrderStatus;
import java.util.List;

public interface ManageOrdersUseCase {
    Order createOrder(String customerName, List<String> items);
    List<Order> getAllOrders();
    Order getOrderById(Long id);
    Order updateOrderStatus(Long orderId, OrderStatus status);
    void updateInvoiceS3Key(Long orderId, String s3Key);
    String getInvoiceDownloadUrl(Long orderId);
}
