package com.polsl.engineering.project.rms;

import com.polsl.engineering.project.rms.security.jwt.JwtProperties;
import com.polsl.engineering.project.rms.security.jwt.RefreshTokenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, RefreshTokenProperties.class})
public class RmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
    }

}
