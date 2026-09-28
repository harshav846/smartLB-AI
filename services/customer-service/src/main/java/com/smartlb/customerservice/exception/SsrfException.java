package com.smartlb.customerservice.exception;

public class SsrfException extends RuntimeException {
    public SsrfException(String message) {
        super(message);
    }
}
