package com.clothingshop.dto;

/**
 * Values of the new staff account form (FR-40), exactly as submitted.
 *
 * @param dob date of birth in ISO format (yyyy-MM-dd)
 */
public record EmployeeForm(String userName, String email, String fullName, String gender, String dob, String phone,
                           String password, String confirmPassword) {
}
