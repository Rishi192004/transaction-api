package com.rishi.transactionapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class TransactionApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionApiApplication.class, args);
    }
}
