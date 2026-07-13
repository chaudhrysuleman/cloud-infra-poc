package com.suleman.poc.adapters.web;

import com.suleman.poc.domain.model.Order;
import com.suleman.poc.domain.model.OrderStatus;
import com.suleman.poc.domain.ports.inbound.ManageOrdersUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    private final ManageOrdersUseCase manageOrdersUseCase;

    @Autowired
    public OrderRestController(ManageOrdersUseCase manageOrdersUseCase) {
        this.manageOrdersUseCase = manageOrdersUseCase;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = manageOrdersUseCase.createOrder(request.customerName(), request.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        try {
            Order order = manageOrdersUseCase.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<?> getInvoiceUrl(@PathVariable Long id) {
        try {
            String presignedUrl = manageOrdersUseCase.getInvoiceDownloadUrl(id);
            return ResponseEntity.ok(Map.of(
                    "orderId", id,
                    "invoiceUrl", presignedUrl,
                    "expiresIn", "15 minutes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> listOrders() {
        return ResponseEntity.ok(manageOrdersUseCase.getAllOrders());
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<Order> shipOrder(@PathVariable Long id) {
        Order updated = manageOrdersUseCase.updateOrderStatus(id, OrderStatus.SHIPPED);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<Order> deliverOrder(@PathVariable Long id) {
        Order updated = manageOrdersUseCase.updateOrderStatus(id, OrderStatus.DELIVERED);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        List<Order> orders = manageOrdersUseCase.getAllOrders();
        long total = orders.size();
        long placed = orders.stream().filter(o -> o.getStatus() == OrderStatus.PLACED).count();
        long processing = orders.stream().filter(o -> o.getStatus() == OrderStatus.PROCESSING).count();
        long shipped = orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count();
        long delivered = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();

        return ResponseEntity.ok(Map.of(
                "total", total,
                "placed", placed,
                "processing", processing,
                "shipped", shipped,
                "delivered", delivered
        ));
    }

    // --- Health Check for AWS Deploy ---
    @GetMapping("/healthcheck")
    public ResponseEntity<Map<String, Object>> healthcheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "parcels-order-system");
        try {
            long total = manageOrdersUseCase.getAllOrders().size();
            status.put("database", "CONNECTED");
            status.put("ordersCount", total);
        } catch (Exception e) {
            status.put("database", "DOWN");
            status.put("error", e.getMessage());
        }
        return ResponseEntity.ok(status);
    }

    public record CreateOrderRequest(String customerName, List<String> items) {}
}
