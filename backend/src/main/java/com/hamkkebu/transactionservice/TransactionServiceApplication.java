package com.hamkkebu.transactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.hamkkebu.transactionservice",
    "com.hamkkebu.common"
})
@EntityScan(basePackages = {
    "com.hamkkebu.transactionservice.data.entity",
    "com.hamkkebu.common.data.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.hamkkebu.transactionservice.repository",
    "com.hamkkebu.common.repository"
})
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
