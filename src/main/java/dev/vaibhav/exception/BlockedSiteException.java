package dev.vaibhav.exception;

public class BlockedSiteException extends RuntimeException {
    public BlockedSiteException(String message) {
        super(message);
    }
}
