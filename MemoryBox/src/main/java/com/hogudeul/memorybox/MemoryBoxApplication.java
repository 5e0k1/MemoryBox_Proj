package com.hogudeul.memorybox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.hogudeul.memorybox.mapper")
@SpringBootApplication
@EnableScheduling
public class MemoryBoxApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MemoryBoxApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MemoryBoxApplication.class);
    }
}
