package com.example.beanlifecycledemo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class LifecycleBean {

    public LifecycleBean() {
        System.out.println("1. ctor          " + this);
    }

    @PostConstruct
    void init() {
        System.out.println("2. @PostConstruct " + this);
    }

    @PreDestroy
    void stop() {
        System.out.println("3. @PreDestroy    " + this);
    }
}
