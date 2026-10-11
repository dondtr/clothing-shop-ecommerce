package com.clothingshop.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Input that breaks a rule of the specification; carries one message per form field, in Vietnamese. */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Invalid input: " + errors.keySet());
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    /** Field name to message, in the order the fields appear on the form. */
    public Map<String, String> getErrors() {
        return errors;
    }
}
