package com.hogudeul.memorybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = "com.hogudeul.memorybox")
public class MemoryBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoryBoxApplication.class, args);
    }
}
