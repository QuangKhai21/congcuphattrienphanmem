package com.db7.j2ee_quanlythucung;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class J2EeQuanLyThuCungApplication {

    public static void main(String[] args) {
        SpringApplication.run(J2EeQuanLyThuCungApplication.class, args);
    }

}
