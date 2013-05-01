package org.springframework.bootstrap.sample.consumer;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(ServiceProperties.class)
@ComponentScan
@ImportResource("integration-context.xml")
public class IntegrationBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(IntegrationBootstrapApplication.class, args);
	}

}
