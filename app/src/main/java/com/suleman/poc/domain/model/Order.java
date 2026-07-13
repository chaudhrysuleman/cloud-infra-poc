package com.suleman.poc.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private Long id;
    private String customerName;
    private List<String> items;
    private OrderStatus status;
    private String invoiceS3Key;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {}

    public Order(Long id, String customerName, List<String> items, OrderStatus status, String invoiceS3Key, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerName = customerName;
        this.items = items;
        this.status = status;
        this.invoiceS3Key = invoiceS3Key;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Order(String customerName, List<String> items) {
        this.customerName = customerName;
        this.items = items;
        this.status = OrderStatus.PLACED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getInvoiceS3Key() {
        return invoiceS3Key;
    }

    public void setInvoiceS3Key(String invoiceS3Key) {
        this.invoiceS3Key = invoiceS3Key;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
