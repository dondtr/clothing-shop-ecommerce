package com.clothingshop.dto;

import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One order in a list (customer history, staff order list, customer lookup). */
public record OrderRow(int id, String customerName, LocalDateTime orderDate, BigDecimal totalAmount, OrderStatus status,
                       PaymentMethod method, PaymentStatus paymentStatus) {
}
