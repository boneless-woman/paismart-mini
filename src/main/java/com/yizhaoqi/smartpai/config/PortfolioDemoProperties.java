package com.yizhaoqi.smartpai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "portfolio.demo")
public class PortfolioDemoProperties {
    private boolean enabled;
    private String username = "demo_user";
    private String password;
    private int maxQuestionLength = 500;
    private int dailyRequests = 100;
}
