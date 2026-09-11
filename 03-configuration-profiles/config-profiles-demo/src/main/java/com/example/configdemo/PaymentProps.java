package com.example.configdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public class PaymentProps {

    private String url = "";
    private String timeout = "";

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTimeout() { return timeout; }
    public void setTimeout(String timeout) { this.timeout = timeout; }
}
