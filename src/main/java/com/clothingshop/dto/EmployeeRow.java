package com.clothingshop.dto;

import com.clothingshop.entity.EmployeeRole;
import java.time.LocalDate;

/** One employee account in the manager's list (FR-40, FR-41). */
public record EmployeeRow(int id, String userName, String fullName, String email, String phone, EmployeeRole role,
                          boolean active, LocalDate hireDate) {
}
