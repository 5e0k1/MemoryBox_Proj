package com.hogudeul.memorybox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.hogudeul.memorybox.mapper")
@SpringBootApplication
public class MemoryBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoryBoxApplication.class, args);
    }
}
