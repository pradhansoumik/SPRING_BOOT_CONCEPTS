package com.example.webmvc;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public OrderResponse create(@Valid @RequestBody OrderRequest req) {
        return orders.create(req);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable long id) {
        return orders.get(id);
    }

    @GetMapping
    public String ping(@RequestParam(defaultValue = "OPEN") String status) {
        return "filter-by-status=" + status + " (demo query param)";
    }
}
