package com.clothingshop.dto;

/**
 * Values of the profile form (FR-04), exactly as submitted.
 *
 * @param dob     date of birth in ISO format (yyyy-MM-dd)
 * @param version version of the account when the form was opened, to detect a concurrent change (section 3.1)
 */
public record ProfileForm(String fullName, String gender, String dob, String phone, Long version) {
}
