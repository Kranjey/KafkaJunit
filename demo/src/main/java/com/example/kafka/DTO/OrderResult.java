package com.example.kafka.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {
    private String orderId;
    private OrderStatus status;
    private String reason;
}
