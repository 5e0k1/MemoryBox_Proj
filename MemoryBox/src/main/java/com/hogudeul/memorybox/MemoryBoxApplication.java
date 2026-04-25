package com.hogudeul.memorybox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@MapperScan("com.hogudeul.memorybox.mapper")
@SpringBootApplication
public class MemoryBoxApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MemoryBoxApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MemoryBoxApplication.class);
    }
}
