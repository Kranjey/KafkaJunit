package com.example.kafka.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.kafka.DTO.OrderEvent;
import com.example.kafka.DTO.OrderResult;
import com.example.kafka.DTO.OrderStatus;

@Service
public class OrderProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);
    private final KafkaTemplate<String, OrderResult> kafkaTemplate;

    public OrderProcessor(KafkaTemplate<String, OrderResult> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "orders.created", groupId = "order-process-group")
    public void processOrder(OrderEvent event) {
        log.info("Received order: {}", event.getOrderId());
        
        OrderResult result;
        if (event.getAmount() > 0) {
            result = new OrderResult(event.getOrderId(), OrderStatus.ACCEPTED, "Validation passed");
            log.info("Order {} accepted", event.getOrderId());
        } else {
            result = new OrderResult(event.getOrderId(), OrderStatus.REJECTED, "Amount must be positive");
            log.warn("Order {} rejected due to invalid amount", event.getOrderId());
        }

        kafkaTemplate.send("orders.processed", event.getOrderId(), result);
    }
}