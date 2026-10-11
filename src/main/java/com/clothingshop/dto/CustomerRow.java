package com.clothingshop.dto;

import com.clothingshop.entity.CustomerLevel;
import java.time.LocalDateTime;

/** One customer in the staff lookup (FR-42 step 1). */
public record CustomerRow(int id, String userName, String fullName, String email, String phone, CustomerLevel level,
                          LocalDateTime createdAt) {
}
