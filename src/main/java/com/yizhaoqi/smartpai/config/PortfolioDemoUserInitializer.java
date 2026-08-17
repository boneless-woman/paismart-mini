package com.yizhaoqi.smartpai.config;

import com.yizhaoqi.smartpai.model.User;
import com.yizhaoqi.smartpai.repository.UserRepository;
import com.yizhaoqi.smartpai.utils.PasswordUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("portfolio-demo")
@Order(20)
public class PortfolioDemoUserInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PortfolioDemoProperties properties;

    public PortfolioDemoUserInitializer(UserRepository users, PortfolioDemoProperties properties) {
        this.users = users;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getPassword() == null || properties.getPassword().length() < 8) {
            throw new IllegalStateException("PORTFOLIO_DEMO_PASSWORD must contain at least 8 characters");
        }
        users.findByUsername(properties.getUsername()).orElseGet(() -> {
            User user = new User();
            user.setUsername(properties.getUsername());
            user.setPassword(PasswordUtil.encode(properties.getPassword()));
            user.setRole(User.Role.USER);
            user.setOrgTags("PUBLIC");
            user.setPrimaryOrg("PUBLIC");
            return users.save(user);
        });
    }
}
