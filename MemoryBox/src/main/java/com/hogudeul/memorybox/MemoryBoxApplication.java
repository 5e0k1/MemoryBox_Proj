package com.hogudeul.memorybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MemoryBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoryBoxApplication.class, args);
    }
}
