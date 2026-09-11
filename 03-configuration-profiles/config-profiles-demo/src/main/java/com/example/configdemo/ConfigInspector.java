package com.example.configdemo;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ConfigInspector implements ApplicationRunner {

    private final Environment env;
    private final PaymentProps props;
    private final ObjectProvider<FakePaymentMarker> fake;

    public ConfigInspector(Environment env, PaymentProps props,
                           ObjectProvider<FakePaymentMarker> fake) {
        this.env = env;
        this.props = props;
        this.fake = fake;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("========== config ==========");
        System.out.println("active profiles : " + String.join(",", env.getActiveProfiles()));
        System.out.println("@Value-style    : " + env.getProperty("app.payment.url"));
        System.out.println("@ConfigProps    : " + props.getUrl() + " timeout=" + props.getTimeout());
        System.out.println("FakePayment bean: " + (fake.getIfAvailable() != null));
        System.out.println("============================");
    }
}
