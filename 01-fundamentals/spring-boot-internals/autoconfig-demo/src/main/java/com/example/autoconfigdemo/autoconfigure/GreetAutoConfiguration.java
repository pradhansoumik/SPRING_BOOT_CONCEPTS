package com.example.autoconfigdemo.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Not under the main class package → component scan will not see this.
 * Boot loads it only because it is listed in AutoConfiguration.imports.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "demo.greet", name = "enabled", havingValue = "true")
public class GreetAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Greeter greeter() {
        return new Greeter("hello from auto-config");
    }
}
