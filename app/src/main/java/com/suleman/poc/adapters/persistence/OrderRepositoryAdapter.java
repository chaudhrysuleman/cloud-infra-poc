package com.suleman.poc.adapters.persistence;

import com.suleman.poc.domain.model.Order;
import com.suleman.poc.domain.ports.outbound.OrderRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository springDataRepository;

    @Autowired
    public OrderRepositoryAdapter(SpringDataOrderRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public List<Order> findAll() {
        return springDataRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Order> findById(Long id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    // --- Mappers ---
    private OrderEntity toEntity(Order domain) {
        return new OrderEntity(
                domain.getId(),
                domain.getCustomerName(),
                domain.getItems(),
                domain.getStatus(),
                domain.getInvoiceS3Key(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    private Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getCustomerName(),
                entity.getItems(),
                entity.getStatus(),
                entity.getInvoiceS3Key(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
