package com.clothingshop.dto;

/**
 * Raw values of the customer registration form (FR-01), exactly as submitted; the service validates them.
 *
 * @param gender FEMALE, MALE or OTHER
 * @param dob    date of birth in ISO format (yyyy-MM-dd), as sent by an HTML date input
 */
public record RegistrationForm(String userName, String password, String confirmPassword, String email,
                               String fullName, String gender, String dob, String phone) {
}
