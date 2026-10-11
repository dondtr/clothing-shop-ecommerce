package com.clothingshop.exception;

import java.util.Objects;

/**
 * Optimistic locking (specification section 3.1): someone else saved the same data first, so this change is not applied
 * and the user is asked to reload.
 */
public class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Dữ liệu đã được người khác thay đổi, vui lòng tải lại";

    public ConflictException() {
        super(MESSAGE);
    }

    /** Refuses a change made from a form that showed version {@code submitted} when the data is now at {@code current}. */
    public static void check(Long current, Long submitted) {
        if (submitted == null || !Objects.equals(current, submitted)) {
            throw new ConflictException();
        }
    }
}
