package com.fintech.fintech_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FintechGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(FintechGatewayApplication.class, args);
	}

}
