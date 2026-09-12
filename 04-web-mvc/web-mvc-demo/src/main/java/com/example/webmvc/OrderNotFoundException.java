package com.example.webmvc;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(long id) {
        super("Order not found: " + id);
    }
}
