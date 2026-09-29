package com.example.demo.exception;

public class SimulatedTransferFailureException extends RuntimeException {
    public SimulatedTransferFailureException(String message) {
        super(message);
    }
}