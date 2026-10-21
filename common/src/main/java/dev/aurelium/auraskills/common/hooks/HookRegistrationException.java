package dev.aurelium.auraskills.common.hooks;

public class HookRegistrationException extends RuntimeException {

    public HookRegistrationException(String message) {
        super(message);
    }

    public HookRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

}
