package com.example.webmvc;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private final AtomicLong seq = new AtomicLong();
    private final Map<Long, OrderResponse> store = new ConcurrentHashMap<>();

    public OrderResponse create(OrderRequest req) {
        long id = seq.incrementAndGet();
        OrderResponse saved = new OrderResponse(id, req.item(), req.qty(), "OPEN");
        store.put(id, saved);
        return saved;
    }

    public OrderResponse get(long id) {
        OrderResponse found = store.get(id);
        if (found == null) {
            throw new OrderNotFoundException(id);
        }
        return found;
    }
}
