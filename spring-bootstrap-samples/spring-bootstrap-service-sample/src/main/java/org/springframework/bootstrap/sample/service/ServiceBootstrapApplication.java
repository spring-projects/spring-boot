package org.springframework.bootstrap.sample.service;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.service.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(ServiceProperties.class)
@ComponentScan
public class ServiceBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ServiceBootstrapApplication.class, args);
	}

}
