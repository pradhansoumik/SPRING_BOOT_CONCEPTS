package com.example.configdemo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.feature.fake-payment", havingValue = "true")
public class FakePaymentMarker {
}
