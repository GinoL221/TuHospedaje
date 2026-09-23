package com.tuhospedaje.exception;

public class LastEnabledAdminException extends RuntimeException {

    public static final String ERROR_CODE = "last_enabled_admin";

    public LastEnabledAdminException() {
        super("At least one enabled administrator must remain.");
    }
}
