package com.example.beanlifecycledemo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ScopeInspector implements ApplicationRunner {

    private final ApplicationContext ctx;

    public ScopeInspector(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        OrderService a = ctx.getBean(OrderService.class);
        OrderService b = ctx.getBean(OrderService.class);
        Cart c1 = ctx.getBean(Cart.class);
        Cart c2 = ctx.getBean(Cart.class);

        System.out.println("========== scopes ==========");
        System.out.println("OrderService singleton same instance? " + (a == b));
        System.out.println("Cart prototype same instance?        " + (c1 == c2));
        System.out.println("================================");
    }
}
