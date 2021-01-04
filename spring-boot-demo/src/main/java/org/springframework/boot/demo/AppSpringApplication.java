package org.springframework.boot.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootConfiguration
public class AppSpringApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(AppSpringApplication.class, args);
		System.out.println("Hello Spring Boot");
	}
}
