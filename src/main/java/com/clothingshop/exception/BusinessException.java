package com.clothingshop.exception;

/** A business operation that cannot be completed; the message is shown to the user as is (Vietnamese). */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
