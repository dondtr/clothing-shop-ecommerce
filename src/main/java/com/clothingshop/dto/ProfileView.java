package com.clothingshop.dto;

import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.Gender;
import java.time.LocalDate;

/**
 * The signed-in user's own information (FR-04). User name and email are shown but cannot be changed (Q35, Q50).
 *
 * @param level   membership level for a customer, {@code null} for an employee
 * @param role    "Khách hàng", "Nhân viên" or "Quản lý"
 * @param version current version of the account, sent back with the form
 */
public record ProfileView(int id, String userName, String email, String fullName, Gender gender, LocalDate dob,
                          String phone, String role, CustomerLevel level, long version) {
}
